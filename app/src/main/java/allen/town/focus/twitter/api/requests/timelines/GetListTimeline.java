package allen.town.focus.twitter.api.requests.timelines;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;

public class GetListTimeline extends HeaderPaginationRequest<Status> {
    public GetListTimeline(String listId, String maxID, String minID, int limit, String sinceID) {
        super(HttpMethod.GET, "/timelines/list/" + listId, new TypeToken<>() {
        });
        if (!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if (!TextUtils.isEmpty(minID))
            addQueryParameter("min_id", minID);
        if (limit > 0)
            addQueryParameter("limit", "" + limit);
        if (!TextUtils.isEmpty(sinceID))
            addQueryParameter("since_id", sinceID);
    }
}
