package org.joinmastodon.android.api.requests.reports;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ReportReason;

import java.util.Collections;
import java.util.List;

public class SendReport extends MastodonAPIRequest<Object> {
    public SendReport(String accountID, ReportReason reason, List<String> statusIDs, List<String> ruleIDs, String comment, boolean forward) {
        super(HttpMethod.POST, "/reports", Object.class);
        Body b = new Body();
        b.accountId = accountID;
        b.statusIds = statusIDs;
        b.comment = comment;
        b.forward = forward;
        b.category = reason;
        b.ruleIds = ruleIDs;
        setRequestBody(b);
    }

    private static class Body {
        public String accountId;
        public List<String> statusIds;
        public String comment;
        public boolean forward;
        public ReportReason category;
        public List<String> ruleIds;
    }
}
