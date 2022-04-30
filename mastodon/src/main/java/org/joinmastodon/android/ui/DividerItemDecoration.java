package org.joinmastodon.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.function.Predicate;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.V;

public class DividerItemDecoration extends RecyclerView.ItemDecoration{
	private Paint paint=new Paint();
	private int paddingStart, paddingEnd;
	private Predicate<RecyclerView.ViewHolder> drawDividerPredicate;
	private boolean drawBelowLastItem;

	public static final Predicate<RecyclerView.ViewHolder> NOT_FIRST=vh->vh.getAbsoluteAdapterPosition()>0;

	public DividerItemDecoration(Context context, @AttrRes int color, float thicknessDp, int paddingStartDp, int paddingEndDp){
		this(context, color, thicknessDp, paddingStartDp, paddingEndDp, null);
	}

	public DividerItemDecoration(Context context, @AttrRes int color, float thicknessDp, int paddingStartDp, int paddingEndDp, Predicate<RecyclerView.ViewHolder> drawDividerPredicate){
		paint.setColor(UiUtils.getThemeColor(context, color));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(V.dp(thicknessDp));
		paddingStart=V.dp(paddingStartDp);
		paddingEnd=V.dp(paddingEndDp);
		this.drawDividerPredicate=drawDividerPredicate;
	}

	public void setDrawBelowLastItem(boolean drawBelowLastItem){
		this.drawBelowLastItem=drawBelowLastItem;
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
			if((drawBelowLastItem || pos<totalItems-1) && (drawDividerPredicate==null || drawDividerPredicate.test(parent.getChildViewHolder(child)))){
				float y=Math.round(child.getY()+child.getHeight());
				y-=(y-paint.getStrokeWidth()/2f)%1f; // Make sure the line aligns with the pixel grid
				paint.setAlpha(Math.round(255f*child.getAlpha()));
				c.drawLine(padLeft+child.getX(), y, child.getX()+child.getWidth()-padRight, y, paint);
			}
		}
	}
}
