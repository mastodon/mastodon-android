package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import org.joinmastodon.android.R;

/**
 * Software-rendering-friendly rounded-corners image view. Relies on arcane xrefmode magic.
 */
public class RoundedImageView extends ImageView{
	private int cornerRadius;
	private boolean roundBottomCorners=true;
	private Paint clearPaint=new Paint(Paint.ANTI_ALIAS_FLAG), paint=new Paint(Paint.ANTI_ALIAS_FLAG);

	public RoundedImageView(Context context){
		this(context, null);
	}

	public RoundedImageView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public RoundedImageView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView);
		cornerRadius=ta.getDimensionPixelOffset(R.styleable.RoundedImageView_cornerRadius, 0);
		roundBottomCorners=ta.getBoolean(R.styleable.RoundedImageView_roundBottomCorners, true);
		ta.recycle();
		setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight()+(roundBottomCorners ? 0 : cornerRadius), cornerRadius);
			}
		});
		setClipToOutline(true);
		clearPaint.setColor(0xFFFFFFFF);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		paint.setColor(0xFF0ff000);
	}

	public void setCornerRadius(int cornerRadius){
		this.cornerRadius=cornerRadius;
		invalidateOutline();
	}

	public void setRoundBottomCorners(boolean roundBottomCorners){
		this.roundBottomCorners=roundBottomCorners;
		invalidateOutline();
	}

	@Override
	public void draw(Canvas canvas){
		if(canvas.isHardwareAccelerated()){
			super.draw(canvas);
			return;
		}
		canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
		canvas.drawRoundRect(0, 0, getWidth(), getHeight()+(roundBottomCorners ? 0 : cornerRadius), cornerRadius, cornerRadius, paint);
		canvas.saveLayer(0, 0, getWidth(), getHeight(), clearPaint);
		super.draw(canvas);
		canvas.restore();
		canvas.restore();
	}
}
