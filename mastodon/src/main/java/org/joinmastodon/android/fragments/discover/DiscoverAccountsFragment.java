package org.joinmastodon.android.fragments.discover;

import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetFollowSuggestions;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FollowSuggestion;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class DiscoverAccountsFragment extends BaseRecyclerFragment<DiscoverAccountsFragment.AccountWrapper> implements ScrollableToTop{
	private String accountID;
	private Map<String, Relationship> relationships=Collections.emptyMap();
	private GetAccountRelationships relationshipsRequest;

	public DiscoverAccountsFragment(){
		super(20);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(relationshipsRequest!=null){
			relationshipsRequest.cancel();
			relationshipsRequest=null;
		}
		currentRequest=new GetFollowSuggestions(count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FollowSuggestion> result){
						onDataLoaded(result.stream().map(fs->new AccountWrapper(fs.account)).collect(Collectors.toList()), false);
						loadRelationships();
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new AccountsAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				outRect.bottom=outRect.left=outRect.right=V.dp(16);
				if(parent.getChildAdapterPosition(view)==0)
					outRect.top=V.dp(16);
			}
		});
		((UsableRecyclerView)list).setDrawSelectorOnTop(true);
	}

	private void loadRelationships(){
		relationships=Collections.emptyMap();
		relationshipsRequest=new GetAccountRelationships(data.stream().map(fs->fs.account.id).collect(Collectors.toList()));
		relationshipsRequest.setCallback(new Callback<>(){
			@Override
			public void onSuccess(List<Relationship> result){
				relationshipsRequest=null;
				relationships=result.stream().collect(Collectors.toMap(rel->rel.id, Function.identity()));
				if(list==null)
					return;
				for(int i=0;i<list.getChildCount();i++){
					RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
					if(holder instanceof AccountViewHolder avh)
						avh.rebind();
				}
			}

			@Override
			public void onError(ErrorResponse error){
				relationshipsRequest=null;
			}
		}).exec(accountID);
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		if(relationshipsRequest!=null){
			relationshipsRequest.cancel();
			relationshipsRequest=null;
		}
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	private class AccountsAdapter extends UsableRecyclerView.Adapter<AccountViewHolder> implements ImageLoaderRecyclerAdapter{

		public AccountsAdapter(){
			super(imgLoader);
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AccountViewHolder();
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public int getImageCountForItem(int position){
			return 2+data.get(position).emojiHelper.getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			AccountWrapper item=data.get(position);
			if(image==0)
				return item.avaRequest;
			else if(image==1)
				return item.coverRequest;
			else
				return item.emojiHelper.getImageRequest(image-2);
		}
	}

	private class AccountViewHolder extends BindableViewHolder<AccountWrapper> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final ImageView cover, avatar;
		private final TextView name, username, bio, followersCount, followingCount, postsCount, followersLabel, followingLabel, postsLabel;
		private final ProgressBarButton actionButton;
		private final ProgressBar actionProgress;
		private final View actionWrap;

		private Relationship relationship;

		public AccountViewHolder(){
			super(getActivity(), R.layout.item_discover_account, list);
			cover=findViewById(R.id.cover);
			avatar=findViewById(R.id.avatar);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			bio=findViewById(R.id.bio);
			followersCount=findViewById(R.id.followers_count);
			followersLabel=findViewById(R.id.followers_label);
			followingCount=findViewById(R.id.following_count);
			followingLabel=findViewById(R.id.following_label);
			postsCount=findViewById(R.id.posts_count);
			postsLabel=findViewById(R.id.posts_label);
			actionButton=findViewById(R.id.action_btn);
			actionProgress=findViewById(R.id.action_progress);
			actionWrap=findViewById(R.id.action_btn_wrap);

			itemView.setOutlineProvider(OutlineProviders.roundedRect(6));
			itemView.setClipToOutline(true);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(12));
			avatar.setClipToOutline(true);
			cover.setOutlineProvider(OutlineProviders.roundedRect(3));
			cover.setClipToOutline(true);
			actionButton.setOnClickListener(this::onActionButtonClick);
		}

		@Override
		public void onBind(AccountWrapper item){
			name.setText(item.parsedName);
			username.setText('@'+item.account.acct);
			bio.setText(item.parsedBio);
			followersCount.setText(UiUtils.abbreviateNumber(item.account.followersCount));
			followingCount.setText(UiUtils.abbreviateNumber(item.account.followingCount));
			postsCount.setText(UiUtils.abbreviateNumber(item.account.statusesCount));
			followersLabel.setText(getResources().getQuantityString(R.plurals.followers, Math.min(999, item.account.followersCount)));
			followingLabel.setText(getResources().getQuantityString(R.plurals.following, Math.min(999, item.account.followingCount)));
			postsLabel.setText(getResources().getQuantityString(R.plurals.posts, Math.min(999, item.account.statusesCount)));
			relationship=relationships.get(item.account.id);
			if(relationship==null){
				actionWrap.setVisibility(View.GONE);
			}else{
				actionWrap.setVisibility(View.VISIBLE);
				UiUtils.setRelationshipToActionButton(relationship, actionButton);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				avatar.setImageDrawable(image);
			}else if(index==1){
				cover.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-2, image);
				name.invalidate();
				bio.invalidate();
			}
			if(image instanceof Animatable a && !a.isRunning())
				a.start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		@Override
		public void onClick(){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}

		private void onActionButtonClick(View v){
			itemView.setHasTransientState(true);
			UiUtils.performAccountAction(getActivity(), item.account, accountID, relationship, actionButton, this::setActionProgressVisible, rel->{
				itemView.setHasTransientState(false);
				relationships.put(item.account.id, rel);
				rebind();
			});
		}

		private void setActionProgressVisible(boolean visible){
			actionButton.setTextVisible(!visible);
			actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
			actionButton.setClickable(!visible);
		}
	}

	protected class AccountWrapper{
		public Account account;
		public ImageLoaderRequest avaRequest, coverRequest;
		public CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
		public CharSequence parsedName, parsedBio;

		public AccountWrapper(Account account){
			this.account=account;
			if(!TextUtils.isEmpty(account.avatar))
				avaRequest=new UrlImageLoaderRequest(account.avatar, V.dp(50), V.dp(50));
			if(!TextUtils.isEmpty(account.header))
				coverRequest=new UrlImageLoaderRequest(account.header, 1000, 1000);
			parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
			if(account.emojis.isEmpty()){
				parsedName=account.displayName;
			}else{
				parsedName=HtmlParser.parseCustomEmoji(account.displayName, account.emojis);
				emojiHelper.setText(new SpannableStringBuilder(parsedName).append(parsedBio));
			}
		}
	}
}
