package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.ArrayList;

import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;

/**
 * Represents a profile field as a name-value pair with optional verification.
 */
@Parcel
public class AccountField extends BaseModel{
	/**
	 * The key of a given field's key-value pair.
	 */
	@RequiredField
	public String name;
	/**
	 * The value associated with the name key.
	 */
	@RequiredField
	public String value;
	/**
	 * Timestamp of when the server verified a URL value for a rel="me” link.
	 */
	public Instant verifiedAt;

	public transient CharSequence parsedValue, parsedName;
	public transient CustomEmojiSpan[] valueEmojis, nameEmojis;
	public transient ArrayList<UrlImageLoaderRequest> emojiRequests;

	@Override
	public String toString(){
		return "AccountField{"+
				"name='"+name+'\''+
				", value='"+value+'\''+
				", verifiedAt="+verifiedAt+
				'}';
	}
}
