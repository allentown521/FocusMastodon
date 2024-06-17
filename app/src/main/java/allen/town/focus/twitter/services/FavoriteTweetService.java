package allen.town.focus.twitter.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import allen.town.focus.twitter.api.requests.statuses.SetStatusFavorited;
import allen.town.focus.twitter.services.abstract_services.KillerIntentService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationUtils;

public class FavoriteTweetService extends KillerIntentService {

    private static final String ARG_ACCOUNT_TO_FAVORITE_WITH = "account_num";
    private static final String ARG_TWEET_ID = "tweet_id";
    private static final String ARG_NOTIFICATION_ID = "notification_id";

    public static Intent getIntent(Context callingContext, int accountNumberToFavoriteWith, long tweetId, int notificationId) {
        Intent favorite = new Intent(callingContext, FavoriteTweetService.class);
        favorite.putExtra(ARG_ACCOUNT_TO_FAVORITE_WITH, accountNumberToFavoriteWith);
        favorite.putExtra(ARG_TWEET_ID, tweetId);
        favorite.putExtra(ARG_NOTIFICATION_ID, notificationId);

        return favorite;
    }

    public FavoriteTweetService() {
        super("FavoriteTweetService");
    }

    @Override
    protected void handleIntent(Intent intent) {
        int accountToFavoriteWith = intent.getIntExtra(ARG_ACCOUNT_TO_FAVORITE_WITH, 1);
        long tweetId = intent.getLongExtra(ARG_TWEET_ID, 1);
        int notificationId = intent.getIntExtra(ARG_NOTIFICATION_ID, 1);


        try {
            if (accountToFavoriteWith == AppSettings.getInstance(this).currentAccount) {
                new SetStatusFavorited(tweetId + "", true).execSync();
            } else {
                new SetStatusFavorited(tweetId + "", true).execSecondAccountSync();
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
