package org.joinmastodon.android.model;

import androidx.annotation.NonNull;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;

@Parcel
public class Status extends BaseModel implements DisplayItemsParent{
	@RequiredField
	public String id;
	@RequiredField
	public String uri;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Account account;
//	@RequiredField
	public String content;
	@RequiredField
	public StatusPrivacy visibility;
	public boolean sensitive;
	@RequiredField
	public String spoilerText;
	@RequiredField
	public List<Attachment> mediaAttachments;
	public Application application;
	@RequiredField
	public List<Mention> mentions;
	@RequiredField
	public List<Hashtag> tags;
	@RequiredField
	public List<Emoji> emojis;
	public long reblogsCount;
	public long favouritesCount;
	public long repliesCount;
	public Instant editedAt;

	public String url;
	public String inReplyToId;
	public String inReplyToAccountId;
	public Status reblog;
	public Poll poll;
	public Card card;
	public String language;
	public String text;
	public List<FilterResult> filtered;

	public boolean favourited;
	public boolean reblogged;
	public boolean muted;
	public boolean bookmarked;
	public boolean pinned;

	public transient boolean spoilerRevealed;
	public transient boolean hasGapAfter;
	private transient String strippedText;

	public Status(){}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		spoilerRevealed=!sensitive;
	}

	@Override
	public String toString(){
		return "Status{"+
				"id='"+id+'\''+
				", uri='"+uri+'\''+
				", createdAt="+createdAt+
				", account="+account+
				", content='"+content+'\''+
				", visibility="+visibility+
				", sensitive="+sensitive+
				", spoilerText='"+spoilerText+'\''+
				", mediaAttachments="+mediaAttachments+
				", application="+application+
				", mentions="+mentions+
				", tags="+tags+
				", emojis="+emojis+
				", reblogsCount="+reblogsCount+
				", favouritesCount="+favouritesCount+
				", repliesCount="+repliesCount+
				", editedAt="+editedAt+
				", url='"+url+'\''+
				", inReplyToId='"+inReplyToId+'\''+
				", inReplyToAccountId='"+inReplyToAccountId+'\''+
				", reblog="+reblog+
				", poll="+poll+
				", card="+card+
				", language='"+language+'\''+
				", text='"+text+'\''+
				", filtered="+filtered+
				", favourited="+favourited+
				", reblogged="+reblogged+
				", muted="+muted+
				", bookmarked="+bookmarked+
				", pinned="+pinned+
				", spoilerRevealed="+spoilerRevealed+
				", hasGapAfter="+hasGapAfter+
				", strippedText='"+strippedText+'\''+
				'}';
	}

	@Override
	public String getID(){
		return id;
	}

	public void update(StatusCountersUpdatedEvent ev){
		favouritesCount=ev.favorites;
		reblogsCount=ev.reblogs;
		repliesCount=ev.replies;
		favourited=ev.favorited;
		reblogged=ev.reblogged;
		bookmarked=ev.bookmarked;
	}

	public Status getContentStatus(){
		return reblog!=null ? reblog : this;
	}

	public String getStrippedText(){
		if(strippedText==null)
			strippedText=HtmlParser.strip(content);
		return strippedText;
	}

	@NonNull
	@Override
	public Status clone(){
		Status copy=(Status) super.clone();
		copy.spoilerRevealed=false;
		return copy;
	}
}
