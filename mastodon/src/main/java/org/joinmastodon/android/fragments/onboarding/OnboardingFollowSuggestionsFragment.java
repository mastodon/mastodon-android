package org.joinmastodon.android.fragments.onboarding;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetFollowSuggestions;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.fragments.account_list.BaseAccountListFragment;
import org.joinmastodon.android.model.FollowSuggestion;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class OnboardingFollowSuggestionsFragment extends BaseAccountListFragment{
	private String accountID;
	private View buttonBar;
	private int numRunningFollowRequests=0;

	public OnboardingFollowSuggestionsFragment(){
		super(R.layout.fragment_onboarding_follow_suggestions, 40);
		itemLayoutRes=R.layout.item_account_list_onboarding;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setTitle(R.string.onboarding_recommendations_title);
		accountID=getArguments().getString("account");
		loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		buttonBar=view.findViewById(R.id.button_bar);

		view.findViewById(R.id.btn_next).setOnClickListener(UiUtils.rateLimitedClickListener(this::onFollowAllClick));
		view.findViewById(R.id.btn_skip).setOnClickListener(UiUtils.rateLimitedClickListener(v->proceed()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setContentInsetsRelative(V.dp(56), 0);
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetFollowSuggestions(40)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FollowSuggestion> result){
						onDataLoaded(result.stream().map(fs->new AccountViewModel(fs.account, accountID, getActivity()).stripLinksFromBio()).collect(Collectors.toList()), false);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		TextView introText=new TextView(getActivity());
		introText.setTextAppearance(R.style.m3_body_large);
		introText.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		introText.setPaddingRelative(V.dp(56), 0, V.dp(24), V.dp(8));
		introText.setText(R.string.onboarding_recommendations_intro);
		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(introText));
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
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
		new SetAccountFollowed(id, true, true, false)
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
		holder.avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
	}
}
