package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.reports.SendReport;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.FinishReportFragmentsEvent;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.ReportReason;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class ReportCommentFragment extends MastodonToolbarFragment{
	private String accountID;
	private Account reportAccount;
	private Button btn;
	private View buttonBar;
	private EditText commentEdit;
	private Switch forwardSwitch;
	private View forwardBtn;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		if(getArguments().getBoolean("fromPost", false))
			setTitle(R.string.report_title_post);
		else
			setTitle(getString(R.string.report_title, reportAccount.acct));
	}


	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_report_comment, container, false);

		TextView title=view.findViewById(R.id.title);
		TextView subtitle=view.findViewById(R.id.subtitle);
		title.setText(R.string.report_comment_title);
		subtitle.setVisibility(View.GONE);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(this::onButtonClick);
		buttonBar=view.findViewById(R.id.button_bar);
		commentEdit=view.findViewById(R.id.text);
		forwardSwitch=view.findViewById(R.id.forward_switch);
		forwardBtn=view.findViewById(R.id.forward_report);
		forwardBtn.setOnClickListener(v->forwardSwitch.toggle());
		String myDomain=AccountSessionManager.getInstance().getAccount(accountID).domain;
		if(!TextUtils.isEmpty(reportAccount.getDomain()) && !myDomain.equalsIgnoreCase(reportAccount.getDomain())){
			TextView forwardTitle=view.findViewById(R.id.forward_title);
			forwardTitle.setText(getString(R.string.forward_report_to_server, reportAccount.getDomain()));
		}else{
			forwardBtn.setVisibility(View.GONE);
		}

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);

		ProgressBar topProgress=view.findViewById(R.id.top_progress);
		topProgress.setProgress(getArguments().containsKey("ruleIDs") ? 75 : 66);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	private void onButtonClick(View v){
		ReportReason reason=ReportReason.valueOf(getArguments().getString("reason"));
		ArrayList<String> statusIDs=getArguments().getStringArrayList("statusIDs");
		ArrayList<String> ruleIDs=getArguments().getStringArrayList("ruleIDs");
		new SendReport(reportAccount.id, reason, statusIDs, ruleIDs, v.getId()==R.id.btn_back ? null : commentEdit.getText().toString(), forwardSwitch.isChecked())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						Bundle args=new Bundle();
						args.putString("account", accountID);
						args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
						args.putString("reason", reason.name());
						args.putBoolean("fromPost", getArguments().getBoolean("fromPost", false));
						args.putParcelable("relationship", getArguments().getParcelable("relationship"));
						Nav.go(getActivity(), ReportDoneFragment.class, args);
						buttonBar.postDelayed(()->E.post(new FinishReportFragmentsEvent(reportAccount.id)), 500);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.sending_report, false)
				.exec(accountID);
	}

	@Subscribe
	public void onFinishReportFragments(FinishReportFragmentsEvent ev){
		if(ev.reportAccountID.equals(reportAccount.id))
			Nav.finish(this);
	}
}
