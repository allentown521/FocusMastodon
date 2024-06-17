package allen.town.focus.twitter.views.popups.profile;

import android.content.Context;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.requests.accounts.GetAccountFollowers;
import allen.town.focus.twitter.model.HeaderPaginationList;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class ProfileFollowersPopup extends ProfileUsersPopup {

    ArrayList<User> ids;

    public ProfileFollowersPopup(Context context, User user) {
        super(context, user);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.followers);
    }

    public void setIds(User u) {

    }

    @Override
    public HeaderPaginationList<User> getData(String cursor) {
        try {
            return UserJSONImplMastodon.createPagableUserList(new GetAccountFollowers(
                    user.getId() + "", cursor, 80).execSync());
        } catch (Exception e) {
            return null;
        }
    }
}