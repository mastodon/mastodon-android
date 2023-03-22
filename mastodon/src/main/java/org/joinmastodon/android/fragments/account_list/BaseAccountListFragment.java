package org.joinmastodon.android.fragments.account_list;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toolbar;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseAccountListFragment extends MastodonRecyclerFragment<BaseAccountListFragment.AccountItem>{
	protected HashMap<String, Relationship> relationships=new HashMap<>();
	protected String accountID;
	protected ArrayList<APIRequest<?>> relationshipsRequests=new ArrayList<>();

	public BaseAccountListFragment(){
		super(40);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void onDataLoaded(List<AccountItem> d, boolean more){
		if(refreshing){
			relationships.clear();
		}
		loadRelationships(d);
		super.onDataLoaded(d, more);
	}

	@Override
	public void onRefresh(){
		for(APIRequest<?> req:relationshipsRequests){
			req.cancel();
		}
		relationshipsRequests.clear();
		super.onRefresh();
	}

	protected void loadRelationships(List<AccountItem> accounts){
		Set<String> ids=accounts.stream().map(ai->ai.account.id).collect(Collectors.toSet());
		GetAccountRelationships req=new GetAccountRelationships(ids);
		relationshipsRequests.add(req);
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						relationshipsRequests.remove(req);
						for(Relationship rel:result){
							relationships.put(rel.id, rel);
						}
						if(list==null)
							return;
						for(int i=0;i<list.getChildCount();i++){
							if(list.getChildViewHolder(list.getChildAt(i)) instanceof AccountViewHolder avh){
								avh.bindRelationship();
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipsRequests.remove(req);
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
//		list.setPadding(0, V.dp(16), 0, V.dp(16));
		list.setClipToPadding(false);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 72, 16));
		updateToolbar();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@CallSuper
	protected void updateToolbar(){
		Toolbar toolbar=getToolbar();
		if(toolbar!=null && toolbar.getNavigationIcon()!=null){
			toolbar.setNavigationContentDescription(R.string.back);
			if(hasSubtitle()){
				toolbar.setTitleTextAppearance(getActivity(), R.style.m3_title_medium);
				toolbar.setSubtitleTextAppearance(getActivity(), R.style.m3_body_medium);
				int color=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary);
				toolbar.setTitleTextColor(color);
				toolbar.setSubtitleTextColor(color);
			}
		}
	}

	protected boolean hasSubtitle(){
		return true;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(16)+insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}else{
			list.setPadding(0, V.dp(16), 0, V.dp(16));
		}
		super.onApplyWindowInsets(insets);
	}

	protected class AccountsAdapter extends UsableRecyclerView.Adapter<AccountViewHolder> implements ImageLoaderRecyclerAdapter{
		public AccountsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AccountViewHolder();
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).emojiHelper.getImageCount()+1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			AccountItem item=data.get(position);
			return image==0 ? item.avaRequest : item.emojiHelper.getImageRequest(image-1);
		}
	}

	protected class AccountViewHolder extends BindableViewHolder<AccountItem> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable, UsableRecyclerView.LongClickable{
		private final TextView name, username;
		private final ImageView avatar;
		private final Button button;
		private final PopupMenu contextMenu;
		private final View menuAnchor;

		public AccountViewHolder(){
			super(getActivity(), R.layout.item_account_list, list);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			avatar=findViewById(R.id.avatar);
			button=findViewById(R.id.button);
			menuAnchor=findViewById(R.id.menu_anchor);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(12));
			avatar.setClipToOutline(true);

			button.setOnClickListener(this::onButtonClick);

			contextMenu=new PopupMenu(getActivity(), menuAnchor);
			contextMenu.inflate(R.menu.profile);
			contextMenu.setOnMenuItemClickListener(this::onContextMenuItemSelected);
		}

		@Override
		public void onBind(AccountItem item){
			name.setText(item.parsedName);
			username.setText("@"+item.account.acct);
			bindRelationship();
		}

		public void bindRelationship(){
			Relationship rel=relationships.get(item.account.id);
			if(rel==null || AccountSessionManager.getInstance().isSelf(accountID, item.account)){
				button.setVisibility(View.GONE);
			}else{
				button.setVisibility(View.VISIBLE);
				UiUtils.setRelationshipToActionButton(rel, button);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				avatar.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				name.invalidate();
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

		@Override
		public boolean onLongClick(){
			return false;
		}

		@Override
		public boolean onLongClick(float x, float y){
			Relationship relationship=relationships.get(item.account.id);
			if(relationship==null)
				return false;
			Menu menu=contextMenu.getMenu();
			Account account=item.account;

			menu.findItem(R.id.share).setTitle(getString(R.string.share_user, account.getDisplayUsername()));
			menu.findItem(R.id.mute).setTitle(getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getDisplayUsername()));
			menu.findItem(R.id.block).setTitle(getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getDisplayUsername()));
			menu.findItem(R.id.report).setTitle(getString(R.string.report_user, account.getDisplayUsername()));
			MenuItem hideBoosts=menu.findItem(R.id.hide_boosts);
			if(relationship.following){
				hideBoosts.setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user, account.getDisplayUsername()));
				hideBoosts.setVisible(true);
			}else{
				hideBoosts.setVisible(false);
			}
			MenuItem blockDomain=menu.findItem(R.id.block_domain);
			if(!account.isLocal()){
				blockDomain.setTitle(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
				blockDomain.setVisible(true);
			}else{
				blockDomain.setVisible(false);
			}

			menuAnchor.setTranslationX(x);
			menuAnchor.setTranslationY(y);
			contextMenu.show();

			return true;
		}

		private void onButtonClick(View v){
			ProgressDialog progress=new ProgressDialog(getActivity());
			progress.setMessage(getString(R.string.loading));
			progress.setCancelable(false);
			UiUtils.performAccountAction(getActivity(), item.account, accountID, relationships.get(item.account.id), button, progressShown->{
				itemView.setHasTransientState(progressShown);
				if(progressShown)
					progress.show();
				else
					progress.dismiss();
			}, result->{
				relationships.put(item.account.id, result);
				bindRelationship();
			});
		}

		private boolean onContextMenuItemSelected(MenuItem item){
			Relationship relationship=relationships.get(this.item.account.id);
			if(relationship==null)
				return false;
			Account account=this.item.account;

			int id=item.getItemId();
			if(id==R.id.share){
				Intent intent=new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, account.url);
				startActivity(Intent.createChooser(intent, item.getTitle()));
			}else if(id==R.id.mute){
				UiUtils.confirmToggleMuteUser(getActivity(), accountID, account, relationship.muting, this::updateRelationship);
			}else if(id==R.id.block){
				UiUtils.confirmToggleBlockUser(getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
			}else if(id==R.id.report){
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("reportAccount", Parcels.wrap(account));
				Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
			}else if(id==R.id.open_in_browser){
				UiUtils.launchWebBrowser(getActivity(), account.url);
			}else if(id==R.id.block_domain){
				UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account.getDomain(), relationship.domainBlocking, ()->{
					relationship.domainBlocking=!relationship.domainBlocking;
					bindRelationship();
				});
			}else if(id==R.id.hide_boosts){
				new SetAccountFollowed(account.id, true, !relationship.showingReblogs)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Relationship result){
								relationships.put(AccountViewHolder.this.item.account.id, result);
								bindRelationship();
							}

							@Override
							public void onError(ErrorResponse error){
								error.showToast(getActivity());
							}
						})
						.wrapProgress(getActivity(), R.string.loading, false)
						.exec(accountID);
			}
			return true;
		}

		private void updateRelationship(Relationship r){
			relationships.put(item.account.id, r);
			bindRelationship();
		}
	}

	protected static class AccountItem{
		public final Account account;
		public final ImageLoaderRequest avaRequest;
		public final CustomEmojiHelper emojiHelper;
		public final CharSequence parsedName;

		public AccountItem(Account account){
			this.account=account;
			avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(50), V.dp(50));
			emojiHelper=new CustomEmojiHelper();
			emojiHelper.setText(parsedName=HtmlParser.parseCustomEmoji(account.displayName, account.emojis));
		}
	}
}
