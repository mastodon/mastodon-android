package org.joinmastodon.android.ui.text;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.widget.Toast;

import org.joinmastodon.android.ui.utils.UiUtils;

public class LinkSpan extends CharacterStyle {

	private int color=0xFF569ace;
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
		switch(getType()){
			case URL -> UiUtils.launchWebBrowser(context, link);
			case MENTION -> UiUtils.openProfileByID(context, accountID, link);
			case HASHTAG -> Toast.makeText(context, "Not implemented yet", Toast.LENGTH_SHORT).show();
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
		HASHTAG
	}
}
