package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.NestableScrollView;

import java.util.List;
import java.util.Map;

import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class FavoriteAccountsWrapScene extends AnnualWrapScene{
	private final Map<String, Account> accounts;
	private final List<AnnualReport.AccountAndCount> topAccounts;
	private LinearLayout scrollContent;

	public FavoriteAccountsWrapScene(Map<String, Account> accounts, List<AnnualReport.AccountAndCount> topAccounts){
		this.accounts=accounts;
		this.topAccounts=topAccounts;
	}

	@Override
	protected View onCreateContentView(Context context){
		NestableScrollView scroll=new NestableScrollView(context);
		scroll.setNestedScrollingEnabled(true);
		LayoutInflater inflater=LayoutInflater.from(context);

		LinearLayout ll=new LinearLayout(context);
		ll.setOrientation(LinearLayout.VERTICAL);
		scrollContent=ll;
		scroll.addView(ll);

		View header=inflater.inflate(R.layout.wrap_faves_header, ll, false);
		TextView title=header.findViewById(R.id.title);
		TextView subtitle=header.findViewById(R.id.subtitle);
		title.setText(replaceBoldWithColor(context.getResources().getText(R.string.wrap_most_reblogged_title), 0xFFBAFF3B));
		SpannableStringBuilder subtitleStr=new SpannableStringBuilder(context.getResources().getText(R.string.wrap_most_reblogged_subtitle));
		int index=subtitleStr.toString().indexOf("%s");
		if(index!=-1){
			subtitleStr.replace(index, index+2, year);
		}
		subtitle.setText(replaceBoldWithColor(subtitleStr, 0xFFFFBE2E));
		ll.addView(header);

		for(int i=0;i<Math.min(topAccounts.size(), 4);i++){
			int boostCount=topAccounts.get(i).count;
			Account account=accounts.get(topAccounts.get(i).accountId);
			if(account==null)
				continue;
			LinearLayout row=(LinearLayout) inflater.inflate(R.layout.wrap_faves_account_row, ll, false);
			row.setDividerDrawable(new EmptyDrawable(V.dp(16), 1));
			row.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
			if(i%2==1){
				View firstChild=row.getChildAt(0);
				row.removeView(firstChild);
				row.addView(firstChild);
			}
			ll.addView(row);

			TextView rank=row.findViewById(R.id.rank);
			TextView count=row.findViewById(R.id.count);
			ImageView cover=row.findViewById(R.id.cover);
			ImageView ava=row.findViewById(R.id.avatar);
			TextView name=row.findViewById(R.id.name);
			TextView username=row.findViewById(R.id.username);
			TextView postsCount=row.findViewById(R.id.posts_value);
			TextView postsLabel=row.findViewById(R.id.posts_label);
			TextView followersCount=row.findViewById(R.id.followers_value);
			TextView followersLabel=row.findViewById(R.id.followers_label);
			TextView followingCount=row.findViewById(R.id.following_value);
			TextView followingLabel=row.findViewById(R.id.following_label);

			SpannableString rankStr=new SpannableString("#"+(i+1));
			rankStr.setSpan(new SuperscriptSpan(), 0, 1, 0);
			rankStr.setSpan(new RelativeSizeSpan(0.6f), 0, 1, 0);
			rank.setText(rankStr);
			count.setText(context.getResources().getQuantityString(R.plurals.x_reblogs, boostCount, boostCount));

			CharSequence nameStr=HtmlParser.parseCustomEmoji(account.displayName, account.emojis);
			name.setText(nameStr);
			UiUtils.loadCustomEmojiInTextView(name);
			username.setText(account.getDisplayUsername());
			postsCount.setText(UiUtils.abbreviateNumber(account.statusesCount));
			postsLabel.setText(context.getResources().getQuantityString(R.plurals.posts, account.statusesCount>1000 ? 1000 : (int)account.statusesCount));
			followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
			followersLabel.setText(context.getResources().getQuantityString(R.plurals.followers, account.followersCount>1000 ? 1000 : (int)account.followersCount));
			followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
			followingLabel.setText(context.getResources().getQuantityString(R.plurals.following, account.followingCount>1000 ? 1000 : (int)account.followingCount));

			ViewImageLoader.loadWithoutAnimation(cover, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(300), 0, List.of(), Uri.parse(account.headerStatic)));
			ViewImageLoader.loadWithoutAnimation(ava, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(56), V.dp(56), List.of(), Uri.parse(account.avatarStatic)));
		}

		return scroll;
	}

	@Override
	protected void onDestroyContentView(){

	}

	@Override
	protected View getViewForScreenshot(){
		return scrollContent;
	}
}
