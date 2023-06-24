package org.joinmastodon.android.fragments.onboarding;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetFollowSuggestions;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.fragments.account_list.BaseAccountListFragment;
import org.joinmastodon.android.model.FollowSuggestion;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class OnboardingFollowSuggestionsFragment extends BaseAccountListFragment{
	private String accountID;
	private View buttonBar;
	private ElevationOnScrollListener onScrollListener;
	private int numRunningFollowRequests=0;

	public OnboardingFollowSuggestionsFragment(){
		super(R.layout.fragment_onboarding_follow_suggestions, 40);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setTitle(R.string.popular_on_mastodon);
		accountID=getArguments().getString("account");
		loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		buttonBar=view.findViewById(R.id.button_bar);
		list.addOnScrollListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));

		view.findViewById(R.id.btn_next).setOnClickListener(UiUtils.rateLimitedClickListener(this::onFollowAllClick));
		view.findViewById(R.id.btn_skip).setOnClickListener(UiUtils.rateLimitedClickListener(v->proceed()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetFollowSuggestions(40)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FollowSuggestion> result){
						onDataLoaded(result.stream().map(fs->new AccountViewModel(fs.account, accountID)).collect(Collectors.toList()), false);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	private void onFollowAllClick(View v){
		if(!loaded || relationships.isEmpty())
			return;
		if(data.isEmpty()){
			proceed();
			return;
		}
		ArrayList<String> accountIdsToFollow=new ArrayList<>();
		for(AccountViewModel acc:data){
			Relationship rel=relationships.get(acc.account.id);
			if(rel==null)
				continue;
			if(rel.canFollow())
				accountIdsToFollow.add(acc.account.id);
		}

		final ProgressDialog progress=new ProgressDialog(getActivity());
		progress.setIndeterminate(false);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setMax(accountIdsToFollow.size());
		progress.setCancelable(false);
		progress.setMessage(getString(R.string.sending_follows));
		progress.show();

		for(int i=0;i<Math.min(accountIdsToFollow.size(), 5);i++){ // Send up to 5 requests in parallel
			followNextAccount(accountIdsToFollow, progress);
		}
	}

	private void followNextAccount(ArrayList<String> accountIdsToFollow, ProgressDialog progress){
		if(accountIdsToFollow.isEmpty()){
			if(numRunningFollowRequests==0){
				progress.dismiss();
				proceed();
			}
			return;
		}
		numRunningFollowRequests++;
		String id=accountIdsToFollow.remove(0);
		new SetAccountFollowed(id, true, true)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Relationship result){
						relationships.put(id, result);
						for(int i=0;i<list.getChildCount();i++){
							if(list.getChildViewHolder(list.getChildAt(i)) instanceof AccountViewHolder svh && svh.getItem().account.id.equals(id)){
								svh.rebind();
								break;
							}
						}
						numRunningFollowRequests--;
						progress.setProgress(progress.getMax()-accountIdsToFollow.size()-numRunningFollowRequests);
						followNextAccount(accountIdsToFollow, progress);
					}

					@Override
					public void onError(ErrorResponse error){
						numRunningFollowRequests--;
						progress.setProgress(progress.getMax()-accountIdsToFollow.size()-numRunningFollowRequests);
						followNextAccount(accountIdsToFollow, progress);
					}
				})
				.exec(accountID);
	}

	private void proceed(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), OnboardingProfileSetupFragment.class, args);
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		super.onConfigureViewHolder(holder);
		holder.setStyle(AccountViewHolder.AccessoryType.BUTTON, true);
	}
}
