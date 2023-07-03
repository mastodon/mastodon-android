package org.joinmastodon.android.api;

import com.google.gson.annotations.SerializedName;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class ApiUtils {
    private ApiUtils() {}

    public static <E extends Enum<E>> List<String> enumSetToStrings(EnumSet<E> e, Class<E> cls) {
        return e.stream().map(ev -> {
            try {
                SerializedName annotation = cls.getField(ev.name()).getAnnotation(SerializedName.class);
                return annotation != null ? annotation.value() : ev.name().toLowerCase();
            } catch (NoSuchFieldException x) {
                throw new RuntimeException(x);
            }
        }).collect(Collectors.toList());
    }
}
