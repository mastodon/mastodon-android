package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.OutlineProviders;

import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CustomViewHelper;

public class AvatarPileView extends LinearLayout implements CustomViewHelper{
	public final ImageView[] avatars=new ImageView[3];
	private final Paint borderPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF tmpRect=new RectF();

	public AvatarPileView(Context context){
		super(context);
		init();
	}

	public AvatarPileView(Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
		init();
	}

	public AvatarPileView(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init(){
		setLayerType(LAYER_TYPE_HARDWARE, null);
		setPaddingRelative(dp(16), 0, 0, 0);
		setClipToPadding(false);
		for(int i=0;i<avatars.length;i++){
			ImageView ava=new ImageView(getContext());
			ava.setScaleType(ImageView.ScaleType.CENTER_CROP);
			ava.setOutlineProvider(OutlineProviders.roundedRect(6));
			ava.setClipToOutline(true);
			ava.setImageResource(R.drawable.image_placeholder);
			ava.setPivotX(dp(16));
			ava.setPivotY(dp(32));
			ava.setRotation((avatars.length-1-i)*(-2f));
			LayoutParams lp=new LayoutParams(dp(32), dp(32));
			lp.gravity=Gravity.CENTER_VERTICAL;
			if(i<avatars.length-1)
				lp.setMarginEnd(dp(-16));
			addView(ava, lp);
			avatars[avatars.length-1-i]=ava;
		}
		borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	public void setVisibleAvatarCount(int count){
		for(int i=0;i<avatars.length;i++){
			avatars[i].setVisibility(i<count ? VISIBLE : INVISIBLE);
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime){
		tmpRect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
		tmpRect.offset(child.getTranslationX(), child.getTranslationY());
		tmpRect.inset(dp(-2), dp(-2));
		canvas.save();
		canvas.rotate(child.getRotation(), child.getLeft()+child.getPivotX(), child.getTop()+child.getPivotY());
		canvas.drawRoundRect(tmpRect, dp(8), dp(8), borderPaint);
		canvas.restore();
		return super.drawChild(canvas, child, drawingTime);
	}
}
