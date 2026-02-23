package org.joinmastodon.android.ui.tooltips;

import android.content.Context;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.UiUtils;

public class DecentralizationExplainerTooltip extends BaseRichTooltip{
	private final Account account;
	private final String accountID;

	public DecentralizationExplainerTooltip(Context context, Account account, String accountID){
		super(context);
		this.account=account;
		this.accountID=accountID;
	}

	@Override
	protected void initView(){
		contentView=LayoutInflater.from(context).inflate(R.layout.profile_decentralization_tooltip, null);
		TextView username=contentView.findViewById(R.id.username);
		TextView domain=contentView.findViewById(R.id.domain);
		username.setText(UiUtils.substituteStringWithSpan(context, R.string.handle_explanation_username, account.username, new TypefaceSpan("sans-serif-medium")));
		String accountDomain=account.getDomain();
		if(accountDomain==null)
			accountDomain=AccountSessionManager.get(accountID).domain;
		domain.setText(UiUtils.substituteStringWithSpan(context, R.string.handle_explanation_domain, accountDomain, new TypefaceSpan("sans-serif-medium")));
	}
}
