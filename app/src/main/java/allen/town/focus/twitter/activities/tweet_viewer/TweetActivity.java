package allen.town.focus.twitter.activities.tweet_viewer;

import static allen.town.focus_common.ad.InterstitialAdManager.loadAd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.VideoView;

import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.reflect.TypeToken;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.activities.media_viewer.VideoViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.ImageViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.adapters.DisplayItemsAdapter;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.listeners.MultipleImageTouchListener;
import allen.town.focus.twitter.model.Emoji;
import allen.town.focus.twitter.model.ReTweeterAccount;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.twittertext.CustomEmojiHelper;
import allen.town.focus.twitter.ui.displayitems.PollOptionStatusDisplayItem;
import allen.town.focus.twitter.utils.ExpansionViewHelper;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.NotificationUtils;
import allen.town.focus.twitter.utils.ReplyUtils;
import allen.town.focus.twitter.utils.TweetLinkUtils;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.VideoMatcherUtil;
import allen.town.focus.twitter.utils.text.TextUtils;
import allen.town.focus.twitter.views.TweetView;
import allen.town.focus.twitter.views.badges.GifBadge;
import allen.town.focus.twitter.views.badges.VideoBadge;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import allen.town.focus_common.ad.BannerAdManager;
import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import de.hdodenhof.circleimageview.CircleImageView;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.UserMentionEntity;
import twitter4j.UserMentionEntityJSONImplMastodon;
import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder;
import xyz.klinker.android.drag_dismiss.delegate_hack.DragDismissDelegateHackHack;

public class TweetActivity extends WhiteToolbarActivity implements DragDismissDelegateHackHack.Callback {

    public static Intent getIntent(Context context, Cursor cursor) {
        return getIntent(context, cursor, false);
    }

    public static Intent getIntent(Context context, Cursor cursor, boolean isSecondAccount) {
        String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
        String name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
        String text = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
        long time = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME));
        String picUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));
        String otherUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL));
        String users = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS));
        String hashtags = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS));
        long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
        String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
        String otherUrls = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL));
        String gifUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ANIMATED_GIF));
        long videoDuration = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_MEDIA_LENGTH));
        String statusUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_STATUS_URL));
        String emoji = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI));
        String retweeter;
        try {
            retweeter = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER));
        } catch (Exception e) {
            retweeter = "";
        }
        String link = "";

        boolean hasGif = gifUrl != null && !gifUrl.isEmpty();
        boolean displayPic = !picUrl.equals("") && !picUrl.contains("youtube");
        if (displayPic) {
            link = picUrl;
        } else {
            link = otherUrls.split("  ")[0];
        }

        Log.v("tweet_page", "clicked");
        Intent viewTweet = new Intent(context, TweetActivity.class);
        viewTweet.putExtra("name", name);
        viewTweet.putExtra("screenname", screenname);
        viewTweet.putExtra("time", time);
        viewTweet.putExtra("tweet", text);
        viewTweet.putExtra("retweeter", retweeter);
        viewTweet.putExtra("webpage", link);
        viewTweet.putExtra("other_links", otherUrl);
        viewTweet.putExtra("picture", displayPic);
        viewTweet.putExtra("tweetid", id);
        viewTweet.putExtra("proPic", profilePic);
        viewTweet.putExtra("users", users);
        viewTweet.putExtra("hashtags", hashtags);
        viewTweet.putExtra("animated_gif", gifUrl);
        viewTweet.putExtra("second_account", isSecondAccount);
        viewTweet.putExtra("video_duration", videoDuration);
        viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
        viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);

        applyDragDismissBundle(context, viewTweet);

        return viewTweet;
    }

    public static void applyDragDismissBundle(Context context, Intent intent) {

        DragDismissIntentBuilder.Theme theme = DragDismissIntentBuilder.Theme.LIGHT;
        AppSettings settings = AppSettings.getInstance(context);

        if (settings.blackTheme) {
            theme = DragDismissIntentBuilder.Theme.BLACK;
        } else if (settings.darkTheme) {
            theme = DragDismissIntentBuilder.Theme.DARK;
        }

        new DragDismissIntentBuilder(context).setPrimaryColorValue(settings.blackTheme ? Color.BLACK : Color.TRANSPARENT).setDragElasticity(DragDismissIntentBuilder.DragElasticity.XLARGE).setShowToolbar(false).setTheme(theme).build(intent);
    }

    private static final long NETWORK_ACTION_DELAY = 200;

    public static final String USE_EXPANSION = "use_expansion";
    public static final String EXPANSION_DIMEN_LEFT_OFFSET = "left_offset";
    public static final String EXPANSION_DIMEN_TOP_OFFSET = "top_offset";
    public static final String EXPANSION_DIMEN_WIDTH = "view_width";
    public static final String EXPANSION_DIMEN_HEIGHT = "view_height";

    public Context context;
    public AppSettings settings;
    public SharedPreferences sharedPrefs;

    public String name;
    public String screenName;
    public String statusUrl;
    public String accountId;
    public String tweet;
    public long time;
    public ReTweeterAccount retweeter;
    public String webpage;
    public String proPic;
    public boolean picture;
    public long tweetId;
    public List<UserMentionEntity> mention;
    public List<Emoji> emojis;
    public String[] hashtags;
    public String[] otherLinks;
    public String linkString;
    public boolean secondAcc = false;
    public String gifVideo;
    public boolean isAConversation = false;
    public long videoDuration = -1;

    protected boolean fromLauncher = false;
    protected boolean fromNotification = false;

    private boolean sharedTransition = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setSharedContentTransition(this);
        super.onCreate(savedInstanceState);

        DragDismissDelegateHackHack delegate = new DragDismissDelegateHackHack(this, this);
        delegate.onCreate(savedInstanceState);

        if (!AppSettings.getInstance(this).dragDismiss) {
            findViewById(R.id.dragdismiss_drag_dismiss_layout).setEnabled(false);
        }
        loadAd(this, false);
        overridePendingTransition(R.anim.activity_slide_up, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loadAd(this, true);
    }

    @Override
    public View onCreateContent(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        Utils.setTaskDescription(this);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        int notificationId = getIntent().getIntExtra("notification_id", -1);
        if (notificationId != -1) {
            notificationManager.cancel(notificationId);
            NotificationUtils.cancelGroupedNotificationWithNoContent(this);
            fromNotification = true;
        }

        context = this;
        settings = AppSettings.getInstance(this);
        sharedPrefs = AppSettings.getSharedPreferences(context);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        if (getIntent().getBooleanExtra("share_trans", false)) {
            sharedTransition = false;
        }

        getFromIntent();

        ArrayList<String> webpages = new ArrayList<String>();

        if (otherLinks == null) {
            otherLinks = new String[0];
        }

        if (gifVideo == null) {
            gifVideo = "no gif surfaceView";
        }
        boolean hasWebpage;
        boolean youtube = false;
        if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
            for (String s : otherLinks) {
                if (s.contains("youtu") && !(s.contains("channel") || s.contains("user") || s.contains("playlist"))) {
                    youtubeVideo = s;
                    youtube = true;
                    break;
                } else {
                    if (!s.contains("pic.twitt")) {
                        webpages.add(s);
                    }
                }
            }

            if (webpages.size() >= 1) {
                hasWebpage = true;
            } else {
                hasWebpage = false;
            }

        } else {
            hasWebpage = false;
        }

        if (hasWebpage && webpages.size() == 1) {
            String web = webpages.get(0);
            if (web.contains(tweetId + "/photo/1") || VideoMatcherUtil.containsThirdPartyVideo(web)) {
                hasWebpage = false;
                gifVideo = webpages.get(0);
            }
        }

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

//        Utils.setUpTweetTheme(context, settings);

        View root = inflater.inflate(R.layout.tweet_activity_new, parent, false);

        if (youtube || (null != gifVideo && !android.text.TextUtils.isEmpty(gifVideo) && (gifVideo.contains(".mp4") || gifVideo.contains(".m3u8") || gifVideo.contains("/photo/1") || VideoMatcherUtil.containsThirdPartyVideo(gifVideo)))) {
            displayPlayButton = true;
        }

        root.findViewById(R.id.line).setBackgroundColor(ThemeStore.accentColor(context));
        setUIElements(root);

        String page = webpages.size() > 0 ? webpages.get(0) : "";
        String embedded = page;

        for (int i = 0; i < webpages.size(); i++) {
            if (TweetView.isEmbeddedTweet(webpages.get(i))) {
                embedded = webpages.get(i);
                break;
            }
        }

        if (hasWebpage && TweetView.isEmbeddedTweet(tweet)) {
            final CardView view = (CardView) root.findViewById(R.id.embedded_tweet_card);

            final long embeddedId = TweetLinkUtils.getTweetIdFromLink(embedded);

            if (embeddedId != 0l) {
                view.setVisibility(View.INVISIBLE);
                new TimeoutThread(() -> {

                    try {
                        Thread.sleep(NETWORK_ACTION_DELAY);
                    } catch (Exception e) {
                    }

                    try {
                        final Status s = new StatusJSONImplMastodon(new GetStatusByID(embeddedId + "").execSync());

                        runOnUiThread(() -> {
                            TweetView v = new TweetView(context, s);
                            v.setCurrentUser(settings.myScreenName);
                            v.setSmallImage(true);

                            view.removeAllViews();
                            view.addView(v.getView());
                            view.setVisibility(View.VISIBLE);
                        });
                    } catch (Exception e) {
                    }
                }).start();
            }
        }

        return root;
    }

    boolean displayPlayButton = false;
    public VideoView video;
    public boolean videoError = false;

    String youtubeVideo = "";

    @Override
    public void onBackPressed() {
        if (!hidePopups()) {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        // this is used in the onStart() for the home fragment to tell whether or not it should refresh
        // tweetmarker. Since coming out of this will only call onResume(), it isn't needed.
        //sharedPrefs.edit().putBoolean("from_activity", true).commit();

        if (expansionHelper != null) {
            expansionHelper.stop();
        }

        super.finish();
        overridePendingTransition(0, R.anim.activity_slide_down);
    }

    public boolean hidePopups() {
        if (expansionHelper != null && expansionHelper.hidePopups()) {
            return true;
        }

        return false;
    }

    public void getFromIntent() {
        Intent from = getIntent();

        name = from.getStringExtra("name");
        screenName = from.getStringExtra("screenname");
        accountId = from.getStringExtra(AppSettings.ACCOUNT_ID);
        tweet = from.getStringExtra("tweet");
        time = from.getLongExtra("time", 0);
        retweeter = JsonHelper.parseObject(from.getStringExtra("retweeter"), ReTweeterAccount.class);
        webpage = from.getStringExtra("webpage");
        tweetId = from.getLongExtra("tweetid", 0);
        picture = from.getBooleanExtra("picture", false);
        proPic = from.getStringExtra("proPic");
        secondAcc = from.getBooleanExtra("second_account", false);
        gifVideo = from.getStringExtra("animated_gif");
        isAConversation = from.getBooleanExtra("conversation", false);
        videoDuration = from.getLongExtra("video_duration", -1);
        statusUrl = from.getStringExtra(HomeSQLiteHelper.COLUMN_STATUS_URL);
        emojis = JsonHelper.parseObjectList(from.getStringExtra(HomeSQLiteHelper.COLUMN_EMOJI), new TypeToken<List<Emoji>>() {
        }.getType());

        mention = JsonHelper.parseObjectList(from.getStringExtra("users"), new TypeToken<List<UserMentionEntityJSONImplMastodon>>() {
        }.getType());

        try {
            hashtags = from.getStringExtra("hashtags").split("  ");
        } catch (Exception e) {
            hashtags = null;
        }

        try {
            linkString = from.getStringExtra("other_links");
            otherLinks = linkString.split("  ");
        } catch (Exception e) {
            otherLinks = null;
        }

    }


    private void setTransitionNames() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            profilePic.setTransitionName("pro_pic");
            nametv.setTransitionName("name");
            screennametv.setTransitionName("screen_name");
            tweettv.setTransitionName("tweet");
            image.setTransitionName("image");
        }
    }

    public CircleImageView profilePic;
    public ImageView image;
    public FontPrefTextView retweetertv;
    public FontPrefTextView repliesTv;
    public RecyclerView pollRecyclerView;
    public FontPrefTextView timetv;
    public FontPrefTextView nametv;
    public FontPrefTextView screennametv;
    public FontPrefTextView tweettv;

    public void setUIElements(final View layout) {

        //设置底部广告padding bottom
        NestedScrollView mainView = findViewById(R.id.dragdismiss_scroll_view);
        mainView.setPadding(mainView.getPaddingLeft(), mainView.getPaddingTop(), mainView.getPaddingRight(), mainView.getPaddingBottom() + (!App.getInstance().isAdBlockUser() ? (int) getResources().getDimension(R.dimen.external_player_height) : 0));

        BannerAdManager.showBannerAd(this, findViewById(R.id.bottom_adView));

        nametv = (FontPrefTextView) layout.findViewById(R.id.name);
        screennametv = (FontPrefTextView) layout.findViewById(R.id.screen_name);
        tweettv = (FontPrefTextView) layout.findViewById(R.id.tweet);
        retweetertv = (FontPrefTextView) layout.findViewById(R.id.retweeter);
        repliesTv = (FontPrefTextView) layout.findViewById(R.id.reply_to);
        profilePic = (CircleImageView) layout.findViewById(R.id.profile_pic);
        image = (ImageView) layout.findViewById(R.id.image);
        timetv = (FontPrefTextView) layout.findViewById(R.id.time);

        tweettv.setTextSize(settings.textSize);
        screennametv.setTextSize(settings.textSize - 2);
        nametv.setTextSize(settings.textSize + 4);
        timetv.setTextSize(settings.textSize - 3);
        retweetertv.setTextSize(settings.textSize - 3);
        repliesTv.setTextSize(settings.textSize - 2);

        pollRecyclerView = (RecyclerView) layout.findViewById(R.id.poll_list);
        pollRecyclerView.setNestedScrollingEnabled(false);

        View.OnClickListener viewPro = view -> {
            if (!hidePopups()) {
                ProfilePager.start(context, name, accountId, proPic);
            }
        };

        glide(proPic, profilePic);
        profilePic.setOnClickListener(viewPro);

        layout.findViewById(R.id.person_info).setOnClickListener(viewPro);
        nametv.setOnClickListener(viewPro);
        screennametv.setOnClickListener(viewPro);

        final String formatText = Html.fromHtml(tweet).toString();
        final String replies = settings.compressReplies ? ReplyUtils.getReplyingToHandles(formatText) : "";
        final boolean showCompressReply = isAConversation && settings.compressReplies && replies != null && !replies.isEmpty() && mention != null && mention.size() > 0;
        if (showCompressReply) {
            tweet = formatText;
            for (String name :
                    replies.split(" ")) {
                if (name.length() > 1) {
                    tweet = tweet.replace(name, "");
                }
            }

            if (tweet.endsWith("\n\n")) {
                //格式化将<p>变成了两个换行符，暂时这样处理
                int position = tweet.lastIndexOf("\n\n");
                tweet = tweet.substring(0, position);
            }

            final String replyToText = context.getString(R.string.reply_to);

            if (ReplyUtils.showMultipleReplyNames(replies)) {
                repliesTv.setText(replyToText + " " + ReplyUtils.getReplyingNamesToHandles(mention));
                HtmlParser.linkifyText(repliesTv, emojis, mention, false);
            } else {
                final String firstPerson = mention.get(0).getName();
                final String othersText = context.getString(R.string.others) + " " + (mention.size() - 1) + " " + context.getString(R.string.noti_reply);
                repliesTv.setText(replyToText + " " + firstPerson + " & " + othersText);

                String[] nameList = new String[mention.size()];
                for (int i = 0; i < mention.size(); i++) {
                    UserMentionEntity userMentionEntity = mention.get(i);
                    nameList[i] = userMentionEntity.getName();
                }

                Link others = new Link(othersText).setUnderlined(false).setTextColor(ThemeStore.accentColor(context)).setOnClickListener(clickedText -> {
                    new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme).setItems(nameList, (dialog, which) -> ProfilePager.start(context, mention.get(which).getId() + "")).show();
                });
                Link first = new Link(firstPerson).setUnderlined(false).setTextColor(ThemeStore.accentColor(context)).setOnClickListener(clickedText -> {
                    ProfilePager.start(context, mention.get(0).getId() + "");
                });

                LinkBuilder.on(repliesTv).addLink(others).addLink(first).build();
            }

            repliesTv.setVisibility(View.VISIBLE);
        }

        if (picture || displayPlayButton) { // if there is a picture already loaded (or we have a vine/twimg surfaceView)

            if (displayPlayButton && VideoMatcherUtil.containsThirdPartyVideo(gifVideo)) {
                image.setBackgroundResource(android.R.color.black);
            } else {
                glide(webpage, image);
            }

            if (displayPlayButton) {
                layout.findViewById(R.id.play_button).setVisibility(View.VISIBLE);
                if (gifVideo != null && VideoMatcherUtil.isTwitterGifLink(gifVideo)) {
                    ((ImageView) layout.findViewById(R.id.play_button)).setImageDrawable(new GifBadge(this));
                } else {
                    ((ImageView) layout.findViewById(R.id.play_button)).setImageDrawable(new VideoBadge(this, videoDuration));
                }
            }

            MultipleImageTouchListener listener = new MultipleImageTouchListener(webpage);
            image.setOnTouchListener(listener);

            image.setOnClickListener(view -> {
                if (!hidePopups()) {
                    if (displayPlayButton) {
                        String links = "";
                        for (String s : otherLinks) {
                            links += s + "  ";
                        }

                        VideoViewerActivity.startActivity(context, tweetId, gifVideo, links);
                    } else {
                        ImageViewerActivity.Companion.startActivity(context, tweetId, image, listener.getImageTouchPosition(), webpage.split(" "));
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                image.setClipToOutline(true);
            }

            ViewGroup.LayoutParams layoutParams = image.getLayoutParams();
            layoutParams.height = (int) getResources().getDimension(R.dimen.header_condensed_height);
            image.setLayoutParams(layoutParams);

        } else {
            // remove the picture
            image.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);

        boolean replace = false;
        boolean embeddedTweetFound = TweetView.isEmbeddedTweet(tweet);

        if (settings.inlinePics && (tweet.contains("pic.twitter.com/") || embeddedTweetFound)) {
            if (tweet.lastIndexOf(".") == tweet.length() - 1) {
                replace = true;
            }
        }

        try {
            tweettv.setText(replace ? tweet.substring(0, tweet.length() - (embeddedTweetFound ? 33 : 25)) : tweet);
        } catch (Exception e) {
            tweettv.setText(tweet);
        }
        tweettv.setTextIsSelectable(true);


        //Date tweetDate = new Date(time);
        setTime(time);

        if (retweeter != null && !android.text.TextUtils.isEmpty(retweeter.username)) {
            retweetertv.setText(getResources().getString(R.string.retweeter) + ReTweeterAccount.getRetweeterFormatUrl(retweeter.displayName, retweeter.id));
            retweetertv.setVisibility(View.VISIBLE);
        }

        String text = tweet;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNames();
        }

        // last bool is whether it should open in the external browser or not
        if (showCompressReply) {
            //压缩回复会将post中的@信息移除掉，所以需要格式化，那么用HtmlParser就不会正常解析hashtag了
            TextUtils.linkifyText(context, tweettv, null, true, null, false);
            tweettv.setText(CustomEmojiHelper.emojify(tweettv.getText(), emojis, tweettv, false));
        } else {
            HtmlParser.linkifyText(tweettv, emojis, mention, false);
        }
        HtmlParser.linkifyText(retweetertv, emojis, mention, false);


        expansionHelper = new ExpansionViewHelper(context, tweetId);
        expansionHelper.setSecondAcc(secondAcc);
        expansionHelper.setBackground(layout.findViewById(R.id.content));
        expansionHelper.setInReplyToArea((LinearLayout) layout.findViewById(R.id.conversation_area));
        expansionHelper.setWebLink(otherLinks);
        expansionHelper.setUser(screenName);
        expansionHelper.setStatusUrl(statusUrl);
        expansionHelper.setText(text);
        expansionHelper.setUpOverflow();
        expansionHelper.fromNotification(fromNotification);
        expansionHelper.setLoadCallback(status -> {
            if (status != null) {
                setTime(status.getCreatedAt().getTime());
                //加载投票布局
                UiUtils.setPollRecyclerView(pollRecyclerView, this, secondAcc, status);
            }
        });

        LinearLayout ex = (LinearLayout) layout.findViewById(R.id.expansion_area);
        ex.addView(expansionHelper.getExpansion());
    }

    private void setTime(long time) {
        String timeDisplay;


        DateFormat dateFormatter = new SimpleDateFormat("MMM d yyyy", Locale.getDefault());
        DateFormat timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            dateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        Locale locale = context.getResources().getConfiguration().locale;
        if (locale != null && !locale.getLanguage().equals("en")) {
            dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        }

        if (!settings.militaryTime) {
            timeDisplay = timeFormatter.format(time) + "\n" + dateFormatter.format(time);
        } else {
            timeDisplay = timeFormatter.format(time).replace("24:", "00:") + "\n" + dateFormatter.format(time);
        }

        timetv.setText(timeDisplay);
    }

    private ExpansionViewHelper expansionHelper;

    private void glide(String url, ImageView target) {
        try {
            Glide.with(TweetActivity.this).load(url).dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).into(target);
        } catch (Exception e) {
            // activity destroyed
        }
    }
}
