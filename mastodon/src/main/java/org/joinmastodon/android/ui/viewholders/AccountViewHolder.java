package org.joinmastodon.android.ui.viewholders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountEndorsed;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.AddAccountToListsFragment;
import org.joinmastodon.android.fragments.ManageFollowedHashtagsFragment;
import org.joinmastodon.android.fragments.SavedPostsTimelineFragment;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.fragments.profile.ProfileQrCodeFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.fragments.settings.SettingsAccountFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableRelativeLayout;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.parceler.Parcels;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import androidx.annotation.LayoutRes;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class AccountViewHolder extends BindableViewHolder<AccountViewModel> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable, UsableRecyclerView.LongClickable{
	protected final TextView name, username, followers, verifiedLink, bio;
	public final ImageView avatar;
	protected final ProgressBarButton button;
	protected final PopupMenu contextMenu;
	protected final View menuAnchor;
	protected final TypefaceSpan mediumSpan=new TypefaceSpan("sans-serif-medium");
	protected final CheckableRelativeLayout view;
	protected final View checkbox;
	protected final ProgressBar actionProgress;
	protected final ImageButton menuButton;

	private final String accountID;
	private final Fragment fragment;
	private final Map<String, Relationship> relationships;

	private Consumer<AccountViewHolder> onClick;
	private Predicate<AccountViewHolder> onLongClick;
	private Consumer<MenuItem> onCustomMenuItemSelected;
	private AccessoryType accessoryType;
	private boolean showBio;
	private boolean checked;

	public AccountViewHolder(Fragment fragment, ViewGroup list, Map<String, Relationship> relationships){
		this(fragment, list, relationships, R.layout.item_account_list);
	}

	public AccountViewHolder(Fragment fragment, ViewGroup list, Map<String, Relationship> relationships, @LayoutRes int layout){
		super(fragment.getActivity(), layout, list);
		this.fragment=fragment;
		this.accountID=Objects.requireNonNull(fragment.getArguments().getString("account"));
		this.relationships=relationships;

		view=(CheckableRelativeLayout) itemView;
		name=findViewById(R.id.name);
		username=findViewById(R.id.username);
		avatar=findViewById(R.id.avatar);
		button=findViewById(R.id.button);
		menuAnchor=findViewById(R.id.menu_anchor);
		followers=findViewById(R.id.followers_count);
		verifiedLink=findViewById(R.id.verified_link);
		bio=findViewById(R.id.bio);
		checkbox=findViewById(R.id.checkbox);
		actionProgress=findViewById(R.id.action_progress);
		menuButton=findViewById(R.id.options_btn);

		avatar.setOutlineProvider(OutlineProviders.roundedRect(10));
		avatar.setClipToOutline(true);

		button.setOnClickListener(this::onButtonClick);

		contextMenu=new PopupMenu(fragment.getActivity(), menuAnchor);
		contextMenu.inflate(R.menu.profile);
		contextMenu.setOnMenuItemClickListener(this::onContextMenuItemSelected);
		if(menuButton!=null)
			menuButton.setOnClickListener(v->showMenuFromButton());

		setStyle(AccessoryType.BUTTON, false);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBind(AccountViewModel item){
		name.setText(item.parsedName);
		username.setText("@"+item.account.acct);
		bindFollowerCount();
		bindVerifiedLink();
		bindRelationship();
		if(showBio){
			if(TextUtils.isEmpty(item.parsedBio)){
				bio.setVisibility(View.GONE);
			}else{
				bio.setVisibility(View.VISIBLE);
				bio.setText(item.parsedBio);
			}
		}
	}

	protected void bindFollowerCount(){
		if(followers!=null){
			String followersStr=fragment.getResources().getQuantityString(R.plurals.x_followers, item.account.followersCount>1000 ? 999 : (int)item.account.followersCount);
			String followersNum=UiUtils.abbreviateNumber(item.account.followersCount);
			int index=followersStr.indexOf("%,d");
			followersStr=followersStr.replace("%,d", followersNum);
			SpannableStringBuilder followersFormatted=new SpannableStringBuilder(followersStr);
			if(index!=-1){
				followersFormatted.setSpan(mediumSpan, index, index+followersNum.length(), 0);
			}
			followers.setText(followersFormatted);
		}
	}

	protected void bindVerifiedLink(){
		if(verifiedLink!=null){
			boolean hasVerifiedLink=item.verifiedLink!=null;
			if(!hasVerifiedLink)
				verifiedLink.setText(R.string.no_verified_link);
			else
				verifiedLink.setText(item.verifiedLink);
			verifiedLink.setCompoundDrawablesRelativeWithIntrinsicBounds(hasVerifiedLink ? R.drawable.ic_check_small_16px : R.drawable.ic_help_16px, 0, 0, 0);
			int tintColor=UiUtils.getThemeColor(fragment.getActivity(), hasVerifiedLink ? R.attr.colorM3Primary : R.attr.colorM3Secondary);
			verifiedLink.setTextColor(tintColor);
			verifiedLink.setCompoundDrawableTintList(ColorStateList.valueOf(tintColor));
		}
	}

	public void bindRelationship(){
		if(relationships==null || accessoryType!=AccessoryType.BUTTON)
			return;
		Relationship rel=relationships.get(item.account.id);
		if(rel==null || AccountSessionManager.getInstance().isSelf(accountID, item.account)){
			button.setVisibility(View.GONE);
		}else{
			button.setVisibility(View.VISIBLE);
			UiUtils.setRelationshipToActionButton(rel, item.account, button, true);
		}
	}

	@Override
	public void setImage(int index, Drawable image){
		if(index==0){
			avatar.setImageDrawable(image);
		}else{
			item.emojiHelper.setImageDrawable(index-1, image);
			name.invalidate();
			bio.invalidate();
		}

		if(image instanceof Animatable a && !a.isRunning())
			a.start();
	}

	@Override
	public void clearImage(int index){
		if(index==0){
			avatar.setImageResource(R.drawable.image_placeholder);
		}else{
			setImage(index, null);
		}
	}

	@Override
	public void onClick(){
		if(onClick!=null){
			onClick.accept(this);
			return;
		}
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(item.account));
		Nav.go(fragment.getActivity(), ProfileFragment.class, args);
	}

	@Override
	public boolean onLongClick(){
		return false;
	}

	@Override
	public boolean onLongClick(float x, float y){
		if(onLongClick!=null && onLongClick.test(this))
			return true;
		if(accessoryType==AccessoryType.MENU || !prepareMenu())
			return false;
		menuAnchor.setTranslationX(x);
		menuAnchor.setTranslationY(y);
		contextMenu.show();
		return true;
	}

	protected void onButtonClick(View v){
		if(relationships==null)
			return;
		itemView.setHasTransientState(true);
		UiUtils.performAccountAction((Activity) v.getContext(), item.account, accountID, relationships.get(item.account.id), button, this::setActionProgressVisible, rel->{
			itemView.setHasTransientState(false);
			relationships.put(item.account.id, rel);
			bindRelationship();
		});
	}

	public void setActionProgressVisible(boolean visible){
		if(visible)
			actionProgress.setIndeterminateTintList(button.getTextColors());
		button.setTextVisible(!visible);
		actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		button.setClickable(!visible);
	}

	private boolean onContextMenuItemSelected(MenuItem item){
		Relationship relationship=relationships.get(this.item.account.id);
		if(relationship==null)
			return false;
		Account account=this.item.account;

		int id=item.getItemId();
		if(id==R.id.share){
			UiUtils.openSystemShareSheet(fragment.getActivity(), account);
		}else if(id==R.id.mute){
			UiUtils.confirmToggleMuteUser(fragment.getActivity(), accountID, account, relationship.muting, this::updateRelationship);
		}else if(id==R.id.block){
			UiUtils.confirmToggleBlockUser(fragment.getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			args.putParcelable("relationship", Parcels.wrap(relationship));
			Nav.go(fragment.getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(fragment.getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(fragment.getActivity(), accountID, account, relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship(relationship);
			}, this::updateRelationship);
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs, relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							if(fragment.getActivity()!=null)
								error.showToast(fragment.getActivity());
						}
					})
					.wrapProgress(fragment.getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.add_to_list){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			Nav.go(fragment.getActivity(), AddAccountToListsFragment.class, args);
		}else if(id==R.id.copy_link){
			fragment.getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, account.url));
			UiUtils.maybeShowTextCopiedToast(fragment.getActivity());
		}else if(id==R.id.qr_code){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			ProfileQrCodeFragment qf=new ProfileQrCodeFragment();
			qf.setArguments(args);
			qf.show(fragment.getChildFragmentManager(), "qrDialog");
		}else if(id==R.id.favorites){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("isFavorites", true);
			Nav.go(fragment.getActivity(), SavedPostsTimelineFragment.class, args);
		}else if(id==R.id.bookmarks){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("isFavorites", false);
			Nav.go(fragment.getActivity(), SavedPostsTimelineFragment.class, args);
		}else if(id==R.id.followed_hashtags){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(fragment.getActivity(), ManageFollowedHashtagsFragment.class, args);
		}else if(id==R.id.account_settings){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(fragment.getActivity(), SettingsAccountFragment.class, args);
		}else if(id==R.id.remove_follower){
			UiUtils.confirmAndRemoveFollower(fragment.getActivity(), accountID, account, this::updateRelationship);
		}else if(id==R.id.personal_note){
			UiUtils.editAccountPersonalNote(fragment.getActivity(), accountID, account, relationship, this::updateRelationship);
		}else if(id==R.id.feature){
			new SetAccountEndorsed(account.id, !relationship.endorsed)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							if(fragment.getActivity()!=null)
								error.showToast(fragment.getActivity());
						}
					})
					.wrapProgress(fragment.getActivity(), R.string.loading, true)
					.exec(accountID);
		}
		return true;
	}

	private void updateRelationship(Relationship r){
		relationships.put(item.account.id, r);
		bindRelationship();
	}

	public void setOnClickListener(Consumer<AccountViewHolder> listener){
		onClick=listener;
	}

	public void setOnLongClickListener(Predicate<AccountViewHolder> onLongClick){
		this.onLongClick=onLongClick;
	}

	public void setOnCustomMenuItemSelectedListener(Consumer<MenuItem> onCustomMenuItemSelected){
		this.onCustomMenuItemSelected=onCustomMenuItemSelected;
	}

	public void setStyle(AccessoryType accessoryType, boolean showBio){
		if(accessoryType!=this.accessoryType){
			this.accessoryType=accessoryType;
			switch(accessoryType){
				case NONE -> {
					button.setVisibility(View.GONE);
					if(checkbox!=null)
						checkbox.setVisibility(View.GONE);
					if(menuButton!=null)
						menuButton.setVisibility(View.GONE);
				}
				case CHECKBOX -> {
					if(checkbox==null)
						throw new UnsupportedOperationException();
					button.setVisibility(View.GONE);
					checkbox.setVisibility(View.VISIBLE);
					menuButton.setVisibility(View.GONE);
					checkbox.setBackground(new CheckBox(checkbox.getContext()).getButtonDrawable());
				}
				case RADIOBUTTON -> {
					if(checkbox==null)
						throw new UnsupportedOperationException();
					button.setVisibility(View.GONE);
					checkbox.setVisibility(View.VISIBLE);
					menuButton.setVisibility(View.GONE);
					checkbox.setBackground(new RadioButton(checkbox.getContext()).getButtonDrawable());
				}
				case BUTTON, CUSTOM_BUTTON -> {
					button.setVisibility(View.VISIBLE);
					if(checkbox!=null)
						checkbox.setVisibility(View.GONE);
					if(menuButton!=null)
						menuButton.setVisibility(View.GONE);
				}
				case MENU -> {
					if(menuButton==null)
						throw new UnsupportedOperationException();
					button.setVisibility(View.GONE);
					if(checkbox!=null)
						checkbox.setVisibility(View.GONE);
					menuButton.setVisibility(View.VISIBLE);
				}
			}
			view.setCheckable(accessoryType==AccessoryType.CHECKBOX || accessoryType==AccessoryType.RADIOBUTTON);
		}
		this.showBio=showBio;
		bio.setVisibility(showBio ? View.VISIBLE : View.GONE);
	}

	private boolean prepareMenu(){
		if(relationships==null || AccountSessionManager.getInstance().isSelf(accountID, item.account))
			return false;
		Relationship relationship=relationships.get(item.account.id);
		if(relationship==null)
			return false;
		Menu menu=contextMenu.getMenu();
		Account account=item.account;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()){
			menu.setGroupDividerEnabled(true);
		}

		if(relationship.followedBy)
			menu.findItem(R.id.remove_follower).setTitle(UiUtils.makeRedString(itemView.getContext(), itemView.getContext().getString(R.string.remove_follower)));
		else
			menu.findItem(R.id.remove_follower).setVisible(false);
		menu.findItem(R.id.mute).setTitle(itemView.getContext().getString(relationship.muting ? R.string.unmute_account : R.string.mute_account, account.getDisplayUsername()));
		menu.findItem(R.id.block).setTitle(UiUtils.makeRedString(itemView.getContext(), relationship.blocking ? R.string.unblock_account : R.string.block_account));
		menu.findItem(R.id.report).setTitle(UiUtils.makeRedString(itemView.getContext(), R.string.report_account));
		if(relationship.following){
			MenuItem hideBoosts=menu.findItem(R.id.hide_boosts);
			hideBoosts.setVisible(true);
			hideBoosts.setTitle(itemView.getContext().getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user));
			MenuItem feature=menu.findItem(R.id.feature);
			feature.setVisible(true);
			feature.setTitle(itemView.getContext().getString(relationship.endorsed ? R.string.unfeature_user : R.string.feature_user));
		}else{
			menu.findItem(R.id.hide_boosts).setVisible(false);
			menu.findItem(R.id.feature).setVisible(false);
		}
		if(!account.isLocal())
			menu.findItem(R.id.block_domain).setTitle(UiUtils.makeRedString(itemView.getContext(), relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
		else
			menu.findItem(R.id.block_domain).setVisible(false);
		menu.findItem(R.id.add_to_list).setVisible(relationship.following);
		menu.findItem(R.id.personal_note).setTitle(TextUtils.isEmpty(relationship.note) ? R.string.add_user_personal_note : R.string.edit_user_personal_note);
		return true;
	}

	private void showMenuFromButton(){
		if(!prepareMenu())
			return;
		int[] xy={0, 0};
		itemView.getLocationInWindow(xy);
		int x=xy[0], y=xy[1];
		menuButton.getLocationInWindow(xy);
		menuAnchor.setTranslationX(xy[0]-x+menuButton.getWidth()/2f);
		menuAnchor.setTranslationY(xy[1]-y+menuButton.getHeight());
		contextMenu.show();
	}

	public void setChecked(boolean checked){
		this.checked=checked;
		view.setChecked(checked);
	}

	public PopupMenu getContextMenu(){
		return contextMenu;
	}

	public ProgressBarButton getButton(){
		return button;
	}

	public enum AccessoryType{
		NONE,
		BUTTON,
		CHECKBOX,
		RADIOBUTTON,
		MENU,
		CUSTOM_BUTTON
	}
}
