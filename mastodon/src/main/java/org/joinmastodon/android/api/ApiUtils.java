package org.joinmastodon.android.api;

import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.Response;

public class ApiUtils{
	private static final Pattern LINK_HEADER_PATTERN=Pattern.compile("(?:(?:,\\s*)?<([^>]+)>|;\\s*(\\w+)=['\"](\\w+)['\"])");

	private ApiUtils(){
		//no instance
	}

	public static <E extends Enum<E>> List<String> enumSetToStrings(EnumSet<E> e){
		return e.stream().map(ev->{
			try{
				SerializedName annotation=ev.getDeclaringClass().getField(ev.name()).getAnnotation(SerializedName.class);
				return annotation!=null ? annotation.value() : ev.name().toLowerCase();
			}catch(NoSuchFieldException x){
				throw new RuntimeException(x);
			}
		}).collect(Collectors.toList());
	}

	public static <E extends Enum<E>> String enumSetToCommaSeparatedString(EnumSet<E> e){
		return e.stream().map(ev->{
			try{
				SerializedName annotation=ev.getDeclaringClass().getField(ev.name()).getAnnotation(SerializedName.class);
				return annotation!=null ? annotation.value() : ev.name().toLowerCase();
			}catch(NoSuchFieldException x){
				throw new RuntimeException(x);
			}
		}).collect(Collectors.joining(","));
	}

	public static PaginationHeaders parsePaginationHeaders(Response httpResponse){
		String link=httpResponse.header("Link");
		Uri next=null, prev=null;
		if(!TextUtils.isEmpty(link)){
			Matcher matcher=LINK_HEADER_PATTERN.matcher(link);
			String url=null;
			while(matcher.find()){
				if(url==null){
					String _url=matcher.group(1);
					if(_url==null)
						continue;
					url=_url;
				}else{
					String paramName=matcher.group(2);
					String paramValue=matcher.group(3);
					if(paramName==null || paramValue==null)
						return new PaginationHeaders(next, prev);
					if("rel".equals(paramName)){
						switch(paramValue){
							case "next" -> next=Uri.parse(url);
							case "prev" -> prev=Uri.parse(url);
						}
						url=null;
					}
				}
			}
		}
		return new PaginationHeaders(next, prev);
	}
}
