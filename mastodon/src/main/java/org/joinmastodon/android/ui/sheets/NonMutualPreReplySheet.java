package org.joinmastodon.android.ui.sheets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NonMutualPreReplySheet extends PreReplySheet{
	private boolean fullBioShown=false;

	@SuppressLint("DefaultLocale")
	public NonMutualPreReplySheet(@NonNull Context context, ResultListener resultListener, Account account, String accountID){
		super(context, resultListener);
		icon.setImageResource(R.drawable.ic_waving_hand_24px);
		title.setText(R.string.non_mutual_sheet_title);
		text.setText(R.string.non_mutual_sheet_text);

		LinearLayout userInfo=new LinearLayout(context);
		userInfo.setOrientation(LinearLayout.HORIZONTAL);
		userInfo.setBackgroundResource(R.drawable.bg_user_info);
		UiUtils.setAllPaddings(userInfo, 12);

		ImageView ava=new ImageView(context);
		ava.setScaleType(ImageView.ScaleType.CENTER_CROP);
		ava.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		ava.setOutlineProvider(OutlineProviders.roundedRect(12));
		ava.setClipToOutline(true);
		ava.setForeground(context.getResources().getDrawable(R.drawable.fg_user_info_ava, context.getTheme()));
		userInfo.addView(ava, UiUtils.makeLayoutParams(56, 56, 0, 0, 12, 0));
		ViewImageLoader.loadWithoutAnimation(ava, context.getResources().getDrawable(R.drawable.image_placeholder), new UrlImageLoaderRequest(account.avatarStatic, V.dp(56), V.dp(56)));

		LinearLayout nameAndFields=new LinearLayout(context);
		nameAndFields.setOrientation(LinearLayout.VERTICAL);
		nameAndFields.setMinimumHeight(V.dp(56));
		nameAndFields.setGravity(Gravity.CENTER_VERTICAL);
		userInfo.addView(nameAndFields, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		TextView name=new TextView(context);
		name.setSingleLine();
		name.setEllipsize(TextUtils.TruncateAt.END);
		name.setTextAppearance(R.style.m3_title_medium);
		name.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3OnSurface));
		if(AccountSessionManager.get(accountID).getLocalPreferences().customEmojiInNames){
			name.setText(HtmlParser.parseCustomEmoji(account.displayName, account.emojis));
			UiUtils.loadCustomEmojiInTextView(name);
		}else{
			name.setText(account.displayName);
		}
		name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
		nameAndFields.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(24)));
		if(!TextUtils.isEmpty(account.note)){
			CharSequence strippedBio=HtmlParser.parseCustomEmoji(HtmlParser.stripAndRemoveInvisibleSpans(account.note), account.emojis);
			TextView bioShort=new TextView(context);
			bioShort.setTextAppearance(R.style.m3_body_medium);
			bioShort.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3Secondary));
			bioShort.setMaxLines(2);
			bioShort.setEllipsize(TextUtils.TruncateAt.END);
			bioShort.setText(strippedBio);
			nameAndFields.addView(bioShort, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			TextView bioFull=new TextView(context);
			bioFull.setTextAppearance(R.style.m3_body_medium);
			bioFull.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3Secondary));
			bioFull.setText(strippedBio);
			bioFull.setVisibility(View.GONE);
			nameAndFields.addView(bioFull, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			nameAndFields.setOnClickListener(v->{
				UiUtils.beginLayoutTransition((ViewGroup) getWindow().getDecorView());
				fullBioShown=!fullBioShown;
				if(fullBioShown){
					bioFull.setVisibility(View.VISIBLE);
					bioShort.setVisibility(View.GONE);
				}else{
					bioFull.setVisibility(View.GONE);
					bioShort.setVisibility(View.VISIBLE);
				}
			});
			UiUtils.loadCustomEmojiInTextView(bioShort);
			UiUtils.loadCustomEmojiInTextView(bioFull);
		}else{
			TextView username=new TextView(context);
			username.setTextAppearance(R.style.m3_body_medium);
			username.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3Secondary));
			username.setSingleLine();
			username.setEllipsize(TextUtils.TruncateAt.END);
			username.setText(account.getDisplayUsername());
			username.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
			nameAndFields.addView(username, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(20)));
		}

		contentWrap.addView(userInfo, UiUtils.makeLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 8));

		for(int i=0;i<3;i++){
			View item=context.getSystemService(LayoutInflater.class).inflate(R.layout.item_other_numbered_rule, contentWrap, false);
			TextView number=item.findViewById(R.id.number);
			number.setText(String.format("%d", i+1));
			TextView title=item.findViewById(R.id.title);
			TextView text=item.findViewById(R.id.text);
			title.setText(switch(i){
				case 0 -> R.string.non_mutual_title1;
				case 1 -> R.string.non_mutual_title2;
				case 2 -> R.string.non_mutual_title3;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			text.setText(switch(i){
				case 0 -> R.string.non_mutual_text1;
				case 1 -> R.string.non_mutual_text2;
				case 2 -> R.string.non_mutual_text3;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			contentWrap.addView(item);
		}
	}
}
