package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.statuses.GetStatusSourceText;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import androidx.annotation.LayoutRes;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public final Status status;
	private boolean hasVisibilityToggle;
	boolean needBottomPadding;
	private String extraText;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status, String extraText){
		super(parentID, parentFragment);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? user.avatar : user.avatarStatic, V.dp(50), V.dp(50));
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		this.status=status;
		HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
		if(status!=null){
			hasVisibilityToggle=status.sensitive || !TextUtils.isEmpty(status.spoilerText);
			if(!hasVisibilityToggle && !status.mediaAttachments.isEmpty()){
				for(Attachment att:status.mediaAttachments){
					if(att.type!=Attachment.Type.AUDIO){
						hasVisibilityToggle=true;
						break;
					}
				}
			}
		}
		this.extraText=extraText;
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>0){
			return emojiHelper.getImageRequest(index-1);
		}
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, timeAndUsername, extraText;
		private final ImageView avatar, more;
		private final PopupMenu optionsMenu;
		private Relationship relationship;
		private APIRequest<?> currentRelationshipRequest;

		public Holder(Activity activity, ViewGroup parent){
			this(activity, R.layout.display_item_header, parent);
		}

		protected Holder(Activity activity, @LayoutRes int layout, ViewGroup parent){
			super(activity, layout, parent);
			name=findViewById(R.id.name);
			timeAndUsername=findViewById(R.id.time_and_username);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			extraText=findViewById(R.id.extra_text);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(10));
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);

			optionsMenu=new PopupMenu(activity, more);
			optionsMenu.inflate(R.menu.post);
			optionsMenu.setOnMenuItemClickListener(menuItem->{
				Account account=item.user;
				int id=menuItem.getItemId();
				if(id==R.id.edit){
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("editStatus", Parcels.wrap(item.status));
					if(TextUtils.isEmpty(item.status.content) && TextUtils.isEmpty(item.status.spoilerText)){
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else{
						new GetStatusSourceText(item.status.id)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(GetStatusSourceText.Response result){
										args.putString("sourceText", result.text);
										args.putString("sourceSpoiler", result.spoilerText);
										Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(item.parentFragment.getActivity());
									}
								})
								.wrapProgress(item.parentFragment.getActivity(), R.string.loading, true)
								.exec(item.parentFragment.getAccountID());
					}
				}else if(id==R.id.delete){
					UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{});
				}else if(id==R.id.mute){
					UiUtils.confirmToggleMuteUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.muting, r->{});
				}else if(id==R.id.block){
					UiUtils.confirmToggleBlockUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.blocking, r->{});
				}else if(id==R.id.report){
					Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("status", Parcels.wrap(item.status));
					args.putParcelable("reportAccount", Parcels.wrap(item.status.account));
					args.putParcelable("relationship", Parcels.wrap(relationship));
					Nav.go(item.parentFragment.getActivity(), ReportReasonChoiceFragment.class, args);
				}else if(id==R.id.open_in_browser){
					UiUtils.launchWebBrowser(activity, item.status.url);
				}else if(id==R.id.follow){
					if(relationship==null)
						return true;
					ProgressDialog progress=new ProgressDialog(activity);
					progress.setCancelable(false);
					progress.setMessage(activity.getString(R.string.loading));
					UiUtils.performAccountAction(activity, account, item.parentFragment.getAccountID(), relationship, null, visible->{
						if(visible)
							progress.show();
						else
							progress.dismiss();
					}, rel->{
						relationship=rel;
						Toast.makeText(activity, activity.getString(rel.following ? R.string.followed_user : rel.requested ? R.string.following_user_requested : R.string.unfollowed_user, account.getDisplayUsername()), Toast.LENGTH_SHORT).show();
					});
				}else if(id==R.id.block_domain){
					UiUtils.confirmToggleBlockDomain(activity, item.parentFragment.getAccountID(), account.getDomain(), relationship!=null && relationship.domainBlocking, ()->{});
				}else if(id==R.id.bookmark){
					AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked);
				}
				return true;
			});
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			String time;
			if(item.status==null || item.status.editedAt==null)
				time=UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt);
			else
				time=item.parentFragment.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt));

			timeAndUsername.setText(time+" · @"+item.user.acct);
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(16) : 0);
			if(TextUtils.isEmpty(item.extraText)){
				extraText.setVisibility(View.GONE);
			}else{
				extraText.setVisibility(View.VISIBLE);
				extraText.setText(item.extraText);
			}
			more.setVisibility(item.inset ? View.GONE : View.VISIBLE);
			avatar.setClickable(!item.inset);
			avatar.setContentDescription(item.parentFragment.getString(R.string.avatar_description, item.user.acct));
			if(currentRelationshipRequest!=null){
				currentRelationshipRequest.cancel();
			}
			relationship=null;
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
				name.invalidate();
			}else{
				avatar.setImageDrawable(drawable);
			}
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}

		private void onMoreClick(View v){
			updateOptionsMenu();
			optionsMenu.show();
			if(relationship==null && currentRelationshipRequest==null){
				currentRelationshipRequest=new GetAccountRelationships(Collections.singletonList(item.user.id))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Relationship> result){
								if(!result.isEmpty()){
									relationship=result.get(0);
									updateOptionsMenu();
								}
								currentRelationshipRequest=null;
							}

							@Override
							public void onError(ErrorResponse error){
								currentRelationshipRequest=null;
							}
						})
						.exec(item.parentFragment.getAccountID());
			}
		}

		private void updateOptionsMenu(){
			Account account=item.user;
			Menu menu=optionsMenu.getMenu();
			boolean isOwnPost=AccountSessionManager.getInstance().isSelf(item.parentFragment.getAccountID(), account);
			menu.findItem(R.id.edit).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.open_in_browser).setVisible(item.status!=null);
			MenuItem blockDomain=menu.findItem(R.id.block_domain);
			MenuItem mute=menu.findItem(R.id.mute);
			MenuItem block=menu.findItem(R.id.block);
			MenuItem report=menu.findItem(R.id.report);
			MenuItem follow=menu.findItem(R.id.follow);
			MenuItem bookmark=menu.findItem(R.id.bookmark);
			if(item.status!=null){
				bookmark.setVisible(true);
				bookmark.setTitle(item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark);
			}else{
				bookmark.setVisible(false);
			}
			if(isOwnPost){
				mute.setVisible(false);
				block.setVisible(false);
				report.setVisible(false);
				follow.setVisible(false);
				blockDomain.setVisible(false);
			}else{
				mute.setVisible(true);
				block.setVisible(true);
				report.setVisible(true);
				follow.setVisible(relationship==null || relationship.following || (!relationship.blocking && !relationship.blockedBy && !relationship.domainBlocking && !relationship.muting));
				mute.setTitle(item.parentFragment.getString(relationship!=null && relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getDisplayUsername()));
				block.setTitle(item.parentFragment.getString(relationship!=null && relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getDisplayUsername()));
				report.setTitle(item.parentFragment.getString(R.string.report_user, account.getDisplayUsername()));
				if(!account.isLocal()){
					blockDomain.setVisible(true);
					blockDomain.setTitle(item.parentFragment.getString(relationship!=null && relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
				}else{
					blockDomain.setVisible(false);
				}
				follow.setTitle(item.parentFragment.getString(relationship!=null && relationship.following ? R.string.unfollow_user : R.string.follow_user, account.getDisplayUsername()));
			}
		}
	}
}
