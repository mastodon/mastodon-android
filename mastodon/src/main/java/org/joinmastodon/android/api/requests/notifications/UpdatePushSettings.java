package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.PushSubscription;

import java.io.IOException;

import okhttp3.Response;

public class UpdatePushSettings extends MastodonAPIRequest<PushSubscription>{
	private final PushSubscription.Policy policy;

	public UpdatePushSettings(PushSubscription.Alerts alerts, PushSubscription.Policy policy){
		super(HttpMethod.PUT, "/push/subscription", PushSubscription.class);
		setRequestBody(new Request(alerts, policy));
		this.policy=policy;
	}

	@Override
	public void validateAndPostprocessResponse(PushSubscription respObj, Response httpResponse) throws IOException{
		super.validateAndPostprocessResponse(respObj, httpResponse);
		respObj.policy=policy;
	}

	private static class Request{
		public Data data=new Data();
		public PushSubscription.Policy policy;

		public Request(PushSubscription.Alerts alerts, PushSubscription.Policy policy){
			this.data.alerts=alerts;
			this.policy=policy;
		}

		private static class Data{
			public PushSubscription.Alerts alerts;
		}
	}
}
