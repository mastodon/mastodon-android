package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.filters.GetFilters;
import org.joinmastodon.android.events.SettingsFilterCreatedOrUpdatedEvent;
import org.joinmastodon.android.events.SettingsFilterDeletedEvent;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class SettingsFiltersFragment extends BaseSettingsFragment<Filter>{
	private Button fab;

	public SettingsFiltersFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_text_fab);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_filters);
		loadData();
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetFilters()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Filter> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.stream().map(f->makeListItem(f)).collect(Collectors.toList()));
					}
				})
				.exec(accountID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setText(R.string.settings_add_filter);
		fab.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add_24px, 0, 0, 0);
		fab.setOnClickListener(v->onAddFilterClick());
	}

	private void onFilterClick(ListItem<Filter> filter){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("filter", Parcels.wrap(filter.parentObject));
		Nav.go(getActivity(), EditFilterFragment.class, args);
	}

	private void onAddFilterClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), EditFilterFragment.class, args);
	}

	private ListItem<Filter> makeListItem(Filter f){
		ListItem<Filter> item=new ListItem<>(f.title, getString(f.isActive() ? R.string.filter_active : R.string.filter_inactive), this::onFilterClick, f);
		return item;
	}

	@Subscribe
	public void onFilterDeleted(SettingsFilterDeletedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(int i=0;i<data.size();i++){
			if(data.get(i).parentObject.id.equals(ev.filterID)){
				data.remove(i);
				itemsAdapter.notifyItemRemoved(i);
				break;
			}
		}
	}

	@Subscribe
	public void onFilterCreatedOrUpdated(SettingsFilterCreatedOrUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(ListItem<Filter> item:data){
			if(item.parentObject.id.equals(ev.filter.id)){
				item.parentObject=ev.filter;
				item.title=ev.filter.title;
				item.subtitle=getString(ev.filter.isActive() ? R.string.filter_active : R.string.filter_inactive);
				rebindItem(item);
				return;
			}
		}
		data.add(makeListItem(ev.filter));
		itemsAdapter.notifyItemInserted(data.size()-1);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		UiUtils.applyBottomInsetToFAB(fab, insets);
		super.onApplyWindowInsets(insets);
	}
}
