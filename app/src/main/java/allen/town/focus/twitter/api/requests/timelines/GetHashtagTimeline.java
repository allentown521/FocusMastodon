package allen.town.focus.twitter.api.requests.timelines;

import com.google.gson.reflect.TypeToken;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;

/**
 * 实际header没有分页
 */
public class GetHashtagTimeline extends HeaderPaginationRequest<Status> {
    public GetHashtagTimeline(String hashtag, String maxID, String minID, int limit, String sinceID) {
        super(HttpMethod.GET, "/timelines/tag/" + hashtag, new TypeToken<>() {
        });
        if (maxID != null)
            addQueryParameter("max_id", maxID);
        if (minID != null)
            addQueryParameter("min_id", minID);
        if (limit > 0)
            addQueryParameter("limit", "" + limit);
        if (sinceID != null)
            addQueryParameter("since_id", sinceID);
    }
}
