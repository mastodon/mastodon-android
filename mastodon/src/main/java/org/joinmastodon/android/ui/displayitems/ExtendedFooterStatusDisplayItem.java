package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.StatusEditHistoryFragment;
import org.joinmastodon.android.fragments.StatusQuotesFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.account_list.StatusFavoritesListFragment;
import org.joinmastodon.android.fragments.account_list.StatusReblogsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusRelatedAccountListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredLinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import me.grishka.appkit.Nav;

public class ExtendedFooterStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	private final String accountID;

	private static final DateTimeFormatter TIME_FORMATTER_LONG=DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

	public ExtendedFooterStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status, String accountID){
		super(parentID, callbacks, context);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.EXTENDED_FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<ExtendedFooterStatusDisplayItem>{
		private final TextView info;
		private final TextView replies, favorites, reblogs, quotes;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_extended_footer, parent);
			replies=findViewById(R.id.replies);
			reblogs=findViewById(R.id.reblogs);
			favorites=findViewById(R.id.favorites);
			quotes=findViewById(R.id.quotes);
			info=findViewById(R.id.info);

			reblogs.setOnClickListener(v->startAccountListFragment(StatusReblogsListFragment.class));
			favorites.setOnClickListener(v->startAccountListFragment(StatusFavoritesListFragment.class));
			quotes.setOnClickListener(v->startQuotesFragment());
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(ExtendedFooterStatusDisplayItem item){
			Status s=item.status;
			replies.setText(this.item.context.getResources().getQuantityString(R.plurals.x_replies, (int) item.status.repliesCount, item.status.repliesCount));
			favorites.setText(this.item.context.getResources().getQuantityString(R.plurals.x_favorites, (int) item.status.favouritesCount, item.status.favouritesCount));
			reblogs.setText(this.item.context.getResources().getQuantityString(R.plurals.x_reblogs, (int) item.status.reblogsCount, item.status.reblogsCount));
			if(AccountSessionManager.get(item.accountID).getInstanceInfo().supportsQuotePostAuthoring()){
				quotes.setVisibility(View.VISIBLE);
				quotes.setText(this.item.context.getResources().getQuantityString(R.plurals.x_quotes, (int) item.status.quotesCount, item.status.quotesCount));
			}else{
				quotes.setVisibility(View.GONE);
			}

			ZonedDateTime dt=item.status.createdAt.atZone(ZoneId.systemDefault());
			SpannableStringBuilder ssb=new SpannableStringBuilder();
			if(s.editedAt!=null){
				ssb.append(item.context.getString(R.string.last_edit_at_x, UiUtils.DATE_TIME_FORMATTER.format(dt)), new NonColoredLinkSpan(null, ls->startEditHistoryFragment(), LinkSpan.Type.CUSTOM, null, null, null), 0);
				ssb.append(" • ");
				ssb.append(item.context.getString(R.string.post_published_at_x, UiUtils.DATE_TIME_FORMATTER.format(dt)), new NonColoredLinkSpan(null, ls->showTimeSnackbar(), LinkSpan.Type.CUSTOM, null, null, null), 0);
			}else{
				ssb.append(UiUtils.DATE_TIME_FORMATTER.format(dt), new NonColoredLinkSpan(null, ls->showTimeSnackbar(), LinkSpan.Type.CUSTOM, null, null, null), 0);
			}

			if(item.status.application!=null && !TextUtils.isEmpty(item.status.application.name)){
				ssb.append(" • ");
				if(TextUtils.isEmpty(item.status.application.website))
					ssb.append(item.status.application.name);
				else
					ssb.append(item.status.application.name, new NonColoredLinkSpan(item.status.application.website, null, LinkSpan.Type.URL, null, null, null), 0);
			}
			ssb.append(" • ");
			ssb.append(item.context.getString(switch(item.status.visibility){
				case PUBLIC -> R.string.visibility_public;
				case UNLISTED -> R.string.visibility_unlisted;
				case PRIVATE -> R.string.visibility_followers_only;
				case DIRECT -> R.string.visibility_private;
			}));
			info.setText(ssb);
		}

		@Override
		public boolean isEnabled(){
			return false;
		}

		private void startAccountListFragment(Class<? extends StatusRelatedAccountListFragment> cls){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("status", Parcels.wrap(item.status));
			Nav.go((Activity) item.context, cls, args);
		}

		private void startEditHistoryFragment(){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putString("id", item.status.id);
			Nav.go((Activity) item.context, StatusEditHistoryFragment.class, args);
		}

		private void startQuotesFragment(){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("status", Parcels.wrap(item.status));
			Nav.go((Activity) item.context, StatusQuotesFragment.class, args);
		}

		private void showTimeSnackbar(){
			Snackbar sb=new Snackbar.Builder(itemView.getContext())
					.setText(itemView.getContext().getString(R.string.posted_at, TIME_FORMATTER_LONG.format(item.status.createdAt.atZone(ZoneId.systemDefault()))))
					.create();
			if(item.callbacks instanceof ThreadFragment tf){
				tf.showSnackbar(sb);
			}else{
				sb.show();
			}
		}
	}
}
