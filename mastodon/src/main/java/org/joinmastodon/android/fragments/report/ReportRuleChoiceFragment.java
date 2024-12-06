package org.joinmastodon.android.fragments.report;

import android.os.Bundle;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.FinishReportFragmentsEvent;
import org.joinmastodon.android.model.Instance;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;

public class ReportRuleChoiceFragment extends BaseReportChoiceFragment{
	@Override
	protected ChoiceItem getHeaderItem(){
		return new ChoiceItem(getString(R.string.report_choose_rule), getString(R.string.report_choose_rule_subtitle), null);
	}

	@Override
	protected void populateItems(){
		isMultipleChoice=true;
		Instance inst=AccountSessionManager.getInstance().getInstanceInfo(AccountSessionManager.getInstance().getAccount(accountID).domain);
		if(inst!=null && inst.rules!=null){
			for(Instance.Rule rule:inst.rules){
				items.add(new ChoiceItem(rule.text, null, rule.id));
			}
		}
	}

	@Override
	protected void onButtonClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(reportStatus));
		args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
		args.putString("reason", getArguments().getString("reason"));
		args.putStringArrayList("ruleIDs", selectedIDs);
		args.putParcelable("relationship", getArguments().getParcelable("relationship"));
		Nav.go(getActivity(), ReportAddPostsChoiceFragment.class, args);
	}

	@Subscribe
	public void onFinishReportFragments(FinishReportFragmentsEvent ev){
		if(ev.reportAccountID.equals(reportAccount.id))
			Nav.finish(this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		progressBar.setProgress(25);
	}
}
