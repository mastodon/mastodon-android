package org.joinmastodon.android.api.requests.reports;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ReportReason;

import java.util.List;

public class SendReport extends MastodonAPIRequest<Object>{
	public SendReport(String accountID, ReportReason reason, List<String> statusIDs, List<String> collectionIDs, List<String> ruleIDs, String comment, boolean forward){
		super(HttpMethod.POST, "/reports", Object.class);
		Body b=new Body();
		b.accountId=accountID;
		b.statusIds=statusIDs;
		b.collectionIds=collectionIDs;
		b.comment=comment;
		b.forward=forward;
		b.category=reason;
		b.ruleIds=ruleIDs;
		setRequestBody(b);
	}

	private static class Body{
		public String accountId;
		public List<String> statusIds;
		public List<String> collectionIds;
		public String comment;
		public boolean forward;
		public ReportReason category;
		public List<String> ruleIds;
	}
}
