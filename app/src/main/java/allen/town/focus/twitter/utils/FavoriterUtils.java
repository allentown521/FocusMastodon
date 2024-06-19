package allen.town.focus.twitter.utils;


import allen.town.focus.twitter.api.requests.statuses.GetStatusFavorites;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus_common.util.Timber;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class FavoriterUtils {

    public HeaderPaginationList<User> getFavoriters(long tweetId, String nextPage) {
        HeaderPaginationList<User> users = new HeaderPaginationList<>();

        try {
            users = UserJSONImplMastodon.createPagableUserList(new GetStatusFavorites(tweetId + "", nextPage, 80).execSync());
        } catch (Exception e) {
            Timber.e("Error getting users: %s", e);
        }

        return users;
    }


}
