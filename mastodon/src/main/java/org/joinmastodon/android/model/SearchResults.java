package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;

import java.util.List;

public class SearchResults extends BaseModel {
    public List<Account> accounts;
    public List<Status> statuses;
    public List<Hashtag> hashtags;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        if (accounts != null) {
            for (Account acc : accounts)
                acc.postprocess();
        }
        if (statuses != null) {
            for (Status s : statuses)
                s.postprocess();
        }
        if (hashtags != null) {
            for (Hashtag t : hashtags)
                t.postprocess();
        }
    }
}
