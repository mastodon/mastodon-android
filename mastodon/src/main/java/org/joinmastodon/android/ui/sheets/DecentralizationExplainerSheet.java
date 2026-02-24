package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;

public class DecentralizationExplainerSheet extends M3BottomSheet{
	public DecentralizationExplainerSheet(@NonNull Context context, String accountID, Account account){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_decentralization_info, null);
		setContentView(content);
		TextView username=findViewById(R.id.username);
		TextView domain=findViewById(R.id.domain);
		username.setText(UiUtils.substituteStringWithSpan(context, R.string.handle_explanation_username, account.username, new TypefaceSpan("sans-serif-medium")));
		String accountDomain=account.getDomain();
		if(accountDomain==null)
			accountDomain=AccountSessionManager.get(accountID).domain;
		domain.setText(UiUtils.substituteStringWithSpan(context, R.string.handle_explanation_domain, accountDomain, new TypefaceSpan("sans-serif-medium")));
	}
}
