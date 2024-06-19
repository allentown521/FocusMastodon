package allen.town.focus.twitter.api.requests.notifications;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;


import java.util.EnumSet;
import java.util.List;

import allen.town.focus.twitter.api.ApiUtils;
import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Notification;

public class GetNotifications extends HeaderPaginationRequest<Notification> {
    public GetNotifications(String maxID, String sinceId, int limit, EnumSet<Notification.Type> includeTypes) {
        super(HttpMethod.GET, "/notifications", new TypeToken<>() {
        });
        if (!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if (limit > 0)
            addQueryParameter("limit", "" + limit);
        if (!TextUtils.isEmpty(sinceId))
            addQueryParameter("since_id", "" + sinceId);
        /*
        所有分页的端点都有四个参数：since_id、max_id、min_id 和 limit。 since_id允许你在返回的数据中指定你想要的最小id，
        但你仍然会得到最新的数据，所以如果最新的和since_id之间的状态太多，有些将不会被返回。另一方面，min_id 从给定的 id 开始，为您提供具有最小 id 和更新的状态。
        同样，max_id 允许您指定您想要的最大 id。通过指定其中的 min_id 或 max_id（通常，只有一个，而不是两者，尽管从 Mastodon 版本 3.3.0 开始支持指定两者），您可以向前和向后浏览页面
         */
//        addQueryParameter("min_id", "" + sinceId);

        if (includeTypes != null) {
            for (String type : ApiUtils.enumSetToStrings(includeTypes, Notification.Type.class)) {
                addQueryParameter("types[]", type);
            }
            for (String type : ApiUtils.enumSetToStrings(EnumSet.complementOf(includeTypes), Notification.Type.class)) {
                addQueryParameter("exclude_types[]", type);
            }
        }
        removeUnsupportedItems = true;
    }
}
