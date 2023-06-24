package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import me.grishka.appkit.fragments.OnBackPressedListener;

public class FilterContextFragment extends BaseSettingsFragment<FilterContext> implements OnBackPressedListener{
	private EnumSet<FilterContext> context;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_filter_context);
		context=(EnumSet<FilterContext>) getArguments().getSerializable("context");
		onDataLoaded(Arrays.stream(FilterContext.values()).map(c->{
			CheckableListItem<FilterContext> item=new CheckableListItem<>(c.getDisplayNameRes(), 0, CheckableListItem.Style.CHECKBOX, context.contains(c), null);
			item.parentObject=c;
			item.isEnabled=true;
			item.onClick=()->toggleCheckableItem(item);
			return item;
		}).collect(Collectors.toList()));
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	public boolean onBackPressed(){
		context=EnumSet.noneOf(FilterContext.class);
		for(ListItem<FilterContext> item:data){
			if(((CheckableListItem<FilterContext>) item).checked)
				context.add(item.parentObject);
		}
		Bundle args=new Bundle();
		args.putSerializable("context", context);
		setResult(true, args);
		return false;
	}
}
