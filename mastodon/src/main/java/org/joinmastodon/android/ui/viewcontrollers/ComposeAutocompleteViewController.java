package org.joinmastodon.android.ui.viewcontrollers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ComposeAutocompleteViewController{
	private Activity activity;
	private String accountID;
	private FrameLayout contentView;
	private UsableRecyclerView list;
	private ListImageLoaderWrapper imgLoader;
	private ProgressBar progress;
	private List<WrappedAccount> users=Collections.emptyList();
	private List<Hashtag> hashtags=Collections.emptyList();
	private List<WrappedEmoji> emojis=Collections.emptyList();
	private Mode mode;
	private APIRequest currentRequest;
	private Runnable usersDebouncer=this::doSearchUsers, hashtagsDebouncer=this::doSearchHashtags;
	private String lastText;
	private boolean listIsHidden=true;

	private UsersAdapter usersAdapter;
	private HashtagsAdapter hashtagsAdapter;
	private EmojisAdapter emojisAdapter;

	private Consumer<String> completionSelectedListener;

	public ComposeAutocompleteViewController(Activity activity, String accountID){
		this.activity=activity;
		this.accountID=accountID;
		contentView=new FrameLayout(activity);

		list=new UsableRecyclerView(activity);
		list.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
		list.setItemAnimator(new BetterItemAnimator());
		list.setVisibility(View.GONE);
		list.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(12));
		list.setClipToPadding(false);
		list.setSelector(null);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(parent.getChildAdapterPosition(view)<parent.getAdapter().getItemCount()-1)
					outRect.right=V.dp(8);
			}
		});
		contentView.addView(list, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		progress=new ProgressBar(activity);
		FrameLayout.LayoutParams progressLP=new FrameLayout.LayoutParams(V.dp(48), V.dp(48), Gravity.CENTER_HORIZONTAL|Gravity.TOP);
		progressLP.topMargin=V.dp(16);
		contentView.addView(progress, progressLP);

		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);
	}

	public void setText(String text){
		if(mode==Mode.USERS){
			list.removeCallbacks(usersDebouncer);
		}else if(mode==Mode.HASHTAGS){
			list.removeCallbacks(hashtagsDebouncer);
		}
		if(text==null)
			return;
		Mode prevMode=mode;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
		mode=switch(text.charAt(0)){
			case '@' -> Mode.USERS;
			case '#' -> Mode.HASHTAGS;
			case ':' -> Mode.EMOJIS;
			default -> throw new IllegalStateException("Unexpected value: "+text.charAt(0));
		};
		if(prevMode!=mode){
			list.setAdapter(switch(mode){
				case USERS -> {
					if(usersAdapter==null)
						usersAdapter=new UsersAdapter();
					yield usersAdapter;
				}
				case EMOJIS -> {
					if(emojisAdapter==null)
						emojisAdapter=new EmojisAdapter();
					yield emojisAdapter;
				}
				case HASHTAGS -> {
					if(hashtagsAdapter==null)
						hashtagsAdapter=new HashtagsAdapter();
					yield hashtagsAdapter;
				}
			});
			if(mode!=Mode.EMOJIS){
				list.setVisibility(View.GONE);
				progress.setVisibility(View.VISIBLE);
				listIsHidden=true;
			}else if(listIsHidden){
				list.setVisibility(View.VISIBLE);
				progress.setVisibility(View.GONE);
				listIsHidden=false;
			}
		}
		lastText=text;
		if(mode==Mode.USERS){
			list.postDelayed(usersDebouncer, 300);
		}else if(mode==Mode.HASHTAGS){
			list.postDelayed(hashtagsDebouncer, 300);
		}else if(mode==Mode.EMOJIS){
			String _text=text.substring(1); // remove ':'
			List<WrappedEmoji> oldList=emojis;
			List<Emoji> allEmojis = AccountSessionManager.getInstance()
					.getCustomEmojis(AccountSessionManager.getInstance().getAccount(accountID).domain)
					.stream()
					.flatMap(ec->ec.emojis.stream())
					.filter(e->e.visibleInPicker)
					.collect(Collectors.toList());
			List<Emoji> startsWithSearch = allEmojis.stream().filter(e -> e.shortcode.toLowerCase().startsWith(_text.toLowerCase())).collect(Collectors.toList());
			emojis=Stream.concat(startsWithSearch.stream(), allEmojis.stream()
					.filter(e -> !startsWithSearch.contains(e))
					.filter(e -> e.shortcode.toLowerCase().contains(_text.toLowerCase())))
					.map(WrappedEmoji::new)
					.collect(Collectors.toList());
			UiUtils.updateList(oldList, emojis, list, emojisAdapter, (e1, e2)->e1.emoji.shortcode.equals(e2.emoji.shortcode));
			imgLoader.updateImages();
		}
	}

	public void setCompletionSelectedListener(Consumer<String> completionSelectedListener){
		this.completionSelectedListener=completionSelectedListener;
	}

	public View getView(){
		return contentView;
	}

	private void doSearchUsers(){
		currentRequest=new GetSearchResults(lastText, GetSearchResults.Type.ACCOUNTS, false)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(SearchResults result){
						currentRequest=null;
						List<WrappedAccount> oldList=users;
						users=result.accounts.stream().map(WrappedAccount::new).collect(Collectors.toList());
						UiUtils.updateList(oldList, users, list, usersAdapter, (a1, a2)->a1.account.id.equals(a2.account.id));
						imgLoader.updateImages();
						if(listIsHidden){
							listIsHidden=false;
							V.setVisibilityAnimated(list, View.VISIBLE);
							V.setVisibilityAnimated(progress, View.GONE);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
					}
				})
				.exec(accountID);
	}

	private void doSearchHashtags(){
		currentRequest=new GetSearchResults(lastText, GetSearchResults.Type.HASHTAGS, false)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(SearchResults result){
						currentRequest=null;
						List<Hashtag> oldList=hashtags;
						hashtags=result.hashtags;
						UiUtils.updateList(oldList, hashtags, list, hashtagsAdapter, (t1, t2)->t1.name.equals(t2.name));
						imgLoader.updateImages();
						if(listIsHidden){
							listIsHidden=false;
							V.setVisibilityAnimated(list, View.VISIBLE);
							V.setVisibilityAnimated(progress, View.GONE);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
					}
				})
				.exec(accountID);
	}

	private class UsersAdapter extends UsableRecyclerView.Adapter<UserViewHolder> implements ImageLoaderRecyclerAdapter{
		public UsersAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new UserViewHolder();
		}

		@Override
		public int getItemCount(){
			return users.size();
		}

		@Override
		public void onBindViewHolder(UserViewHolder holder, int position){
			holder.bind(users.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return 1/*+users.get(position).emojiHelper.getImageCount()*/;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			WrappedAccount a=users.get(position);
			if(image==0)
				return a.avaRequest;
			return a.emojiHelper.getImageRequest(image-1);
		}
	}

	private class UserViewHolder extends BindableViewHolder<WrappedAccount> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final ImageView ava;
		private final TextView username;

		private UserViewHolder(){
			super(activity, R.layout.item_autocomplete_user, list);
			ava=findViewById(R.id.photo);
			username=findViewById(R.id.username);
			ava.setOutlineProvider(OutlineProviders.OVAL);
			ava.setClipToOutline(true);
		}

		@Override
		public void onBind(WrappedAccount item){
			username.setText("@"+item.account.acct);
		}

		@Override
		public void onClick(){
			completionSelectedListener.accept("@"+item.account.acct);
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				ava.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
			}
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}

	private class HashtagsAdapter extends RecyclerView.Adapter<HashtagViewHolder>{

		@NonNull
		@Override
		public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new HashtagViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position){
			holder.bind(hashtags.get(position));
		}

		@Override
		public int getItemCount(){
			return hashtags.size();
		}
	}

	private class HashtagViewHolder extends BindableViewHolder<Hashtag> implements UsableRecyclerView.Clickable{
		private final TextView text;

		private HashtagViewHolder(){
			super(activity, R.layout.item_autocomplete_hashtag, list);
			text=(TextView) itemView;
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(Hashtag item){
			text.setText("#"+item.name);
		}

		@Override
		public void onClick(){
			completionSelectedListener.accept("#"+item.name);
		}
	}

	private class EmojisAdapter extends UsableRecyclerView.Adapter<EmojiViewHolder> implements ImageLoaderRecyclerAdapter{
		public EmojisAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new EmojiViewHolder();
		}

		@Override
		public int getItemCount(){
			return emojis.size();
		}

		@Override
		public void onBindViewHolder(EmojiViewHolder holder, int position){
			holder.bind(emojis.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return 1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return emojis.get(position).request;
		}
	}

	private class EmojiViewHolder extends BindableViewHolder<WrappedEmoji> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final ImageView ava;
		private final TextView name;

		private EmojiViewHolder(){
			super(activity, R.layout.item_autocomplete_user, list);
			ava=findViewById(R.id.photo);
			name=findViewById(R.id.username);
		}

		@Override
		public void setImage(int index, Drawable image){
			ava.setImageDrawable(image);
		}

		@Override
		public void clearImage(int index){
			ava.setImageDrawable(null);
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(WrappedEmoji item){
			name.setText(":"+item.emoji.shortcode+":");
		}

		@Override
		public void onClick(){
			completionSelectedListener.accept(":"+item.emoji.shortcode+":");
		}
	}

	private static class WrappedAccount{
		private Account account;
		private CharSequence parsedName;
		private CustomEmojiHelper emojiHelper;
		private ImageLoaderRequest avaRequest;

		public WrappedAccount(Account account){
			this.account=account;
			parsedName=HtmlParser.parseCustomEmoji(account.displayName, account.emojis);
			emojiHelper=new CustomEmojiHelper();
			emojiHelper.setText(parsedName);
			avaRequest=new UrlImageLoaderRequest(account.avatar, V.dp(50), V.dp(50));
		}
	}

	private static class WrappedEmoji{
		private Emoji emoji;
		private ImageLoaderRequest request;

		public WrappedEmoji(Emoji emoji){
			this.emoji=emoji;
			request=new UrlImageLoaderRequest(emoji.url, V.dp(44), V.dp(44));
		}
	}

	private enum Mode{
		USERS,
		HASHTAGS,
		EMOJIS
	}
}
