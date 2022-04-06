package org.joinmastodon.android.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MastodonDetailedErrorResponse extends MastodonErrorResponse{
	public Map<String, List<FieldError>> detailedErrors;

	public MastodonDetailedErrorResponse(String error, int httpStatus){
		super(error, httpStatus);
	}

	public static class FieldError{
		public String error;
		public String description;
	}
}
