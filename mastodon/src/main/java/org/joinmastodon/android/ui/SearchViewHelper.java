package org.joinmastodon.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.function.Consumer;

import me.grishka.appkit.utils.V;

public class SearchViewHelper{
	private LinearLayout searchLayout;
	private EditText searchEdit;
	private ImageButton clearSearchButton;
	private View divider;
	private String currentQuery;
	private Consumer<String> listener;
	private Runnable debouncer=()->{
		currentQuery=searchEdit.getText().toString();
		if(listener!=null){
			listener.accept(currentQuery);
		}
	};
	private boolean isEmpty=true;
	private Runnable enterCallback;
	private Consumer<String> listenerWithoutDebounce;

	public SearchViewHelper(Context context, Context toolbarContext, String hint){
		searchLayout=new LinearLayout(context);
		searchLayout.setOrientation(LinearLayout.HORIZONTAL);

		searchEdit=new EditText(context);
		searchEdit.setHint(hint);
		searchEdit.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		searchEdit.setBackground(null);
		searchEdit.addTextChangedListener(new SimpleTextWatcher(e->{
			searchEdit.removeCallbacks(debouncer);
			searchEdit.postDelayed(debouncer, 300);
			boolean newIsEmpty=e.length()==0;
			if(isEmpty!=newIsEmpty){
				isEmpty=newIsEmpty;
				V.setVisibilityAnimated(clearSearchButton, isEmpty ? View.INVISIBLE : View.VISIBLE);
			}
			if(listenerWithoutDebounce!=null)
				listenerWithoutDebounce.accept(e.toString());
		}));
		searchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		searchEdit.setOnEditorActionListener((v, actionId, event)->{
			searchEdit.removeCallbacks(debouncer);
			debouncer.run();
			if(enterCallback!=null)
				enterCallback.run();
			return true;
		});
		searchEdit.setTextAppearance(R.style.m3_body_large);
		searchEdit.setHintTextColor(UiUtils.getThemeColor(toolbarContext, R.attr.colorM3OnSurfaceVariant));
		searchEdit.setTextColor(UiUtils.getThemeColor(toolbarContext, R.attr.colorM3OnSurface));
		searchLayout.addView(searchEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

		clearSearchButton=new ImageButton(context);
		clearSearchButton.setImageResource(R.drawable.ic_baseline_close_24);
		clearSearchButton.setContentDescription(context.getString(R.string.clear));
		clearSearchButton.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(context, R.attr.colorM3OnSurfaceVariant)));
		clearSearchButton.setBackground(UiUtils.getThemeDrawable(toolbarContext, android.R.attr.actionBarItemBackground));
		clearSearchButton.setOnClickListener(v->{
			searchEdit.setText("");
			searchEdit.removeCallbacks(debouncer);
			debouncer.run();
		});
		clearSearchButton.setVisibility(View.INVISIBLE);
		searchLayout.addView(clearSearchButton, new LinearLayout.LayoutParams(V.dp(56), ViewGroup.LayoutParams.MATCH_PARENT));
	}

	public void setListeners(Consumer<String> listener, Consumer<String> listenerWithoutDebounce){
		this.listener=listener;
		this.listenerWithoutDebounce=listenerWithoutDebounce;
	}

	public void install(Toolbar toolbar){
		toolbar.getLayoutParams().height=V.dp(72);
		toolbar.setMinimumHeight(V.dp(72));
		if(searchLayout.getParent()!=null)
			((ViewGroup) searchLayout.getParent()).removeView(searchLayout);
		toolbar.addView(searchLayout, new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		toolbar.setBackgroundResource(R.drawable.bg_m3_surface3);
		searchEdit.requestFocus();
	}

	public void addDivider(ViewGroup contentView){
		divider=new View(contentView.getContext());
		divider.setBackgroundColor(UiUtils.getThemeColor(contentView.getContext(), R.attr.colorM3Outline));
		contentView.addView(divider, 1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1)));
	}

	public LinearLayout getSearchLayout(){
		return searchLayout;
	}

	public void setEnterCallback(Runnable enterCallback){
		this.enterCallback=enterCallback;
	}

	public void setQuery(String q){
		currentQuery=q;
		searchEdit.setText(currentQuery);
		searchEdit.setSelection(searchEdit.length());
		searchEdit.removeCallbacks(debouncer);
	}

	public String getQuery(){
		return currentQuery;
	}

	public View getDivider(){
		return divider;
	}

	public EditText getSearchEdit(){
		return searchEdit;
	}
}
