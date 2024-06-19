package allen.town.focus.twitter.utils;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.data.sq_lite.QueuedDataSource;
import allen.town.focus.twitter.settings.AppSettings;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class UpdateUtils {

    private static final long SEC = 1000;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;

    private static final long SUPPORTER_TIMEOUT = 90 * DAY;

    public static void checkUpdate(final Activity context) {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        boolean justInstalled = runFirstInstalled(sharedPrefs);

        if (!justInstalled) {
            // version specific things
            if (sharedPrefs.getBoolean("need_mute_fix", true)) {
                String current = sharedPrefs.getString("muted_hashtags", "");
                String newString = current.replaceAll("  ", " ");
                sharedPrefs.edit().putString("muted_hashtags", newString)
                        .putBoolean("need_mute_fix", false)
                        .commit();
            }

            if (sharedPrefs.getBoolean("need_queue_deleted", true)) {
                sharedPrefs.edit().putBoolean("need_queue_deleted", false).commit();
                QueuedDataSource.getInstance(context).deleteAllQueuedTweets();
            }
        }

        sharedPrefs = AppSettings.getInstance(context).sharedPrefs;
        runEveryUpdate(context, sharedPrefs);
    }

    public static boolean runFirstInstalled(final SharedPreferences sharedPrefs) {
        if (sharedPrefs.getBoolean("fresh_install", true)) {
            SharedPreferences.Editor e = sharedPrefs.edit();
            e.putBoolean("fresh_install", false);
            e.putLong("first_run_time", new Date().getTime());

            // show them all for now
            Set<String> set = new HashSet<String>();
            for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
                set.add("" + i); // activity
            }

            e.putStringSet(AppSettings.DRAWER_SHOWN_ITEMS + "1", set);
            e.putStringSet(AppSettings.DRAWER_SHOWN_ITEMS + "2", set);

            // reset their pages to just home,
            String pageIdentifier = "account_" + 1 + "_page_";
            e.putInt(pageIdentifier + 1, AppSettings.PAGE_TYPE_HOME);
            e.putInt(pageIdentifier + 2, AppSettings.PAGE_TYPE_MENTIONS);
            e.putInt(pageIdentifier + 3, AppSettings.PAGE_TYPE_DMS);
            e.putInt(pageIdentifier + 4, AppSettings.PAGE_TYPE_ACTIVITY);
            e.putInt(pageIdentifier + 5, AppSettings.PAGE_TYPE_RETWEET);
            e.putInt(pageIdentifier + 6, AppSettings.PAGE_TYPE_DISCOVER);
            e.putInt(pageIdentifier + 7, AppSettings.PAGE_TYPE_LOCAL_TIMELINE);
            e.putInt(pageIdentifier + 8, AppSettings.PAGE_TYPE_LIST);
            e.putInt(pageIdentifier + 9, AppSettings.PAGE_TYPE_FAV_USERS_TWEETS);

            pageIdentifier = "account_" + 2 + "_page_";
            e.putInt(pageIdentifier + 1, AppSettings.PAGE_TYPE_HOME);
            e.putInt(pageIdentifier + 2, AppSettings.PAGE_TYPE_MENTIONS);
            e.putInt(pageIdentifier + 3, AppSettings.PAGE_TYPE_DMS);
            e.putInt(pageIdentifier + 4, AppSettings.PAGE_TYPE_ACTIVITY);
            e.putInt(pageIdentifier + 5, AppSettings.PAGE_TYPE_RETWEET);
            e.putInt(pageIdentifier + 6, AppSettings.PAGE_TYPE_DISCOVER);
            e.putInt(pageIdentifier + 7, AppSettings.PAGE_TYPE_LOCAL_TIMELINE);
            e.putInt(pageIdentifier + 8, AppSettings.PAGE_TYPE_LIST);
            e.putInt(pageIdentifier + 9, AppSettings.PAGE_TYPE_FAV_USERS_TWEETS);

            e.putInt(AppSettings.DEFAULT_TIMELINE_PAGE + 1, 0);
            e.putInt(AppSettings.DEFAULT_TIMELINE_PAGE + 2, 0);

            e.putLong("original_activity_refresh_" + 1, Calendar.getInstance().getTimeInMillis());
            e.putLong("original_activity_refresh_" + 2, Calendar.getInstance().getTimeInMillis());

            e.commit();

            return true;
        } else {
            return false;
        }
    }

    public static void runEveryUpdate(final Context context, final SharedPreferences sharedPrefs) {

        ServiceUtils.rescheduleAllServices(context);

        int storedAppVersion = sharedPrefs.getInt("app_version", 0);
        int currentAppVersion = getAppVersion(context);

        if (storedAppVersion < currentAppVersion) {
            if (storedAppVersion > 0) {
                //更新的逻辑，第一次安装的不需要进这个分支

            }
            sharedPrefs.edit().putInt("app_version", currentAppVersion).commit();
        }
    }

    protected static int getAppVersion(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager()
                    .getPackageInfo(c.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            return -1;
        }
    }
}
