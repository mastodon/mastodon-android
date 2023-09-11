package org.joinmastodon.android.api.gson;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OptionalItemListDeserializer implements JsonDeserializer<List<?>> {

    private static final String TAG = "OptionalBaseModelListTypeAdapterFactory";

    @Override
    public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Type valueType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];

        List<Object> list = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray()) {
            try {
                list.add(context.deserialize(item, valueType));
            } catch (JsonParseException e) {
                Log.w(TAG, "Ignoring invalid object from list", e);
            }
        }
        return list;
    }
}
