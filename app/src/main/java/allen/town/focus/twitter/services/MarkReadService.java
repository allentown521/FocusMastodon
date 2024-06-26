package allen.town.focus.twitter.services;
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

import android.util.Log;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.InteractionsDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.services.abstract_services.KillerIntentService;
import allen.town.focus.twitter.settings.AppSettings;

public class MarkReadService extends KillerIntentService {

    SharedPreferences sharedPrefs;

    public MarkReadService() {
        super("MarkReadService");
    }

    @Override
    public void handleIntent(Intent intent) {

    }

    public static void markRead(Context context) {

        Log.v("Focus_for_Mastodon_mark_read", "running the mark read service for account 1");

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        // clear custom light flow broadcast
        Intent lightFlow = new Intent("allen.town.focus.twitter.CLEARED_NOTIFICATION");
        context.sendBroadcast(lightFlow);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        final int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is availible

        MentionsDataSource.getInstance(context).markAllRead(currentAccount);
        HomeDataSource.getInstance(context).markAllRead(currentAccount);
        InteractionsDataSource.getInstance(context).markAllRead(currentAccount);

        sharedPrefs.edit().putInt(AppSettings.DM_UNREAD_STARTER + currentAccount, 0).commit();

        ReadInteractionsService.markRead(context);
    }

}
