package org.joinmastodon.android.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.Response;

public class GetStatusEditHistory extends MastodonAPIRequest<List<Status>>{
	public GetStatusEditHistory(String id){
		super(HttpMethod.GET, "/statuses/"+id+"/history", new TypeToken<>(){});
	}

	@Override
	public void validateAndPostprocessResponse(List<Status> respObj, Response httpResponse) throws IOException{
		int i=0;
		for(Status s:respObj){
			s.uri="";
			s.id="fakeID"+i;
			s.visibility=StatusPrivacy.PUBLIC;
			s.mentions=Collections.emptyList();
			s.tags=Collections.emptyList();
			i++;
		}
		super.validateAndPostprocessResponse(respObj, httpResponse);
	}
}
