package allen.town.focus.twitter.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import allen.town.focus.twitter.api.requests.statuses.SetStatusReblogged;
import allen.town.focus.twitter.services.abstract_services.KillerIntentService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationUtils;

public class RetweetService extends KillerIntentService {

    private static final String ARG_ACCOUNT_TO_RETWEET_WITH = "account_num";
    private static final String ARG_TWEET_ID = "tweet_id";
    private static final String ARG_NOTIFICATION_ID = "notification_id";

    public static Intent getIntent(Context callingContext, int accountNumberToRetweetWith, long tweetId, int notificationId) {
        Intent retweet = new Intent(callingContext, RetweetService.class);
        retweet.putExtra(ARG_ACCOUNT_TO_RETWEET_WITH, accountNumberToRetweetWith);
        retweet.putExtra(ARG_TWEET_ID, tweetId);
        retweet.putExtra(ARG_NOTIFICATION_ID, notificationId);

        return retweet;
    }

    public RetweetService() {
        super("RetweetService");
    }

    @Override
    protected void handleIntent(Intent intent) {
        int accountToFavoriteWith = intent.getIntExtra(ARG_ACCOUNT_TO_RETWEET_WITH, 1);
        long tweetId = intent.getLongExtra(ARG_TWEET_ID, 1);
        int notificationId = intent.getIntExtra(ARG_NOTIFICATION_ID, 1);

        try {
            if (accountToFavoriteWith == AppSettings.getInstance(this).currentAccount) {
                new SetStatusReblogged(tweetId + "", true).execSync();
            } else {
                new SetStatusReblogged(tweetId + "", true).execSecondAccountSync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        notificationManager.cancel(notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationUtils.cancelGroupedNotificationWithNoContent(this);
        }
    }
}
