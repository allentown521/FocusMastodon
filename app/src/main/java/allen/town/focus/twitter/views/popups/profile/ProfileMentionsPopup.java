package allen.town.focus.twitter.views.popups.profile;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;


public class ProfileMentionsPopup extends ProfileListPopupLayout {

    public ProfileMentionsPopup(Context context, View main, User user) {
        super(context, main, user);
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.mentions);
    }

    public boolean incrementQuery() {
        return !TextUtils.isEmpty(nextPage);
    }


    @Override
    public List<StatusJSONImplMastodon> getData() {
        try {
            HeaderPaginationList<StatusJSONImplMastodon> statuses;
            HeaderPaginationList<Notification> list = new GetNotifications(nextPage, "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();
            statuses = HeaderPaginationList.copyOnlyPage(list);
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).status != null) {
                        statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                    }
                }
            }

            List<StatusJSONImplMastodon> filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(App.getInstance()).mySessionId, Filter.FilterContext.HOME)).collect(Collectors.toList());
            statuses.clear();
            statuses.addAll(filteredList);
            return statuses;
        } catch (Exception e) {
            return null;
        }
    }
}
