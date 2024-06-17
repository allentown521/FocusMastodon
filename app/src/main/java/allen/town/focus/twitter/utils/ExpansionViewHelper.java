package allen.town.focus.twitter.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.Html;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.BrowserActivity;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.SavedTweetsFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.statuses.DeleteStatus;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.api.requests.statuses.GetStatusContext;
import allen.town.focus.twitter.data.sq_lite.BookmarkedTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.DMDataSource;
import allen.town.focus.twitter.data.sq_lite.FavoriteTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.ListDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.data.sq_lite.SavedTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.UserTweetsDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.StatusContext;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.views.TweetView;
import allen.town.focus.twitter.views.popups.ConversationPopupLayout;
import allen.town.focus.twitter.views.popups.TweetInteractionsPopup;
import allen.town.focus.twitter.views.popups.WebPopupLayout;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import allen.town.focus_common.util.ThemeUtils;
import allen.town.focus_common.util.Timber;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import rx.Observable;
import rx.schedulers.Schedulers;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class ExpansionViewHelper {

    private static final int CONVO_CARD_LIST_SIZE = 50;

    public interface TweetLoaded {
        void onLoad(Status status);
    }

    private TweetLoaded loadedCallback;

    public void setLoadCallback(TweetLoaded callback) {
        this.loadedCallback = callback;
    }

    Context context;
    AppSettings settings;
    public long id;

    // root view
    private View expansion;

    // area that is used for the previous tweets in the conversation
    private View inReplyToArea;
    private LinearLayout inReplyToTweets;

    private View countsView;
    private View buttonsRoot;
    private TextView tweetCounts;
    private ImageButton overflowButton;
    private TextView repliesText;
    private View repliesButton;

    private ListView replyList;
    private LinearLayout convoSpinner;
    private View convoLayout;

    private FontPrefTextView tweetSource;

    private ConversationPopupLayout convoPopup;
    private WebPopupLayout webPopup;
    private TweetInteractionsPopup interactionsPopup;

    private View convoProgress;
    private FrameLayout convoCard;
    private CardView embeddedTweetCard;
    private LinearLayout convoTweetArea;

    private boolean landscape;

    private TweetButtonUtils tweetButtonUtils;

    public ExpansionViewHelper(Context context, long tweetId) {
        this.tweetButtonUtils = new TweetButtonUtils(context);
        this.context = context;
        this.settings = AppSettings.getInstance(context);
        this.id = tweetId;

        // get the base view
        expansion = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet_expansion, null, false);
        landscape = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        setViews();
        setClicks();
        getInfo();
    }

    private void setViews() {
        countsView = expansion.findViewById(R.id.counts_layout);
        buttonsRoot = expansion.findViewById(R.id.tweet_buttons);

        tweetCounts = (TextView) expansion.findViewById(R.id.tweet_counts);
        repliesButton = expansion.findViewById(R.id.show_all_tweets_button);
        repliesText = (TextView) expansion.findViewById(R.id.replies_text);
        overflowButton = (ImageButton) expansion.findViewById(R.id.overflow_button);

        tweetSource = (FontPrefTextView) expansion.findViewById(R.id.tweet_source);

        ((LinearLayout.LayoutParams) repliesText.getLayoutParams()).bottomMargin = Utils.getStatusBarHeight(context);

        repliesText.setTextColor(ThemeStore.accentColor(context));

        convoLayout = ((Activity) context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        replyList = (ListView) convoLayout.findViewById(R.id.listView);
        convoSpinner = (LinearLayout) convoLayout.findViewById(R.id.spinner);

        if (settings.darkTheme) {
            expansion.findViewById(R.id.compose_button).setAlpha(.75f);
        }

        tweetSource.setOnClickListener(v -> {
            if (status != null) {
                // we allow them to mute the client
                final String client = android.text.Html.fromHtml(status.getSource()).toString();
                new AccentMaterialDialog(
                        context,
                        R.style.MaterialAlertDialogTheme
                )
                        .setTitle(context.getResources().getString(R.string.mute_client) + "?")
                        .setMessage(client)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                            SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


                            String current = sharedPrefs.getString("muted_clients", "");
                            sharedPrefs.edit().putString("muted_clients", current + client + "   ").commit();
                            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();

                            dialogInterface.dismiss();

                            ((Activity) context).finish();

                            if (context instanceof DrawerActivity) {
                                context.startActivity(new Intent(context, MainActivity.class));
                                ((Activity) context).overridePendingTransition(0, 0);
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                        .create()
                        .show();
            } else {
                // tell them the client hasn't been found
                TopSnackbarUtil.showSnack(context, R.string.client_not_found, Toast.LENGTH_SHORT);
            }
        });

        convoProgress = expansion.findViewById(R.id.convo_spinner);
        convoCard = (FrameLayout) expansion.findViewById(R.id.convo_card);
        embeddedTweetCard = (CardView) expansion.findViewById(R.id.embedded_tweet_card);
        convoTweetArea = (LinearLayout) expansion.findViewById(R.id.tweets_content);
    }

    private void setClicks() {
        repliesButton.setOnClickListener(view -> {
            if (status != null) {
                if (convoPopup == null) {
                    convoPopup = new ConversationPopupLayout(context, convoLayout);
                    if (context.getResources().getBoolean(R.bool.isTablet)) {
                        if (landscape) {
                            convoPopup.setWidthByPercent(.6f);
                            convoPopup.setHeightByPercent(.8f);
                        } else {
                            convoPopup.setWidthByPercent(.85f);
                            convoPopup.setHeightByPercent(.68f);
                        }
                        convoPopup.setCenterInScreen();
                    }
                }

                if (adapter != null && adapter.getCount() >= CONVO_CARD_LIST_SIZE) {
                    replyList.setSelection(CONVO_CARD_LIST_SIZE - 1);
                }

                isRunning = true;

                convoPopup.setExpansionPointForAnim(view);
                convoPopup.show();
            } else {
                TopSnackbarUtil.showSnack(context, "Loading Tweet...", Toast.LENGTH_SHORT);
            }
        });

        tweetCounts.setOnClickListener(v -> {
            if (interactionsPopup == null) {
                interactionsPopup = new TweetInteractionsPopup(context);
                if (context.getResources().getBoolean(R.bool.isTablet)) {
                    if (landscape) {
                        interactionsPopup.setWidthByPercent(.6f);
                        interactionsPopup.setHeightByPercent(.8f);
                    } else {
                        interactionsPopup.setWidthByPercent(.85f);
                        interactionsPopup.setHeightByPercent(.68f);
                    }
                    interactionsPopup.setCenterInScreen();
                }
            }

            interactionsPopup.setExpansionPointForAnim(v);

            if (status != null) {
                interactionsPopup.setInfo(status.getUser().getScreenName(), status.getId());
            } else {
                interactionsPopup.setInfo(screenName, id);
            }

            interactionsPopup.show();
        });
    }

    private void showEmbeddedCard(TweetView view) {
        embeddedTweetCard.addView(view.getView());
        startAlphaAnimation(embeddedTweetCard,
                AppSettings.getInstance(context).darkTheme ? .75f : 1.0f);
    }

    /**
     * 显示讨论列表，但是只显示其中一部分
     *
     * @param tweets
     */
    private void showConvoCard(List<Status> tweets) {
        int numTweets;

        if (tweets.size() >= CONVO_CARD_LIST_SIZE) {
            numTweets = CONVO_CARD_LIST_SIZE;
        } else {
            numTweets = tweets.size();
        }

        if (tweets.size() > CONVO_CARD_LIST_SIZE) {
            repliesButton.setVisibility(View.VISIBLE);
        } else {
            repliesText.setVisibility(View.GONE);
            repliesButton.getLayoutParams().height = Utils.toDP(24, context);
            repliesButton.requestLayout();
        }

        View tweetDivider;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
        List<TweetView> tweetViews = new ArrayList<>();

        for (int i = 0; i < numTweets; i++) {
            TweetView v = new TweetView(context, tweets.get(i));
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            if (i != 0) {
                tweetDivider = new View(context);
                tweetDivider.setLayoutParams(params);

                tweetDivider.setBackgroundColor(ThemeUtils.getColorFromAttr(context, R.attr.drawerDividerColor));

                convoTweetArea.addView(tweetDivider);
            }

            tweetViews.add(v);
            convoTweetArea.addView(v.getView());
        }

        hideConvoProgress();
        if (numTweets != 0) {
            convoCard.setVisibility(View.VISIBLE);
        }
    }

    private void hideConvoProgress() {
        final View spinner = convoProgress;
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (spinner.getVisibility() != View.INVISIBLE) {
                    spinner.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim.setDuration(250);
        spinner.startAnimation(anim);
    }

    public String[] otherLinks;

    public void setWebLink(String[] otherLinks) {
        String webLink = null;
        this.otherLinks = otherLinks;

        ArrayList<String> webpages = new ArrayList<String>();

        if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
            for (String s : otherLinks) {
                if (!s.contains("youtu")) {
                    if (!s.contains("pic.twitt")) {
                        webpages.add(s);
                    }
                }
            }

            if (webpages.size() >= 1) {
                webLink = webpages.get(0);
            } else {
                webLink = null;
            }
        }

        if (webLink != null && webLink.contains("/status/")) {
            long embeddedTweetId = TweetLinkUtils.getTweetIdFromLink(webLink);

            if (embeddedTweetId != 0l) {
                embeddedTweetCard.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startAlphaAnimation(final View v, float finish) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, View.ALPHA, 0, finish);
        alpha.setDuration(0);
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        alpha.start();
    }

    private String screenName;
    private String statusUrl;

    public void setUser(String name) {
        screenName = name;
    }

    public void setStatusUrl(String url) {
        statusUrl = url;
    }

    private String tweet;

    public void setText(String t) {
        tweet = t;
    }

    public void setUpOverflow() {
        final PopupMenu menu = new PopupMenu(context, overflowButton);
        final boolean tweetIsSaved = SavedTweetsDataSource.getInstance(context).isTweetSaved(id, settings.currentAccount);

        if (screenName.equals(AppSettings.getInstance(context).myScreenName)) {
            // my tweet

            final int UPDATE_TWEET = 1;
            final int COPY_LINK = 2;
            final int COPY_TEXT = 3;
            final int OPEN_TO_BROWSER = 4;
            final int DELETE_TWEET = 5;
            final int SAVE_TWEET = 6;

            menu.getMenu().add(Menu.NONE, UPDATE_TWEET, Menu.NONE, context.getString(R.string.update_tweet));

            if (FeatureFlags.SAVED_TWEETS) {
                menu.getMenu().add(Menu.NONE, SAVE_TWEET, Menu.NONE, context.getString(tweetIsSaved ? R.string.remove_from_saved_tweets : R.string.save_for_later));
            }

            menu.getMenu().add(Menu.NONE, COPY_LINK, Menu.NONE, context.getString(R.string.copy_link));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));
            menu.getMenu().add(Menu.NONE, OPEN_TO_BROWSER, Menu.NONE, context.getString(R.string.open_to_browser));
            menu.getMenu().add(Menu.NONE, DELETE_TWEET, Menu.NONE, context.getString(R.string.menu_delete_tweet));

            menu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case UPDATE_TWEET:
                        updateTweet();
                        break;
                    case SAVE_TWEET:
                        if (tweetIsSaved) {
                            removeSavedTweet();
                        } else {
                            saveTweet();
                        }
                        break;
                    case DELETE_TWEET:
                        new DeleteTweet(() -> {
                            AppSettings.getInstance(context).sharedPrefs
                                    .edit().putBoolean("just_muted", true).commit();

                            ((Activity) context).finish();

                            if (context instanceof DrawerActivity) {
                                context.startActivity(new Intent(context, MainActivity.class));
                                ((Activity) context).overridePendingTransition(0, 0);
                            }
                        }).execute();

                        break;
                    case COPY_LINK:
                        copyLink();
                        break;
                    case COPY_TEXT:
                        copyText();
                        break;
                    case OPEN_TO_BROWSER:
                        openToBrowser();
                        break;
                }
                return false;
            });
        } else {
            // someone else's tweet

            final int UPDATE_TWEET = 1;
            final int COPY_LINK = 2;
            final int COPY_TEXT = 3;
            final int OPEN_TO_BROWSER = 4;
            final int TRANSLATE = 5;
            final int SAVE_TWEET = 6;

            menu.getMenu().add(Menu.NONE, UPDATE_TWEET, Menu.NONE, context.getString(R.string.update_tweet));

            if (FeatureFlags.SAVED_TWEETS) {
                menu.getMenu().add(Menu.NONE, SAVE_TWEET, Menu.NONE, context.getString(tweetIsSaved ? R.string.remove_from_saved_tweets : R.string.save_for_later));
            }

            menu.getMenu().add(Menu.NONE, COPY_LINK, Menu.NONE, context.getString(R.string.copy_link));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));
            menu.getMenu().add(Menu.NONE, OPEN_TO_BROWSER, Menu.NONE, context.getString(R.string.open_to_browser));
            menu.getMenu().add(Menu.NONE, TRANSLATE, Menu.NONE, context.getString(R.string.menu_translate));

            menu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case UPDATE_TWEET:
                        updateTweet();
                        break;
                    case SAVE_TWEET:
                        if (tweetIsSaved) {
                            removeSavedTweet();
                        } else {
                            saveTweet();
                        }
                        break;
                    case COPY_LINK:
                        copyLink();
                        break;
                    case COPY_TEXT:
                        copyText();
                        break;
                    case TRANSLATE:
                        String url;

                        String formatTweet = Html.fromHtml(tweet).toString();
                        try {
                            url = settings.translateUrl + URLEncoder.encode(formatTweet, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            url = settings.translateUrl + formatTweet;
                        }

                        final LinearLayout webLayout = (LinearLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.web_popup_layout, null, false);
                        final WebView web = (WebView) webLayout.findViewById(R.id.webview);

                        web.getSettings().setBuiltInZoomControls(true);
                        web.getSettings().setDisplayZoomControls(false);
                        web.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
                        web.getSettings().setUseWideViewPort(true);
                        web.getSettings().setLoadWithOverviewMode(true);
                        web.getSettings().setSavePassword(true);
                        web.getSettings().setSaveFormData(true);
                        web.getSettings().setJavaScriptEnabled(true);
                        web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                        web.getSettings().setPluginState(WebSettings.PluginState.OFF);

                        // enable navigator.geolocation
                        web.getSettings().setGeolocationEnabled(true);
                        web.getSettings().setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");

                        // enable Web Storage: localStorage, sessionStorage
                        web.getSettings().setDomStorageEnabled(true);

                        web.setWebViewClient(new HelloWebViewClient());

                        web.loadUrl(url);
                        if (webPopup == null) {
                            webPopup = new WebPopupLayout(context, webLayout);
                            if (context.getResources().getBoolean(R.bool.isTablet)) {
                                if (landscape) {
                                    webPopup.setWidthByPercent(.6f);
                                    webPopup.setHeightByPercent(.8f);
                                } else {
                                    webPopup.setWidthByPercent(.85f);
                                    webPopup.setHeightByPercent(.68f);
                                }
                                webPopup.setCenterInScreen();
                            }
                        }
                        webPopup.show();
                        break;
                    case OPEN_TO_BROWSER:
                        openToBrowser();
                        break;
                }
                return false;
            });
        }

        overflowButton.setOnClickListener(view -> menu.show());
    }

    private void updateTweet() {
        convoSpinner.setVisibility(View.VISIBLE);
        convoProgress.setVisibility(View.VISIBLE);
        convoTweetArea.removeAllViews();
        convoCard.setVisibility(View.GONE);
        repliesButton.setVisibility(View.GONE);

        isRunning = true;
        firstRun = true;
        cardShown = false;
        adapter = null;

        getInfo();
    }

    private void saveTweet() {
        SavedTweetsDataSource.getInstance(context).createTweet(status, settings.currentAccount);
        context.sendBroadcast(new Intent(SavedTweetsFragment.REFRESH_ACTION));

        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);
        if (sharedPreferences.getBoolean("alert_save_tweet", true)) {
            sharedPreferences.edit().putBoolean("alert_save_tweet", false).commit();
            new AccentMaterialDialog(
                    context,
                    R.style.MaterialAlertDialogTheme
            )
                    .setMessage(R.string.saved_tweet_description)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        if (context instanceof Activity) {
                            ((Activity) context).finish();
                        }
                    }).show();
        } else {
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }

    private void removeSavedTweet() {
        SavedTweetsDataSource.getInstance(context).deleteTweet(status.getId());
        context.sendBroadcast(new Intent(SavedTweetsFragment.REFRESH_ACTION));

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    private void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("mastodon_link", statusUrl);
        clipboard.setPrimaryClip(clip);

        TopSnackbarUtil.showSnack(context, R.string.copied_link, Toast.LENGTH_SHORT);
    }

    private void copyText() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("mastodon_text", tweetButtonUtils.restoreLinks(Html.fromHtml(tweet).toString()));
        clipboard.setPrimaryClip(clip);

        TopSnackbarUtil.showSnack(context, R.string.copied, Toast.LENGTH_SHORT);
    }

    private void openToBrowser() {
        Intent browser = new Intent(context, BrowserActivity.class);
        browser.putExtra("url", statusUrl);
        context.startActivity(browser);
    }

    public void setBackground(View v) {
        View background = v;

        background.setOnTouchListener((view, motionEvent) -> hidePopups());
    }

    public void setInReplyToArea(LinearLayout inReplyToArea) {
        this.inReplyToArea = inReplyToArea;
        this.inReplyToTweets = (LinearLayout) inReplyToArea.findViewById(R.id.conversation_tweets);

        this.inReplyToArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return hidePopups();
            }
        });
    }

    public boolean hidePopups() {
        boolean hidden = false;
        try {
            if (convoLayout.isShown()) {
                convoPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }

        try {
            if (webPopup.isShowing()) {
                webPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }
        try {
            if (interactionsPopup.isShowing()) {
                interactionsPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }

        return hidden;
    }

    private boolean secondAcc = false;

    public void setSecondAcc(boolean sec) {
        secondAcc = sec;
        tweetButtonUtils.setIsSecondAcc(sec);
    }

    private boolean fromNotification = false;

    public void fromNotification(boolean fromNotification) {
        this.fromNotification = true;
    }


    public View getExpansion() {
        return expansion;
    }

    private Status status = null;

    public void getInfo() {
        getInfo(fromNotification);
    }

    public void getInfo(final boolean fromNotification) {

        Thread getInfo = new TimeoutThread(() -> {
            //放外面数据还没有初始化
            tweetButtonUtils.setUpShare(buttonsRoot, tweet, screenName, statusUrl);
            boolean tweetLoadedSuccessfully = false;
            try {

                if (secondAcc) {
                    status = new StatusJSONImplMastodon(new GetStatusByID(id + "").execSecondAccountSync());
                } else {
                    status = new StatusJSONImplMastodon(new GetStatusByID(id + "").execSync());
                }

                int currentAccount = AppSettings.getSharedPreferences(context).getInt(AppSettings.CURRENT_ACCOUNT, 1);
                if (secondAcc) {
                    if (currentAccount == 1) {
                        currentAccount = 2;
                    } else {
                        currentAccount = 1;
                    }
                }
                int finalCurrentAccount = currentAccount;
                Observable.just(0).subscribeOn(Schedulers.io()).subscribe(i -> {
                    //poll更新了，但是不知道从哪个列表进来的，所以就全部更新一次
                    HomeDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    MentionsDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    DMDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    FavoriteTweetsDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    BookmarkedTweetsDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    SavedTweetsDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                    UserTweetsDataSource.getInstance(context).updateTweet(status, finalCurrentAccount);
                });


                getConversationAndEmbeddedTweet();

                if (status.isRetweet()) {
                    status = status.getRetweetedStatus();
                    id = status.getId();
                }

                tweetLoadedSuccessfully = true;
                ((Activity) context).runOnUiThread(() -> {
                    if (loadedCallback != null) {
                        loadedCallback.onLoad(status);
                    }
                });
            } catch (Exception e) {
                if (fromNotification) {
                    AnalyticsHelper.errorLoadingTweetFromNotification(context, e.getMessage());
                }
            }

            final boolean loadSuccess = tweetLoadedSuccessfully;
            ((Activity) context).runOnUiThread(() -> tweetButtonUtils.setUpButtons(status, id, countsView, buttonsRoot, true, loadSuccess));
        });

        getInfo.setPriority(Thread.MAX_PRIORITY);
        getInfo.start();
    }

    public void stop() {
        isRunning = false;
    }

    /**
     * 获取会话记录
     */
    private void getConversationAndEmbeddedTweet() {
        Thread getConvo = new TimeoutThread(() -> {
            if (!isRunning) {
                return;
            }

            final List<Status> replies = new ArrayList<>();
            final List<Status> discussList = new ArrayList<>();
            try {

                if (status.isRetweet()) {
                    status = status.getRetweetedStatus();
                }

                if (status == null) {
                    return;
                }

                //通过replyToStatusId一直找到最原始的那条post，然后倒序排列显示
                try {
                    StatusContext statusContext;
                    if (secondAcc) {
                        statusContext = new GetStatusContext(status.getId() + "").execSecondAccountSync();
                    } else {
                        statusContext = new GetStatusContext(status.getId() + "").execSync();
                    }
                    replies.addAll(StatusJSONImplMastodon.createStatusList(statusContext.ancestors).stream().filter(new StatusFilterPredicate(secondAcc ? AppSettings.getInstance(context).secondSessionId : AppSettings.getInstance(context).mySessionId, Filter.FilterContext.THREAD)).collect(Collectors.toList()));
                    discussList.addAll(StatusJSONImplMastodon.createStatusList(statusContext.descendants).stream().filter(new StatusFilterPredicate(secondAcc ? AppSettings.getInstance(context).secondSessionId : AppSettings.getInstance(context).mySessionId, Filter.FilterContext.THREAD)).collect(Collectors.toList()));

                } catch (Exception e) {
                    // the list of replies has ended, but we dont want to go to null
                    Timber.e(e, "No replies for tweet");
                }

            } catch (Exception e) {
                Timber.e(e, "No replies for tweet");
            }

            ((Activity) context).runOnUiThread(() -> {
                try {
                    if (replies.size() > 0) {

/*                        ArrayList<Status> reversed = new ArrayList<Status>();
                        for (int i = replies.size() - 1; i >= 0; i--) {
                            reversed.add(replies.get(i));
                        }*/
                        showInReplyToViews(replies);
                    }

                    if (status != null) {
                        // everything here worked, so get the discussion on the tweet
                        if (discussList.size() == 0) {
                            // nothing to show, so tell them that
                            ((Activity) context).runOnUiThread(() -> {
                                hideConvoProgress();
                            });
                        } else {
                            cardShown = true;
                            if (adapter == null || adapter.getCount() == 0) {
                                convoSpinner.setVisibility(View.GONE);
                                //这是弹窗显示的完整的回复listview
                                adapter = new TimelineArrayAdapter(context, discussList);
                                adapter.setCanUseQuickActions(false);
                                replyList.setAdapter(adapter);
                                replyList.setVisibility(View.VISIBLE);
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            ((Activity) context).runOnUiThread(() -> showConvoCard(discussList));
                        }
                    }

                } catch (Exception e) {
                    // none and it got the null object
                    Timber.e(e, "No replies for tweet");
                }


            });
        });

        getConvo.setPriority(Thread.MAX_PRIORITY);
        getConvo.start();
    }

    public boolean isRunning = true;

    public List<Status> profileTweets;
    private boolean foundProfileTweet = false;
    public TimelineArrayAdapter adapter;
    private boolean cardShown = false;
    private boolean firstRun = true;


    // expand collapse animation: http://stackoverflow.com/questions/4946295/android-expand-collapse-animation
    public void showInReplyToViews(List<Status> replies) {
        for (int i = 0; i < replies.size(); i++) {
            View statusView = new TweetView(context, replies.get(i)).setUseSmallerMargins(true).getView();
            statusView.findViewById(R.id.background).setPadding(0, Utils.toDP(12, context), 0, Utils.toDP(12, context));

            inReplyToTweets.addView(statusView);

            if (i != replies.size() - 1) {
                View tweetDivider = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
                tweetDivider.setLayoutParams(params);

                tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.text_drawer));

                inReplyToTweets.addView(tweetDivider);
            }
        }

        inReplyToArea.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        inReplyToArea.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = inReplyToArea.getMeasuredHeight() +
                (settings.picturesType == AppSettings.CONDENSED_TWEETS || settings.picturesType == AppSettings.CONDENSED_NO_IMAGES
                        ? 0 : Utils.toDP(28, context));

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        inReplyToArea.getLayoutParams().height = 1;
        inReplyToArea.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                inReplyToArea.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                inReplyToArea.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                readjustExpansionArea();
            }
        });

        // 1dp/ms
        //a.setDuration((int)(targetHeight / inReplyToArea.getContext().getResources().getDisplayMetrics().density));
        a.setDuration(200);
        inReplyToArea.startAnimation(a);
    }

    // used on the adapter
    // when the in reply to section is shown, it will create a giant white area at the bottom of the
    // screen that could be half the size. We get rid of that by readjusting the min height of the expansion
    View expandArea;

    public void setExpandArea(View expandArea) {
        this.expandArea = expandArea;
    }

    public void readjustExpansionArea() {
        if (expandArea != null) {
            expandArea.setMinimumHeight(expandArea.getMinimumHeight() - inReplyToArea.getMeasuredHeight());
            expandArea.requestLayout();
        }
    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        Runnable onFinish;

        public DeleteTweet(Runnable onFinish) {
            this.onFinish = onFinish;
        }

        protected Boolean doInBackground(String... urls) {

            try {

                HomeDataSource.getInstance(context).deleteTweet(id);
                MentionsDataSource.getInstance(context).deleteTweet(id);
                ListDataSource.getInstance(context).deleteTweet(id);

                try {
                    if (secondAcc) {
                        new DeleteStatus(id + "").execSecondAccountSync();
                    } else {
                        new DeleteStatus(id + "").execSync();
                    }
                } catch (Exception x) {

                }

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT);
            }

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
            onFinish.run();
        }
    }

}
