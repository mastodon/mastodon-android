package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@AllFieldsAreRequired
@Parcel
public class Poll extends BaseModel{
	public String id;
	public Instant expiresAt;
	public boolean expired;
	public boolean multiple;
	public int votersCount;
	public boolean voted;
	public List<Integer> ownVotes;
	public List<Option> options;
	public List<Emoji> emojis;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(Emoji e:emojis)
			e.postprocess();
	}

	@Override
	public String toString(){
		return "Poll{"+
				"id='"+id+'\''+
				", expiresAt="+expiresAt+
				", expired="+expired+
				", multiple="+multiple+
				", votersCount="+votersCount+
				", voted="+voted+
				", ownVotes="+ownVotes+
				", options="+options+
				", emojis="+emojis+
				'}';
	}

	@Parcel
	public static class Option{
		public String title;
		public Integer votesCount;

		@Override
		public String toString(){
			return "Option{"+
					"title='"+title+'\''+
					", votesCount="+votesCount+
					'}';
		}
	}
}
