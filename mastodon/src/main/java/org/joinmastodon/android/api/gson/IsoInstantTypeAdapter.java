package org.joinmastodon.android.api.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class IsoInstantTypeAdapter extends TypeAdapter<Instant>{
	@Override
	public void write(JsonWriter out, Instant value) throws IOException{
		if(value==null)
			out.nullValue();
		else
			out.value(DateTimeFormatter.ISO_INSTANT.format(value));
	}

	@Override
	public Instant read(JsonReader in) throws IOException{
		if(in.peek()==JsonToken.NULL){
			in.nextNull();
			return null;
		}
		try{
			return DateTimeFormatter.ISO_INSTANT.parse(in.nextString(), Instant::from);
		}catch(DateTimeParseException x){
			return null;
		}
	}
}
