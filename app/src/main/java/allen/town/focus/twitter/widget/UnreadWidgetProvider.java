package allen.town.focus.twitter.widget;
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

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.compose.WidgetCompose;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.glide.CircleBitmapTransform;
import allen.town.focus.twitter.utils.redirects.RedirectToDMs;
import allen.town.focus.twitter.utils.redirects.RedirectToMentions;
import allen.town.focus.twitter.utils.redirects.RedirectToTimeline;
import allen.town.focus.twitter.widget.timeline.TimelineWidgetProvider;

import allen.town.focus_common.util.Timber;

public class UnreadWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent updateWidget = new Intent(context, UnreadWidgetService.class);
        context.startService(updateWidget);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TimelineWidgetProvider.Companion.getREFRESH_ACTION()) ||
                intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            Intent updateWidget = new Intent(context, UnreadWidgetService.class);

            try {
                context.startService(updateWidget);
            } catch (Exception e) {
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    public static class UnreadWidgetService extends IntentService {
        public UnreadWidgetService() {
            super("unread_widget_service");
        }

        @Override
        public IBinder onBind(Intent arg0) {
            return null;
        }

        @Override
        protected void onHandleIntent(Intent arg0) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName thisAppWidget = new ComponentName(this.getPackageName(), UnreadWidgetProvider.class.getName());
            int[] appWidgetIds = mgr.getAppWidgetIds(thisAppWidget);

            Timber.v("running service");

            int res = 0;
            switch (Integer.parseInt(AppSettings.getSharedPreferences(this)
                    .getString("widget_theme", "4"))) {
                case 0:
                    res = R.layout.widget_unread_trans_light;
                    break;
                case 1:
                    res = R.layout.widget_unread_trans_black;
                    break;
                case 2:
                    res = R.layout.widget_unread_trans_light;
                    break;
                case 3:
                    res = R.layout.widget_unread_trans_black;
                    break;
                case 4:
                    res = R.layout.widget_unread_trans_light;
                    break;
                case 5:
                    res = R.layout.widget_unread_trans_black;
                    break;
                case 6:
                    res = R.layout.widget_unread_trans;
                    break;
            }

            RemoteViews views = new RemoteViews(this.getPackageName(), res);

            for (int i = 0; i < appWidgetIds.length; i++) {
                Timber.v("in for loop");
                int appWidgetId = appWidgetIds[i];

                Intent quickText = new Intent(this, WidgetCompose.class);
                quickText.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent quickPending = PendingIntent.getActivity(this, 0, quickText, Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));

                Intent openApp = new Intent(this, RedirectToTimeline.class);
                openApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent openAppPending = PendingIntent.getActivity(this, 0, openApp, Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));

                Intent mentions = new Intent(this, RedirectToMentions.class);
                mentions.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                PendingIntent mentionsPending = PendingIntent.getActivity(this, 0, mentions, Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));

                Intent dms = new Intent(this, RedirectToDMs.class);
                dms.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                PendingIntent dmsPending = PendingIntent.getActivity(this, 0, dms, Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));

                views.setOnClickPendingIntent(R.id.launcherIcon, openAppPending);
                views.setOnClickPendingIntent(R.id.replyButton, quickPending);
                views.setOnClickPendingIntent(R.id.timeline, openAppPending);
                views.setOnClickPendingIntent(R.id.mentions, mentionsPending);
                views.setOnClickPendingIntent(R.id.dms, dmsPending);

                // get the counts
                try {
                    SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);
                    int currentAccount = AppSettings.getInstance(this).widgetAccountNum;

                    String dm = sharedPrefs.getInt(AppSettings.DM_UNREAD_STARTER + currentAccount, 0) + "";
                    String mention = MentionsDataSource.getInstance(this).getUnreadCount(currentAccount) + "";
                    String home = HomeDataSource.getInstance(this).getPosition(currentAccount, sharedPrefs.getLong("current_position_" + currentAccount, 0)) + "";

                    views.setTextViewText(R.id.home_text, home);
                    views.setTextViewText(R.id.mention_text, mention);
                    views.setTextViewText(R.id.dm_text, dm);
                    views.setImageViewBitmap(R.id.widget_pro_pic, getCachedPic(AppSettings.getInstance(this).myProfilePicUrl));

                    mgr.updateAppWidget(appWidgetId, views);
                } catch (Exception e) {

                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetList);
            }

            stopSelf();
        }

        public Bitmap getCachedPic(String url) {
            try {
                return Glide.with(this)
                        .asBitmap()
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(
                                new CenterCrop(),
                                new RoundedCorners(96)
                        )
                        //在这里用下面的这个会导致图片显示不出来
//                        .transform(new CircleBitmapTransform(this))
//                        .transform(new RoundedCornersTransformation(16, 0))
                        .into(200,200)
                        .get();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}