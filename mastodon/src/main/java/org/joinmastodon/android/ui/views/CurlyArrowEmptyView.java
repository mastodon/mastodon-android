package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.CustomViewHelper;

public class CurlyArrowEmptyView extends LinearLayout implements CustomViewHelper{
	private int pointGravity=Gravity.TOP | Gravity.END;
	private int pointOffsetX, pointOffsetY;
	private float startingPointX, startingPointY, endingPointX, endingPointY;
	private Path path=new Path(), arrowhead=new Path();
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Rect tmpRect1=new Rect(), tmpRect2=new Rect();
	private Matrix matrix=new Matrix();

	public CurlyArrowEmptyView(Context context){
		this(context, null);
	}

	public CurlyArrowEmptyView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CurlyArrowEmptyView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		setWillNotDraw(false);

		paint.setColor(UiUtils.getThemeColor(getContext(), R.attr.colorM3OutlineVariant));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(dp(3));
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);

		// 22x12
		arrowhead.moveTo(dp(-11), dp(12));
		arrowhead.lineTo(0, 0);
		arrowhead.lineTo(dp(11), dp(12));
	}

	@Override
	public void onViewAdded(View child){
		if(getChildCount()==1 && child instanceof TextView tv){
			tv.setShadowLayer(dp(5), 0, 0, UiUtils.getThemeColor(getContext(), R.attr.colorM3Surface));
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);
		updatePoints();
	}

	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		canvas.drawPath(path, paint);
	}

	private void updatePoints(){
		if(getChildCount()==0)
			return;
		View lastChild=getChildAt(getChildCount()-1);
		if(isRTL()){
			startingPointX=lastChild.getLeft()-dp(8);
		}else{
			startingPointX=lastChild.getRight()+dp(8);
		}
		startingPointY=lastChild.getTop()+lastChild.getHeight()/2f;

		tmpRect1.set(0, 0, getWidth(), getHeight());
		tmpRect1.inset(pointOffsetX, pointOffsetY);
		Gravity.apply(pointGravity, 2, 2, tmpRect1, tmpRect2, getLayoutDirection());
		endingPointX=tmpRect2.centerX();
		endingPointY=tmpRect2.centerY();
		updatePath();
	}

	public void setGravityAndOffsets(int gravity, int offsetX, int offsetY){
		pointGravity=gravity;
		pointOffsetX=dp(offsetX);
		pointOffsetY=dp(offsetY);
		updatePoints();
	}

	private boolean isRTL(){
		return getLayoutDirection()==LAYOUT_DIRECTION_RTL;
	}

	private void updatePath(){
		RectF src=new RectF(), dst=new RectF();
		dst.set(Math.min(startingPointX, endingPointX), Math.min(startingPointY, endingPointY), Math.max(startingPointX, endingPointX), Math.max(startingPointY, endingPointY));
		boolean isReversed=false;

		path.rewind();
		path.moveTo(0.92f,292.1f);
		path.cubicTo(53.42f,283.6f,71.67f,171.4f,46.42f,148.09f);
		path.cubicTo(33.42f,136.09f,16.92f,137.59f,16.92f,156.09f);
		path.cubicTo(16.92f,175.59f,36.66f,193.48f,55.39f,186.59f);
		path.cubicTo(100.7f,169.94f,104.24f,38.38f,93.92f,1.1f);
		if(dst.width()>dst.height()){
			path.computeBounds(src, false);
			matrix.setRotate(90, src.centerX(), src.centerY());
			matrix.postScale(-1f, 1f, src.centerX(), src.centerY());
			path.transform(matrix);
			isReversed=true;
		}
		PathMeasure pm=new PathMeasure(path, false);
		float[] pos=new float[2], tan=new float[2];
		pm.getPosTan(isReversed ? pm.getLength() : 0, pos, null);
		src.left=pos[0];
		src.bottom=pos[1];
		pm.getPosTan(isReversed ? 0 : pm.getLength(), pos, null);
		src.right=pos[0];
		src.top=pos[1];

		matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
		if(startingPointX>endingPointX)
			matrix.postScale(-1f, 1f, dst.centerX(), dst.centerY());
		if(startingPointY<endingPointY)
			matrix.postScale(1f, -1f, dst.centerX(), dst.centerY());
		path.transform(matrix);
		pm.setPath(path, false);
		pm.getPosTan(isReversed ? 0 : pm.getLength(), pos, tan);
		matrix.setTranslate(pos[0], pos[1]);
		float angle=(float)Math.toDegrees(Math.atan2(tan[1], tan[0]))+90f;
		if(isReversed)
			angle+=180f;
		matrix.postRotate(angle%360f, pos[0], pos[1]);
		path.addPath(arrowhead, matrix);
	}
}
