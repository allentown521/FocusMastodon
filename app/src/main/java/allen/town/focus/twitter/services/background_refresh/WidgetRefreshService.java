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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.api.requests.timelines.GetHomeTimeline;
import allen.town.focus.twitter.data.sq_lite.HomeContentProvider;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.services.PreCacheService;
import allen.town.focus.twitter.services.abstract_services.KillerIntentService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationChannelUtil;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.widget.WidgetProvider;
import allen.town.focus_common.util.Timber;
import twitter4j.StatusJSONImplMastodon;

public class WidgetRefreshService extends KillerIntentService {

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public WidgetRefreshService() {
        super("WidgetRefreshService");
    }

    @Override
    public void handleIntent(Intent intent) {
        // it is refreshing elsewhere, so don't start
        if (WidgetRefreshService.isRunning || TimelineRefreshService.isRunning || !MainActivity.canSwitch) {
            return;
        }

        WidgetRefreshService.isRunning = true;
        sharedPrefs = AppSettings.getSharedPreferences(this);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NotificationChannelUtil.WIDGET_REFRESH_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(getResources().getString(R.string.refreshing) + "...")
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.refreshing_widget) + "...")
                        .setProgress(100, 100, true)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_round_sync_24));

        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(6, mBuilder.build());

        final Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);
        HomeDataSource dataSource = HomeDataSource.getInstance(context);

        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

        boolean foundStatus = false;


        long[] lastId;
        long id;
        try {
            lastId = dataSource.getLastIds(currentAccount);
            id = lastId[0];
        } catch (Exception e) {
            WidgetRefreshService.isRunning = false;
            mNotificationManager.cancel(6);
            return;
        }

        if (id == 0) {
            id = 1;
        }

        for (int i = 0; i < settings.maxTweetsRefresh; i++) {
            try {
                if (!foundStatus) {

                    HeaderPaginationList<StatusJSONImplMastodon> list = StatusJSONImplMastodon.createStatusList(
                            new GetHomeTimeline(null, null, 40, id + "")
                                    .execSync());
                    if (list.size() > 0) {
                        id = list.get(0).getId();

                        List<StatusJSONImplMastodon> filteredList = list.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.HOME)).collect(Collectors.toList());
                        statuses.addAll(filteredList);
                    }

                    if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                        Timber.v("found status");
                        foundStatus = true;
                    } else {
                        Timber.v("haven't found status");
                        foundStatus = false;
                    }
                }
            } catch (Exception e) {
                // the page doesn't exist
                foundStatus = true;
            } catch (OutOfMemoryError o) {
                // don't know why...
            }
        }

        Timber.v("got statuses, new = " + statuses.size());

        // hash set to remove duplicates I guess
        HashSet hs = new HashSet();
        hs.addAll(statuses);
        statuses.clear();
        statuses.addAll(hs);

        Timber.v("tweets after hashset: " + statuses.size());

        lastId = dataSource.getLastIds(currentAccount);

        int inserted = HomeDataSource.getInstance(context).insertTweets(statuses, currentAccount, lastId);

        if (inserted > 0 && statuses.size() > 0) {
            sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
        }

        if (settings.preCacheImages) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PreCacheService.cache(context);
                }
            }).start();
        }

        WidgetProvider.updateWidget(this);
        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);
        sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();

        mNotificationManager.cancel(6);

        WidgetRefreshService.isRunning = false;
    }
}