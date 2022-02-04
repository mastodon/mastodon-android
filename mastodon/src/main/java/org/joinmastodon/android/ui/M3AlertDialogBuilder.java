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
			buttonBar.setPadding(V.dp(16), V.dp(24), V.dp(16), V.dp(24));
			((View)buttonBar.getParent()).setPadding(0, 0, 0, 0);
		}
		return alert;
	}
}
