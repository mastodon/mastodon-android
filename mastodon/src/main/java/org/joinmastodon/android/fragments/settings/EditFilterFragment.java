package org.joinmastodon.android.fragments.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.filters.CreateFilter;
import org.joinmastodon.android.api.requests.filters.DeleteFilter;
import org.joinmastodon.android.api.requests.filters.UpdateFilter;
import org.joinmastodon.android.events.SettingsFilterCreatedOrUpdatedEvent;
import org.joinmastodon.android.events.SettingsFilterDeletedEvent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterKeyword;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.parceler.Parcels;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class EditFilterFragment extends BaseSettingsFragment<Void> implements OnBackPressedListener{
	private static final int WORDS_RESULT=370;
	private static final int CONTEXT_RESULT=651;

	private Filter filter;
	private ListItem<Void> durationItem, wordsItem, contextItem;
	private CheckableListItem<Void> cwItem;
	private FloatingHintEditTextLayout titleEditLayout;
	private EditText titleEdit;

	private Instant endsAt;
	private ArrayList<FilterKeyword> keywords=new ArrayList<>();
	private ArrayList<String> deletedWordIDs=new ArrayList<>();
	private EnumSet<FilterContext> context=EnumSet.allOf(FilterContext.class);
	private boolean dirty;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		filter=Parcels.unwrap(getArguments().getParcelable("filter"));
		setTitle(filter==null ? R.string.settings_add_filter : R.string.settings_edit_filter);
		onDataLoaded(List.of(
				durationItem=new ListItem<>(R.string.settings_filter_duration, 0, this::onDurationClick),
				wordsItem=new ListItem<>(R.string.settings_filter_muted_words, 0, this::onWordsClick),
				contextItem=new ListItem<>(R.string.settings_filter_context, 0, this::onContextClick),
				cwItem=new CheckableListItem<>(R.string.settings_filter_show_cw, R.string.settings_filter_show_cw_explanation, CheckableListItem.Style.SWITCH, filter==null || filter.filterAction==FilterAction.WARN, ()->toggleCheckableItem(cwItem))
		));

		if(filter!=null){
			endsAt=filter.expiresAt;
			keywords.addAll(filter.keywords);
			context=filter.context;
			data.add(new ListItem<>(R.string.settings_delete_filter, 0, this::onDeleteClick, R.attr.colorM3Error, false));
		}

		updateDurationItem();
		updateWordsItem();
		updateContextItem();
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		titleEditLayout=(FloatingHintEditTextLayout) getActivity().getLayoutInflater().inflate(R.layout.floating_hint_edit_text, list, false);
		titleEdit=titleEditLayout.findViewById(R.id.edit);
		titleEdit.setHint(R.string.settings_filter_title);
		titleEditLayout.updateHint();
		if(filter!=null)
			titleEdit.setText(filter.title);

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(titleEditLayout));
		adapter.addAdapter(super.getAdapter());
		return adapter;
	}

	@Override
	protected int indexOfItemsAdapter(){
		return 1;
	}

	private void onDurationClick(){
		int[] durationOptions={
				1800,
				3600,
				12*3600,
				24*3600,
				3*24*3600,
				7*24*3600
		};
		ArrayList<String> options=Arrays.stream(durationOptions).mapToObj(d->UiUtils.formatDuration(getActivity(), d)).collect(Collectors.toCollection(ArrayList<String>::new));
		options.add(0, getString(R.string.filter_duration_forever));
		options.add(getString(R.string.filter_duration_custom));
		Instant[] newEnd={null};
		boolean[] isCustom={false};
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_filter_duration_title)
				.setSupportingText(endsAt==null ? null : getString(R.string.settings_filter_ends, UiUtils.formatRelativeTimestampAsMinutesAgo(getActivity(), endsAt, false)))
				.setSingleChoiceItems(options.toArray(new String[0]), -1, (dlg, item)->{
					AlertDialog a=(AlertDialog) dlg;
					if(item==options.size()-1){ // custom
						showCustomDurationAlert(isCustom[0] ? newEnd[0] : null, date->{
							if(date==null){
								a.getListView().setItemChecked(item, false);
							}else{
								isCustom[0]=true;
								newEnd[0]=date;
								a.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
							}
						});
					}else{
						isCustom[0]=false;
						if(item==0){
							newEnd[0]=null;
						}else{
							newEnd[0]=Instant.now().plusSeconds(durationOptions[item-1]);
						}
						a.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}
				})
				.setPositiveButton(R.string.ok, (dlg, item)->{
					if(!Objects.equals(endsAt, newEnd[0])){
						endsAt=newEnd[0];
						updateDurationItem();
						dirty=true;
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
		alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
	}

	private void showCustomDurationAlert(Instant currentValue, Consumer<Instant> callback){
		DatePicker picker=new DatePicker(getActivity());
		picker.setMinDate(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000L);
		if(currentValue!=null){
			ZonedDateTime dt=currentValue.atZone(ZoneId.systemDefault());
			picker.updateDate(dt.getYear(), dt.getMonthValue()-1, dt.getDayOfMonth());
		}
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setView(picker)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					((AlertDialog)dlg).setOnDismissListener(null);
					callback.accept(LocalDate.of(picker.getYear(), picker.getMonth()+1, picker.getDayOfMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant());
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
		alert.setOnDismissListener(dialog->callback.accept(null));
	}

	private void onWordsClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelableArrayList("words", (ArrayList<? extends Parcelable>) keywords.stream().map(Parcels::wrap).collect(Collectors.toCollection(ArrayList::new)));
		Nav.goForResult(getActivity(), FilterWordsFragment.class, args, WORDS_RESULT, this);
	}

	private void onContextClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putSerializable("context", context);
		Nav.goForResult(getActivity(), FilterContextFragment.class, args, CONTEXT_RESULT, this);
	}

	private void onDeleteClick(){
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(getString(R.string.settings_delete_filter_title, filter.title))
				.setMessage(R.string.settings_delete_filter_confirmation)
				.setPositiveButton(R.string.delete, (dlg, item)->deleteFilter())
				.setNegativeButton(R.string.cancel, null)
				.show();
		alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
	}

	private void updateDurationItem(){
		if(endsAt==null){
			durationItem.subtitle=getString(R.string.filter_duration_forever);
		}else{
			durationItem.subtitle=getString(R.string.settings_filter_ends, UiUtils.formatRelativeTimestampAsMinutesAgo(getActivity(), endsAt, false));
		}
		rebindItem(durationItem);
	}

	private void updateWordsItem(){
		wordsItem.subtitle=getResources().getQuantityString(R.plurals.settings_x_muted_words, keywords.size(), keywords.size());
		rebindItem(wordsItem);
	}

	private void updateContextItem(){
		List<String> values=context.stream().map(c->getString(c.getDisplayNameRes())).collect(Collectors.toList());
		contextItem.subtitle=switch(values.size()){
			case 0 -> null;
			case 1 -> values.get(0);
			case 2 -> getString(R.string.selection_2_options, values.get(0), values.get(1));
			case 3 -> getString(R.string.selection_3_options, values.get(0), values.get(1), values.get(2));
			default -> getString(R.string.selection_4_or_more, values.get(0), values.get(1), values.size()-2);
		};
		rebindItem(contextItem);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.settings_edit_filter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==R.id.save){
			saveFilter();
		}
		return true;
	}

	private void saveFilter(){
		if(titleEdit.length()==0){
			titleEditLayout.setErrorState(getString(R.string.required_form_field_blank));
			return;
		}
		MastodonAPIRequest<Filter> req;
		if(filter==null){
			req=new CreateFilter(titleEdit.getText().toString(), context, cwItem.checked ? FilterAction.WARN : FilterAction.HIDE, endsAt==null ? 0 : (int)(endsAt.getEpochSecond()-Instant.now().getEpochSecond()), keywords);
		}else{
			req=new UpdateFilter(filter.id, titleEdit.getText().toString(), context, cwItem.checked ? FilterAction.WARN : FilterAction.HIDE, endsAt==null ? 0 : (int)(endsAt.getEpochSecond()-Instant.now().getEpochSecond()), keywords, deletedWordIDs);
		}
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Filter result){
						E.post(new SettingsFilterCreatedOrUpdatedEvent(accountID, result));
						Nav.finish(EditFilterFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.saving, true)
				.exec(accountID);
	}

	private void deleteFilter(){
		new DeleteFilter(filter.id)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Void result){
						E.post(new SettingsFilterDeletedEvent(accountID, filter.id));
						Nav.finish(EditFilterFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.deleting, false)
				.exec(accountID);
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		if(success){
			if(reqCode==CONTEXT_RESULT){
				EnumSet<FilterContext> context=(EnumSet<FilterContext>) result.getSerializable("context");
				if(!context.equals(this.context)){
					this.context=context;
					dirty=true;
					updateContextItem();
				}
			}else if(reqCode==WORDS_RESULT){
				ArrayList<FilterKeyword> old=new ArrayList<>(keywords);
				keywords.clear();
				result.getParcelableArrayList("words").stream().map(p->(FilterKeyword)Parcels.unwrap(p)).forEach(keywords::add);
				if(!old.equals(keywords)){
					dirty=true;
					updateWordsItem();
				}
				deletedWordIDs.addAll(result.getStringArrayList("deleted"));
			}
		}
	}

	private boolean isDirty(){
		return dirty || (filter!=null && !titleEdit.getText().toString().equals(filter.title)) || (filter!=null && (filter.filterAction==FilterAction.WARN)!=cwItem.checked);
	}

	@Override
	public boolean onBackPressed(){
		if(isDirty()){
			UiUtils.showConfirmationAlert(getActivity(), R.string.discard_changes, 0, R.string.discard, ()->Nav.finish(this));
			return true;
		}
		return false;
	}
}
