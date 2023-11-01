package org.joinmastodon.android.model;


import org.joinmastodon.android.api.RequiredField;

public class Translation extends BaseModel{
	@RequiredField
	public String content;
	@RequiredField
	public String detectedSourceLanguage;
	@RequiredField
	public String provider;
	public MediaAttachment[] mediaAttachments;

	public static class MediaAttachment {
		public String id;
		public String description;
	}
}
