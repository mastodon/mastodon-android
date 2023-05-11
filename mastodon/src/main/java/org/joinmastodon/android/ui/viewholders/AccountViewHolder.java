package org.joinmastodon.android.ui.viewholders;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.TypefaceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.HashMap;
import java.util.Objects;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class AccountViewHolder extends BindableViewHolder<AccountViewModel> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable, UsableRecyclerView.LongClickable{
	private final TextView name, username, followers, verifiedLink;
	private final ImageView avatar;
	private final Button button;
	private final PopupMenu contextMenu;
	private final View menuAnchor;
	private final TypefaceSpan mediumSpan=new TypefaceSpan("sans-serif-medium");

	private final String accountID;
	private final Fragment fragment;
	private final HashMap<String, Relationship> relationships;

	public AccountViewHolder(Fragment fragment, ViewGroup list, HashMap<String, Relationship> relationships){
		super(fragment.getActivity(), R.layout.item_account_list, list);
		this.fragment=fragment;
		this.accountID=Objects.requireNonNull(fragment.getArguments().getString("account"));
		this.relationships=relationships;

		name=findViewById(R.id.name);
		username=findViewById(R.id.username);
		avatar=findViewById(R.id.avatar);
		button=findViewById(R.id.button);
		menuAnchor=findViewById(R.id.menu_anchor);
		followers=findViewById(R.id.followers_count);
		verifiedLink=findViewById(R.id.verified_link);

		avatar.setOutlineProvider(OutlineProviders.roundedRect(16));
		avatar.setClipToOutline(true);

		button.setOnClickListener(this::onButtonClick);

		contextMenu=new PopupMenu(fragment.getActivity(), menuAnchor);
		contextMenu.inflate(R.menu.profile);
		contextMenu.setOnMenuItemClickListener(this::onContextMenuItemSelected);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBind(AccountViewModel item){
		name.setText(item.parsedName);
		username.setText("@"+item.account.acct);
		String followersStr=fragment.getResources().getQuantityString(R.plurals.x_followers, item.account.followersCount>1000 ? 999 : (int)item.account.followersCount);
		String followersNum=UiUtils.abbreviateNumber(item.account.followersCount);
		int index=followersStr.indexOf("%,d");
		followersStr=followersStr.replace("%,d", followersNum);
		SpannableStringBuilder followersFormatted=new SpannableStringBuilder(followersStr);
		if(index!=-1){
			followersFormatted.setSpan(mediumSpan, index, index+followersNum.length(), 0);
		}
		followers.setText(followersFormatted);
		boolean hasVerifiedLink=item.verifiedLink!=null;
		if(!hasVerifiedLink)
			verifiedLink.setText(R.string.no_verified_link);
		else
			verifiedLink.setText(item.verifiedLink);
		verifiedLink.setCompoundDrawablesRelativeWithIntrinsicBounds(hasVerifiedLink ? R.drawable.ic_check_small_16px : R.drawable.ic_help_16px, 0, 0, 0);
		int tintColor=UiUtils.getThemeColor(fragment.getActivity(), hasVerifiedLink ? R.attr.colorM3Primary : R.attr.colorM3Secondary);
		verifiedLink.setTextColor(tintColor);
		verifiedLink.setCompoundDrawableTintList(ColorStateList.valueOf(tintColor));
		bindRelationship();
	}

	public void bindRelationship(){
		Relationship rel=relationships.get(item.account.id);
		if(rel==null || AccountSessionManager.getInstance().isSelf(accountID, item.account)){
			button.setVisibility(View.GONE);
		}else{
			button.setVisibility(View.VISIBLE);
			UiUtils.setRelationshipToActionButtonM3(rel, button);
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
		if(index==0){
			avatar.setImageResource(R.drawable.image_placeholder);
		}else{
			setImage(index, null);
		}
	}

	@Override
	public void onClick(){
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
		Relationship relationship=relationships.get(item.account.id);
		if(relationship==null)
			return false;
		Menu menu=contextMenu.getMenu();
		Account account=item.account;

		menu.findItem(R.id.share).setTitle(fragment.getString(R.string.share_user, account.getDisplayUsername()));
		menu.findItem(R.id.mute).setTitle(fragment.getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getDisplayUsername()));
		menu.findItem(R.id.block).setTitle(fragment.getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getDisplayUsername()));
		menu.findItem(R.id.report).setTitle(fragment.getString(R.string.report_user, account.getDisplayUsername()));
		MenuItem hideBoosts=menu.findItem(R.id.hide_boosts);
		if(relationship.following){
			hideBoosts.setTitle(fragment.getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user, account.getDisplayUsername()));
			hideBoosts.setVisible(true);
		}else{
			hideBoosts.setVisible(false);
		}
		MenuItem blockDomain=menu.findItem(R.id.block_domain);
		if(!account.isLocal()){
			blockDomain.setTitle(fragment.getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
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
		ProgressDialog progress=new ProgressDialog(fragment.getActivity());
		progress.setMessage(fragment.getString(R.string.loading));
		progress.setCancelable(false);
		UiUtils.performAccountAction(fragment.getActivity(), item.account, accountID, relationships.get(item.account.id), button, progressShown->{
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
			fragment.startActivity(Intent.createChooser(intent, item.getTitle()));
		}else if(id==R.id.mute){
			UiUtils.confirmToggleMuteUser(fragment.getActivity(), accountID, account, relationship.muting, this::updateRelationship);
		}else if(id==R.id.block){
			UiUtils.confirmToggleBlockUser(fragment.getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			Nav.go(fragment.getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(fragment.getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(fragment.getActivity(), accountID, account.getDomain(), relationship.domainBlocking, ()->{
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
							error.showToast(fragment.getActivity());
						}
					})
					.wrapProgress(fragment.getActivity(), R.string.loading, false)
					.exec(accountID);
		}
		return true;
	}

	private void updateRelationship(Relationship r){
		relationships.put(item.account.id, r);
		bindRelationship();
	}
}
