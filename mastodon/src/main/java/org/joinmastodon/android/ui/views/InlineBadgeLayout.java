package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.grishka.appkit.utils.CustomViewHelper;

/**
 * Displays a badge after the last line of a TextView, like an inline-block in CSS.
 * The TextView must be the first child. The badge may be the second child. There can't be more than 2 child views.
 */
public class InlineBadgeLayout extends ViewGroup implements CustomViewHelper{
	private int badgeX, badgeY;

	public InlineBadgeLayout(Context context){
		this(context, null);
	}

	public InlineBadgeLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public InlineBadgeLayout(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int count=getChildCount();
		if(count==0)
			return;
		if(count>2)
			throw new IllegalStateException("2 child views at most! There are "+count+" instead");
		if(!(getChildAt(0) instanceof TextView textView))
			throw new IllegalStateException("0th child view MUST be a TextView");
		measureChild(textView, widthMeasureSpec, heightMeasureSpec);
		View badge=count==2 ? getChildAt(1) : null;
		int extraHeight=0;
		if(badge!=null && badge.getVisibility()!=GONE){
			measureChild(badge, widthMeasureSpec, heightMeasureSpec);
			int pad=dp(4);
			Layout layout=textView.getLayout();
			int lastLine=layout.getLineCount()-1;
			int lastLineWidth=(int)layout.getLineWidth(lastLine);
			if(textView.getMeasuredWidth()-lastLineWidth<badge.getMeasuredWidth()+pad){
				badgeX=getLayoutDirection()==LAYOUT_DIRECTION_RTL ? textView.getMeasuredWidth()-badge.getMeasuredWidth() : 0;
				badgeY=textView.getMeasuredHeight()+pad;
				extraHeight=badge.getMeasuredHeight()+pad;
			}else{
				if(getLayoutDirection()==LAYOUT_DIRECTION_RTL && (textView.getGravity() & Gravity.START)!=0)
					badgeX=textView.getMeasuredWidth()-lastLineWidth-pad-badge.getMeasuredWidth();
				else
					badgeX=lastLineWidth+pad;
				badgeY=(layout.getLineBottom(lastLine)-layout.getLineTop(lastLine))/2-badge.getMeasuredHeight()/2+layout.getLineTop(lastLine);
			}
		}
		setMeasuredDimension(textView.getMeasuredWidth(), textView.getMeasuredHeight()+extraHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		int count=getChildCount();
		if(count==0)
			return;
		View textView=getChildAt(0);
		textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
		View badge=count==2 ? getChildAt(1) : null;
		if(badge!=null && badge.getVisibility()!=GONE){
			badge.layout(badgeX, badgeY, badgeX+badge.getMeasuredWidth(), badgeY+badge.getMeasuredHeight());
		}
	}
}
