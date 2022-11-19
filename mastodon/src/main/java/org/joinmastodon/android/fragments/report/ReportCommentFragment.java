package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.reports.SendReport;
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
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.V;

public class ReportCommentFragment extends MastodonToolbarFragment{
	private String accountID;
	private Account reportAccount;
	private Button btn;
	private View buttonBar;
	private EditText commentEdit;

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
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		setTitle(getString(R.string.report_title, reportAccount.acct));
	}


	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_report_comment, container, false);

		TextView title=view.findViewById(R.id.title);
		TextView subtitle=view.findViewById(R.id.subtitle);
		TextView stepCounter=view.findViewById(R.id.step_counter);
		title.setText(R.string.report_comment_title);
		subtitle.setVisibility(View.GONE);
		stepCounter.setText(getString(R.string.step_x_of_n, 3, 3));

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(this::onButtonClick);
		view.findViewById(R.id.btn_back).setOnClickListener(this::onButtonClick);
		buttonBar=view.findViewById(R.id.button_bar);
		commentEdit=view.findViewById(R.id.text);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			buttonBar.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
	}

	private void onButtonClick(View v){
		ReportReason reason=ReportReason.valueOf(getArguments().getString("reason"));
		ArrayList<String> statusIDs=getArguments().getStringArrayList("statusIDs");
		ArrayList<String> ruleIDs=getArguments().getStringArrayList("ruleIDs");
		new SendReport(reportAccount.id, reason, statusIDs, ruleIDs, v.getId()==R.id.btn_back ? null : commentEdit.getText().toString(), true)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						Bundle args=new Bundle();
						args.putString("account", accountID);
						args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
						args.putString("reason", reason.name());
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
