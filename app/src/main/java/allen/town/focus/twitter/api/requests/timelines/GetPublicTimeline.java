package allen.town.focus.twitter.api.requests.timelines;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;

public class GetPublicTimeline extends HeaderPaginationRequest<Status> {
    public GetPublicTimeline(boolean local, boolean remote, String maxID, int limit, String sinceID) {
        super(HttpMethod.GET, "/timelines/public", new TypeToken<>() {
        });
        if (local)
            addQueryParameter("local", "true");
        if (remote)
            addQueryParameter("remote", "true");
        if (!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", limit + "");
        if (sinceID != null)
            addQueryParameter("since_id", sinceID);
    }
}
