package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.History;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

import me.grishka.appkit.utils.V;

public class HashtagChartView extends View{
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Path strokePath=new Path(), fillPath=new Path();
	private CornerPathEffect pathEffect=new CornerPathEffect(V.dp(3));
	private float[] relativeOffsets=new float[7];

	public HashtagChartView(Context context){
		this(context, null);
	}

	public HashtagChartView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public HashtagChartView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		paint.setStrokeWidth(V.dp(1.71f));
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
	}

	public void setData(List<History> data){
		int max=0;
		for(History h:data){
			max=Math.max(h.accounts, max);
		}
		if(relativeOffsets.length!=data.size())
			relativeOffsets=new float[data.size()];
		int i=0;
		for(History h:data){
			relativeOffsets[i]=(float)h.accounts/max;
			i++;
		}
		updatePath();
	}

	private void updatePath(){
		if(getWidth()<1)
			return;
		strokePath.rewind();
		fillPath.rewind();
		float step=(getWidth()-V.dp(2))/(float)(relativeOffsets.length-1);
		float maxH=getHeight()-V.dp(2);
		float x=getWidth()-V.dp(1);
		strokePath.moveTo(x, maxH-maxH*relativeOffsets[0]+V.dp(1));
		fillPath.moveTo(getWidth(), getHeight()-V.dp(1));
		fillPath.lineTo(x, maxH-maxH*relativeOffsets[0]+V.dp(1));
		for(int i=1;i<relativeOffsets.length;i++){
			float offset=relativeOffsets[i];
			x-=step;
			float y=maxH-maxH*offset+V.dp(1);
			strokePath.lineTo(x, y);
			fillPath.lineTo(x, y);
		}
		fillPath.lineTo(V.dp(1), getHeight()-V.dp(1));
		fillPath.close();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		super.onSizeChanged(w, h, oldw, oldh);
		updatePath();
	}

	@Override
	protected void onDraw(Canvas canvas){
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(UiUtils.getThemeColor(getContext(), R.attr.colorAccentLightest));
		paint.setPathEffect(null);
		canvas.drawPath(fillPath, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(UiUtils.getThemeColor(getContext(), android.R.attr.colorAccent));
		paint.setPathEffect(pathEffect);
		canvas.drawPath(strokePath, paint);
	}
}
