package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.List;

@Parcel
public class FilterResult extends BaseModel{
	@RequiredField
	public Filter filter;

	public List<String> keywordMatches;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		filter.postprocess();
	}
}
