package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.StatusQuotePolicy;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.adapters.SpinnerListItemsAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class ComposerVisibilitySheet extends BottomSheet{
	private final Spinner visibilitySpinner;
	private final Spinner quotePolicySpinner;
	private final FloatingHintEditTextLayout visibilitySpinnerLayout;
	private final FloatingHintEditTextLayout quotePolicySpinnerLayout;
	private StatusQuotePolicy userSelectedPolicy;
	private final Listener listener;

	public ComposerVisibilitySheet(@NonNull Context context, StatusPrivacy defaultVisibility, StatusQuotePolicy defaultPolicy, boolean canChangeVisibility, StatusPrivacy maxVisibility, Listener listener){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_compose_visibility, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		Button cancelBtn=findViewById(R.id.cancel);
		Button saveBtn=findViewById(R.id.save);
		cancelBtn.setOnClickListener(v->dismiss());

		userSelectedPolicy=defaultPolicy;
		this.listener=listener;

		visibilitySpinnerLayout=findViewById(R.id.visibility_spinner_wrap);
		visibilitySpinner=findViewById(R.id.visibility_spinner);
		ArrayList<ListItem<StatusPrivacy>> items=new ArrayList<>();
		if(!maxVisibility.isLessVisibleThan(StatusPrivacy.PUBLIC))
			items.add(new ListItem<>(R.string.visibility_public, R.string.visibility_subtitle_public, R.drawable.ic_public_24px, StatusPrivacy.PUBLIC, null));
		if(!maxVisibility.isLessVisibleThan(StatusPrivacy.UNLISTED))
			items.add(new ListItem<>(R.string.visibility_unlisted, R.string.visibility_subtitle_unlisted, R.drawable.ic_clear_night_24px, StatusPrivacy.UNLISTED, null));
		items.add(new ListItem<>(R.string.visibility_followers_only, R.string.visibility_subtitle_followers, R.drawable.ic_lock_24px, StatusPrivacy.PRIVATE, null));
		items.add(new ListItem<>(R.string.visibility_private, R.string.visibility_subtitle_private, R.drawable.ic_alternate_email_24px, StatusPrivacy.DIRECT, null));
		for(ListItem<?> item:items)
			item.isEnabled=true;
		visibilitySpinner.setAdapter(new SpinnerListItemsAdapter<>(new GenericListItemsAdapter<>(items)));
		ViewGroup.MarginLayoutParams llp=(ViewGroup.MarginLayoutParams)visibilitySpinnerLayout.getLabel().getLayoutParams();
		llp.setMarginStart(llp.getMarginStart()+V.dp(16));
		for(int i=0;i<items.size();i++){
			if(items.get(i).parentObject==defaultVisibility){
				visibilitySpinner.setSelection(i);
				break;
			}
		}

		if(!canChangeVisibility){
			visibilitySpinner.setEnabled(false);
			visibilitySpinnerLayout.setAlpha(0.5f);
			visibilitySpinnerLayout.setErrorTextAsDescription(context.getString(R.string.edit_visibility_explanation));
		}

		quotePolicySpinnerLayout=findViewById(R.id.quote_policy_spinner_wrap);
		quotePolicySpinner=findViewById(R.id.quote_policy_spinner);
		ArrayAdapter<String> quotePolicyAdapter=new ArrayAdapter<>(context, R.layout.item_spinner, List.of(
				context.getString(R.string.quote_policy_public),
				context.getString(R.string.quote_policy_followers),
				context.getString(R.string.quote_policy_nobody)
		));
		quotePolicySpinner.setAdapter(quotePolicyAdapter);
		llp=(ViewGroup.MarginLayoutParams)quotePolicySpinnerLayout.getLabel().getLayoutParams();
		llp.setMarginStart(llp.getMarginStart()+V.dp(16));

		visibilitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
				updateQuotePolicyForVisibility(items.get(position).parentObject);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent){

			}
		});

		saveBtn.setOnClickListener(v->{
			if(listener.onSelected(this, items.get(visibilitySpinner.getSelectedItemPosition()).parentObject, StatusQuotePolicy.values()[quotePolicySpinner.getSelectedItemPosition()])){
				dismiss();
			}
		});

		updateQuotePolicyForVisibility(defaultVisibility); // Also sets initial value to quotePolicySpinner
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	}

	private void updateQuotePolicyForVisibility(StatusPrivacy visibility){
		switch(visibility){
			case PUBLIC -> {
				quotePolicySpinner.setEnabled(true);
				quotePolicySpinner.setSelection(userSelectedPolicy.ordinal());
				quotePolicySpinnerLayout.setAlpha(1);
				quotePolicySpinnerLayout.setErrorTextAsDescription(null);
			}
			case UNLISTED -> {
				quotePolicySpinner.setEnabled(true);
				quotePolicySpinner.setSelection(userSelectedPolicy.ordinal());
				quotePolicySpinnerLayout.setAlpha(1);
				quotePolicySpinnerLayout.setErrorTextAsDescription(getContext().getString(R.string.quote_policy_for_unlisted_explanation));
			}
			case PRIVATE -> {
				quotePolicySpinner.setEnabled(false);
				userSelectedPolicy=StatusQuotePolicy.values()[quotePolicySpinner.getSelectedItemPosition()];
				quotePolicySpinner.setSelection(StatusQuotePolicy.NOBODY.ordinal());
				quotePolicySpinnerLayout.setAlpha(0.5f);
				quotePolicySpinnerLayout.setErrorTextAsDescription(getContext().getString(R.string.quote_policy_for_private_explanation));
			}
			case DIRECT -> {
				quotePolicySpinner.setEnabled(false);
				userSelectedPolicy=StatusQuotePolicy.values()[quotePolicySpinner.getSelectedItemPosition()];
				quotePolicySpinner.setSelection(StatusQuotePolicy.NOBODY.ordinal());
				quotePolicySpinnerLayout.setAlpha(0.5f);
				quotePolicySpinnerLayout.setErrorTextAsDescription(getContext().getString(R.string.quote_policy_for_direct_explanation));
			}
		}
	}

	public interface Listener{
		boolean onSelected(ComposerVisibilitySheet sheet, StatusPrivacy visibility, StatusQuotePolicy policy);
	}
}
