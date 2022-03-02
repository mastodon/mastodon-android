package org.joinmastodon.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.V;

public class DividerItemDecoration extends RecyclerView.ItemDecoration{
	private Paint paint=new Paint();
	private int paddingStart, paddingEnd;

	public DividerItemDecoration(Context context, @AttrRes int color, float thicknessDp, int paddingStartDp, int paddingEndDp){
		paint.setColor(UiUtils.getThemeColor(context, color));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(V.dp(thicknessDp));
		paddingStart=V.dp(paddingStartDp);
		paddingEnd=V.dp(paddingEndDp);
	}

	@Override
	public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
		boolean isRTL=parent.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
		int padLeft=isRTL ? paddingEnd : paddingStart;
		int padRight=isRTL ? paddingStart : paddingEnd;
		int totalItems=parent.getAdapter().getItemCount();
		for(int i=0;i<parent.getChildCount();i++){
			View child=parent.getChildAt(i);
			int pos=parent.getChildAdapterPosition(child);
			if(pos<totalItems-1){
				float y=Math.round(child.getY()+child.getHeight()-paint.getStrokeWidth()/2f);
				paint.setAlpha(Math.round(255f*child.getAlpha()));
				c.drawLine(padLeft+child.getX(), y, child.getX()+child.getWidth()-padRight, y, paint);
			}
		}
	}
}
