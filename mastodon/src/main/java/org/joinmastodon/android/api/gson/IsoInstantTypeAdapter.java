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
		String nextString;
		try {
			nextString = in.nextString();
		}catch(Exception e){
			return null;
		}

		try{
			return DateTimeFormatter.ISO_INSTANT.parse(nextString, Instant::from);
		}catch(DateTimeParseException x){}

		try{
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(nextString, Instant::from);
		}catch(DateTimeParseException x){}

		return null;
	}
}
