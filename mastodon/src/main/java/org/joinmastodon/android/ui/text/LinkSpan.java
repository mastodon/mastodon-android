package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import org.joinmastodon.android.ui.utils.UiUtils;

public class LinkSpan extends CharacterStyle {

	private int color=0xFF00FF00;
	private OnLinkClickListener listener;
	private String link;
	private Type type;
	private String accountID;

	public LinkSpan(String link, OnLinkClickListener listener, Type type, String accountID){
		this.listener=listener;
		this.link=link;
		this.type=type;
		this.accountID=accountID;
	}

	public int getColor(){
		return color;
	}

	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setColor(color=tp.linkColor);
	}
	
	public void onClick(Context context){
		switch(getType()){
			case URL -> UiUtils.openURL(context, accountID, link);
			case MENTION -> UiUtils.openProfileByID(context, accountID, link);
			case HASHTAG -> UiUtils.openHashtagTimeline(context, accountID, link);
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
