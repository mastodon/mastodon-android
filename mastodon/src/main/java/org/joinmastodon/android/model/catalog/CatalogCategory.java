package org.joinmastodon.android.model.catalog;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.model.BaseModel;

@AllFieldsAreRequired
public class CatalogCategory extends BaseModel {
    public String category;
    public int serversCount;

    @Override
    public String toString() {
        return "CatalogCategory{" +
                "category='" + category + '\'' +
                ", serversCount=" + serversCount +
                '}';
    }
}
