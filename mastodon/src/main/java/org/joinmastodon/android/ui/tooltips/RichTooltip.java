package org.joinmastodon.android.ui.tooltips;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.OutlineProviders;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.StringRes;

public class RichTooltip extends BaseRichTooltip{
	private CharSequence title;
	private CharSequence message;
	private List<CharSequence> buttonTitles;
	private List<DialogInterface.OnClickListener> buttonListeners;

	private TextView titleView, messageView;
	private Button button1, button2;

	public RichTooltip(Context context){
		super(context);
	}

	public RichTooltip setTitle(CharSequence title){
		this.title=title;
		return this;
	}

	public RichTooltip setTitle(@StringRes int title){
		this.title=context.getString(title);
		return this;
	}

	public RichTooltip setMessage(CharSequence message){
		this.message=message;
		return this;
	}

	public RichTooltip setMessage(@StringRes int message){
		this.message=context.getString(message);
		return this;
	}

	public RichTooltip addButton(CharSequence text, DialogInterface.OnClickListener listener){
		if(buttonTitles==null){
			buttonTitles=new ArrayList<>();
			buttonListeners=new ArrayList<>();
		}
		if(buttonTitles.size()==2)
			throw new IllegalArgumentException("2 buttons at most");
		buttonTitles.add(text);
		buttonListeners.add(listener);
		return this;
	}

	public RichTooltip addButton(@StringRes int text, DialogInterface.OnClickListener listener){
		return addButton(context.getString(text), listener);
	}

	@Override
	protected void initView(){
		contentView=LayoutInflater.from(context).inflate(R.layout.rich_tooltip, null);
		titleView=contentView.findViewById(R.id.title);
		messageView=contentView.findViewById(R.id.content);
		button1=contentView.findViewById(R.id.button1);
		button2=contentView.findViewById(R.id.button2);

		if(TextUtils.isEmpty(title)){
			titleView.setVisibility(View.GONE);
		}else{
			titleView.setText(title);
		}
		messageView.setText(message);

		if(buttonTitles==null || buttonTitles.isEmpty()){
			contentView.findViewById(R.id.button_bar).setVisibility(View.GONE);
		}else{
			button1.setText(buttonTitles.get(0));
			button1.setOnClickListener(v->onButtonClick(0));

			if(buttonTitles.size()==1){
				button2.setVisibility(View.GONE);
			}else{
				button2.setText(buttonTitles.get(1));
				button2.setOnClickListener(v->onButtonClick(1));
			}
		}
	}

	private void onButtonClick(int index){
		DialogInterface.OnClickListener cl=buttonListeners.get(index);
		if(cl!=null)
			cl.onClick(null, index);
		else
			dismiss();
	}

}
