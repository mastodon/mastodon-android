package org.joinmastodon.android.model;

import android.text.TextUtils;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;

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
	public String spoilerText="";
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
	public Quote quote;

	public boolean favourited;
	public boolean reblogged;
	public Boolean muted;
	public boolean bookmarked;
	public Boolean pinned;

	public transient EnumSet<SpoilerType> revealedSpoilers=EnumSet.noneOf(SpoilerType.class);
	public transient boolean hasGapAfter;
	private transient String strippedText;
	public transient TranslationState translationState=TranslationState.HIDDEN;
	public transient Translation translation;

	public Status(){}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(application!=null)
			application.postprocess();
		for(Mention m:mentions)
			m.postprocess();
		for(Hashtag t:tags)
			t.postprocess();
		for(Emoji e:emojis)
			e.postprocess();
		for(Attachment a:mediaAttachments)
			a.postprocess();
		account.postprocess();
		if(poll!=null)
			poll.postprocess();
		if(card!=null)
			card.postprocess();
		if(reblog!=null)
			reblog.postprocess();
		if(filtered!=null){
			for(FilterResult fr:filtered)
				fr.postprocess();
		}
		if(quote!=null)
			quote.postprocess();

		if(!sensitive && (reblog==null || !reblog.sensitive) && TextUtils.isEmpty(spoilerText)){
			revealedSpoilers.add(SpoilerType.CONTENT_WARNING);
		}
	}

	@Override
	public String toString(){
		return "Status{"+
				"account="+account+
				", id='"+id+'\''+
				", uri='"+uri+'\''+
				", createdAt="+createdAt+
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
				", revealedSpoilers="+revealedSpoilers+
				", hasGapAfter="+hasGapAfter+
				", strippedText='"+strippedText+'\''+
				", translationState="+translationState+
				", translation="+translation+
				'}';
	}

	@Override
	public String getID(){
		return id;
	}

	@Override
	public String getAccountID(){
		return getContentStatus().account.id;
	}

	public void update(StatusCountersUpdatedEvent ev){
		switch(ev.type){
			case FAVORITES -> {
				favouritesCount=ev.favorites;
				favourited=ev.favorited;
			}
			case REBLOGS -> {
				reblogsCount=ev.reblogs;
				reblogged=ev.reblogged;
			}
			case REPLIES -> repliesCount=ev.replies;
			case BOOKMARKS -> bookmarked=ev.bookmarked;
		}
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
		copy.revealedSpoilers=EnumSet.noneOf(SpoilerType.class);
		copy.translationState=TranslationState.HIDDEN;
		return copy;
	}

	public boolean isEligibleForTranslation(){
		return !TextUtils.isEmpty(content) && !TextUtils.isEmpty(language) && !Objects.equals(Locale.getDefault().getLanguage(), language)
				&& (visibility==StatusPrivacy.PUBLIC || visibility==StatusPrivacy.UNLISTED);
	}

	public enum TranslationState{
		HIDDEN,
		SHOWN,
		LOADING
	}

	public enum SpoilerType{
		CONTENT_WARNING,
		FILTER
	}
}
