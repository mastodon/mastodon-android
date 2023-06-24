package org.joinmastodon.android.model.viewmodel;

import android.text.TextUtils;

import org.joinmastodon.android.model.Card;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CardViewModel{
	public final Card card;
	public final ImageLoaderRequest imageRequest;

	public CardViewModel(Card card, int width, int height){
		this.card=card;
		this.imageRequest=TextUtils.isEmpty(card.image) ? null : new UrlImageLoaderRequest(card.image, V.dp(width), V.dp(height));
	}
}
