package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
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
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import me.grishka.appkit.Nav;

public class ExtendedFooterStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	private final String accountID;

	private static final DateTimeFormatter TIME_FORMATTER=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	private static final DateTimeFormatter TIME_FORMATTER_LONG=DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
	private static final DateTimeFormatter DATE_FORMATTER=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

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
		private final TextView time, date, app, dateAppSeparator;
		private final TextView favorites, reblogs, quotes, editHistory;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_extended_footer, parent);
			reblogs=findViewById(R.id.reblogs);
			favorites=findViewById(R.id.favorites);
			quotes=findViewById(R.id.quotes);
			editHistory=findViewById(R.id.edit_history);
			time=findViewById(R.id.time);
			date=findViewById(R.id.date);
			app=findViewById(R.id.app_name);
			dateAppSeparator=findViewById(R.id.date_app_separator);

			reblogs.setOnClickListener(v->startAccountListFragment(StatusReblogsListFragment.class));
			favorites.setOnClickListener(v->startAccountListFragment(StatusFavoritesListFragment.class));
			quotes.setOnClickListener(v->startQuotesFragment());
			editHistory.setOnClickListener(v->startEditHistoryFragment());
			time.setOnClickListener(v->showTimeSnackbar());
			app.setOnClickListener(v->UiUtils.launchWebBrowser(context, item.status.application.website));
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(ExtendedFooterStatusDisplayItem item){
			Status s=item.status;
			favorites.setText(getFormattedPlural(R.plurals.x_favorites, item.status.favouritesCount));
			reblogs.setText(getFormattedPlural(R.plurals.x_reblogs, item.status.reblogsCount));
			if(AccountSessionManager.get(item.accountID).getInstanceInfo().supportsQuotePostAuthoring()){
				quotes.setVisibility(View.VISIBLE);
				quotes.setText(getFormattedPlural(R.plurals.x_quotes, item.status.quotesCount));
			}else{
				quotes.setVisibility(View.GONE);
			}
			if(s.editedAt!=null){
				editHistory.setVisibility(View.VISIBLE);
				ZonedDateTime dt=s.editedAt.atZone(ZoneId.systemDefault());
				String time=TIME_FORMATTER.format(dt);
				if(!dt.toLocalDate().equals(LocalDate.now())){
					time+=" · "+DATE_FORMATTER.format(dt);
				}
				editHistory.setText(getFormattedSubstitutedString(R.string.last_edit_at_x, time));
			}else{
				editHistory.setVisibility(View.GONE);
			}
			ZonedDateTime dt=item.status.createdAt.atZone(ZoneId.systemDefault());
			time.setText(TIME_FORMATTER.format(dt));
			date.setText(DATE_FORMATTER.format(dt));
			if(item.status.application!=null && !TextUtils.isEmpty(item.status.application.name)){
				app.setVisibility(View.VISIBLE);
				dateAppSeparator.setVisibility(View.VISIBLE);
				app.setText(item.status.application.name);
				app.setEnabled(!TextUtils.isEmpty(item.status.application.website));
			}else{
				app.setVisibility(View.GONE);
				dateAppSeparator.setVisibility(View.GONE);
			}
		}

		@Override
		public boolean isEnabled(){
			return false;
		}

		private SpannableStringBuilder getFormattedPlural(@PluralsRes int res, long quantity){
			String str=item.context.getResources().getQuantityString(res, (int)quantity, quantity);
			String formattedNumber=String.format(Locale.getDefault(), "%,d", quantity);
			int index=str.indexOf(formattedNumber);
			SpannableStringBuilder ssb=new SpannableStringBuilder(str);
			if(index>=0){
				ForegroundColorSpan colorSpan=new ForegroundColorSpan(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurfaceVariant));
				ssb.setSpan(colorSpan, index, index+formattedNumber.length(), 0);
				Object typefaceSpan;
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
					typefaceSpan=new TypefaceSpan(Typeface.create(Typeface.DEFAULT, 600, false));
				}else{
					typefaceSpan=new StyleSpan(Typeface.BOLD);
				}
				ssb.setSpan(typefaceSpan, index, index+formattedNumber.length(), 0);
			}
			return ssb;
		}

		private SpannableStringBuilder getFormattedSubstitutedString(@StringRes int res, String substitution){
			String str=item.context.getString(res, substitution);
			int index=item.context.getString(res).indexOf("%s");
			SpannableStringBuilder ssb=new SpannableStringBuilder(str);
			if(index>=0){
				ForegroundColorSpan colorSpan=new ForegroundColorSpan(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurfaceVariant));
				ssb.setSpan(colorSpan, index, index+substitution.length(), 0);
				Object typefaceSpan;
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
					typefaceSpan=new TypefaceSpan(Typeface.create(Typeface.DEFAULT, 600, false));
				}else{
					typefaceSpan=new StyleSpan(Typeface.BOLD);
				}
				ssb.setSpan(typefaceSpan, index, index+substitution.length(), 0);
			}
			return ssb;
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
