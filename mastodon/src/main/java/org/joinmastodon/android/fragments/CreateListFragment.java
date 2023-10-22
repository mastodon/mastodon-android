package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.CreateList;
import org.joinmastodon.android.api.requests.lists.UpdateList;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.FinishListCreationFragmentEvent;
import org.joinmastodon.android.events.ListCreatedEvent;
import org.joinmastodon.android.events.ListUpdatedEvent;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class CreateListFragment extends BaseEditListFragment{
	private Button nextButton;
	private View buttonBar;
	private FollowList followList;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.create_list);
		setSubtitle(getString(R.string.step_x_of_y, 1, 2));
		setLayout(R.layout.fragment_login);
		if(savedInstanceState!=null)
			followList=Parcels.unwrap(savedInstanceState.getParcelable("list"));
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	protected int getNavigationIconDrawableResource(){
		return R.drawable.ic_baseline_close_24;
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		nextButton=view.findViewById(R.id.btn_next);
		nextButton.setOnClickListener(this::onNextClick);
		nextButton.setText(R.string.create);
		buttonBar=view.findViewById(R.id.button_bar);
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	protected List<View> getViewsForElevationEffect(){
		return List.of(getToolbar(), buttonBar);
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putParcelable("list", Parcels.wrap(followList));
	}

	private void onNextClick(View v){
		String title=titleEdit.getText().toString().trim();
		if(TextUtils.isEmpty(title)){
			titleEditLayout.setErrorState(getString(R.string.required_form_field_blank));
			return;
		}
		if(followList==null){
			new CreateList(title, getSelectedRepliesPolicy(), exclusiveItem.checked)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(FollowList result){
							followList=result;
							proceed(false);
							E.post(new ListCreatedEvent(accountID, result));
							AccountSessionManager.get(accountID).getCacheController().addList(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, true)
					.exec(accountID);
		}else if(!title.equals(followList.title) || getSelectedRepliesPolicy()!=followList.repliesPolicy || exclusiveItem.checked!=followList.exclusive){
			new UpdateList(followList.id, title, getSelectedRepliesPolicy(), exclusiveItem.checked)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(FollowList result){
							followList=result;
							proceed(true);
							E.post(new ListUpdatedEvent(accountID, result));
							AccountSessionManager.get(accountID).getCacheController().updateList(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, true)
					.exec(accountID);
		}else{
			proceed(true);
		}
	}

	private void proceed(boolean needLoadMembers){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("list", Parcels.wrap(followList));
		args.putBoolean("needLoadMembers", needLoadMembers);
		Nav.go(getActivity(), CreateListAddMembersFragment.class, args);
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(contentView.getWindowToken(), 0);
	}

	@Subscribe
	public void onFinishListCreationFragment(FinishListCreationFragmentEvent ev){
		if(ev.accountID.equals(accountID) && followList!=null && ev.listID.equals(followList.id)){
			Nav.finish(this);
		}
	}
}
