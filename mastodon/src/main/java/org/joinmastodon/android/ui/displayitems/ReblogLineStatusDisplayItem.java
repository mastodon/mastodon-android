package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredLinkSpan;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.util.regex.Pattern;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ReblogLineStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	public Account account;
	private String accountID;
	private Instant time;
	private ImageLoaderRequest avaRequest;

	public ReblogLineStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Account account, Instant time, String accountID){
		super(parentID, callbacks, context);
		this.accountID=accountID;
		this.time=time;
		updateAccount(account);
	}

	private void updateAccount(Account account){
		this.account=account;
		SpannableStringBuilder parsedName=new SpannableStringBuilder(account.displayName);
		if(GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(parsedName, account.emojis);
		emojiHelper.setText(parsedName);

		SpannableStringBuilder ssb=new SpannableStringBuilder();
		String[] parts=context.getString(R.string.user_boosted, "{{name}}").split(Pattern.quote("{{name}}"), 2);
		if(parts.length>1 && !TextUtils.isEmpty(parts[0]))
			ssb.append(parts[0]);

		ssb.append(parsedName, new NonColoredLinkSpan(null, s->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(account));
			args.putString("followReferrer", "status");
			Nav.go((Activity) context, ProfileFragment.class, args);
		}, LinkSpan.Type.CUSTOM, null, null, null), 0);

		if(parts.length==1){
			ssb.append(' ');
			ssb.append(parts[0]);
		}else if(!TextUtils.isEmpty(parts[1])){
			ssb.append(parts[1]);
		}

		text=ssb;
		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(50), V.dp(50));
	}

	@Override
	public Type getType(){
		return Type.REBLOG_LINE;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount()+1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return index==0 ? avaRequest : emojiHelper.getImageRequest(index-1);
	}

	public static class Holder extends StatusDisplayItem.Holder<ReblogLineStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text, timestamp;
		private final ImageView ava;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reblog_line, parent);
			text=findViewById(R.id.text);
			timestamp=findViewById(R.id.time);
			ava=findViewById(R.id.ava);
			OutlineProviders.clipToOval(ava);
		}

		@Override
		public void onBind(ReblogLineStatusDisplayItem item){
			text.setText(item.text);
			timestamp.setText(UiUtils.formatRelativeTimestamp(timestamp.getContext(), item.time));
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				if(image==null)
					ava.setImageResource(R.drawable.image_placeholder);
				else
					ava.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				text.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
