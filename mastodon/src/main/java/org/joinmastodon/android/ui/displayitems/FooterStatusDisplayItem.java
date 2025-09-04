package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.account_list.StatusFavoritesListFragment;
import org.joinmastodon.android.fragments.account_list.StatusReblogsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusRelatedAccountListFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.QuoteApproval;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.sheets.ListItemsSheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;
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
		private final View replyBtn, boostBtn, favoriteBtn, shareBtn;
		private final PopupMenu boostLongTapMenu, favoriteLongTapMenu;
		private final View spacer1, spacer2;

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
			spacer1=findViewById(R.id.spacer1);
			spacer2=findViewById(R.id.spacer2);

			float[] hsb={0, 0, 0};
			Color.colorToHSV(UiUtils.getThemeColor(activity, R.attr.colorM3Primary), hsb);
			hsb[1]+=0.1f;
			hsb[2]+=0.16f;

			buttonColors=new ColorStateList(new int[][]{
					{android.R.attr.state_selected},
					{android.R.attr.state_enabled},
					{}
			}, new int[]{
					Color.HSVToColor(hsb),
					UiUtils.getThemeColor(activity, R.attr.colorM3Outline),
					UiUtils.getThemeColor(activity, R.attr.colorM3Outline) & 0x80FFFFFF
			});

			boost.setTextColor(buttonColors);
			boost.setCompoundDrawableTintList(buttonColors);
			favorite.setTextColor(buttonColors);
			favorite.setCompoundDrawableTintList(buttonColors);

			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
				UiUtils.fixCompoundDrawableTintOnAndroid6(reply);
				UiUtils.fixCompoundDrawableTintOnAndroid6(boost);
				UiUtils.fixCompoundDrawableTintOnAndroid6(favorite);
			}
			replyBtn=findViewById(R.id.reply_btn);
			boostBtn=findViewById(R.id.boost_btn);
			favoriteBtn=findViewById(R.id.favorite_btn);
			shareBtn=findViewById(R.id.share_btn);
			replyBtn.setOnClickListener(this::onReplyClick);
			replyBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			boostBtn.setOnClickListener(this::onBoostClick);
			boostBtn.setOnLongClickListener(this::onBoostLongClick);
			boostBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			favoriteBtn.setOnClickListener(this::onFavoriteClick);
			favoriteBtn.setOnLongClickListener(this::onFavoriteLongClick);
			favoriteBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);
			shareBtn.setOnClickListener(this::onShareClick);
			shareBtn.setAccessibilityDelegate(buttonAccessibilityDelegate);

			favoriteLongTapMenu=new PopupMenu(activity, favoriteBtn);
			favoriteLongTapMenu.inflate(R.menu.favorite_longtap);
			favoriteLongTapMenu.setOnMenuItemClickListener(this::onLongTapMenuItemSelected);
			boostLongTapMenu=new PopupMenu(activity, boostBtn);
			boostLongTapMenu.inflate(R.menu.boost_longtap);
			boostLongTapMenu.setOnMenuItemClickListener(this::onLongTapMenuItemSelected);
		}

		@Override
		public void onBind(FooterStatusDisplayItem item){
			spacer1.setVisibility(item.fullWidth ? View.VISIBLE : View.GONE);
			spacer2.setVisibility(item.fullWidth ? View.VISIBLE : View.GONE);
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 8 : 56), itemView.getPaddingTop(), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			bindButton(reply, item.status.repliesCount);
			bindButton(boost, item.status.reblogsCount);
			bindButton(favorite, item.status.favouritesCount);
			boostBtn.setSelected(item.status.reblogged);
			favoriteBtn.setSelected(item.status.favourited);
			boolean isOwn=item.status.account.id.equals(AccountSessionManager.getInstance().getAccount(item.accountID).self.id);
			boostBtn.setEnabled(item.status.visibility==StatusPrivacy.PUBLIC || item.status.visibility==StatusPrivacy.UNLISTED
					|| (item.status.visibility==StatusPrivacy.PRIVATE && isOwn));
			Drawable d=itemView.getResources().getDrawable(switch(item.status.visibility){
				case PUBLIC, UNLISTED -> R.drawable.ic_boost;
				case PRIVATE -> isOwn ? R.drawable.ic_boost_private : R.drawable.ic_boost_disabled_24px;
				case DIRECT -> R.drawable.ic_boost_disabled_24px;
			}, itemView.getContext().getTheme());
			d.setBounds(0, 0, V.dp(20), V.dp(20));
			boost.setCompoundDrawablesRelative(d, null, null, null);
		}

		private void bindButton(TextView btn, long count){
			if(count>0 && !item.hideCounts){
				btn.setText(UiUtils.abbreviateNumber(count));
				btn.setCompoundDrawablePadding(V.dp(6));
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
			Instance instance=AccountSessionManager.get(item.accountID).getInstanceInfo();
			if(instance.supportsQuotePostAuthoring()){
				ListItemsSheet sheet=new ListItemsSheet(itemView.getContext());
				sheet.add(new ListItem<>(item.status.reblogged ? R.string.undo_reblog : R.string.button_reblog, 0, R.drawable.ic_repeat_24px, o->{
					doBoost();
					sheet.dismiss();
				}));
				if(item.status.quoteApproval==null || item.status.quoteApproval.currentUser==QuoteApproval.CurrentUserPolicy.UNKNOWN || item.status.quoteApproval.currentUser==QuoteApproval.CurrentUserPolicy.DENIED){
					sheet.add(new ListItem<>(R.string.create_quote,
							item.status.quoteApproval!=null && item.status.quoteApproval.automatic.contains(QuoteApproval.Policy.FOLLOWERS) ? R.string.cannot_quote_post_followers_only : R.string.cannot_quote_post,
							R.drawable.ic_format_quote_off_fill1_24px, null));
				}else{
					sheet.add(new ListItem<>(R.string.create_quote, 0, R.drawable.ic_format_quote_fill1_24px, o->{
						sheet.dismiss();
						Bundle args=new Bundle();
						args.putString("account", item.accountID);
						args.putParcelable("quote", Parcels.wrap(item.status));
						Nav.go((Activity) item.context, ComposeFragment.class, args);
					}));
				}
				sheet.show();
			}else{
				if(GlobalUserPreferences.confirmBoost){
					PopupMenu menu=new PopupMenu(itemView.getContext(), boost);
					menu.getMenu().add(R.string.button_reblog);
					menu.setOnMenuItemClickListener(item->{
						doBoost();
						return true;
					});
					menu.show();
				}else{
					doBoost();
				}
			}
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

		private void onShareClick(View v){
			UiUtils.openSystemShareSheet(v.getContext(), item.status);
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
			if(id==R.id.share_btn)
				return R.string.button_share;
			return 0;
		}
	}
}
