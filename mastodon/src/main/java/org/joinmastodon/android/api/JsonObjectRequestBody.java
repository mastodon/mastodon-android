package org.joinmastodon.android.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class JsonObjectRequestBody extends RequestBody{
	private final byte[] body;

	public JsonObjectRequestBody(Object obj) throws IOException{
		try{
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			if(obj instanceof JsonElement)
				writer.write(obj.toString());
			else
				MastodonAPIController.gson.toJson(obj, writer);
			writer.flush();
			body=out.toByteArray();
		}catch(JsonIOException x){
			throw new IOException(x);
		}
	}

	@Override
	public MediaType contentType(){
		return MediaType.get("application/json");
	}

	@Override
	public long contentLength() throws IOException{
		return body.length;
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException{
		sink.write(body);
	}
}
