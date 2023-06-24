package org.joinmastodon.android.model.viewmodel;

import android.content.Context;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.SearchResult;

public class SearchResultViewModel{
	public SearchResult result;
	public AccountViewModel account;
	public ListItem<Hashtag> hashtagItem;

	public SearchResultViewModel(SearchResult result, String accountID, boolean isRecents){
		this.result=result;
		switch(result.type){
			case ACCOUNT -> account=new AccountViewModel(result.account, accountID);
			case HASHTAG -> {
				hashtagItem=new ListItem<>((isRecents ? "#" : "")+result.hashtag.name, null, isRecents ? R.drawable.ic_history_24px : R.drawable.ic_tag_24px, null, result.hashtag);
				hashtagItem.isEnabled=true;
			}
		}
	}
}
