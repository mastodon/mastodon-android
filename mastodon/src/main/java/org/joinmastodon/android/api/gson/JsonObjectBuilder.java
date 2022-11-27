package org.joinmastodon.android.api.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonObjectBuilder{
	private JsonObject obj=new JsonObject();

	public JsonObjectBuilder add(String key, JsonElement el){
		obj.add(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, String el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, Number el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, boolean el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, JsonObjectBuilder el){
		obj.add(key, el.build());
		return this;
	}

	public JsonObjectBuilder add(String key, JsonArrayBuilder el){
		obj.add(key, el.build());
		return this;
	}

	public JsonObject build(){
		return obj;
	}
}
