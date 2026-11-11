package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusSourceText;
import org.joinmastodon.android.api.requests.statuses.RevokeStatusQuote;
import org.joinmastodon.android.api.requests.statuses.SetStatusConversationMuted;
import org.joinmastodon.android.api.requests.statuses.SetStatusInteractionPolicies;
import org.joinmastodon.android.api.requests.statuses.SetStatusPinned;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.fragments.AddAccountToListsFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusFavoritesListFragment;
import org.joinmastodon.android.fragments.account_list.StatusReblogsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusRelatedAccountListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.sheets.ComposerVisibilitySheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Locale;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class FooterStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	private final String accountID;
	public boolean hideCounts;

	public FooterStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status, String accountID){
		super(parentID, callbacks, context);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<FooterStatusDisplayItem>{
		private final TextView reply, boost, favorite;
		private final ImageView share;
		private final ColorStateList buttonColors;
		private final View replyBtn, boostBtn, favoriteBtn, menuBtn, bookmarkBtn;
		private final PopupMenu boostLongTapMenu, favoriteLongTapMenu;
		private final PopupMenu optionsMenu;

		private final View.AccessibilityDelegate buttonAccessibilityDelegate=new View.AccessibilityDelegate(){
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info){
				super.onInitializeAccessibilityNodeInfo(host, info);
				info.setClassName(Button.class.getName());
				info.setText(item.context.getString(descriptionForId(host.getId())));
			}
		};

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_footer, parent);
			reply=findViewById(R.id.reply);
			boost=findViewById(R.id.boost);
			favorite=findViewById(R.id.favorite);
			share=findViewById(R.id.share);

			buttonColors=new ColorStateList(new int[][]{
					{android.R.attr.state_selected},
					{android.R.attr.state_enabled},
					{}
			}, new int[]{
					UiUtils.getThemeColor(activity, R.attr.colorM3Primary),
					UiUtils.getThemeColor(activity, R.attr.colorM3OnSurface),
					UiUtils.getThemeColor(activity, R.attr.colorM3OnSurface) & 0x80FFFFFF
			});

			boost.setTextColor(buttonColors);
			boost.setCompoundDrawableTintList(buttonColors);
			favorite.setTextColor(buttonColors);
			favorite.setCompoundDrawableTintList(buttonColors);

			ImageView bookmarkIcon=findViewById(R.id.bookmark);
			bookmarkIcon.setImageTintList(buttonColors);

			replyBtn=findViewById(R.id.reply_btn);
			boostBtn=findViewById(R.id.boost_btn);
			favoriteBtn=findViewById(R.id.favorite_btn);
			bookmarkBtn=findViewById(R.id.bookmark_btn);
			menuBtn=findViewById(R.id.menu_btn);
			replyBtn.setOnClickListener(this::onReplyClick);
			replyBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			boostBtn.setOnClickListener(this::onBoostClick);
			boostBtn.setOnLongClickListener(this::onBoostLongClick);
			boostBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			favoriteBtn.setOnClickListener(this::onFavoriteClick);
			favoriteBtn.setOnLongClickListener(this::onFavoriteLongClick);
			favoriteBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			bookmarkBtn.setOnClickListener(this::onBookmarkClick);
			bookmarkBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			menuBtn.setOnClickListener(this::onMenuClick);
			menuBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);

			favoriteLongTapMenu=new PopupMenu(activity, favoriteBtn);
			favoriteLongTapMenu.inflate(R.menu.favorite_longtap);
			favoriteLongTapMenu.setOnMenuItemClickListener(this::onLongTapMenuItemSelected);
			boostLongTapMenu=new PopupMenu(activity, boostBtn);
			boostLongTapMenu.inflate(R.menu.boost_longtap);
			boostLongTapMenu.setOnMenuItemClickListener(this::onLongTapMenuItemSelected);

			optionsMenu=new PopupMenu(activity, menuBtn);
			optionsMenu.inflate(R.menu.post);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic())
				optionsMenu.getMenu().setGroupDividerEnabled(true);
			optionsMenu.setOnMenuItemClickListener(menuItem->{
				Account account=item.status.account;
				Relationship relationship=item.callbacks.getRelationship(account.id);
				int id=menuItem.getItemId();
				if(id==R.id.edit){
					final Bundle args=new Bundle();
					args.putString("account", item.accountID);
					args.putParcelable("editStatus", Parcels.wrap(item.status));
					if(TextUtils.isEmpty(item.status.content) && TextUtils.isEmpty(item.status.spoilerText)){
						Nav.go((Activity) item.context, ComposeFragment.class, args);
					}else{
						new GetStatusSourceText(item.status.id)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(GetStatusSourceText.Response result){
										args.putString("sourceText", result.text);
										args.putString("sourceSpoiler", result.spoilerText);
										Nav.go((Activity) item.context, ComposeFragment.class, args);
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(item.context);
									}
								})
								.wrapProgress((Activity) item.context, R.string.loading, true)
								.exec(item.accountID);
					}
				}else if(id==R.id.delete){
					UiUtils.confirmDeletePost((Activity) item.context, item.accountID, item.status, s->{});
				}else if(id==R.id.mute){
					UiUtils.confirmToggleMuteUser((Activity) item.context, item.accountID, account, relationship!=null && relationship.muting, r->{});
				}else if(id==R.id.block){
					UiUtils.confirmToggleBlockUser((Activity) item.context, item.accountID, account, relationship!=null && relationship.blocking, r->{});
				}else if(id==R.id.report){
					Bundle args=new Bundle();
					args.putString("account", item.accountID);
					args.putParcelable("status", Parcels.wrap(item.status));
					args.putParcelable("reportAccount", Parcels.wrap(item.status.account));
					args.putParcelable("relationship", Parcels.wrap(relationship));
					Nav.go((Activity) item.context, ReportReasonChoiceFragment.class, args);
				}else if(id==R.id.open_in_browser){
					UiUtils.launchWebBrowser(activity, item.status.url);
				}else if(id==R.id.follow){
					if(relationship==null)
						return true;
					ProgressDialog progress=new ProgressDialog(activity);
					progress.setCancelable(false);
					progress.setMessage(activity.getString(R.string.loading));
					UiUtils.performAccountAction(activity, account, item.accountID, relationship, null, visible->{
						if(visible)
							progress.show();
						else
							progress.dismiss();
					}, rel->{
						item.callbacks.putRelationship(account.id, rel);
						Toast.makeText(activity, activity.getString(rel.following ? R.string.followed_user : rel.requested ? R.string.following_user_requested : R.string.unfollowed_user, account.getDisplayUsername()), Toast.LENGTH_SHORT).show();
					}, "status");
				}else if(id==R.id.share){
					UiUtils.openSystemShareSheet(activity, item.status);
				}else if(id==R.id.translate){
					item.callbacks.togglePostTranslation(item.status, item.parentID);
				}else if(id==R.id.add_to_list){
					Bundle args=new Bundle();
					args.putString("account", item.accountID);
					args.putParcelable("targetAccount", Parcels.wrap(account));
					Nav.go(activity, AddAccountToListsFragment.class, args);
				}else if(id==R.id.copy_link){
					activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, item.status.url));
					UiUtils.maybeShowTextCopiedToast(activity);
				}else if(id==R.id.pin){
					new SetStatusPinned(item.status.id, !item.status.pinned)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Status result){
									item.status.pinned=!item.status.pinned;
									new Snackbar.Builder(activity)
											.setText(item.status.pinned ? R.string.post_pinned : R.string.post_unpinned)
											.show();
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, true)
							.exec(item.accountID);
				}else if(id==R.id.mute_conversation){
					new SetStatusConversationMuted(item.status.id, !item.status.muted)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Status result){
									// TODO snackbar?
									item.status.muted=result.muted;
								}

								@Override
								public void onError(ErrorResponse error){
									error.showToast(activity);
								}
							})
							.wrapProgress(activity, R.string.loading, true)
							.exec(item.accountID);
				}else if(id==R.id.change_quote_policy){
					ComposerVisibilitySheet sheet=new ComposerVisibilitySheet(activity, item.status.visibility, item.status.quoteApproval.toQuotePolicy(),
							false, false, StatusPrivacy.PUBLIC, item.accountID, (s, visibility, policy)->{
						new SetStatusInteractionPolicies(item.status.id, policy)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(Status result){
										item.status.quoteApproval=result.quoteApproval;
										s.dismiss();
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(activity);
									}
								})
								.wrapProgress(activity, R.string.loading, true)
								.exec(item.accountID);
						return false;
					});
					sheet.show();
				}else if(id==R.id.remove_quote){
					new M3AlertDialogBuilder(activity)
							.setTitle(R.string.remove_quote_confirm_title)
							.setMessage(R.string.remove_quote_confirm)
							.setPositiveButton(R.string.remove_quote_button, (dlg, which)->{
								new RevokeStatusQuote(item.status.quote.quotedStatus.id, item.status.id)
										.setCallback(new Callback<>(){
											@Override
											public void onSuccess(Status result){
												item.status.quote=result.quote;
												E.post(new StatusUpdatedEvent(item.status));
											}

											@Override
											public void onError(ErrorResponse error){
												error.showToast(activity);
											}
										})
										.wrapProgress(activity, R.string.loading, true)
										.exec(item.accountID);
							})
							.setNegativeButton(R.string.cancel, null)
							.show();
				}
				return true;
			});
		}

		@Override
		public void onBind(FooterStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 8 : 56), itemView.getPaddingTop(), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			bindButton(reply, item.status.repliesCount);
			bindButton(boost, item.status.reblogsCount);
			bindButton(favorite, item.status.favouritesCount);
			boostBtn.setSelected(item.status.reblogged);
			favoriteBtn.setSelected(item.status.favourited);
			boolean isOwn=item.status.account.id.equals(AccountSessionManager.getInstance().getAccount(item.accountID).self.id);
			boostBtn.setEnabled(item.status.visibility==StatusPrivacy.PUBLIC || item.status.visibility==StatusPrivacy.UNLISTED
					|| (item.status.visibility==StatusPrivacy.PRIVATE && isOwn));
//			Drawable d=itemView.getResources().getDrawable(switch(item.status.visibility){
//				case PUBLIC, UNLISTED -> R.drawable.ic_boost;
//				case PRIVATE -> isOwn ? R.drawable.ic_boost_private : R.drawable.ic_boost_disabled_24px;
//				case DIRECT -> R.drawable.ic_boost_disabled_24px;
//			}, itemView.getContext().getTheme());
//			d.setBounds(0, 0, V.dp(20), V.dp(20));
//			boost.setCompoundDrawablesRelative(d, null, null, null);
			bookmarkBtn.setSelected(item.status.bookmarked);
		}

		private void bindButton(TextView btn, long count){
			if(count>0 && !item.hideCounts){
				btn.setText(UiUtils.abbreviateNumber(count));
				btn.setCompoundDrawablePadding(V.dp(4));
			}else{
				btn.setText("");
				btn.setCompoundDrawablePadding(0);
			}
		}

		private void onReplyClick(View v){
			item.callbacks.maybeShowPreReplySheet(item.status, ()->{
				Bundle args=new Bundle();
				args.putString("account", item.accountID);
				args.putParcelable("replyTo", Parcels.wrap(item.status));
				Nav.go((Activity) item.context, ComposeFragment.class, args);
			});
		}

		private void onBoostClick(View v){
			UiUtils.handleStatusBoostClick((Activity) itemView.getContext(), boost, item.accountID, item.status, this::doBoost, null);
		}

		private void doBoost(){
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setReblogged(item.status, !item.status.reblogged);
			boost.setSelected(item.status.reblogged);
			bindButton(boost, item.status.reblogsCount);
		}

		private void onFavoriteClick(View v){
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setFavorited(item.status, !item.status.favourited);
			favorite.setSelected(item.status.favourited);
			bindButton(favorite, item.status.favouritesCount);
		}

		private void onBookmarkClick(View v){
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked);
			favorite.setSelected(item.status.bookmarked);
		}

		private void onMenuClick(View v){
			updateOptionsMenu();
			optionsMenu.show();
		}

		private boolean onBoostLongClick(View v){
			MenuItem boost=boostLongTapMenu.getMenu().findItem(R.id.boost);
			boost.setTitle(item.status.reblogged ? R.string.undo_reblog : R.string.button_reblog);
			boostLongTapMenu.show();
			return true;
		}

		private boolean onFavoriteLongClick(View v){
			MenuItem favorite=favoriteLongTapMenu.getMenu().findItem(R.id.favorite);
			MenuItem bookmark=favoriteLongTapMenu.getMenu().findItem(R.id.bookmark);
			favorite.setTitle(item.status.favourited ? R.string.undo_favorite : R.string.button_favorite);
			bookmark.setTitle(item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark);
			favoriteLongTapMenu.show();
			return true;
		}

		private boolean onLongTapMenuItemSelected(MenuItem item){
			int id=item.getItemId();
			if(id==R.id.favorite){
				onFavoriteClick(null);
			}else if(id==R.id.boost){
				onBoostClick(null);
			}else if(id==R.id.bookmark){
				AccountSessionManager.getInstance().getAccount(this.item.accountID).getStatusInteractionController().setBookmarked(this.item.status, !this.item.status.bookmarked);
			}else if(id==R.id.view_favorites){
				startAccountListFragment(StatusFavoritesListFragment.class);
			}else if(id==R.id.view_boosts){
				startAccountListFragment(StatusReblogsListFragment.class);
			}
			return true;
		}

		private void startAccountListFragment(Class<? extends StatusRelatedAccountListFragment> cls){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("status", Parcels.wrap(item.status));
			Nav.go((Activity) item.context, cls, args);
		}

		private int descriptionForId(int id){
			if(id==R.id.reply_btn)
				return R.string.button_reply;
			if(id==R.id.boost_btn)
				return R.string.button_reblog;
			if(id==R.id.favorite_btn)
				return R.string.button_favorite;
			if(id==R.id.menu_btn)
				return R.string.more_options;
			if(id==R.id.bookmark_btn)
				return item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark;
			return 0;
		}

		private void updateOptionsMenu(){
			if(item.context==null)
				return;
			Account account=item.status.account;
			Relationship relationship=item.callbacks.getRelationship(account.id);
			Menu menu=optionsMenu.getMenu();
			boolean isOwnPost=AccountSessionManager.getInstance().isSelf(item.accountID, account);
			boolean canTranslate=item.status!=null && item.status.getContentStatus().isEligibleForTranslation();
			MenuItem translate=menu.findItem(R.id.translate);
			translate.setVisible(canTranslate);
			if(canTranslate){
				if(item.status.translationState==Status.TranslationState.SHOWN)
					translate.setTitle(R.string.translation_show_original);
				else
					translate.setTitle(item.context.getString(R.string.translate_post, Locale.forLanguageTag(item.status.getContentStatus().language).getDisplayLanguage()));
			}
			menu.findItem(R.id.edit).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.change_quote_policy).setVisible(item.status!=null && isOwnPost && item.status.quoteApproval!=null && (item.status.visibility==StatusPrivacy.PUBLIC || item.status.visibility==StatusPrivacy.UNLISTED));
			menu.findItem(R.id.open_in_browser).setVisible(item.status!=null);
			MenuItem mute=menu.findItem(R.id.mute);
			MenuItem block=menu.findItem(R.id.block);
			MenuItem report=menu.findItem(R.id.report);
			MenuItem follow=menu.findItem(R.id.follow);
			MenuItem pin=menu.findItem(R.id.pin);
			MenuItem muteConversation=menu.findItem(R.id.mute_conversation);
			MenuItem removeQuote=menu.findItem(R.id.remove_quote);
			if(item.status!=null){
				pin.setVisible(item.status.pinned!=null && isOwnPost);
				if(item.status.pinned!=null && isOwnPost){
					pin.setTitle(item.status.pinned ? R.string.unpin_post : R.string.pin_post);
				}
			}else{
				pin.setVisible(false);
				removeQuote.setVisible(false);
			}
			if(isOwnPost){
				mute.setVisible(false);
				block.setVisible(false);
				report.setVisible(false);
				follow.setVisible(false);
				removeQuote.setVisible(false);
			}else{
				mute.setVisible(true);
				block.setVisible(true);
				report.setVisible(true);

				String truncatedName=account.displayName.length()>20 ? (account.displayName.substring(0, 15)+"…") : account.displayName;

				follow.setVisible(relationship==null || relationship.following || (!relationship.blocking && !relationship.blockedBy && !relationship.domainBlocking && !relationship.muting));
				mute.setTitle(item.context.getString(relationship!=null && relationship.muting ? R.string.unmute_user : R.string.mute_user, truncatedName));
				block.setTitle(item.context.getString(relationship!=null && relationship.blocking ? R.string.unblock_user : R.string.block_user, truncatedName));
				report.setTitle(item.context.getString(R.string.report_user, truncatedName));
				follow.setTitle(item.context.getString(relationship!=null && relationship.following ? R.string.unfollow_user : R.string.follow_user, truncatedName));

				if(item.status.quote!=null && item.status.quote.quotedStatus!=null && AccountSessionManager.getInstance().isSelf(item.accountID, item.status.quote.quotedStatus.account)){
					removeQuote.setVisible(true);
					removeQuote.setTitle(item.context.getString(R.string.remove_quote, truncatedName));
				}else{
					removeQuote.setVisible(false);
				}
			}
			if(item.status.muted!=null){
				muteConversation.setVisible(isOwnPost || item.callbacks instanceof NotificationsListFragment);
				muteConversation.setTitle(item.status.muted ? R.string.unmute_conversation : R.string.mute_conversation);
			}else{
				muteConversation.setVisible(false);
			}
			menu.findItem(R.id.add_to_list).setVisible(relationship!=null && relationship.following);
		}
	}
}
