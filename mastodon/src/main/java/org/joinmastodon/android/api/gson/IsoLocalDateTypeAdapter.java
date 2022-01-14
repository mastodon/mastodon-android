package org.joinmastodon.android.api.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class IsoLocalDateTypeAdapter extends TypeAdapter<LocalDate>{
	@Override
	public void write(JsonWriter out, LocalDate value) throws IOException{
		if(value==null)
			out.nullValue();
		else
			out.value(value.toString());
	}

	@Override
	public LocalDate read(JsonReader in) throws IOException{
		if(in.peek()==JsonToken.NULL){
			in.nextNull();
			return null;
		}
		try{
			return LocalDate.parse(in.nextString());
		}catch(DateTimeParseException x){
			return null;
		}
	}
}
