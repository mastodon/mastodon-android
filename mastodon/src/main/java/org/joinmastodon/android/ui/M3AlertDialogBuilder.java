package org.joinmastodon.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;

import androidx.annotation.StringRes;
import me.grishka.appkit.utils.V;

public class M3AlertDialogBuilder extends AlertDialog.Builder{
	private CharSequence supportingText, title, helpText;
	private AlertDialog alert;

	public M3AlertDialogBuilder(Context context){
		super(context);
	}

	public M3AlertDialogBuilder(Context context, int themeResId){
		super(context, themeResId);
	}

	@Override
	public AlertDialog create(){
		if(!TextUtils.isEmpty(helpText) && !TextUtils.isEmpty(supportingText))
			throw new IllegalStateException("You can't have both help text and supporting text in the same alert");

		if(!TextUtils.isEmpty(supportingText)){
			View titleLayout=getContext().getSystemService(LayoutInflater.class).inflate(R.layout.alert_title_with_supporting_text, null);
			TextView title=titleLayout.findViewById(R.id.title);
			TextView subtitle=titleLayout.findViewById(R.id.subtitle);
			title.setText(this.title);
			subtitle.setText(supportingText);
			setCustomTitle(titleLayout);
		}else if(!TextUtils.isEmpty(helpText)){
			View titleLayout=getContext().getSystemService(LayoutInflater.class).inflate(R.layout.alert_title_with_help, null);
			TextView title=titleLayout.findViewById(R.id.title);
			TextView helpText=titleLayout.findViewById(R.id.help_text);
			View helpButton=titleLayout.findViewById(R.id.help);
			title.setText(this.title);
			helpText.setText(this.helpText);
			helpButton.setOnClickListener(v->{
				helpText.setVisibility(helpText.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE);
				helpButton.setSelected(helpText.getVisibility()==View.VISIBLE);
			});
			setCustomTitle(titleLayout);
		}

		alert=super.create();
		alert.create();
		Button btn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		if(btn!=null){
			View buttonBar=(View) btn.getParent();
			buttonBar.setPadding(V.dp(16), V.dp(16), V.dp(16), V.dp(16));
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
		return alert;
	}

	public M3AlertDialogBuilder setSupportingText(CharSequence text){
		supportingText=text;
		return this;
	}

	public M3AlertDialogBuilder setSupportingText(@StringRes int text){
		supportingText=getContext().getString(text);
		return this;
	}

	@Override
	public M3AlertDialogBuilder setTitle(CharSequence title){
		super.setTitle(title);
		this.title=title;
		return this;
	}

	@Override
	public M3AlertDialogBuilder setTitle(@StringRes int title){
		super.setTitle(title);
		this.title=getContext().getString(title);
		return this;
	}

	public M3AlertDialogBuilder setHelpText(CharSequence text){
		helpText=text;
		return this;
	}

	public M3AlertDialogBuilder setHelpText(@StringRes int text){
		helpText=getContext().getString(text);
		return this;
	}
}
