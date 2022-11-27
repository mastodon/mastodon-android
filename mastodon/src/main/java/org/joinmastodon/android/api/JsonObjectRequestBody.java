package org.joinmastodon.android.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class JsonObjectRequestBody extends RequestBody{
	private final Object obj;

	public JsonObjectRequestBody(Object obj){
		this.obj=obj;
	}

	@Override
	public MediaType contentType(){
		return MediaType.get("application/json;charset=utf-8");
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException{
		try{
			OutputStreamWriter writer=new OutputStreamWriter(sink.outputStream(), StandardCharsets.UTF_8);
			if(obj instanceof JsonElement)
				writer.write(obj.toString());
			else
				MastodonAPIController.gson.toJson(obj, writer);
			writer.flush();
		}catch(JsonIOException x){
			throw new IOException(x);
		}
	}
}
