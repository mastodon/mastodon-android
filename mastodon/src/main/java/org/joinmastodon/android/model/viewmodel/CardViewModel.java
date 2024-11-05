package org.joinmastodon.android.model.viewmodel;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CardViewModel{
	public final Object parentObject;
	public final Card card;
	public final ImageLoaderRequest imageRequest;
	public final UrlImageLoaderRequest authorAvaRequest;
	public final SpannableStringBuilder parsedAuthorName;
	public final CustomEmojiHelper authorNameEmojiHelper=new CustomEmojiHelper();

	public CardViewModel(Card card, int width, int height, Object parentObject, String accountID){
		this.card=card;
		this.parentObject=parentObject;
		this.imageRequest=TextUtils.isEmpty(card.image) ? null : new UrlImageLoaderRequest(card.image, V.dp(width), V.dp(height));

		Account authorAccount=getAuthorAccount();

		if(authorAccount!=null){
			parsedAuthorName=new SpannableStringBuilder(authorAccount.displayName);
			if(GlobalUserPreferences.customEmojiInNames)
				HtmlParser.parseCustomEmoji(parsedAuthorName, authorAccount.emojis);
			authorNameEmojiHelper.setText(parsedAuthorName);
			authorAvaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? authorAccount.avatar : authorAccount.avatarStatic, V.dp(50), V.dp(50));
		}else{
			parsedAuthorName=null;
			authorAvaRequest=null;
		}
	}

	public int getImageCount(){
		return 1+(getAuthorAccount()!=null ? (1+authorNameEmojiHelper.getImageCount()) : 0);
	}

	public ImageLoaderRequest getImageRequest(int index){
		return switch(index){
			case 0 -> imageRequest;
			case 1 -> authorAvaRequest;
			default -> authorNameEmojiHelper.getImageRequest(index-2);
		};
	}

	public Account getAuthorAccount(){
		if(card.authors!=null && !card.authors.isEmpty() && card.authors.get(0).account!=null)
			return card.authors.get(0).account;
		else
			return card.authorAccount;
	}
}
