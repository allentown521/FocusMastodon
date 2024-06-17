package allen.town.focus.twitter.services.background_refresh;
/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationUtils;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.Timber;
import twitter4j.StatusJSONImplMastodon;

public class SecondMentionsRefreshService extends Worker {

    private final Context context;

    public SecondMentionsRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(SecondMentionsRefreshService.class).build());
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return Result.success();
        }

        boolean update = false;
        int numberNew = 0;

        try {

            int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

            if (currentAccount == 1) {
                currentAccount = 2;
            } else {
                currentAccount = 1;
            }

            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long lastNotiId = dataSource.getLastIds(currentAccount)[0];
            List<Notification> list = new GetNotifications("", lastNotiId > 0 ? lastNotiId + "" : "", 30, EnumSet.of(Notification.Type.MENTION)).execSecondAccountSync();

            List<StatusJSONImplMastodon> statuses = new ArrayList<>();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).status != null) {
                        statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                    }
                }
            }

            List filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).secondSessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

            numberNew = MentionsDataSource.getInstance(context).insertTweets(filteredList, currentAccount);

            if (numberNew > 0) {
                sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_MENTIONS, true).commit();

                if (settings.notifications && settings.mentionsNot) {
                    NotificationUtils.notifySecondMentions(context, currentAccount);
                }

                context.sendBroadcast(new Intent(IntentConstant.REFRESH_SECOND_MENTIONS_ACTION));
            }

        } catch (Exception e) {
            // Error in updating status
            Timber.e("Twitter Update Error", e);
        }

        return Result.success();
    }
}