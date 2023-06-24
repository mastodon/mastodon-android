package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.adapters.InstanceRulesAdapter;

import androidx.recyclerview.widget.RecyclerView;

public class SettingsServerRulesFragment extends MastodonRecyclerFragment<Instance.Rule>{
	private String accountID;

	public SettingsServerRulesFragment(){
		super(20);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		Instance instance=AccountSessionManager.getInstance().getInstanceInfo(AccountSessionManager.get(accountID).domain);
		onDataLoaded(instance.rules);
		setRefreshEnabled(false);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		return new InstanceRulesAdapter(data);
	}

	@Override
	protected View onCreateFooterView(LayoutInflater inflater){
		return inflater.inflate(R.layout.load_more_with_end_mark, null);
	}

	public RecyclerView getList(){
		return list;
	}
}
