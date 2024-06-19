package allen.town.focus.twitter.services;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import allen.town.focus.twitter.services.background_refresh.DirectMessageRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.ActivityUtils;

public class NotificationListener extends NotificationListenerService {

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (AppSettings.getInstance(this).interceptTwitterNotifications &&
                sbn.getPackageName().equals("org.joinmastodon.android")) {
            new Thread(() -> {
                ActivityUtils utils = new ActivityUtils(NotificationListener.this);
                boolean newActivity = utils.refreshActivity();

                if (newActivity) {
                    utils.postNotification();
                }

                DirectMessageRefreshService.startNow(NotificationListener.this);
            }).start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cancelNotification(sbn.getKey());
            } else {
                cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}
