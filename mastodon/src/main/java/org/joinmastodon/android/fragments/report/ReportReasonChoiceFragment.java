package org.joinmastodon.android.fragments.report;

import android.os.Bundle;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.FinishReportFragmentsEvent;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.ReportReason;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;

public class ReportReasonChoiceFragment extends BaseReportChoiceFragment{
	@Override
	protected Item getHeaderItem(){
		return new Item(reportStatus!=null ? getString(R.string.report_choose_reason) : getString(R.string.report_choose_reason_account, reportAccount.acct), getString(R.string.report_choose_reason_subtitle), null);
	}

	@Override
	protected void populateItems(){
		items.add(new Item(getString(R.string.report_reason_personal), getString(R.string.report_reason_personal_subtitle), ReportReason.PERSONAL.name()));
		items.add(new Item(getString(R.string.report_reason_spam), getString(R.string.report_reason_spam_subtitle), ReportReason.SPAM.name()));
		Instance inst=AccountSessionManager.getInstance().getInstanceInfo(AccountSessionManager.getInstance().getAccount(accountID).domain);
		if(inst!=null && inst.rules!=null && !inst.rules.isEmpty()){
			items.add(new Item(getString(R.string.report_reason_violation), getString(R.string.report_reason_violation_subtitle), ReportReason.VIOLATION.name()));
		}
		items.add(new Item(getString(R.string.report_reason_other), getString(R.string.report_reason_other_subtitle), ReportReason.OTHER.name()));
	}

	@Override
	protected void onButtonClick(){
		ReportReason reason=ReportReason.valueOf(selectedIDs.get(0));
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(reportStatus));
		args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
		args.putString("reason", reason.name());
		switch(reason){
			case PERSONAL -> {
				Nav.go(getActivity(), ReportDoneFragment.class, args);
				content.postDelayed(()->Nav.finish(this), 500);
			}
			case SPAM, OTHER -> Nav.go(getActivity(), ReportAddPostsChoiceFragment.class, args);
			case VIOLATION -> Nav.go(getActivity(), ReportRuleChoiceFragment.class, args);
		}
	}

	@Override
	protected int getStepNumber(){
		return 1;
	}

	@Subscribe
	public void onFinishReportFragments(FinishReportFragmentsEvent ev){
		if(ev.reportAccountID.equals(reportAccount.id))
			Nav.finish(this);
	}
}
