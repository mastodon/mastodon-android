package org.joinmastodon.android.fragments.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.FilterKeyword;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.V;

public class FilterWordsFragment extends BaseSettingsFragment<FilterKeyword> implements OnBackPressedListener{
	private ImageButton fab;
	private ActionMode actionMode;
	private ArrayList<ListItem<FilterKeyword>> selectedItems=new ArrayList<>();
	private ArrayList<String> deletedItemIDs=new ArrayList<>();
	private MenuItem deleteItem;

	public FilterWordsFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_filter_muted_words);
		onDataLoaded(getArguments().getParcelableArrayList("words").stream().map(p->{
			FilterKeyword word=Parcels.unwrap(p);
			ListItem<FilterKeyword> item=new ListItem<>(word.keyword, null, null, word);
			item.isEnabled=true;
			item.onClick=()->onWordClick(item);
			return item;
		}).collect(Collectors.toList()));
		setHasOptionsMenu(true);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	private void onWordClick(ListItem<FilterKeyword> item){
		showAlertForWord(item.parentObject);
	}

	private void onSelectionModeWordClick(CheckableListItem<FilterKeyword> item){
		if(selectedItems.remove(item)){
			item.checked=false;
		}else{
			item.checked=true;
			selectedItems.add(item);
		}
		rebindItem(item);
		updateActionModeTitle();
	}

	@Override
	public boolean onBackPressed(){
		Bundle result=new Bundle();
		result.putParcelableArrayList("words", (ArrayList<? extends Parcelable>) data.stream().map(i->i.parentObject).map(Parcels::wrap).collect(Collectors.toCollection(ArrayList::new)));
		result.putStringArrayList("deleted", deletedItemIDs);
		setResult(true, result);
		return false;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setImageResource(R.drawable.ic_add_24px);
		fab.setContentDescription(getString(R.string.add_muted_word));
		fab.setOnClickListener(v->onFabClick());
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		int fabInset=0;
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			fabInset=insets.getSystemWindowInsetBottom();
		}
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+fabInset;
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.settings_filter_words, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		enterSelectionMode(item.getItemId()==R.id.select_all);
		return true;
	}

	@Override
	public boolean wantsLightStatusBar(){
		if(actionMode!=null)
			return UiUtils.isDarkTheme();
		return super.wantsLightStatusBar();
	}

	private void onFabClick(){
		showAlertForWord(null);
	}

	private void showAlertForWord(FilterKeyword word){
		AlertDialog.Builder bldr=new M3AlertDialogBuilder(getActivity())
				.setHelpText(R.string.filter_add_word_help)
				.setTitle(word==null ? R.string.add_muted_word : R.string.edit_muted_word)
				.setNegativeButton(R.string.cancel, null);

		FloatingHintEditTextLayout editWrap=(FloatingHintEditTextLayout) bldr.getContext().getSystemService(LayoutInflater.class).inflate(R.layout.floating_hint_edit_text, null);
		EditText edit=editWrap.findViewById(R.id.edit);
		edit.setHint(R.string.filter_word_or_phrase);
		edit.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		editWrap.updateHint();
		bldr.setView(editWrap)
				.setPositiveButton(word==null ? R.string.add : R.string.save, null);

		if(word!=null){
			edit.setText(word.keyword);
			bldr.setNeutralButton(R.string.delete, null);
		}
		AlertDialog alert=bldr.show();
		if(word!=null){
			Button deleteBtn=alert.getButton(AlertDialog.BUTTON_NEUTRAL);
			deleteBtn.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
			deleteBtn.setOnClickListener(v->confirmDeleteWords(Collections.singletonList(word), alert::dismiss));
		}
		Button saveBtn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		saveBtn.setEnabled(false);
		saveBtn.setOnClickListener(v->{
			String input=edit.getText().toString();
			for(ListItem<FilterKeyword> item:data){
				if(item.parentObject.keyword.equalsIgnoreCase(input)){
					editWrap.setErrorState(getString(R.string.filter_word_already_in_list));
					return;
				}
			}
			if(word==null){
				FilterKeyword w=new FilterKeyword();
				w.wholeWord=true;
				w.keyword=input;
				ListItem<FilterKeyword> item=new ListItem<>(w.keyword, null, null, w);
				item.isEnabled=true;
				item.onClick=()->onWordClick(item);
				data.add(item);
				itemsAdapter.notifyItemInserted(data.size()-1);
			}else{
				word.keyword=input;
				word.wholeWord=true;
				for(ListItem<FilterKeyword> item:data){
					if(item.parentObject==word){
						rebindItem(item);
						break;
					}
				}
			}
			alert.dismiss();
		});
		edit.addTextChangedListener(new SimpleTextWatcher(e->saveBtn.setEnabled(e.length()>0)));
	}

	private void confirmDeleteWords(List<FilterKeyword> words, Runnable onConfirmed){
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(words.size()==1 ? getString(R.string.settings_delete_filter_word, words.get(0).keyword) : getResources().getQuantityString(R.plurals.settings_delete_x_filter_words, words.size(), words.size()))
//				.setMessage(R.string.settings_delete_filter_confirmation)
				.setPositiveButton(R.string.delete, (dlg, item)->{
					if(onConfirmed!=null)
						onConfirmed.run();
					removeWords(words);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
		alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
	}

	private void removeWords(List<FilterKeyword> words){
		ArrayList<Integer> indexes=new ArrayList<>();
		for(int i=0;i<data.size();i++){
			if(words.contains(data.get(i).parentObject)){
				indexes.add(0, i);
			}
		}
		for(int index:indexes){
			data.remove(index);
			itemsAdapter.notifyItemRemoved(index);
		}
		for(FilterKeyword w:words){
			if(w.id!=null)
				deletedItemIDs.add(w.id);
		}
	}

	private void enterSelectionMode(boolean selectAll){
		if(actionMode!=null)
			return;
		V.setVisibilityAnimated(fab, View.GONE);

		actionMode=getActivity().startActionMode(new ActionMode.Callback(){
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu){
				ObjectAnimator anim=ObjectAnimator.ofInt(getActivity().getWindow(), "statusBarColor", elevationOnScrollListener.getCurrentStatusBarColor(), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
				anim.setEvaluator(new IntEvaluator(){
					@Override
					public Integer evaluate(float fraction, Integer startValue, Integer endValue){
						return UiUtils.alphaBlendColors(startValue, endValue, fraction);
					}
				});
				anim.start();
				((FragmentStackActivity) getActivity()).invalidateSystemBarColors(FilterWordsFragment.this);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu){
				mode.getMenuInflater().inflate(R.menu.settings_filter_words_action_mode, menu);
				for(int i=0;i<menu.size();i++){
					Drawable icon=menu.getItem(i).getIcon().mutate();
					icon.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnPrimary));
					menu.getItem(i).setIcon(icon);
				}
				deleteItem=menu.findItem(R.id.delete);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item){
				if(item.getItemId()==R.id.delete){
					confirmDeleteWords(selectedItems.stream().map(i->i.parentObject).collect(Collectors.toList()), ()->leaveSelectionMode(false));
				}
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode){
				leaveSelectionMode(true);
				ObjectAnimator anim=ObjectAnimator.ofInt(getActivity().getWindow(), "statusBarColor", UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary), elevationOnScrollListener.getCurrentStatusBarColor());
				anim.setEvaluator(new IntEvaluator(){
					@Override
					public Integer evaluate(float fraction, Integer startValue, Integer endValue){
						return UiUtils.alphaBlendColors(startValue, endValue, fraction);
					}
				});
				anim.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						getActivity().getWindow().setStatusBarColor(0);
					}
				});
				anim.start();
				((FragmentStackActivity) getActivity()).invalidateSystemBarColors(FilterWordsFragment.this);
			}
		});

		selectedItems.clear();
		for(int i=0;i<data.size();i++){
			ListItem<FilterKeyword> item=data.get(i);
			CheckableListItem<FilterKeyword> newItem=new CheckableListItem<>(item.title, null, CheckableListItem.Style.CHECKBOX, selectAll, null);
			newItem.isEnabled=true;
			newItem.onClick=()->onSelectionModeWordClick(newItem);
			newItem.parentObject=item.parentObject;
			if(selectAll)
				selectedItems.add(newItem);
			data.set(i, newItem);
		}
		itemsAdapter.notifyItemRangeChanged(0, data.size());
		updateActionModeTitle();
	}

	private void leaveSelectionMode(boolean fromActionMode){
		if(actionMode==null)
			return;
		ActionMode actionMode=this.actionMode;
		this.actionMode=null;
		if(!fromActionMode)
			actionMode.finish();
		V.setVisibilityAnimated(fab, View.VISIBLE);
		selectedItems.clear();

		for(int i=0;i<data.size();i++){
			ListItem<FilterKeyword> item=data.get(i);
			ListItem<FilterKeyword> newItem=new ListItem<>(item.title, null, null);
			newItem.isEnabled=true;
			newItem.onClick=()->onWordClick(newItem);
			newItem.parentObject=item.parentObject;
			data.set(i, newItem);
		}
		itemsAdapter.notifyItemRangeChanged(0, data.size());
	}

	private void updateActionModeTitle(){
		actionMode.setTitle(getResources().getQuantityString(R.plurals.x_items_selected, selectedItems.size(), selectedItems.size()));
		deleteItem.setEnabled(!selectedItems.isEmpty());
	}
}
