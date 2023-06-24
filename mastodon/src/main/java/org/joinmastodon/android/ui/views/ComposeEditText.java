package org.joinmastodon.android.ui.views;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;
import android.widget.EditText;

import java.util.Objects;

import androidx.annotation.RequiresApi;

public class ComposeEditText extends EditText{
	private SelectionListener selectionListener;
	private InputConnection currentInputConnection;

	public ComposeEditText(Context context){
		super(context);
	}

	public ComposeEditText(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public ComposeEditText(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	public ComposeEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd){
		super.onSelectionChanged(selStart, selEnd);
		if(selectionListener!=null)
			selectionListener.onSelectionChanged(selStart, selEnd);
	}

	public void setSelectionListener(SelectionListener selectionListener){
		this.selectionListener=selectionListener;
	}

	public InputConnection getCurrentInputConnection(){
		return currentInputConnection;
	}

	// Support receiving images from keyboards
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs){
		final InputConnection ic=super.onCreateInputConnection(outAttrs);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N_MR1){
			outAttrs.contentMimeTypes=selectionListener.onGetAllowedMediaMimeTypes();
			return currentInputConnection=new MediaAcceptingInputConnection(ic);
		}
		return currentInputConnection=ic;
	}

	// Support pasting images
	@Override
	public boolean onTextContextMenuItem(int id){
		if(id==android.R.id.paste){
			ClipboardManager clipboard=getContext().getSystemService(ClipboardManager.class);
			ClipData clip=clipboard.getPrimaryClip();
			if(processClipData(clip))
				return true;
		}
		return super.onTextContextMenuItem(id);
	}

	// Support drag-and-dropping images in multiwindow mode
	@Override
	public boolean onDragEvent(DragEvent event){
		if(event.getAction()==DragEvent.ACTION_DROP && Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			if(((Activity) getContext()).requestDragAndDropPermissions(event)!=null)
				return processClipData(event.getClipData());
		}
		return super.onDragEvent(event);
	}

	private boolean processClipData(ClipData clip){
		if(clip==null)
			return false;
		boolean processedAny=false;
		for(int i=0;i<clip.getItemCount();i++){
			Uri uri=clip.getItemAt(i).getUri();
			if(uri!=null){
				processedAny=true;
				selectionListener.onAddMediaAttachmentFromEditText(uri, Objects.toString(clip.getItemAt(i).getText(), null));
			}
		}
		return processedAny;
	}

	public interface SelectionListener{
		void onSelectionChanged(int start, int end);
		String[] onGetAllowedMediaMimeTypes();
		boolean onAddMediaAttachmentFromEditText(Uri uri, String description);
	}

	private class MediaAcceptingInputConnection extends InputConnectionWrapper{
		public MediaAcceptingInputConnection(InputConnection conn){
			super(conn, false);
		}

		@RequiresApi(api=Build.VERSION_CODES.N_MR1)
		@Override
		public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts){
			Uri contentUri=inputContentInfo.getContentUri();
			if(contentUri==null)
				return false;
			inputContentInfo.requestPermission();
			return selectionListener.onAddMediaAttachmentFromEditText(contentUri, Objects.toString(inputContentInfo.getDescription().getLabel(), null));
		}
	}
}
