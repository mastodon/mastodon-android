package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;

import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.ui.utils.UiUtils;

public class LinkSpan extends CharacterStyle {

	protected int color=0xFF00FF00;
	private OnLinkClickListener listener;
	private String link;
	private Type type;
	private String accountID;
	private Object linkObject;
	private Object parentObject;

	public LinkSpan(String link, OnLinkClickListener listener, Type type, String accountID, Object linkObject, Object parentObject){
		this.listener=listener;
		this.link=link;
		this.type=type;
		this.accountID=accountID;
		this.linkObject=linkObject;
		this.parentObject=parentObject;
	}

	public int getColor(){
		return color;
	}

	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setColor(color=tp.linkColor);
		tp.setUnderlineText(true);
	}
	
	public void onClick(Context context){
		switch(getType()){
			case URL -> UiUtils.openURL(context, accountID, link, parentObject);
			case MENTION -> {
				String username, domain;
				if(linkObject instanceof Mention m && !TextUtils.isEmpty(m.acct)){
					String[] parts=m.acct.split("@", 2);
					username=parts[0];
					if(parts.length==2){
						domain=parts[1];
					}else{
						domain=AccountSessionManager.get(accountID).domain;
					}
				}else{
					username=domain=null;
				}
				UiUtils.openProfileByID(context, accountID, link, username, domain);
			}
			case HASHTAG -> {
				if(linkObject instanceof Hashtag ht)
					UiUtils.openHashtagTimeline(context, accountID, ht);
				else
					UiUtils.openHashtagTimeline(context, accountID, link);
			}
			case CUSTOM -> listener.onLinkClick(this);
		}
	}

	public String getLink(){
		return link;
	}

	public Type getType(){
		return type;
	}

	public void setListener(OnLinkClickListener listener){
		this.listener=listener;
	}

	public interface OnLinkClickListener{
		void onLinkClick(LinkSpan span);
	}

	public enum Type{
		URL,
		MENTION,
		HASHTAG,
		CUSTOM
	}
}
