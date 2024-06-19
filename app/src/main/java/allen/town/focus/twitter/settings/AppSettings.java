package allen.town.focus.twitter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.data.EmojiStyle;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.text.EmojiInitializer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import code.name.monkey.retromusic.extensions.FragmentExtensionsUtils;
import code.name.monkey.retromusic.util.theme.ThemeMode;
import xyz.klinker.android.drag_dismiss.util.AndroidVersionUtils;

public class AppSettings {

    public static AppSettings settings;

    public static AppSettings getInstance(Context context) {
        if (settings == null) {
            settings = new AppSettings(context);
        }
        return settings;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        if (context == null) {
            return null;
        }

        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void invalidate() {
        settings = null;
    }

    public SharedPreferences sharedPrefs;

    //drawer 需要显示的items
    public static final String DRAWER_SHOWN_ITEMS = "drawer_elements_shown_";
    public static final String DEFAULT_TIMELINE_PAGE = "default_timeline_page_";
    public static final String CURRENT_ACCOUNT = "current_account";
    public static final String OPEN_WHAT_PAGE = "open_what_page";
    public static final String OPEN_A_PAGE = "open_a_page";
    public static final String FONT_TYPE = "font_type";
    public static final String EMOJI_STYLE = "emoji_style";
    public static final String SHOULD_REFRESH = "should_refresh";
    public static final String MUTED_USERS_ID = "muted_users";
    public static final String MUFFLED_USERS_ID = "muffled_users";
    public static final String MUTED_REGEX = "muted_regex";
    public static final String SHOW_MUTED_MENTIONS = "show_muted_mentions";
    public static final String ACCOUNT_ID = "accountId";
    public static final String SYNC_INTERVAL = "sync_interval";
    public static final String INTERCEPT_TWITTER = "noti_intercept_twitter";

    public static final String BROADCAST_MARK_POSITION = "allen.town.focus.twitter.MARK_POSITION";
    public static final String FROM_DRAW = "from_drawer";
    public static final String PAGE_TO_OPEN = "page_to_open";
    public static final String DONT_REFRESH = "dont_refresh";
    public static final String REFRESH_ME = "refresh_me";
    public static final String REFRESH_ME_DM = "refresh_me_dm";
    public static final String DM_UNREAD_STARTER = "dm_unread_";
    public static final String NUMBER_NEW = "number_new";
    public static final String REFRESH_ME_LIST_STARTER = "refresh_me_list_";
    public static final String REFRESH_ME_MENTIONS = "refresh_me_mentions";
    public static final String REFRESH_ME_USER_STARTER = "refresh_me_user_";
    public static final String LAST_DIRECT_MESSAGE_STARTER ="last_direct_message_id_";
    public static final String WEB_PREVIEW_TIMELINE_KEY ="web_previews_timeline";
    public static final String NEED_ADD_LIST_HEADER ="need_add_list_header";

    public static final int THEME_RED = 0;
    public static final int THEME_YELLOW = 12;
    public static final int THEME_BLACK = 19;
    public static final int THEME_DARK_BACKGROUND_COLOR = THEME_RED;
    public static final int THEME_WHITE = 21;

    public static final int DEFAULT_THEME = THEME_RED;
    public static final int DEFAULT_MAIN_THEME = AndroidVersionUtils.isAndroidQ() ? 4 : 1; // 0 = light, 1 = dark, 2 = black (Android Q is a bit different)

    public static final int WIDGET_LIGHT = 0;
    public static final int WIDGET_DARK = 1;
    public static final int WIDGET_TRANS_LIGHT = 2;
    public static final int WIDGET_TRANS_BLACK = 3;
    public static final int WIDGET_MATERIAL_LIGHT = 4;
    public static final int WIDGET_MATERIAL_DARK = 5;

    public static final int PAGE_TWEET = 0;
    public static final int PAGE_WEB = 1;
    public static final int PAGE_CONVO = 2;

    public static final int PICTURES_NORMAL = 0;
    public static final int PICTURES_SMALL = 1;
    public static final int PICTURES_NONE = 2;
    public static final int CONDENSED_TWEETS = 3;
    public static final int CONDENSED_NO_IMAGES = 4;
    public static final int REVAMPED_TWEETS = 5;

    public static final int PAGE_TYPE_NONE = 0;
    public static final int PAGE_TYPE_PICS = 1;//图片时间线
    public static final int PAGE_TYPE_LINKS = 2;//链接时间线
    public static final int PAGE_TYPE_LIST_TIMELINE = 3;//列表时间线
    public static final int PAGE_TYPE_FAV_USERS_TWEETS = 4;//收藏用户的嘟文
    public static final int PAGE_TYPE_HOME = 5;//主页
    public static final int PAGE_TYPE_MENTIONS = 6;//提及
    public static final int PAGE_TYPE_DMS = 7;//私信
    public static final int PAGE_TYPE_SECOND_MENTIONS = 8;//第二个账号提及
    public static final int PAGE_TYPE_WORLD_TIMELINE = 9;//跨站时间线
    public static final int PAGE_TYPE_LOCAL_TIMELINE = 10;//本地时间线
    public static final int PAGE_TYPE_HASHTAGS = 11;//热门hashtags
    public static final int PAGE_TYPE_ACTIVITY = 12;//活动
    public static final int PAGE_TYPE_FAVORITE_STATUS = 13;//喜欢的嘟文
    public static final int PAGE_TYPE_USER_TWEETS = 14;//已选择用户的嘟文
    public static final int PAGE_TYPE_SAVED_TWEETS = 15;//保存的嘟文
    public static final int PAGE_TYPE_BOOKMARKED_TWEETS = 16;//加书签的嘟文
    public static final int PAGE_TYPE_LIST = 17;//列表
    public static final int PAGE_TYPE_RETWEET = 18;//转推
    public static final int PAGE_TYPE_FAV_USERS = 19;//收藏用户列表
    public static final int PAGE_TYPE_DISCOVER = 20;//发现

    public static final int LAYOUT_Focus_for_Mastodon = 0;
    public static final int LAYOUT_HANGOUT = 1;
    public static final int LAYOUT_FULL_SCREEN = 2;

    public static final int QUOTE_STYLE_TWITTER = 0;
    public static final int QUOTE_STYLE_Focus_for_Mastodon = 1;
    public static final int QUOTE_STYLE_RT = 2;
    public static final int QUOTE_STYLE_VIA = 3;

    public static final int NAV_BAR_AUTOMATIC = 0;
    public static final int NAV_BAR_PRESENT = 1;
    public static final int NAV_BAR_NONE = 2;

    public static final int AUTOPLAY_ALWAYS = 0;
    public static final int AUTOPLAY_WIFI = 1;
    public static final int AUTOPLAY_NEVER = 2;

    public String authenticationToken;
    public String authenticationTokenSecret;
    public String secondAuthToken;
    public String secondAuthTokenSecret;
    public String myScreenName;
    public String secondScreenName;
    public String myName;
    public String secondName;
    public String myBackgroundUrl;
    public String myProfilePicUrl;
    public String secondProfilePicUrl;

    public boolean darkTheme;
    public boolean blackTheme;
    public boolean isTwitterLoggedIn;
    public boolean reverseClickActions;
    public boolean advanceWindowed;
    public boolean notifications;
    public boolean interceptTwitterNotifications;
    public boolean led;
    public boolean vibrate;
    public boolean sound;
    public boolean refreshOnStart;
    public boolean swipeHideNavigation;
    public boolean autoTrim;
    public boolean uiExtras;
    public boolean wakeScreen;
    public boolean nightMode;
    public boolean militaryTime;
    public boolean syncMobile;
    public boolean useEmoji;
    public boolean inlinePics;
    public boolean extraPages;
    public boolean fullScreenBrowser;
    public boolean favoriteUserNotifications;
    public boolean syncSecondMentions;
    public boolean displayScreenName;
    public boolean inAppBrowser;
    public boolean showBoth;
    public boolean absoluteDate;
    public boolean useToast;
    public boolean autoInsertHashtags;
    public boolean alwaysCompose;
    public boolean twitlonger;
    public boolean twitpic;
    public boolean tweetmarker;
    public boolean tweetmarkerManualOnly;
    public boolean jumpingWorkaround;
    public boolean floatingCompose;
    public boolean openKeyboard;
    public boolean alwaysMobilize;
    public boolean mobilizeOnData;
    public boolean preCacheImages;
    public boolean fastTransitions;
    public boolean topDown;
    public boolean headsUp;
    public boolean useSnackbar;
    public boolean crossAccActions;
    public boolean useInteractionDrawer;
    public boolean staticUi;
    public boolean higherQualityImages;
    public boolean useMentionsOnWidget;
    public boolean widgetImages;
    public boolean usePeek;
    public boolean dualPanels;
    public boolean detailedQuotes;
    public boolean fingerprintLock;
    public boolean followersOnlyAutoComplete;
    public boolean autoCompleteHashtags;
    public boolean largerWidgetImages;
    public boolean showProfilePictures;
    public boolean compressReplies;
    public boolean cropImagesOnTimeline;
    public boolean webPreviews;
    public boolean widgetDisplayScreenname;
    public boolean onlyAutoPlayGifs;
    public boolean alwaysShowButtons;
    public boolean dragDismiss;

    // notifications
    public boolean timelineNot;
    public boolean mentionsNot;
    public boolean dmsNot;
    public boolean followersNot;
    public boolean favoritesNot;
    public boolean retweetNot;
    public boolean activityNot;
    public String ringtone;

    // theme stuff
    public boolean addonTheme;
    public String addonThemePackage;
    public boolean roundContactImages;
    public int backgroundColor;
    public boolean translateProfileHeader;
    public boolean nameAndHandleOnTweet = false;
    public boolean combineProPicAndImage = false;
    public boolean sendToComposeWindow = false;
    public boolean showTitleStrip = true;
    public String accentColor;
    public int accentInt;
    public int pagerTitleInt;
    public Drawable actionBar = null;
    public Drawable customBackground = null;

    public int theme;
    public int layout;
    public int currentAccount;
    public int textSize;
    public int widgetTextSize;
    public int maxTweetsRefresh;
    public int timelineSize;
    public int mentionsSize;
    public int dmSize;
    public int listSize;
    public int userTweetsSize;
    public int numberOfAccounts = 0;
    public int pageToOpen;
    public int quoteStyle;
    public int navBarOption;
    public int picturesType;
    public int autoplay;
    public int widgetAccountNum;
    public int lineSpacingScalar;

    public long syncInterval;
    public String myId;
    //这种写法不对，自己当前域看到的accountId和其他域看到的不一样
    public String secondId;

    public String mySessionId;
    public String secondSessionId;

    public String translateUrl;
    public String browserSelection;

    public EmojiStyle emojiStyle;
    public int tweetCharacterCount = 500;

    public AppSettings(Context context) {
        sharedPrefs = getSharedPreferences(context);
        setPrefs(sharedPrefs, context);
    }

    public AppSettings(SharedPreferences sharedPrefs, Context context) {
        setPrefs(sharedPrefs, context);
    }

    public void setPrefs(SharedPreferences sharedPrefs, Context context) {
        // Strings
        if (sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1) == 1) {
            authenticationToken = sharedPrefs.getString("authentication_token_1", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_1", "none");
            secondAuthToken = sharedPrefs.getString("authentication_token_2", "none");
            secondAuthTokenSecret = sharedPrefs.getString("authentication_token_secret_2", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_1", "");
            secondScreenName = sharedPrefs.getString("twitter_screen_name_2", "");
            myName = sharedPrefs.getString("twitter_users_name_1", "");
            secondName = sharedPrefs.getString("twitter_users_name_2", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_1", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_1", "");
            secondProfilePicUrl = sharedPrefs.getString("profile_pic_url_2", "");
            myId = sharedPrefs.getString("twitter_id_1", "0");
            secondId = sharedPrefs.getString("twitter_id_2", "0");
            mySessionId = sharedPrefs.getString("session_id_1", "0");
            secondSessionId = sharedPrefs.getString("session_id_2", "0");
        } else {
            authenticationToken = sharedPrefs.getString("authentication_token_2", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_2", "none");
            secondAuthToken = sharedPrefs.getString("authentication_token_1", "none");
            secondAuthTokenSecret = sharedPrefs.getString("authentication_token_secret_1", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_2", "");
            secondScreenName = sharedPrefs.getString("twitter_screen_name_1", "");
            myName = sharedPrefs.getString("twitter_users_name_2", "");
            secondName = sharedPrefs.getString("twitter_users_name_1", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_2", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_2", "");
            secondProfilePicUrl = sharedPrefs.getString("profile_pic_url_1", "");
            myId = sharedPrefs.getString("twitter_id_2", "0");
            secondId = sharedPrefs.getString("twitter_id_1", "0");
            mySessionId = sharedPrefs.getString("session_id_2", "0");
            secondSessionId = sharedPrefs.getString("session_id_1", "0");
        }

        // Booleans
        ThemeMode themeMode = FragmentExtensionsUtils.getGeneralThemeValue(context);
        if (themeMode == ThemeMode.BLACK) {
            darkTheme = true;
            blackTheme = true;
        } else if (themeMode == ThemeMode.DARK) {
            darkTheme = true;
            blackTheme = false;
        } else if (themeMode == ThemeMode.LIGHT) {
            darkTheme = false;
            blackTheme = false;
        } else if (themeMode == ThemeMode.AUTO) {
            int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            darkTheme = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
            nightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        }


        isTwitterLoggedIn = sharedPrefs.getBoolean("is_logged_in_1", false) || sharedPrefs.getBoolean("is_logged_in_2", false);
        reverseClickActions = sharedPrefs.getBoolean("reverse_click_actions", false);
        advanceWindowed = sharedPrefs.getBoolean("advance_windowed", true);
        led = sharedPrefs.getBoolean("led", true);
        sound = sharedPrefs.getBoolean("sound", true);
        vibrate = sharedPrefs.getBoolean("vibrate", true);
        headsUp = sharedPrefs.getBoolean("heads_up", false);
        refreshOnStart = sharedPrefs.getBoolean("refresh_on_start", true);
        swipeHideNavigation = sharedPrefs.getBoolean("swipe_hide_navigation", true);
        autoTrim = sharedPrefs.getBoolean("auto_trim", true);
        uiExtras = sharedPrefs.getBoolean("ui_extras", true);
        wakeScreen = sharedPrefs.getBoolean("wake", true) && !Utils.isAndroidO();
        militaryTime = sharedPrefs.getBoolean("military_time", false);
        syncMobile = sharedPrefs.getBoolean("sync_mobile_data", true);
        extraPages = sharedPrefs.getBoolean("extra_pages", true);
        fullScreenBrowser = sharedPrefs.getBoolean("full_screen_browser", true);
        favoriteUserNotifications = sharedPrefs.getBoolean("favorite_users_notifications", true);
        syncSecondMentions = sharedPrefs.getBoolean("sync_second_mentions", true);
        displayScreenName = sharedPrefs.getBoolean("display_screen_name", false);
        inAppBrowser = sharedPrefs.getBoolean("inapp_browser", true);
        showBoth = sharedPrefs.getBoolean("both_handle_name", false);
        timelineNot = sharedPrefs.getBoolean("timeline_notifications", true);
        mentionsNot = sharedPrefs.getBoolean("mentions_notifications", true);
        dmsNot = sharedPrefs.getBoolean("direct_message_notifications", true);
        favoritesNot = sharedPrefs.getBoolean("favorite_notifications", true);
        retweetNot = sharedPrefs.getBoolean("retweet_notifications", true);
        followersNot = sharedPrefs.getBoolean("follower_notifications", true);
        absoluteDate = sharedPrefs.getBoolean("absolute_date", false);
        useToast = sharedPrefs.getBoolean("use_toast", true);
        autoInsertHashtags = sharedPrefs.getBoolean("auto_insert_hashtags", false);
        alwaysCompose = sharedPrefs.getBoolean("always_compose", false);
        twitlonger = false;
        twitpic = false;//sharedPrefs.getBoolean("twitpic", false);
        jumpingWorkaround = sharedPrefs.getBoolean("jumping_workaround", false);
        floatingCompose = sharedPrefs.getBoolean("floating_compose", true);
        openKeyboard = sharedPrefs.getBoolean("open_keyboard", false);
        preCacheImages = !sharedPrefs.getString("pre_cache", "1").equals("0");
        topDown = sharedPrefs.getBoolean("top_down_mode", false);
        useSnackbar = sharedPrefs.getBoolean("use_snackbar", false);
        //不用账号看到的同一篇帖子id不一样
        crossAccActions = sharedPrefs.getBoolean("fav_rt_multiple_accounts", false);
        activityNot = sharedPrefs.getBoolean("activity_notifications", true);
        useInteractionDrawer = sharedPrefs.getBoolean("interaction_drawer", true);
        staticUi = sharedPrefs.getBoolean("static_ui", false);
        higherQualityImages = sharedPrefs.getBoolean("high_quality_images", true);
        useMentionsOnWidget = sharedPrefs.getString("widget_timeline", "0").equals("1");
        widgetImages = sharedPrefs.getBoolean("widget_images", true);
        usePeek = sharedPrefs.getBoolean("use_peek", true);
        dualPanels = sharedPrefs.getBoolean("dual_panel", context.getResources().getBoolean(R.bool.dual_panels));
        detailedQuotes = sharedPrefs.getBoolean("detailed_quotes", false);
        browserSelection = sharedPrefs.getString("browser_selection", context.getResources().getString(R.string.custom_tab_title));
        fingerprintLock = sharedPrefs.getBoolean("fingerprint_lock", false);
        followersOnlyAutoComplete = sharedPrefs.getBoolean("followers_only_auto_complete", false);
        autoCompleteHashtags = sharedPrefs.getBoolean("hashtag_auto_complete", true);
        largerWidgetImages = sharedPrefs.getBoolean("widget_larger_images", false);
        showProfilePictures = sharedPrefs.getBoolean("show_profile_pictures", true);
        compressReplies = sharedPrefs.getBoolean("new_twitter_replies", true);
        cropImagesOnTimeline = sharedPrefs.getBoolean("crop_images_timeline", true);
        webPreviews = sharedPrefs.getBoolean(WEB_PREVIEW_TIMELINE_KEY, false) && App.getInstance().checkSupporter(context,false);
        widgetDisplayScreenname = sharedPrefs.getBoolean("widget_display_screenname", true);
        onlyAutoPlayGifs = sharedPrefs.getBoolean("autoplay_gifs", false);
        alwaysShowButtons = sharedPrefs.getBoolean("always_show_tweet_buttons", false);
        dragDismiss = sharedPrefs.getBoolean("drag_dismiss", true);

        if (EmojiInitializer.INSTANCE.isAlreadyUsingGoogleAndroidO()) {
            this.emojiStyle = EmojiStyle.DEFAULT;
        } else {
            String emojiStyle = sharedPrefs.getString(AppSettings.EMOJI_STYLE, "android_o");
            switch (emojiStyle) {
                case "android_o":
                    this.emojiStyle = EmojiStyle.ANDROID_O;
                    break;
                default:
                    this.emojiStyle = EmojiStyle.DEFAULT;
            }
        }

        notifications = true;

        interceptTwitterNotifications = sharedPrefs.getBoolean(INTERCEPT_TWITTER, false);

        if (sharedPrefs.getString("pre_cache", "1").equals("2")) {
            sharedPrefs.edit().putBoolean("pre_cache_wifi_only", true).commit();
        } else {
            sharedPrefs.edit().putBoolean("pre_cache_wifi_only", false).commit();
        }

        tweetmarker = false;
        tweetmarkerManualOnly = false;

        // set up the mobilized (plain text) browser
        String mobilize = sharedPrefs.getString("plain_text_browser", "0");
        if (mobilize.equals("0")) {
            alwaysMobilize = false;
            mobilizeOnData = false;
        } else if (mobilize.equals("1")) {
            alwaysMobilize = true;
            mobilizeOnData = true;
        } else {
            alwaysMobilize = false;
            mobilizeOnData = true;
        }

        picturesType = Integer.parseInt(sharedPrefs.getString("timeline_pictures", "0"));
        if (picturesType == PICTURES_NONE || picturesType == CONDENSED_NO_IMAGES) {
            inlinePics = false;
        } else {
            inlinePics = true;
        }


        ringtone = sharedPrefs.getString("ringtone",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

        useEmoji = true;

        // Integers
        currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
        try {
            theme = sharedPrefs.getInt("material_theme_" + currentAccount, DEFAULT_THEME);
        } catch (ClassCastException e) {
            theme = Integer.parseInt(sharedPrefs.getString("material_theme_" + currentAccount, "" + DEFAULT_THEME));
        }
        layout = LAYOUT_FULL_SCREEN;
        textSize = Integer.parseInt(sharedPrefs.getString("text_size", "14"));
        widgetTextSize = Integer.parseInt(sharedPrefs.getString("widget_text_size", "14"));
        maxTweetsRefresh = Integer.parseInt(sharedPrefs.getString("max_tweets", "1"));
        timelineSize = Integer.parseInt(sharedPrefs.getString("timeline_size", "500"));
        mentionsSize = Integer.parseInt(sharedPrefs.getString("mentions_size", "100"));
        dmSize = Integer.parseInt(sharedPrefs.getString("dm_size", "100"));
        listSize = Integer.parseInt(sharedPrefs.getString("list_size", "200"));
        userTweetsSize = Integer.parseInt(sharedPrefs.getString("user_tweets_size", "200"));
        pageToOpen = Integer.parseInt(sharedPrefs.getString("viewer_page", "0"));
        quoteStyle = Integer.parseInt(sharedPrefs.getString("quote_style", "0"));
        navBarOption = Integer.parseInt(sharedPrefs.getString("nav_bar_option", "0"));
        autoplay = Integer.parseInt(sharedPrefs.getString("autoplay", AUTOPLAY_NEVER + ""));
        lineSpacingScalar = picturesType == CONDENSED_TWEETS ? 3 : Integer.parseInt(sharedPrefs.getString("line_spacing", "5"));

        String widgetAccount = sharedPrefs.getString("widget_account", "").replace("@", "");
        if (widgetAccount.equals(myScreenName.replace("@", "")) || widgetAccount.isEmpty()) {
            widgetAccountNum = currentAccount;
        } else {
            if (currentAccount == 1) {
                widgetAccountNum = 2;
            } else {
                widgetAccountNum = 1;
            }
        }

        // Longs
        syncInterval = Long.parseLong(sharedPrefs.getString(SYNC_INTERVAL, "1800000"));

        translateUrl = sharedPrefs.getString("translate_url", "https://translate.google.com/#view=home&op=translate&sl=auto&tl=en&text=");


        if (sharedPrefs.getBoolean("quiet_hours", false)) {
            int quietStartHour = sharedPrefs.getInt("quiet_start_hour", 22);
            int quietStartMin = sharedPrefs.getInt("quiet_start_min", 0);
            int quietEndHour = sharedPrefs.getInt("quiet_end_hour", 6);
            int quietEndMin = sharedPrefs.getInt("quiet_end_min", 0);

            if (isInsideRange(quietStartHour, quietStartMin, quietEndHour, quietEndMin)) {
                Log.v("quiet_hours", "quiet hours on");
                notifications = false;
                timelineNot = false;
                mentionsNot = false;
                favoritesNot = false;
                retweetNot = false;
                followersNot = false;
                dmsNot = false;
                activityNot = false;
            }
        }

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            numberOfAccounts++;
        }
        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            numberOfAccounts++;
        }

        if (numberOfAccounts != 2) {
            syncSecondMentions = false;
            crossAccActions = false;
        }


        if (revampedTweets()) {
            detailedQuotes = true;
        }

        if (sharedPrefs.getBoolean("data_saver_mode", false)) {
            refreshOnStart = false;
            syncMobile = false;
            syncSecondMentions = false;
            higherQualityImages = false;
            webPreviews = false;

            if (autoplay == AUTOPLAY_ALWAYS) {
                autoplay = AUTOPLAY_WIFI;
            }
        }
    }

    private static boolean isInsideRange(int startHour, int startMin, int endHour, int endMin) {

        String pattern = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        try {
            Date start = sdf.parse(startHour + ":" + startMin);
            Date end = sdf.parse(endHour + ":" + endMin);
            Date current = sdf.parse(hour + ":" + minutes);

            Log.v("date_range", "current: " + current.toString() + ", start: " + start.toString() + ", end: " + end.toString());

            // we expect that the start date will be something like 22 and the end will be 6
            if (start.after(end)) {
                return current.after(start) || current.before(end);
            } else { // but some people could do quiet hours during the day, so start = 9 and end = 17
                return current.after(start) && current.before(end);
            }
        } catch (Exception e) {
            return false;
        }
    }


    protected void setValue(String key, boolean value, Context context) {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);

        sharedPreferences.edit()
                .putBoolean(key, value)
                .commit();
    }

    protected void setValue(String key, int value, Context context) {
        try {
            SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);


            sharedPreferences.edit()
                    .putInt(key, value)
                    .commit();
        } catch (Exception e) {

        }

    }

    protected void setValue(String key, String value, Context context) {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);


        sharedPreferences.edit()
                .putString(key, value)
                .commit();

    }


    public static boolean dualPanels(Context context) {
        AppSettings settings = AppSettings.getInstance(context);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                settings.dualPanels) {
            return true;
        } else {
            return false;
        }
    }

    //TODO: 这里不知道作用
    public static boolean isWhiteToolbar(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        return settings.theme == AppSettings.THEME_WHITE || settings.theme == THEME_YELLOW;
    }

    public boolean condensedTweets() {
        return picturesType == CONDENSED_NO_IMAGES || picturesType == CONDENSED_TWEETS;
    }

    public boolean revampedTweets() {
        return picturesType == REVAMPED_TWEETS;
    }

    public static boolean isLimitedTweetCharLanguage() {
        String systemLanguage = Locale.getDefault().getLanguage();
        String[] limitingLanguages = new String[]{"ko"};

        for (String limitingLanguage : limitingLanguages) {
            if (limitingLanguage.equals(systemLanguage)) {
                return true;
            }
        }

        return false;
    }

}
