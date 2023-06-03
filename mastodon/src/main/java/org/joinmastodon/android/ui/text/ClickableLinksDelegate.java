package org.joinmastodon.android.ui.text;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.Spanned;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.CustomViewHelper;

public class ClickableLinksDelegate implements CustomViewHelper{

	private final Paint hlPaint;
	private Path hlPath;
	private LinkSpan selectedSpan;
	private final TextView view;

	private final GestureDetector gestureDetector;

	public ClickableLinksDelegate(TextView view) {
		this.view=view;
		hlPaint=new Paint();
		hlPaint.setAntiAlias(true);
		hlPaint.setPathEffect(new CornerPathEffect(dp(3)));
		gestureDetector = new GestureDetector(view.getContext(), new LinkGestureListener(), view.getHandler());
	}

	public boolean onTouch(MotionEvent event) {
		if(event.getAction()==MotionEvent.ACTION_CANCEL){
			// the gestureDetector does not provide a callback for CANCEL, therefore:
			// remove background color of view before passing event to gestureDetector
			resetAndInvalidate();
		}
		return gestureDetector.onTouchEvent(event);
	}

	/**
	 * remove highlighting from span and let the system redraw the view
	 */
	private void resetAndInvalidate() {
		hlPath=null;
		selectedSpan=null;
		view.invalidate();
	}

	public void onDraw(Canvas canvas){
		if(hlPath!=null){
			canvas.save();
			canvas.translate(0, view.getPaddingTop());
			canvas.drawPath(hlPath, hlPaint);
			canvas.restore();
		}
	}

	@Override
	public Resources getResources(){
		return view.getResources();
	}

	/**
	 * GestureListener for spans that represent URLs.
	 * onDown: on start of touch event, set highlighting
	 * onSingleTapUp: when there was a (short) tap, call onClick and reset highlighting
	 * onLongPress: copy URL to clipboard, let user know, reset highlighting
	 */
	private class LinkGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(@NonNull MotionEvent event) {
			int line=-1;
			Rect rect=new Rect();
			Layout l=view.getLayout();
			for(int i=0;i<l.getLineCount();i++){
				view.getLineBounds(i, rect);
				if(rect.contains((int)event.getX(), (int)event.getY())){
					line=i;
					break;
				}
			}
			if(line==-1){
				return false;
			}
			CharSequence text=view.getText();
			if(text instanceof Spanned s){
				LinkSpan[] spans=s.getSpans(0, s.length()-1, LinkSpan.class);
				if(spans.length>0){
					for(LinkSpan span:spans){
						int start=s.getSpanStart(span);
						int end=s.getSpanEnd(span);
						int lstart=l.getLineForOffset(start);
						int lend=l.getLineForOffset(end);
						if(line>=lstart && line<=lend){
							boolean isRTL=l.getParagraphDirection(line)==-1;
							if(line==lstart && ((!isRTL && event.getX()-view.getPaddingLeft()<l.getPrimaryHorizontal(start)) || (isRTL && event.getX()-view.getPaddingLeft()>l.getPrimaryHorizontal(start)))){
								continue;
							}
							if(line==lend && ((!isRTL && event.getX()-view.getPaddingLeft()>l.getPrimaryHorizontal(end)) || (isRTL && event.getX()-view.getPaddingLeft()<l.getPrimaryHorizontal(end)))){
								continue;
							}
							hlPath=new Path();
							selectedSpan=span;
							hlPaint.setColor((span.getColor() & 0x00FFFFFF) | 0x33000000);
							//l.getSelectionPath(start, end, hlPath);
							for(int j=lstart;j<=lend;j++){
								Rect bounds=new Rect();
								l.getLineBounds(j, bounds);
								isRTL=l.getParagraphDirection(line)==-1;
								//bounds.left+=view.getPaddingLeft();
								if(j==lstart){
									int startOffset=Math.round(l.getPrimaryHorizontal(start));
									if(isRTL)
										bounds.right=startOffset;
									else
										bounds.left=startOffset;
								}
								if(j==lend){
									int endOffset=Math.round(l.getPrimaryHorizontal(end));
									if(isRTL)
										bounds.left=endOffset;
									else
										bounds.right=endOffset;
								}else{
									CharSequence lineChars=view.getText().subSequence(l.getLineStart(j), l.getLineEnd(j));
									bounds.right=Math.round(view.getPaint().measureText(lineChars.toString()))/*+view.getPaddingRight()*/;
								}
								bounds.inset(dp(-2), dp(-2));
								hlPath.addRect(new RectF(bounds), Path.Direction.CW);
							}
							hlPath.offset(view.getPaddingLeft(), 0);
							view.invalidate();
							return true;
						}
					}
				}
			}
			return super.onDown(event);
		}

		@Override
		public boolean onSingleTapUp(@NonNull MotionEvent event) {
			if(selectedSpan!=null){
				view.playSoundEffect(SoundEffectConstants.CLICK);
				selectedSpan.onClick(view.getContext());
				resetAndInvalidate();
				return true;
			}
			return false;
		}

		@Override
		public void onLongPress(@NonNull MotionEvent event) {
			//if target is not a link, don't copy
			if (selectedSpan == null) return;
			if (selectedSpan.getType() != LinkSpan.Type.URL) return;
			//copy link text to clipboard
			ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setPrimaryClip(ClipData.newPlainText("", selectedSpan.getLink()));
			//show toast, android from S_V2 on has built-in popup, as documented in
			//https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
				Toast.makeText(view.getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
			}
			//reset view
			resetAndInvalidate();
		}
	}
}
