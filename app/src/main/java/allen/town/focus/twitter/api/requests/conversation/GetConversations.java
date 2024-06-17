package allen.town.focus.twitter.api.requests.conversation;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Conversation;


public class GetConversations extends HeaderPaginationRequest<Conversation> {
    public GetConversations(String maxID, String sinceId, int limit) {
        super(HttpMethod.GET, "/conversations", new TypeToken<>() {
        });
        if (!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", limit + "");
        if (!TextUtils.isEmpty(sinceId))
            addQueryParameter("since_id", "" + sinceId);
    }
}
