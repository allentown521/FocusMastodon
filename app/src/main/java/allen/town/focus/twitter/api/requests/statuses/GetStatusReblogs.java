package allen.town.focus.twitter.api.requests.statuses;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Account;


public class GetStatusReblogs extends HeaderPaginationRequest<Account> {
    public GetStatusReblogs(String id, String sinceId, String maxID, int limit) {
        super(HttpMethod.GET, "/statuses/" + id + "/reblogged_by", new TypeToken<>() {
        });
        if (maxID != null)
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", limit + "");
        if (!TextUtils.isEmpty(sinceId))
            addQueryParameter("since_id", "" + sinceId);
    }
}
