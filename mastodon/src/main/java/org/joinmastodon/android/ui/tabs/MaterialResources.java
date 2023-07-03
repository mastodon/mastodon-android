package org.joinmastodon.android.ui.tabs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

class MaterialResources {
    public static Drawable getDrawable(Context context, TypedArray a, int attr) {
        return a.getDrawable(attr);
    }

    public static ColorStateList getColorStateList(Context context, TypedArray a, int attr) {
        return a.getColorStateList(attr);
    }
}
