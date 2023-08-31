package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CreateStatus extends MastodonAPIRequest<Status>{
	public CreateStatus(CreateStatus.Request req, String uuid){
		super(HttpMethod.POST, "/statuses", Status.class);
		setRequestBody(req);
		addHeader("Idempotency-Key", uuid);
	}

	public static class Request{
		public String status;
		public List<MediaAttribute> mediaAttributes;
		public List<String> mediaIds;
		public Poll poll;
		public String inReplyToId;
		public boolean sensitive;
		public String spoilerText;
		public StatusPrivacy visibility;
		public Instant scheduledAt;
		public String language;

		public static class Poll{
			public ArrayList<String> options=new ArrayList<>();
			public int expiresIn;
			public boolean multiple;
			public boolean hideTotals;
		}

		public static class MediaAttribute{
			public String id;
			public String description;
			public String focus;

			public MediaAttribute(String id, String description, String focus){
				this.id=id;
				this.description=description;
				this.focus=focus;
			}
		}
	}
}
