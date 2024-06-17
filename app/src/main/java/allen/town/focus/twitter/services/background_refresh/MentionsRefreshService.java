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
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus_common.util.Timber;
import twitter4j.StatusJSONImplMastodon;

public class MentionsRefreshService extends Worker {

    private final Context context;
    public MentionsRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "mention-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(MentionsRefreshService.class).build());
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.syncInterval / 1000; // convert to seconds

        if (settings.syncInterval != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(MentionsRefreshService.class, refreshInterval, TimeUnit.SECONDS)
                            .setConstraints(new Constraints.Builder()
                                    .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                    .build())
                            .build();
            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        sharedPrefs = AppSettings.getSharedPreferences(context);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        try {

            int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long[] lastNotiId = dataSource.getLastIds(currentAccount);
            List<Notification> list = new GetNotifications("", lastNotiId[0] > 0 ? lastNotiId[0] + "" : "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();

            List<StatusJSONImplMastodon> statuses = new ArrayList<>();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).status != null) {
                        statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                    }
                }
            }

            List filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

            int inserted = MentionsDataSource.getInstance(context).insertTweets(filteredList, currentAccount);

            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_MENTIONS, true).commit();

            if (settings.notifications && settings.mentionsNot && inserted > 0) {
                NotificationUtils.refreshNotification(context);
            }

            if (settings.syncSecondMentions) {
                SecondMentionsRefreshService.startNow(context);
            }

        } catch (Exception e) {
            // Error in updating status
            Timber.e("Twitter Update Error", e);
        }

        return Result.success();
    }
}