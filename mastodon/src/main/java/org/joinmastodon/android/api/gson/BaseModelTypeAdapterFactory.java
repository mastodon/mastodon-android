package org.joinmastodon.android.api.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.joinmastodon.android.model.BaseModel;

import java.io.IOException;

public class BaseModelTypeAdapterFactory implements TypeAdapterFactory{
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type){
		if(!BaseModel.class.isAssignableFrom(type.getRawType())){
			return null;
		}

		final TypeAdapter<T> delegate=gson.getDelegateAdapter(this, type);
		return new TypeAdapter<>(){
			@Override
			public void write(JsonWriter out, T value) throws IOException{
				delegate.write(out, value);
			}

			@Override
			public T read(JsonReader in) throws IOException{
				T baseModel=delegate.read(in);
				if (baseModel != null)
					((BaseModel) baseModel).postprocess();
				return baseModel;
			}
		};
	}
}
