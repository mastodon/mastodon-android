package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class LinkSpan extends CharacterStyle {

	private int color=0xFF569ace;
	private OnLinkClickListener listener;
	private String link;
	private Type type;

	public LinkSpan(String link, OnLinkClickListener listener, Type type) {
		this.listener=listener;
		this.link=link;
		this.type=type;
	}

	public void setColor(int c){
		color=c;
	}

	public int getColor(){
		return color;
	}

	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setColor(color);
	}
	
	public void onClick(Context context){
		if(listener!=null)
			listener.onLinkClick(this);
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
		HASHTAG
	}
}
