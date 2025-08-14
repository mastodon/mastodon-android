package org.joinmastodon.android.ui.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.text.ClickableLinksDelegate;
import org.joinmastodon.android.ui.text.CodeBlockSpan;
import org.joinmastodon.android.ui.text.DeleteWhenCopiedSpan;
import org.joinmastodon.android.ui.text.MonospaceSpan;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.V;

public class LinkedTextView extends TextView{

	private ClickableLinksDelegate delegate=new ClickableLinksDelegate(this);
	private boolean needInvalidate;
	private ActionMode currentActionMode;
	private Paint bgPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private Path tmpPath=new Path();

	public LinkedTextView(Context context){
		this(context, null);
	}

	public LinkedTextView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public LinkedTextView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		setCustomSelectionActionModeCallback(new ActionMode.Callback(){
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu){
				currentActionMode=mode;
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu){
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item){
				onTextContextMenuItem(item.getItemId());
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode){
				currentActionMode=null;
			}
		});
		bgPaint.setColor(UiUtils.getThemeColor(context, R.attr.colorRichTextContainer));
		bgPaint.setPathEffect(new CornerPathEffect(V.dp(2)));
	}

	public boolean onTouchEvent(MotionEvent ev){
		if(delegate.onTouch(ev)) return true;
		return super.onTouchEvent(ev);
	}

	public void onDraw(Canvas c){
		if(getText() instanceof Spanned spanned){
			c.save();
			c.translate(getTotalPaddingLeft(), getTotalPaddingTop());
			Layout layout=getLayout();
			MonospaceSpan[] monospaceSpans=spanned.getSpans(0, spanned.length(), MonospaceSpan.class);
			for(MonospaceSpan span:monospaceSpans){
				int start=spanned.getSpanStart(span);
				int end=spanned.getSpanEnd(span);
				int startLine=layout.getLineForOffset(start);

				// Because text rendering in Android sucks, do this to handle spans that come immediately after a line break caused by word wrapping.
				// Otherwise, in addition to the correct part, the background will also be incorrectly drawn as a trailing thing on the previous line
				if(layout.getLineEnd(startLine)-1==start)
					start++;
				layout.getSelectionPath(start, end, tmpPath);
				c.drawPath(tmpPath, bgPaint);
			}
			CodeBlockSpan[] blockSpans=spanned.getSpans(0, spanned.length(), CodeBlockSpan.class);
			for(CodeBlockSpan span:blockSpans){
				c.drawRoundRect(V.dp(-4), layout.getLineTop(layout.getLineForOffset(spanned.getSpanStart(span)))-V.dp(8), layout.getWidth()+V.dp(4),
						layout.getLineBottom(layout.getLineForOffset(spanned.getSpanEnd(span)))+V.dp(4), V.dp(2), V.dp(2), bgPaint);
			}
			c.restore();
		}
		super.onDraw(c);
		delegate.onDraw(c);
		if(needInvalidate)
			invalidate();
	}

	// a hack to support animated emoji on <9.0
	public void setInvalidateOnEveryFrame(boolean invalidate){
		needInvalidate=invalidate;
		if(invalidate)
			invalidate();
	}

	@Override
	public boolean onTextContextMenuItem(int id){
		if(id==android.R.id.copy){
			final int selStart=getSelectionStart();
			final int selEnd=getSelectionEnd();
			int min=Math.max(0, Math.min(selStart, selEnd));
			int max=Math.max(0, Math.max(selStart, selEnd));
			final ClipData copyData=ClipData.newPlainText(null, deleteTextWithinDeleteSpans(getText().subSequence(min, max)));
			ClipboardManager clipboard=getContext().getSystemService(ClipboardManager.class);
			try {
				clipboard.setPrimaryClip(copyData);
			} catch (Throwable t) {
				Log.w("LinkedTextView", t);
			}
			if(currentActionMode!=null){
				currentActionMode.finish();
			}
			return true;
		}
		return super.onTextContextMenuItem(id);
	}

	private CharSequence deleteTextWithinDeleteSpans(CharSequence text){
		if(text instanceof Spanned spanned){
			DeleteWhenCopiedSpan[] delSpans=spanned.getSpans(0, text.length(), DeleteWhenCopiedSpan.class);
			if(delSpans.length>0){
				SpannableStringBuilder ssb=new SpannableStringBuilder(spanned);
				for(DeleteWhenCopiedSpan span:delSpans){
					int start=ssb.getSpanStart(span);
					int end=ssb.getSpanStart(span);
					if(start==-1)
						continue;
					ssb.delete(start, end+1);
				}
				return ssb;
			}
		}
		return text;
	}
}
