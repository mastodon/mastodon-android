package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;

@AllFieldsAreRequired
public class Translation extends BaseModel{
	public String content;
	public String detectedSourceLanguage;
	public String provider;
}
