package org.joinmastodon.android.ui;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import me.grishka.appkit.utils.V;

/**
 * Created by grishka on 17.08.15.
 */
public abstract class PopupKeyboard{

	protected View keyboardPopupView;
	protected Activity activity;
	private int initialHeight;
	private int prevWidth;
	private int keyboardHeight;
	private boolean needShowOnHide=false;
	private boolean keyboardWasVisible=false;
	private OnIconChangeListener iconListener;

	public static final int ICON_HIDDEN=0;
	public static final int ICON_ARROW=1;
	public static final int ICON_KEYBOARD=2;

	public PopupKeyboard(Activity activity){
		this.activity=activity;
	}

	protected abstract View onCreateView();

	private void ensureView(){
		if(keyboardPopupView==null){
			keyboardPopupView=onCreateView();
			keyboardPopupView.setVisibility(View.GONE);
		}
	}

	public View getView(){
		ensureView();
		return keyboardPopupView;
	}

	public boolean isVisible(){
		ensureView();
		return keyboardPopupView.getVisibility()==View.VISIBLE;
	}

	public void toggleKeyboardPopup(View textField){
		ensureView();
		if(keyboardPopupView.getVisibility()==View.VISIBLE){
			if(keyboardWasVisible){
				keyboardWasVisible=false;
				InputMethodManager imm=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(textField, 0);
			}else{
				keyboardPopupView.setVisibility(View.GONE);
			}
			if(iconListener!=null)
				iconListener.onIconChanged(ICON_HIDDEN);
			return;
		}
		if(keyboardHeight>0){
			needShowOnHide=true;
			keyboardWasVisible=true;
			InputMethodManager imm=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
			if(iconListener!=null)
				iconListener.onIconChanged(ICON_KEYBOARD);
		}else{
			doShowKeyboardPopup();
			if(iconListener!=null)
				iconListener.onIconChanged(ICON_ARROW);
		}
	}

	protected Window getWindow(){
		return activity.getWindow();
	}

	public void setOnIconChangedListener(OnIconChangeListener l){
		iconListener=l;
	}

	public void onContentViewSizeChanged(int w, int h, int oldw, int oldh){
		if(oldw==0 || w!=prevWidth){
			initialHeight=h;
			prevWidth=w;
			onWidthChanged(w);
		}
		if(h>initialHeight){
			initialHeight=h;
		}
		if(initialHeight!=0 && w==oldw){
			keyboardHeight=initialHeight-h;
			if(keyboardHeight!=0){
				DisplayMetrics dm=activity.getResources().getDisplayMetrics();
				activity.getSharedPreferences("emoji", Context.MODE_PRIVATE).edit().putInt("kb_size"+dm.widthPixels+"_"+dm.heightPixels, keyboardHeight).commit();
			}
			if(needShowOnHide && keyboardHeight==0){
				((View)keyboardPopupView.getParent()).getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						((View)keyboardPopupView.getParent()).getViewTreeObserver().removeOnPreDrawListener(this);
						doShowKeyboardPopup();
						return false;
					}
				});
				needShowOnHide=false;
			}
			if(keyboardHeight>0 && keyboardPopupView.getVisibility()==View.VISIBLE){
				if(iconListener!=null)
					iconListener.onIconChanged(ICON_HIDDEN);
				((View)keyboardPopupView.getParent()).getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						((View)keyboardPopupView.getParent()).getViewTreeObserver().removeOnPreDrawListener(this);
						keyboardPopupView.setVisibility(View.GONE);
						return false;
					}
				});
			}
		}
	}

	public void hide(){
		ensureView();
		if(keyboardPopupView.getVisibility()==View.VISIBLE){
			keyboardPopupView.setVisibility(View.GONE);
			keyboardWasVisible=false;
			if(iconListener!=null)
				iconListener.onIconChanged(ICON_HIDDEN);
		}
	}

	public void onConfigurationChanged(){

	}

	protected void onWidthChanged(int w){

	}

	protected boolean needWrapContent(){
		return false;
	}

	private void doShowKeyboardPopup(){
		ensureView();
		DisplayMetrics dm=activity.getResources().getDisplayMetrics();
		int height=activity.getSharedPreferences("emoji", Context.MODE_PRIVATE).getInt("kb_size"+dm.widthPixels+"_"+dm.heightPixels, V.dp(200));
		if(needWrapContent()){
			keyboardPopupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.AT_MOST | height);
			height=keyboardPopupView.getMeasuredHeight();
		}
		keyboardPopupView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
		keyboardPopupView.setVisibility(View.VISIBLE);
	}

	public interface OnIconChangeListener{
		public void onIconChanged(int icon);
	}
}
