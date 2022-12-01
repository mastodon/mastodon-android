package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class SplashLogoView extends ImageView{
	private Bitmap shadow;
	private Paint paint=new Paint();

	public SplashLogoView(Context context){
		this(context, null);
	}

	public SplashLogoView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public SplashLogoView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);

	}

	@Override
	protected void onDraw(Canvas canvas){
		if(shadow!=null){
			paint.setColor(0xBF000000);
			canvas.drawBitmap(shadow, 0, 0, paint);
		}
		super.onDraw(canvas);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		super.onSizeChanged(w, h, oldw, oldh);
		if(w!=oldw || h!=oldh)
			updateShadow();
	}

	@Override
	public void setImageDrawable(@Nullable Drawable drawable){
		super.setImageDrawable(drawable);
		updateShadow();
	}

	private void updateShadow(){
		int w=getWidth();
		int h=getHeight();
		Drawable drawable=getDrawable();
		if(w==0 || h==0 || drawable==null)
			return;
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		Bitmap temp=Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
		shadow=Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
		Canvas c=new Canvas(temp);
		c.translate(getWidth()/2f-drawable.getIntrinsicWidth()/2f, getHeight()/2f-drawable.getIntrinsicHeight()/2f);
		drawable.draw(c);
		c=new Canvas(shadow);
		Paint paint=new Paint();
		paint.setShadowLayer(V.dp(2), 0, 0, 0xff000000);
		c.drawBitmap(temp, 0, 0, paint);
	}
}
