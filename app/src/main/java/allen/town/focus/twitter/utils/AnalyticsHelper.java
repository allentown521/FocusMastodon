package allen.town.focus.twitter.utils;

import android.content.Context;
import android.os.Bundle;

import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.crash.Crashlytics;

public class AnalyticsHelper {

    // LOGIN EVENTS
    private static final String START_LOGIN = "START_LOGIN";
    private static final String LOGIN_TO_TWITTER = "LOGIN_TO_TWITTER";
    private static final String FINISH_LOGIN_TO_TWITTER = "FINISH_LOGIN_TO_TWITTER";
    private static final String LOGIN_DOWNLOAD_TWEETS = "LOGIN_DOWNLOADED_TWEETS";
    private static final String FINISH_LOGIN = "FINISH_LOGIN";

    // RATE IT EVENTS
    private static final String SHOW_RATE_IT_PROMPT = "SHOW_RATE_IT_PROMPT";
    private static final String RATE_IT_ON_PLAY_STORE = "RATE_IT_ON_PLAY_STORE";

    // GENERAL LOGGING
    private static final String ERROR_LOADING_FROM_NOTIFICATION = "ERROR_LOADING_FROM_NOTIFICATION";
    private static final String APP_NOT_PURCHASED = "APP_NOT_PURCHASED";
    private static final String APP_NOT_PURCHASED_FIRST_WARNING = "APP_NOT_PURCHASED_FIRST_WARNING";
    private static final String APP_NOT_PURCHASED_LAST_WARNING = "APP_NOT_PURCHASED_LAST_WARNING";
    private static final String APP_PURCHASED = "APP_PURCHASED";

    public static void logEvent(Context context, String event) {
        Bundle bundle = new Bundle();
        logEvent(context, event, bundle);
    }

    private static void logEvent(Context context, String event, Bundle bundle) {
        bundle.putString("screenname", AppSettings.getInstance(context).myScreenName);
        Crashlytics.getInstance().log(event);
    }

    public static void startLogin(Context context) {
        logEvent(context, START_LOGIN);
    }

    public static void loginToTwitter(Context context) {
        logEvent(context, LOGIN_TO_TWITTER);
    }

    public static void finishLoginToTwitter(Context context) {
        logEvent(context, FINISH_LOGIN_TO_TWITTER);
    }

    public static void downloadTweets(Context context) {
        logEvent(context, LOGIN_DOWNLOAD_TWEETS);
    }

    public static void finishLogin(Context context) {
        logEvent(context, FINISH_LOGIN);
    }

    public static void showRateItPrompt(Context context) {
        logEvent(context, SHOW_RATE_IT_PROMPT);
    }

    public static void rateItOnPlayStore(Context context) {
        logEvent(context, RATE_IT_ON_PLAY_STORE);
    }

    public static void errorLoadingTweetFromNotification(Context context, String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putString("error_message", errorMessage);
        logEvent(context, ERROR_LOADING_FROM_NOTIFICATION);
    }

    public static void appNotPurchased(Context context) {
        logEvent(context, APP_NOT_PURCHASED);
    }

    public static void appNotPurchasedFirstWarning(Context context) {
        logEvent(context, APP_NOT_PURCHASED_FIRST_WARNING);
    }

    public static void appNotPurchasedLastWarning(Context context) {
        logEvent(context, APP_NOT_PURCHASED_LAST_WARNING);
    }

    public static void appPurchased(Context context) {
        logEvent(context, APP_PURCHASED);
    }
}
