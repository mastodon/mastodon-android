package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.profile.UpdateProfile;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.model.Profile;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Objects;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class ProfileEditTabSettingsFragment extends BaseSettingsFragment<Void>{
	private CheckableListItem<Void> mediaTabItem, mediaTabRepliesItem, featuredTabItem;
	private UpdateProfile currentSaveRequest;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.edit_profile_tab_settings);
		boolean[] toggles=Objects.requireNonNull((savedInstanceState==null ? getArguments() : savedInstanceState).getBooleanArray("toggles"));
		data.add(mediaTabItem=new CheckableListItem<>(R.string.edit_profile_show_media_tab, R.string.edit_profile_show_media_tab_description, CheckableListItem.Style.SWITCH, toggles[0], this::onMediaTabClick));
		mediaTabRepliesItem=new CheckableListItem<>(R.string.edit_profile_show_media_replies, 0, CheckableListItem.Style.SWITCH, toggles[1], this::onMediaRepliesClick);
		if(mediaTabItem.checked)
			data.add(mediaTabRepliesItem);
		else
			mediaTabItem.dividerAfter=true;
		mediaTabRepliesItem.dividerAfter=true;
		data.add(featuredTabItem=new CheckableListItem<>(R.string.edit_profile_show_featured_tab, R.string.edit_profile_show_featured_tab_description, CheckableListItem.Style.SWITCH, toggles[2], this::onFeaturedClick));
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

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putBooleanArray("toggles", new boolean[]{mediaTabItem.checked, mediaTabRepliesItem.checked, featuredTabItem.checked});
	}

	private void onMediaTabClick(ListItem<?> item){
		toggleCheckableItem(item);
		setIncludeRepliesItemVisible(mediaTabItem.checked);
		saveToServer();
	}

	private void onMediaRepliesClick(ListItem<?> item){
		toggleCheckableItem(item);
		saveToServer();
	}

	private void onFeaturedClick(ListItem<?> item){
		toggleCheckableItem(item);
		saveToServer();
	}

	private void saveToServer(){
		if(currentSaveRequest!=null)
			currentSaveRequest.cancel();
		currentSaveRequest=new UpdateProfile(mediaTabItem.checked, mediaTabRepliesItem.checked, featuredTabItem.checked);
		currentSaveRequest.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Profile result){
						currentSaveRequest=null;
						AccountSessionManager.getInstance().updateAccountProfile(accountID, result);
					}

					@Override
					public void onError(ErrorResponse error){
						currentSaveRequest=null;
						if(getActivity()==null || !(error instanceof MastodonErrorResponse me))
							return;
						final Snackbar[] sb={null};
						sb[0]=new Snackbar.Builder(getActivity())
								.setText(me.getErrorMessage())
								.setAction(R.string.retry, ()->{
									saveToServer();
									sb[0].dismiss();
								})
								.setPersistent()
								.create();
						sb[0].show();
					}
				})
				.exec(accountID);
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
