package allen.town.focus.twitter.views.popups.profile;

import android.content.Context;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.requests.accounts.GetAccountFollowing;
import allen.town.focus.twitter.model.HeaderPaginationList;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class ProfileFriendsPopup extends ProfileUsersPopup {

    public ProfileFriendsPopup(Context context, User user) {
        super(context, user);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.following);
    }

    @Override
    public HeaderPaginationList<User> getData(String cursor) {
        try {
            return UserJSONImplMastodon.createPagableUserList(new GetAccountFollowing(
                    user.getId() + "", cursor, 80).execSync());
        } catch (Exception e) {
            return null;
        }
    }
}
