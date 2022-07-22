package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.joinmastodon.android.ui.PhotoLayoutHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.V;

public class ImageAttachmentFrameLayout extends FrameLayout{
	public static final int MAX_WIDTH=400; // dp

	private PhotoLayoutHelper.TiledLayoutResult tileLayout;
	private PhotoLayoutHelper.TiledLayoutResult.Tile tile;
	private int horizontalInset;

	public ImageAttachmentFrameLayout(@NonNull Context context){
		super(context);
	}

	public ImageAttachmentFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs){
		super(context, attrs);
	}

	public ImageAttachmentFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(isInEditMode()){
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		int w=Math.min(((View)getParent()).getMeasuredWidth(), V.dp(MAX_WIDTH))-horizontalInset;
		int actualHeight=Math.round(tile.height/1000f*w)+V.dp(1)*(tile.rowSpan-1);
		int actualWidth=Math.round(tile.width/1000f*w);
		if(tile.startCol+tile.colSpan<tileLayout.columnSizes.length)
			actualWidth-=V.dp(1);
		heightMeasureSpec=actualHeight | MeasureSpec.EXACTLY;
		widthMeasureSpec=actualWidth | MeasureSpec.EXACTLY;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	public void setLayout(PhotoLayoutHelper.TiledLayoutResult layout, PhotoLayoutHelper.TiledLayoutResult.Tile tile, int horizontalInset){
		tileLayout=layout;
		this.tile=tile;
		this.horizontalInset=horizontalInset;
	}
}
