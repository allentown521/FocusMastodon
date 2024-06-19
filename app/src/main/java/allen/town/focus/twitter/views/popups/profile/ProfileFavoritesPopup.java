package allen.town.focus.twitter.views.popups.profile;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.requests.accounts.GetAccountStatuses;
import allen.town.focus.twitter.api.requests.statuses.GetFavoritedStatuses;
import allen.town.focus.twitter.model.HeaderPaginationList;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;


public class ProfileFavoritesPopup extends ProfileListPopupLayout {

    public ProfileFavoritesPopup(Context context, View main, User user) {
        super(context, main, user);
    }

    public String getTitle() {
        return getResources().getString(R.string.favorites);
    }

    @Override
    public boolean incrementQuery() {
        return !TextUtils.isEmpty(nextPage);
    }

    @Override
    public List<StatusJSONImplMastodon> getData() {
        try {
            HeaderPaginationList<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(new GetFavoritedStatuses(user.getId() + "", nextPage, 20).execSync());
            nextPage = statuses.getNextCursor();
            return statuses;
        } catch (Exception e) {
            return null;
        }
    }
}
