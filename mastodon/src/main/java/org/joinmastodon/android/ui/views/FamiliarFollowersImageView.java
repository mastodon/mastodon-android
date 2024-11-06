package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.CustomViewHelper;

public class FamiliarFollowersImageView extends ImageView implements CustomViewHelper{
	private Paint clearPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Path path=new Path(), rectPath=new Path();

	public FamiliarFollowersImageView(Context context){
		this(context, null);
	}

	public FamiliarFollowersImageView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public FamiliarFollowersImageView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		UiUtils.setAllPaddings(this, 2);
		setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), getResources().getDisplayMetrics().density*8.5f);
			}
		});
		setClipToOutline(true);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	@Override
	protected void onDraw(Canvas canvas){
		float offset=dp(2);
		float radius=dp(6);
		rectPath.rewind();
		rectPath.addRoundRect(offset, offset, getWidth()-offset, getHeight()-offset, radius, radius, Path.Direction.CW);
		canvas.save();
		canvas.clipPath(rectPath); // Unless I do this, the corner pixels still end up dirty
		super.onDraw(canvas);
		canvas.restore();
		path.rewind();
		path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
		path.op(rectPath, Path.Op.DIFFERENCE);
		canvas.drawPath(path, clearPaint);
	}
}
