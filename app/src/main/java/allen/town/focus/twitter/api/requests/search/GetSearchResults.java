package allen.town.focus.twitter.api.requests.search;


import android.text.TextUtils;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.SearchResults;

public class GetSearchResults extends MastodonAPIRequest<SearchResults> {
    public GetSearchResults(String query, Type type, boolean resolve, String maxID, int limit) {
        super(HttpMethod.GET, "/search", SearchResults.class);
        addQueryParameter("q", query);
        if (type != null)
            addQueryParameter("type", type.name().toLowerCase());
        if (resolve)
            addQueryParameter("resolve", "true");
        if (!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", "" + limit);
    }

    @Override
    protected String getPathPrefix() {
        return "/api/v2";
    }

    public enum Type {
        ACCOUNTS,
        HASHTAGS,
        STATUSES
    }
}
