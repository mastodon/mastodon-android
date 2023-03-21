package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;

public class SearchResult extends BaseModel implements DisplayItemsParent{
	public Account account;
	public Hashtag hashtag;
	public Status status;
	@RequiredField
	public Type type;

	public transient String id;
	public transient boolean firstInSection;

	public SearchResult(){}

	public SearchResult(Account acc){
		account=acc;
		type=Type.ACCOUNT;
		generateID();
	}

	public SearchResult(Hashtag tag){
		hashtag=tag;
		type=Type.HASHTAG;
		generateID();
	}

	public SearchResult(Status status){
		this.status=status;
		type=Type.STATUS;
		generateID();
	}

	public String getID(){
		return id;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(account!=null)
			account.postprocess();
		if(hashtag!=null)
			hashtag.postprocess();
		if(status!=null)
			status.postprocess();
		generateID();
	}

	private void generateID(){
		id=switch(type){
			case ACCOUNT -> "acc_"+account.id;
			case HASHTAG -> "tag_"+hashtag.name.hashCode();
			case STATUS -> "post_"+status.id;
		};
	}

	public enum Type{
		ACCOUNT,
		HASHTAG,
		STATUS
	}
}
