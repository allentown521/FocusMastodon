package allen.town.focus.twitter.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.requests.accounts.GetAccountByAcct;
import allen.town.focus.twitter.api.requests.accounts.GetAccountFollowers;
import allen.town.focus.twitter.api.requests.accounts.GetOwnAccount;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.api.requests.timelines.GetHomeTimeline;
import allen.town.focus.twitter.data.sq_lite.ActivityDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.services.background_refresh.MentionsRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.redirects.RedirectToActivity;
import allen.town.focus.twitter.utils.redirects.SwitchAccountsToActivity;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class ActivityUtils {

    private static String TAG = "ActivityUtils";

    private static String GROUP_ACTIVITY = "activity_notification_group";

    public static final int NOTIFICATON_ID = 434;
    public static final int SECOND_NOTIFICATION_ID = 435;

    private Context context;
    private AppSettings settings;
    private SharedPreferences sharedPrefs;
    private boolean useSecondAccount = false;
    private int currentAccount;
    private long lastRefresh;
    private long originalTime; // if the tweets came before this time, then we don't want to show them in activity because it would just get blown up.

    private boolean separateMentionRefresh = false;
    private List<String> notificationItems = new ArrayList<>();
    private String notificationTitle = "";

    public ActivityUtils(Context context) {
        init(context);
    }

    public ActivityUtils(Context context, boolean useSecondAccount) {
        this.useSecondAccount = useSecondAccount;
        init(context);
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }

        this.context = context;
        this.sharedPrefs = AppSettings.getSharedPreferences(context);

        this.settings = AppSettings.getInstance(context);
        this.currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        if (useSecondAccount) {
            if (currentAccount == 1) {
                currentAccount = 2;
            } else {
                currentAccount = 1;
            }
        }

        this.lastRefresh = sharedPrefs.getLong("last_activity_refresh_" + currentAccount, 0l);

        this.originalTime = sharedPrefs.getLong("original_activity_refresh_" + currentAccount, 0l);

        this.notificationTitle = context.getString(R.string.new_activity) + " - @" + (useSecondAccount ? settings.secondScreenName : settings.myScreenName);
    }

    /**
     * Refresh the new followers, mentions, number of favorites, and retweeters
     *
     * @return boolean if there was something new
     */
    public boolean refreshActivity() {
        boolean newActivity = false;

        if (getMentions()) {
            newActivity = true;
        }


        if (getFollowers()) {
            newActivity = true;
        }

        List<StatusJSONImplMastodon> myTweets = getMyTweets();
        if (myTweets != null) {
            if (getRetweets(myTweets)) {
                newActivity = true;
            }

            if (getFavorites(myTweets)) {
                newActivity = true;
            }
        }

        sharedPrefs.edit().putBoolean("refresh_me_activity", true).commit();

        return newActivity;
    }

    public void postNotification() {
        postNotification(NOTIFICATON_ID);
    }

    public void postNotification(int id) {

        if (separateMentionRefresh) {
            MentionsRefreshService.startNow(context);
        }

        if (notificationItems.size() == 0) {
            return;
        }

        PendingIntent contentIntent;
        if (useSecondAccount) {
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, SwitchAccountsToActivity.class), Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, RedirectToActivity.class), Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT));
        }

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context,
                NotificationChannelUtil.INTERACTIONS_CHANNEL);
        summaryBuilder.setContentTitle(notificationTitle);
        summaryBuilder.setSmallIcon(R.drawable.ic_stat_icon);
        summaryBuilder.setContentIntent(contentIntent);

        if (notificationItems.size() > 1) {
            // inbox style
            NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
            inbox.setBigContentTitle(notificationTitle);

            for (String s : notificationItems) {
                inbox.addLine(Html.fromHtml(s));
                activityGroupNotification(contentIntent, Html.fromHtml(s));
            }

            summaryBuilder.setStyle(inbox);
            summaryBuilder.setContentText(notificationItems.size() + " " + context.getString(R.string.items));
            summaryBuilder.setGroup(GROUP_ACTIVITY);
            summaryBuilder.setGroupSummary(true);
        } else {
            // big text style
            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(Html.fromHtml(notificationItems.get(0)));
            bigText.setBigContentTitle(notificationTitle);

            summaryBuilder.setStyle(bigText);
            summaryBuilder.setContentText(Html.fromHtml(notificationItems.get(0)));
        }

        if (settings.headsUp) {
            summaryBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (settings.vibrate) {
            summaryBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        if (settings.sound) {
            try {
                summaryBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                summaryBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.led) {
            summaryBuilder.setLights(0xFFFFFF, 1000, 1000);
        }

        if (settings.wakeScreen) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire(5000);
        }

        summaryBuilder.setAutoCancel(true);


        // Light Flow notification
        NotificationUtils.sendToLightFlow(context, notificationTitle, notificationItems.get(0));

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(id, summaryBuilder.build());
    }

    private void activityGroupNotification(PendingIntent contentIntent, Spanned text) {
        NotificationCompat.Builder individualBuilder = new NotificationCompat.Builder(context,
                NotificationChannelUtil.INTERACTIONS_CHANNEL)
                .setContentTitle(context.getString(R.string.new_activity))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_ACTIVITY)
                .setGroupSummary(false);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(NotificationUtils.generateRandomId(), individualBuilder.build());
    }


    public void insertMentions(List<Status> mentions) {
        try {
            List<String> notis = ActivityDataSource.getInstance(context).insertMentions(mentions, currentAccount);

            if (notis.size() > 0) {
                separateMentionRefresh = true;
            }
//            if (settings.mentionsRefresh != 0) {
//                notificationItems.addAll(notis);
//            } else {
//                separateMentionRefresh = true;
//            }
        } catch (Throwable t) {

        }
    }


    public void insertFollowers(List<User> users) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertNewFollowers(users, currentAccount);
            if (settings.followersNot) notificationItems.add(noti);
        } catch (Throwable t) {

        }
    }

    public boolean tryInsertRetweets(Status status) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertRetweeters(status, currentAccount, useSecondAccount);

            if (noti != null) {
                if (settings.retweetNot) notificationItems.add(noti);
                return true;
            } else {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean tryInsertFavorites(Status status) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertFavoriters(status, currentAccount);

            if (noti != null) {
                if (settings.favoritesNot) notificationItems.add(noti);
                return true;
            } else {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public List<StatusJSONImplMastodon> getMyTweets() {
        try {
            return StatusJSONImplMastodon.createStatusList(
                            useSecondAccount ? new GetHomeTimeline(null, null, 20, null).execSecondAccountSync()
                                    : new GetHomeTimeline(null, null, 20, null).execSync())
                    .stream().filter(new StatusFilterPredicate(useSecondAccount ? AppSettings.getInstance(context).secondSessionId : AppSettings.getInstance(context).mySessionId, Filter.FilterContext.HOME)).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    public void commitLastRefresh(long id) {
        sharedPrefs.edit().putLong("last_activity_refresh_" + currentAccount, id).commit();
    }

    public boolean getMentions() {
        boolean newActivity = false;

        try {
            List<allen.town.focus.twitter.model.Notification> list;

            if (lastRefresh != 0L) {

                if (useSecondAccount) {
                    list = new GetNotifications("", lastRefresh > 0 ? lastRefresh + "" : "", 30, EnumSet.of(allen.town.focus.twitter.model.Notification.Type.MENTION)).execSecondAccountSync();
                } else {
                    list = new GetNotifications("", lastRefresh > 0 ? lastRefresh + "" : "", 30, EnumSet.of(allen.town.focus.twitter.model.Notification.Type.MENTION)).execSync();
                }

                List<StatusJSONImplMastodon> mentions = new ArrayList<>();
                if (list != null && list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).status != null) {
                            mentions.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                        }
                    }
                }

                List<twitter4j.Status> filteredList = mentions.stream().filter(new StatusFilterPredicate(useSecondAccount ? AppSettings.getInstance(context).secondSessionId : AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                if (list.size() > 0) {
                    insertMentions(filteredList);
                    commitLastRefresh(Long.parseLong(list.get(0).id));
                    newActivity = true;
                }
            } else {
                if (useSecondAccount) {
                    list = new GetNotifications("", "", 1, EnumSet.of(allen.town.focus.twitter.model.Notification.Type.MENTION)).execSecondAccountSync();
                } else {
                    list = new GetNotifications("", "", 1, EnumSet.of(allen.town.focus.twitter.model.Notification.Type.MENTION)).execSync();
                }

                List<StatusJSONImplMastodon> lastMention = new ArrayList<>();
                if (list != null && list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).status != null) {
                            lastMention.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                        }
                    }
                }


                if (list.size() > 0) {
                    commitLastRefresh(Long.parseLong(list.get(0).id));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newActivity;
    }


    public boolean getFollowers() {
        boolean newActivity = false;

        try {
            List<User> followers;
            if (useSecondAccount) {
                followers = UserJSONImplMastodon.createPagableUserList(new GetAccountFollowers(
                        AppSettings.getInstance(context).secondId, null, 80).execSecondAccountSync());
            } else {
                followers = UserJSONImplMastodon.createPagableUserList(new GetAccountFollowers(
                        AppSettings.getInstance(context).myId, null, 80).execSync());
            }

            User me = new UserJSONImplMastodon(useSecondAccount ? new GetOwnAccount().execSecondAccountSync() : new GetOwnAccount().execSync());

            int oldFollowerCount = sharedPrefs.getInt("activity_follower_count_" + currentAccount, 0);
            Set<String> latestFollowers = sharedPrefs.getStringSet("activity_latest_followers_" + currentAccount, new HashSet<String>());

            Log.v(TAG, "followers set size: " + latestFollowers.size());
            Log.v(TAG, "old follower count: " + oldFollowerCount);
            Log.v(TAG, "current follower count: " + me.getFollowersCount());

            List<User> newFollowers = new ArrayList<User>();
            if (latestFollowers.size() != 0) {
                for (int i = 0; i < followers.size(); i++) {
                    if (!latestFollowers.contains(followers.get(i).getScreenName())) {
                        Log.v(TAG, "inserting @" + followers.get(i).getScreenName() + " as new follower");
                        newFollowers.add(followers.get(i));
                        newActivity = true;
                    } else {
                        break;
                    }
                }
            }

            insertFollowers(newFollowers);

            latestFollowers.clear();
            for (int i = 0; i < 50; i++) {
                if (i < followers.size()) {
                    latestFollowers.add(followers.get(i).getScreenName());
                } else {
                    break;
                }
            }

            SharedPreferences.Editor e = sharedPrefs.edit();
            e.putStringSet("activity_latest_followers_" + currentAccount, latestFollowers);
            e.putInt("activity_follower_count_" + currentAccount, me.getFollowersCount());
            e.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newActivity;
    }

    public boolean getRetweets(List<StatusJSONImplMastodon> statuses) {
        boolean newActivity = false;

        for (Status s : statuses) {
            if (s.getCreatedAt().getTime() > originalTime && tryInsertRetweets(s)) {
                newActivity = true;
            }
        }

        return newActivity;
    }

    public boolean getFavorites(List<StatusJSONImplMastodon> statuses) {
        boolean newActivity = false;

        for (Status s : statuses) {
            if (s.getCreatedAt().getTime() > originalTime && tryInsertFavorites(s)) {
                newActivity = true;
            }
        }

        return newActivity;
    }
}
