package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;

public class HomeTimelineFragment extends StatusListFragment{
	private ImageButton fab;

	public HomeTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getHomeTimeline(offset>0 ? getMaxID() : null, count, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				});
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setOnClickListener(this::onFabClick);
		updateToolbarLogo();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.home, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		ArrayList<String> options=new ArrayList<>();
		for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
			options.add(session.self.displayName+"\n("+session.self.username+"@"+session.domain+")");
		}
		new M3AlertDialogBuilder(getActivity())
				.setItems(options.toArray(new String[0]), (dialog, which)->{
					AccountSession session=AccountSessionManager.getInstance().getLoggedInAccounts().get(which);
					AccountSessionManager.getInstance().setLastActiveAccountID(session.getID());
					getActivity().finish();
					getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
				})
				.setPositiveButton(R.string.log_out, (dialog, which)->{
					AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
					new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Object result){
									AccountSessionManager.getInstance().removeAccount(session.getID());
									getActivity().finish();
									getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
								}

								@Override
								public void onError(ErrorResponse error){
									AccountSessionManager.getInstance().removeAccount(session.getID());
									getActivity().finish();
									getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
								}
							})
							.wrapProgress(getActivity(), R.string.loading, false)
							.execNoAuth(session.domain);
				})
				.setNegativeButton(R.string.add_account, (dialog, which)->{
					Nav.go(getActivity(), SplashFragment.class, null);
				})
				.show();
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbarLogo();
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Subscribe
	public void onStatusCreated(StatusCreatedEvent ev){
		prependItems(Collections.singletonList(ev.status), true);
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void updateToolbarLogo(){
		ImageView logo=new ImageView(getActivity());
		logo.setScaleType(ImageView.ScaleType.CENTER);
		logo.setImageResource(R.drawable.logo);
		logo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)));
		Toolbar toolbar=getToolbar();
		toolbar.addView(logo, new Toolbar.LayoutParams(Gravity.CENTER));
	}

	@Override
	@Subscribe
	public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
		super.onStatusCountersUpdated(ev);
	}

	@Override
	@Subscribe
	public void onStatusDeleted(StatusDeletedEvent ev){
		super.onStatusDeleted(ev);
	}
}
