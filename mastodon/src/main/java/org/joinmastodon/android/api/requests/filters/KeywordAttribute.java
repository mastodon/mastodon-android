package org.joinmastodon.android.api.requests.filters;

import com.google.gson.annotations.SerializedName;

class KeywordAttribute{
	public String id;
	@SerializedName("_destroy")
	public Boolean delete;
	public String keyword;
	public Boolean wholeWord;

	public KeywordAttribute(String id, Boolean delete, String keyword, Boolean wholeWord){
		this.id=id;
		this.delete=delete;
		this.keyword=keyword;
		this.wholeWord=wholeWord;
	}
}
