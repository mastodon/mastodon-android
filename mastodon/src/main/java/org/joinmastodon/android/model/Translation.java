package org.joinmastodon.android.model;


import org.joinmastodon.android.api.RequiredField;

public class Translation extends BaseModel{
	@RequiredField
	public String content;
	@RequiredField
	public String detectedSourceLanguage;
	@RequiredField
	public String provider;
	public String spoilerText;
	public MediaAttachment[] mediaAttachments;
	public PollTranslation poll;

	public static class MediaAttachment {
		public String id;
		public String description;
	}

	public static class PollTranslation {
		public String id;
		public PollOption[] options;
	}

	public static class PollOption {
		public String title;
	}
}
