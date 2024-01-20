package org.joinmastodon.android.api.requests.annual_reports;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.model.BaseModel;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetAnnualReports extends MastodonAPIRequest<GetAnnualReports.Response>{

	public GetAnnualReports(){
		super(HttpMethod.GET, "/annual_reports", Response.class);
	}

	@AllFieldsAreRequired
	public static class Response extends BaseModel{
		public List<AnnualReportYear> annualReports;
		public List<Account> accounts;
		public List<Status> statuses;

		@Override
		public void postprocess() throws ObjectValidationException{
			super.postprocess();
			for(AnnualReportYear r:annualReports){
				if(r.data==null)
					throw new ObjectValidationException("data is null");
				r.data.postprocess();
			}
			for(Account a:accounts)
				a.postprocess();
			for(Status s:statuses)
				s.postprocess();
		}

		public static class AnnualReportYear{
			public int year;
			public AnnualReport data;
		}
	}
}
