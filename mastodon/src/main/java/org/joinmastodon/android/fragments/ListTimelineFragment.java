package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.timelines.GetListTimeline;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class ListTimelineFragment extends StatusListFragment{
	private FollowList followList;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		followList=Parcels.unwrap(getArguments().getParcelable("list"));
		setTitle(followList.title);
		setHasOptionsMenu(true);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetListTimeline(followList.id, offset>0 ? getMaxID() : null, null, count, null)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.standalone_list_timeline, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("list", Parcels.wrap(followList));
		if(id==R.id.members){
			Nav.go(getActivity(), ListMembersFragment.class, args);
		}else if(id==R.id.edit_list){
			Nav.go(getActivity(), EditListFragment.class, args);
		}
		return true;
	}
}
