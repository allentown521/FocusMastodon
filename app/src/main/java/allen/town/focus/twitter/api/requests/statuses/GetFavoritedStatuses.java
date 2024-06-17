package allen.town.focus.twitter.api.requests.statuses;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;


public class GetFavoritedStatuses extends HeaderPaginationRequest<Status> {
    public GetFavoritedStatuses(String maxID, String sinceId, int limit) {
        super(MastodonAPIRequest.HttpMethod.GET, "/favourites", new TypeToken<>() {
        });
        if (maxID != null)
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", limit + "");
        if (!TextUtils.isEmpty(sinceId))
            addQueryParameter("since_id", "" + sinceId);
    }
}
