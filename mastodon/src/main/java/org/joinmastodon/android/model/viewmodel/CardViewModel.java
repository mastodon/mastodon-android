package org.joinmastodon.android.model.viewmodel;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
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

		if(card.authorAccount!=null){
			parsedAuthorName=new SpannableStringBuilder(card.authorAccount.displayName);
			if(AccountSessionManager.get(accountID).getLocalPreferences().customEmojiInNames)
				HtmlParser.parseCustomEmoji(parsedAuthorName, card.authorAccount.emojis);
			authorNameEmojiHelper.setText(parsedAuthorName);
			authorAvaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? card.authorAccount.avatar : card.authorAccount.avatarStatic, V.dp(50), V.dp(50));
		}else{
			parsedAuthorName=null;
			authorAvaRequest=null;
		}
	}

	public int getImageCount(){
		return 1+(card.authorAccount!=null ? (1+authorNameEmojiHelper.getImageCount()) : 0);
	}

	public ImageLoaderRequest getImageRequest(int index){
		return switch(index){
			case 0 -> imageRequest;
			case 1 -> authorAvaRequest;
			default -> authorNameEmojiHelper.getImageRequest(index-2);
		};
	}
}
