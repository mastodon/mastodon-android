package org.joinmastodon.android.ui.text;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.Layout;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;

import me.grishka.appkit.utils.V;

public class ClickableLinksDelegate {

	private Paint hlPaint;
	private Path hlPath;
	private LinkSpan selectedSpan;
	private TextView view;
	private final Handler longClickHandler = new Handler();

	private final Runnable copyTextToClipboard = () -> {
		//if target is not a link, don't copy
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
	};

	public ClickableLinksDelegate(TextView view) {
		this.view=view;
		hlPaint=new Paint();
		hlPaint.setAntiAlias(true);
		hlPaint.setPathEffect(new CornerPathEffect(V.dp(3)));
//        view.setHighlightColor(view.getResources().getColor(android.R.color.holo_blue_light));
	}

	public boolean onTouch(MotionEvent event) {
		if(event.getAction()==MotionEvent.ACTION_DOWN){
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
			if(text instanceof Spanned){
				Spanned s=(Spanned)text;
				LinkSpan[] spans=s.getSpans(0, s.length()-1, LinkSpan.class);
				if(spans.length>0){
					for(LinkSpan span:spans){
						int start=s.getSpanStart(span);
						int end=s.getSpanEnd(span);
						int lstart=l.getLineForOffset(start);
						int lend=l.getLineForOffset(end);
						if(line>=lstart && line<=lend){
							if(line==lstart && event.getX()-view.getPaddingLeft()<l.getPrimaryHorizontal(start)){
								continue;
							}
							if(line==lend && event.getX()-view.getPaddingLeft()>l.getPrimaryHorizontal(end)){
								continue;
							}
							hlPath=new Path();
							selectedSpan=span;
							longClickHandler.postDelayed(copyTextToClipboard, ViewConfiguration.getLongPressTimeout());
							hlPaint.setColor((span.getColor() & 0x00FFFFFF) | 0x33000000);
							//l.getSelectionPath(start, end, hlPath);
							for(int j=lstart;j<=lend;j++){
								Rect bounds=new Rect();
								l.getLineBounds(j, bounds);
								//bounds.left+=view.getPaddingLeft();
								if(j==lstart){
									bounds.left=Math.round(l.getPrimaryHorizontal(start));
								}
								if(j==lend){
									bounds.right=Math.round(l.getPrimaryHorizontal(end));
								}else{
									CharSequence lineChars=view.getText().subSequence(l.getLineStart(j), l.getLineEnd(j));
									bounds.right=Math.round(view.getPaint().measureText(lineChars.toString()))/*+view.getPaddingRight()*/;
								}
								bounds.inset(V.dp(-2), V.dp(-2));
								hlPath.addRect(new RectF(bounds), Path.Direction.CW);
							}
							hlPath.offset(view.getPaddingLeft(), 0);
							view.invalidate();
							return true;
						}
					}
				}
			}
		}
		if(event.getAction()==MotionEvent.ACTION_UP && selectedSpan!=null){
			longClickHandler.removeCallbacks(copyTextToClipboard);
			view.playSoundEffect(SoundEffectConstants.CLICK);
			selectedSpan.onClick(view.getContext());
			resetAndInvalidate();
			return false;
		}
		if(event.getAction()==MotionEvent.ACTION_CANCEL){
			resetAndInvalidate();
			return false;
		}
		return false;
	}

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

}
