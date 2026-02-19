package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class ProfileEditTabSettingsFragment extends BaseSettingsFragment<Void>{
	private CheckableListItem<Void> mediaTabItem, mediaTabRepliesItem, featuredTabItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.edit_profile_tab_settings);
		data.add(mediaTabItem=new CheckableListItem<>(R.string.edit_profile_show_media_tab, R.string.edit_profile_show_media_tab_description, CheckableListItem.Style.SWITCH, true, this::onMediaTabClick));
		data.add(mediaTabRepliesItem=new CheckableListItem<>(R.string.edit_profile_show_media_replies, 0, CheckableListItem.Style.SWITCH, false, this::onMediaRepliesClick));
		mediaTabRepliesItem.dividerAfter=true;
		data.add(featuredTabItem=new CheckableListItem<>(R.string.edit_profile_show_featured_tab, R.string.edit_profile_show_featured_tab_description, CheckableListItem.Style.SWITCH, true, this::toggleCheckableItem));
		mediaTabItem.checkedChangeListener=this::setIncludeRepliesItemVisible;
		dataLoaded();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(super.getAdapter());

		TextView explanation=new TextView(getActivity());
		explanation.setTextAppearance(R.style.m3_body_medium);
		explanation.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
		explanation.setText(getString(R.string.edit_profile_tabs_explanation, AccountSessionManager.get(accountID).domain));
		UiUtils.setAllPaddings(explanation, 16);
		adapter.addAdapter(new SingleViewRecyclerAdapter(explanation));

		return adapter;
	}

	private void onMediaTabClick(ListItem<?> item){
		toggleCheckableItem(item);
		setIncludeRepliesItemVisible(mediaTabItem.checked);
	}

	private void onMediaRepliesClick(ListItem<?> item){
		toggleCheckableItem(item);
	}

	private void setIncludeRepliesItemVisible(boolean visible){
		if(data.contains(mediaTabRepliesItem)==visible)
			return;
		mediaTabItem.dividerAfter=!visible;
		mediaTabRepliesItem.dividerAfter=visible;
		if(visible){
			data.add(1, mediaTabRepliesItem);
			itemsAdapter.notifyItemInserted(1);
		}else{
			data.remove(mediaTabRepliesItem);
			itemsAdapter.notifyItemRemoved(1);
		}
	}
}
