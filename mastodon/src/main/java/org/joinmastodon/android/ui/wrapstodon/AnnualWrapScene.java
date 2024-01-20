package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

/**
 * Note: all scenes are rendered in a fixed-size viewport of 360*640dp, then scaled as necessary to fit the screen.
 * Works well on common phone screen sizes, might be kinda meh on tablets.
 */
public abstract class AnnualWrapScene{
	protected View contentView;
	protected String year;

	protected abstract View onCreateContentView(Context context);
	protected abstract void onDestroyContentView();

	public View createContentView(Context context){
		if(contentView!=null)
			return contentView;
		return contentView=onCreateContentView(context);
	}

	public void destroyContentView(){
		onDestroyContentView();
		contentView=null;
	}

	protected View getViewForScreenshot(){
		return contentView;
	}

	public Bitmap renderToBitmap(){
		if(contentView==null)
			throw new IllegalStateException();
		View v=getViewForScreenshot();
		Bitmap bmp=Bitmap.createBitmap(1080, Math.round(1080/(float)v.getWidth()*v.getHeight()), Bitmap.Config.ARGB_8888);
		Canvas c=new Canvas(bmp);
		c.drawColor(0xFF17063B);
		float scale=3f/v.getResources().getDisplayMetrics().density;
		c.scale(scale, scale, 0, 0);
		v.draw(c);
		return bmp;
	}

	public void setYear(String year){
		this.year=year;
	}

	protected CharSequence replaceBoldWithColor(CharSequence src, int color){
		Spannable ssb;
		if(src instanceof Spannable s){
			ssb=s;
		}else{
			ssb=new SpannableString(src);
		}

		StyleSpan[] spans=ssb.getSpans(0, ssb.length(), StyleSpan.class);
		for(StyleSpan span:spans){
			int start=ssb.getSpanStart(span);
			int end=ssb.getSpanEnd(span);
			ssb.removeSpan(span);
			ssb.setSpan(new ForegroundColorSpan(color), start, end, 0);
		}

		return ssb;
	}
}
