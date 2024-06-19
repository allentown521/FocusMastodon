package allen.town.focus.twitter.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.compose.RetryCompose;
import allen.town.focus.twitter.api.requests.statuses.CreateStatus;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.data.sq_lite.QueuedDataSource;
import allen.town.focus.twitter.services.abstract_services.KillerIntentService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationChannelUtil;
import allen.town.focus.twitter.utils.NotificationUtils;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class ReplyFromWearService extends KillerIntentService {

    public static final String REPLY_TO_NAME = "reply_to_name";
    public static final String IN_REPLY_TO_ID = "tweet_id";
    public static final String NOTIFICATION_ID = "notification_id";

    public String users = "";
    public String message = "";
    public long tweetId = 0l;
    public int notificationId;

    public boolean finished = false;

    public ReplyFromWearService() {
        super("ReplyFromWear");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    protected void handleIntent(Intent intent) {

        final AppSettings settings = AppSettings.getInstance(this);

        // set up the tweet from the intent
        users = intent.getStringExtra(REPLY_TO_NAME);
        String message = getVoiceReply(intent);
        tweetId = intent.getLongExtra(IN_REPLY_TO_ID, 0l);
        notificationId = intent.getIntExtra(NOTIFICATION_ID, 1);


        if (message == null) {
            makeFailedNotification("Failed to get the reply.", settings);
            return;
        } else {
            this.message = users + " " + message;
        }

        boolean sent = sendTweet();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);

        if (!sent) {
            makeFailedNotification(ReplyFromWearService.this.message, settings);
        }

        try {
            MentionsDataSource.getInstance(this).markRead(tweetId);
        } catch (Exception e) {
        }

        NotificationUtils.cancelGroupedNotificationWithNoContent(this);
    }

    protected int getAccountNumber() {
        return AppSettings.getInstance(this).sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
    }

    public boolean isSecondAccount() {
        return false;
    }

    public boolean sendTweet() {
        try {
            String messageText = message.replace(users, "");

            twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(messageText);
            reply.setAutoPopulateReplyMetadata(true);
            reply.setInReplyToStatusId(tweetId);

            // no picture
            Status status;
            if (isSecondAccount()) {
                status = new StatusJSONImplMastodon(new CreateStatus(CreateStatus.parseStatusUpdate(reply)).execSecondAccountSync());
            } else {
                status = new StatusJSONImplMastodon(new CreateStatus(CreateStatus.parseStatusUpdate(reply)).execSync());
            }
            return status.getId() != 0 && status.getId() != -1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void makeFailedNotification(String text, AppSettings settings) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
                            .setSmallIcon(R.drawable.ic_stat_icon)
                            .setContentTitle(getResources().getString(R.string.tweet_failed))
                            .setContentText(getResources().getString(R.string.tap_to_retry));

            Intent resultIntent = new Intent(this, RetryCompose.class);
            QueuedDataSource.getInstance(this).createDraft(text, settings.currentAccount);
            resultIntent.setAction(Intent.ACTION_SEND);
            resultIntent.setType("text/plain");
            resultIntent.putExtra(Intent.EXTRA_TEXT, text);
            resultIntent.putExtra("failed_notification", true);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            Utils.withMutability(PendingIntent.FLAG_UPDATE_CURRENT)
                    );

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(5, mBuilder.build());
        } catch (Exception e) {

        }
    }

    public String getVoiceReply(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(NotificationUtils.EXTRA_VOICE_REPLY).toString();
        }
        return null;
    }
}
