package org.joinmastodon.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import me.grishka.appkit.utils.V;

public class M3AlertDialogBuilder extends AlertDialog.Builder{
	public M3AlertDialogBuilder(Context context){
		super(context);
	}

	public M3AlertDialogBuilder(Context context, int themeResId){
		super(context, themeResId);
	}

	@Override
	public AlertDialog create(){
		AlertDialog alert=super.create();
		alert.create();
		Button btn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		if(btn!=null){
			View buttonBar=(View) btn.getParent();
			buttonBar.setPadding(V.dp(16), 0, V.dp(16), V.dp(24));
			((View)buttonBar.getParent()).setPadding(0, 0, 0, 0);
		}
		// hacc
		int titleID=getContext().getResources().getIdentifier("title_template", "id", "android");
		if(titleID!=0){
			View title=alert.findViewById(titleID);
			if(title!=null){
				int pad=V.dp(24);
				title.setPadding(pad, pad, pad, pad);
			}
		}
		int titleDividerID=getContext().getResources().getIdentifier("titleDividerNoCustom", "id", "android");
		if(titleDividerID!=0){
			View divider=alert.findViewById(titleDividerID);
			if(divider!=null){
				divider.getLayoutParams().height=0;
			}
		}
		int scrollViewID=getContext().getResources().getIdentifier("scrollView", "id", "android");
		if(scrollViewID!=0){
			View scrollView=alert.findViewById(scrollViewID);
			if(scrollView!=null){
				scrollView.setPadding(0, 0, 0, 0);
			}
		}
		int messageID=getContext().getResources().getIdentifier("message", "id", "android");
		if(messageID!=0){
			View message=alert.findViewById(messageID);
			if(message!=null){
				message.setPadding(message.getPaddingLeft(), message.getPaddingTop(), message.getPaddingRight(), V.dp(24));
			}
		}
		return alert;
	}
}
