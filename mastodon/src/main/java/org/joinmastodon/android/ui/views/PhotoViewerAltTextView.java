package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

import org.joinmastodon.android.R;

import me.grishka.appkit.utils.CustomViewHelper;

public class PhotoViewerAltTextView extends TextView implements CustomViewHelper{
	private String moreText;
	private Paint morePaint=new Paint(), clearPaint=new Paint();
	private Matrix matrix=new Matrix();
	private LinearGradient gradient, rtlGradient;

	public PhotoViewerAltTextView(Context context){
		this(context, null);
	}

	public PhotoViewerAltTextView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public PhotoViewerAltTextView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		moreText=context.getString(R.string.text_show_more).toUpperCase();
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
		gradient=new LinearGradient(0, 0, dp(56), 0, 0x00ffffff, 0xffffffff, Shader.TileMode.CLAMP);
		rtlGradient=new LinearGradient(0, 0, dp(56), 0, 0xffffffff, 0x00ffffff, Shader.TileMode.CLAMP);
		setLayerType(LAYER_TYPE_HARDWARE, null);
	}

	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		Layout layout=getLayout();
		if(layout.getLineCount()>=getMaxLines() && layout.getEllipsisCount(layout.getLineCount()-1)>0){
			int lastLine=layout.getLineCount()-1;
			morePaint.set(getPaint());
			morePaint.setTypeface(Typeface.DEFAULT_BOLD);
			float moreWidth=morePaint.measureText(moreText);
			int lineTop=layout.getLineTop(lastLine);
			int lineBottom=layout.getLineBottom(lastLine);
			int viewRight=getWidth()-getPaddingRight();
			int gradientWidth=dp(56);

			if(layout.getParagraphDirection(lastLine)==Layout.DIR_RIGHT_TO_LEFT){
				matrix.setTranslate(getPaddingLeft()+moreWidth, lineTop);
				rtlGradient.setLocalMatrix(matrix);
				clearPaint.setShader(rtlGradient);
				canvas.drawRect(getPaddingLeft(), lineTop, getPaddingLeft()+moreWidth+gradientWidth, lineBottom, clearPaint);
				canvas.drawText(moreText, getPaddingLeft(), layout.getLineBaseline(lastLine), morePaint);
			}else{
				matrix.setTranslate(viewRight-moreWidth-gradientWidth, lineTop);
				gradient.setLocalMatrix(matrix);
				clearPaint.setShader(gradient);
				canvas.drawRect(viewRight-moreWidth-gradientWidth, lineTop, viewRight, lineBottom, clearPaint);
				canvas.drawText(moreText, viewRight-moreWidth, layout.getLineBaseline(lastLine), morePaint);
			}
		}
	}
}
