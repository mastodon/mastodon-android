package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredLinkSpan;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.parceler.Parcels;

import java.util.regex.Pattern;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class ReplyLineStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	public String targetAccountID;
	public Account account;
	public final boolean isSelfReply;
	private String accountID;

	public ReplyLineStatusDisplayItem(String parentID, Callbacks callbacks, Context context, String targetAccountID, Account account, String accountID){
		super(parentID, callbacks, context);
		this.targetAccountID=targetAccountID;
		this.accountID=accountID;
		isSelfReply=AccountSessionManager.get(accountID).self.id.equals(targetAccountID);
		if(isSelfReply){
			text=context.getString(R.string.in_reply_to_self);
		}else{
			updateAccount(account);
		}
	}

	public void updateAccount(Account account){
		if(isSelfReply)
			return;
		this.account=account;
		if(account!=null){
			SpannableStringBuilder parsedName=new SpannableStringBuilder(account.displayName);
			if(GlobalUserPreferences.customEmojiInNames)
				HtmlParser.parseCustomEmoji(parsedName, account.emojis);
			emojiHelper.setText(parsedName);

			SpannableStringBuilder ssb=new SpannableStringBuilder();
			String[] parts=context.getString(R.string.in_reply_to, "{{name}}").split(Pattern.quote("{{name}}"), 2);
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
		}else{
			text=context.getString(R.string.in_reply_to, "...");
		}
	}

	@Override
	public Type getType(){
		return Type.REPLY_LINE;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<ReplyLineStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reply_line, parent);
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(ReplyLineStatusDisplayItem item){
			text.setText(item.text);
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
