package org.joinmastodon.android.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import me.grishka.appkit.utils.V;

public class ComposeMediaLayout extends ViewGroup {
    private static final int MAX_WIDTH_DP = 400;
    private static final int GAP_DP = 8;
    private static final float ASPECT_RATIO = 0.5625f;

    public ComposeMediaLayout(Context context) {
        this(context, null);
    }

    public ComposeMediaLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComposeMediaLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        @SuppressLint("SwitchIntDef")
        int width = switch (mode) {
            case MeasureSpec.AT_MOST ->
                    Math.min(V.dp(MAX_WIDTH_DP), MeasureSpec.getSize(widthMeasureSpec));
            case MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec);
            default -> throw new IllegalArgumentException("unsupported measure mode");
        };
        int height = Math.round(width * ASPECT_RATIO);
        setMeasuredDimension(width, height);

        // We don't really need this, but some layouts will freak out if you don't measure them
        int childWidth, firstChildHeight, otherChildrenHeight = 0;
        int gap = V.dp(GAP_DP);
        switch (getChildCount()) {
            case 0 -> {
                return;
            }
            case 1 -> {
                childWidth = width;
                firstChildHeight = height;
            }
            case 2 -> {
                childWidth = (width - gap) / 2;
                firstChildHeight = otherChildrenHeight = height;
            }
            case 3 -> {
                childWidth = (width - gap) / 2;
                firstChildHeight = height;
                otherChildrenHeight = (height - gap) / 2;
            }
            default -> {
                childWidth = (width - gap) / 2;
                firstChildHeight = otherChildrenHeight = (height - gap) / 2;
            }
        }
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(childWidth | MeasureSpec.EXACTLY, (i == 0 ? firstChildHeight : otherChildrenHeight) | MeasureSpec.EXACTLY);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int gap = V.dp(GAP_DP);
        int width = r - l;
        int height = b - t;
        int halfWidth = (width - gap) / 2;
        int halfHeight = (height - gap) / 2;
        switch (getChildCount()) {
            case 0 -> {
            }
            case 1 -> getChildAt(0).layout(0, 0, width, height);
            case 2 -> {
                getChildAt(0).layout(0, 0, halfWidth, height);
                getChildAt(1).layout(halfWidth + gap, 0, width, height);
            }
            case 3 -> {
                getChildAt(0).layout(0, 0, halfWidth, height);
                getChildAt(1).layout(halfWidth + gap, 0, width, halfHeight);
                getChildAt(2).layout(halfWidth + gap, halfHeight + gap, width, height);
            }
            default -> {
                getChildAt(0).layout(0, 0, halfWidth, halfHeight);
                getChildAt(1).layout(halfWidth + gap, 0, width, halfHeight);
                getChildAt(2).layout(0, halfHeight + gap, halfWidth, height);
                getChildAt(3).layout(halfWidth + gap, halfHeight + gap, width, height);
            }
        }
    }
}
