package org.joinmastodon.android.api.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class JsonArrayBuilder{
	private JsonArray arr=new JsonArray();

	public JsonArrayBuilder add(JsonElement el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(String el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(Number el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(boolean el){
		arr.add(el);
		return this;
	}

	public JsonArrayBuilder add(JsonObjectBuilder el){
		arr.add(el.build());
		return this;
	}

	public JsonArrayBuilder add(JsonArrayBuilder el){
		arr.add(el.build());
		return this;
	}

	public JsonArray build(){
		return arr;
	}
}
