package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A LinearLayout for TextViews. First child TextView will get truncated if it doesn't fit, remaining will always wrap content.
 */
public class HeaderSubtitleLinearLayout extends LinearLayout {
    public HeaderSubtitleLinearLayout(Context context) {
        super(context);
    }

    public HeaderSubtitleLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderSubtitleLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutChildCount() > 1) {
            int remainingWidth = MeasureSpec.getSize(widthMeasureSpec);
            for (int i = 1; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (v.getVisibility() == GONE)
                    continue;
                v.measure(MeasureSpec.getSize(widthMeasureSpec) | MeasureSpec.AT_MOST, heightMeasureSpec);
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                remainingWidth -= v.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            }
            View first = getChildAt(0);
            if (first instanceof TextView) {
                ((TextView) first).setMaxWidth(remainingWidth);
            }
        } else {
            View first = getChildAt(0);
            if (first instanceof TextView) {
                ((TextView) first).setMaxWidth(Integer.MAX_VALUE);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int getLayoutChildCount() {
        int count = 0;
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getVisibility() != GONE)
                count++;
        }
        return count;
    }
}
