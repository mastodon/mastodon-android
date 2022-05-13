package org.joinmastodon.android.ui.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import org.joinmastodon.android.ui.text.ClickableLinksDelegate;
import org.joinmastodon.android.ui.text.DeleteWhenCopiedSpan;

public class LinkedTextView extends TextView{

	private ClickableLinksDelegate delegate=new ClickableLinksDelegate(this);
	private boolean needInvalidate;
	private ActionMode currentActionMode;

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
	}

	public boolean onTouchEvent(MotionEvent ev){
		if(delegate.onTouch(ev)) return true;
		return super.onTouchEvent(ev);
	}

	public void onDraw(Canvas c){
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
