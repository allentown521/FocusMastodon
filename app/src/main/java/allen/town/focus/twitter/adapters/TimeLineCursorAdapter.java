package allen.town.focus.twitter.adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.reflect.TypeToken;
import com.halilibo.bvpkotlin.BetterVideoPlayer;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.OnPeek;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.simple_videoview.SimpleVideoView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.VideoViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.ImageViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.activities.tweet_viewer.TweetActivity;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.data.WebPreview;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.listeners.MultipleImageTouchListener;
import allen.town.focus.twitter.model.Emoji;
import allen.town.focus.twitter.model.Poll;
import allen.town.focus.twitter.model.ReTweeterAccount;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.twittertext.CustomEmojiHelper;
import allen.town.focus.twitter.ui.displayitems.PollOptionStatusDisplayItem;
import allen.town.focus.twitter.ui.displayitems.StatusDisplayItem;
import allen.town.focus.twitter.utils.BetterVideoCallbackWrapper;
import allen.town.focus.twitter.utils.Expandable;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.ReplyUtils;
import allen.town.focus.twitter.utils.TweetButtonUtils;
import allen.town.focus.twitter.utils.TweetLinkUtils;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.VideoMatcherUtil;
import allen.town.focus.twitter.utils.text.TouchableMovementMethod;
import allen.town.focus.twitter.views.QuotedTweetView;
import allen.town.focus.twitter.views.TweetView;
import allen.town.focus.twitter.views.WebPreviewCard;
import allen.town.focus.twitter.views.badges.GifBadge;
import allen.town.focus.twitter.views.badges.VideoBadge;
import allen.town.focus.twitter.views.peeks.ProfilePeek;
import allen.town.focus.twitter.views.popups.QuickActionsPopup;
import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import de.hdodenhof.circleimageview.CircleImageView;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.UserMentionEntity;
import twitter4j.UserMentionEntityJSONImplMastodon;

public class TimeLineCursorAdapter extends CursorAdapter implements WebPreviewCard.OnLoad {

    public Map<Long, Status> quotedTweets = new HashMap();
    public Map<String, WebPreview> webPreviews = new HashMap();
    public Set<Long> likedStatuses = new HashSet<>();
    public Set<Long> retweetedStatuses = new HashSet<>();
    public Set<Long> bookmarkedStatuses = new HashSet<>();

    @Override
    public void onLinkLoaded(@NotNull String link, @NotNull WebPreview preview) {
        webPreviews.put(link, preview);
    }

    public Set<String> muffledUsers = new HashSet<String>();
    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public LayoutInflater inflater;
    public boolean isDM = false;
    protected SharedPreferences sharedPrefs;
    public boolean secondAcc = false;

    private String othersText;
    private String replyToText;

    public int layout;
    public Resources res;

    private ColorDrawable transparent;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public boolean isHomeTimeline;

    public int contentHeight = 0;
    public int headerMultiplier = 0;


    private int normalPictures;
    private int smallPictures;

    public boolean hasConvo = false;

    public boolean hasExpandedTweet = false;

    private Handler videoHandler;
    private Handler webPreviewHandler;

    private boolean isDataSaver = false;

    private boolean duelPanel;

    int embeddedTweetMinHeight;

    public static class ViewHolder {
        public TextView name;
        public TextView muffledName;
        public TextView screenTV;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
        public TextView replies;
        public LinearLayout expandArea;
        public ImageView image;
        public View background;
        public ImageView playButton;
        public ImageView isAConversation;
        public FrameLayout imageHolder;
        public View rootView;
        public ViewGroup embeddedTweet;
        public View quickActions;
        public SimpleVideoView videoView;
        public LinearLayout conversationArea;
        public WebPreviewCard webPreviewCard;
        public RecyclerView pollRecyclerView;
        public LinearLayout alwaysShownButtons;
        public ImageButton likeButton;
        public ImageButton retweetButton;
        public ImageButton bookmarkButton;

        // revamped tweet
        public View revampedTopLine;
        public View revampedRetweetIcon;
        public View revampedTweetContent;
        public View revampedContentRipple;

        public long tweetId;
        public boolean isFavorited;
        public String proPicUrl;
        public String screenName;
        public String picUrl;
        public String retweeterName;
        public String gifUrl = "";
        public Status status;

        public boolean preventNextClick = false;
        public MultipleImageTouchListener imageTouchListener;
    }

    // This is need for the case that the activity is paused while the handler is counting down
    // for the video playback still. If that happens, we definitely don't want to start the video.
    private boolean activityPaused = false;

    public void activityPaused(boolean paused) {
        activityPaused = paused;
    }

    public void init() {
        init(true);
    }

    public void init(boolean cont) {
        new TimeoutThread(() -> {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                ConnectivityManager connMgr = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connMgr.isActiveNetworkMetered() &&
                        (connMgr.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED ||
                                connMgr.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED)) {
                    isDataSaver = true;
                }
            }
        }).start();

        videoHandler = new Handler();
        webPreviewHandler = new Handler();


        replyToText = context.getString(R.string.reply_to);
        settings = AppSettings.getInstance(context);
        embeddedTweetMinHeight = settings.condensedTweets() ? Utils.toDP(70, context) : Utils.toDP(140, context);

        normalPictures = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        smallPictures = Utils.toDP(120, context);

        sharedPrefs = AppSettings.getSharedPreferences(context);

        duelPanel = AppSettings.dualPanels(context);

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        layout = R.layout.tweet;
        if (settings.revampedTweets()) {
            layout = R.layout.tweet_revamp;
        } else if (settings.condensedTweets()) {
            layout = R.layout.tweet_condensed;
        }

        dateFormatter = new SimpleDateFormat("MMM d", Locale.getDefault());
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            dateFormatter = new SimpleDateFormat("dd MMM", Locale.getDefault());
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        Locale locale = context.getResources().getConfiguration().locale;
        if (locale != null && !locale.getLanguage().equals("en")) {
            dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        }

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (cont && context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        for (String s : cursor.getColumnNames()) {
            if (s.equals(HomeSQLiteHelper.COLUMN_CONVERSATION)) {
                hasConvo = true;
            }
        }

        muffledUsers = UiUtils.getMuffledUsersKyes(sharedPrefs);


        TWEET_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID);
        PRO_PIC_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC);
        TEXT_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT);
        NAME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME);
        SCREEN_NAME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME);
        PIC_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL);
        TIME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME);
        URL_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL);
        USER_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS);
        HASHTAG_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS);
        GIF_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ANIMATED_GIF);
        RETWEETER_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER);
        VIDEO_DURATION_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_MEDIA_LENGTH);
        USER_ID = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USER_ID);

        if (hasConvo) {
            CONVO_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CONVERSATION);
        } else {
            CONVO_COL = -1;
        }
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, Expandable expander, boolean secondAcc) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = false;
        this.secondAcc = secondAcc;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init(false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.muffledName = (TextView) v.findViewById(R.id.muffled_name);
        holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        holder.profilePic = (CircleImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.replies = (TextView) v.findViewById(R.id.reply_to);
        holder.background = v.findViewById(R.id.background);
        holder.isAConversation = (ImageView) v.findViewById(R.id.is_a_conversation);
        holder.embeddedTweet = (ViewGroup) v.findViewById(R.id.embedded_tweet_card);
        holder.quickActions = v.findViewById(R.id.quick_actions);
        holder.image = (ImageView) v.findViewById(R.id.image);
        holder.playButton = (ImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        holder.videoView = (SimpleVideoView) v.findViewById(R.id.video_view);
        holder.conversationArea = (LinearLayout) v.findViewById(R.id.conversation_area);
        holder.webPreviewCard = (WebPreviewCard) v.findViewById(R.id.web_preview_card);
        holder.pollRecyclerView = (RecyclerView) v.findViewById(R.id.poll_list);
        holder.pollRecyclerView.setNestedScrollingEnabled(false);
        holder.alwaysShownButtons = (LinearLayout) v.findViewById(R.id.always_shown_buttons);

        // revamped tweet
        holder.revampedTopLine = v.findViewById(R.id.line_above_profile_picture);
        holder.revampedRetweetIcon = v.findViewById(R.id.retweet_icon);
        holder.revampedTweetContent = v.findViewById(R.id.content_card);
        holder.revampedContentRipple = v.findViewById(R.id.content_ripple);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - (settings.condensedTweets() || settings.revampedTweets() ? 1 : 2));
        holder.name.setTextSize(settings.textSize + (settings.condensedTweets() ? 1 : 4));
        holder.muffledName.setTextSize(settings.textSize);
        holder.time.setTextSize(settings.textSize - (settings.revampedTweets() ? 2 : 3));
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.replies.setTextSize(settings.textSize - 2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        if (!(this instanceof ActivityCursorAdapter) && settings.alwaysShowButtons && holder.alwaysShownButtons.getChildCount() == 0) {
            holder.alwaysShownButtons.addView(LayoutInflater.from(holder.background.getContext()).inflate(R.layout.always_shown_tweet_buttons, null, false));
            holder.alwaysShownButtons.setVisibility(View.VISIBLE);

            holder.likeButton = holder.alwaysShownButtons.findViewById(R.id.always_like_button);
            holder.retweetButton = holder.alwaysShownButtons.findViewById(R.id.always_retweet_button);
            holder.bookmarkButton = holder.alwaysShownButtons.findViewById(R.id.always_bookmark_button);
        }

        // some things we just don't need to configure every time
        holder.muffledName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.background.performClick();
                debounceClick(holder.muffledName);
            }
        });

        holder.expandArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.background.performClick();
                debounceClick(holder.expandArea);
            }
        });

        holder.expandArea.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holder.background.performLongClick();
                return false;
            }
        });

        holder.tweet.setSoundEffectsEnabled(false);
        holder.tweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    holder.background.performClick();
                }
                debounceClick(holder.tweet);
            }
        });

        holder.tweet.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    holder.background.performLongClick();
                    holder.preventNextClick = true;
                }
                return false;
            }
        });

        if (settings.revampedTweets()) {
            holder.tweet.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!TouchableMovementMethod.touched && event.getAction() == MotionEvent.ACTION_DOWN) {
                        forceRippleAnimation(holder.revampedContentRipple, event);
                    }

                    return false;
                }
            });

            holder.time.setTextSize(13);
        } else {
            holder.retweeter.setSoundEffectsEnabled(false);
            holder.retweeter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!TouchableMovementMethod.touched) {
                        holder.background.performClick();
                    }

                    debounceClick(holder.retweeter);
                }
            });

            holder.retweeter.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!TouchableMovementMethod.touched) {
                        holder.background.performLongClick();
                        holder.preventNextClick = true;
                    }
                    return false;
                }
            });
        }

        if (settings.picturesType == AppSettings.PICTURES_SMALL &&
                holder.imageHolder.getHeight() != smallPictures) {
            ViewGroup.LayoutParams params = holder.imageHolder.getLayoutParams();
            params.height = smallPictures;
            holder.imageHolder.setLayoutParams(params);
        }

        if (settings.detailedQuotes) {
            holder.embeddedTweet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
        }

        holder.rootView = v;

        v.setTag(holder);

        holder.imageTouchListener = new MultipleImageTouchListener();
        holder.image.setOnTouchListener(holder.imageTouchListener);

        return v;
    }

    private List<Video> videos = new ArrayList();

    public void playCurrentVideo() {
        for (Video v : videos) {
            v.playCurrentVideo();
        }
    }

    public void stopOnScroll() {
        for (Video v : videos) {
            v.stopOnScroll();
        }
    }

    public void stopOnScroll(int firstVisible, int lastVisible) {
        for (Video v : videos) {
            Log.v("Focus_for_Mastodon_video", "video position: " + v.positionOnTimeline + ", first: " + firstVisible + ", last: " + lastVisible);
            if (v.positionOnTimeline > lastVisible || v.positionOnTimeline < firstVisible) {
                v.stopOnScroll();
            }
        }
    }

    public void resetVideoHandler() {
        videoHandler.removeCallbacksAndMessages(null);
    }

    protected int TWEET_COL;
    protected int PRO_PIC_COL;
    protected int TEXT_COL;
    protected int NAME_COL;
    protected int SCREEN_NAME_COL;
    protected int PIC_COL;
    protected int TIME_COL;
    protected int URL_COL;
    protected int USER_COL;
    protected int HASHTAG_COL;
    protected int GIF_COL;
    protected int CONVO_COL;
    protected int RETWEETER_COL;
    protected int VIDEO_DURATION_COL;
    protected int USER_ID;

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.expandArea.getVisibility() != View.GONE) {
            removeExpansion(holder, false);
        }

        holder.webPreviewCard.clear();
        if (holder.webPreviewCard.getVisibility() != View.GONE) {
            holder.webPreviewCard.setVisibility(View.GONE);
        }

        if (holder.embeddedTweet.getChildCount() > 0 || holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.setVisibility(View.GONE);

            if (settings.detailedQuotes) {
                holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
            }
        }

        if (holder.conversationArea.getChildCount() > 0) {
            holder.conversationArea.removeAllViews();

            ViewGroup.LayoutParams params = holder.conversationArea.getLayoutParams();
            params.height = 0;
            holder.conversationArea.setLayoutParams(params);
        }

        for (int i = 0; i < videos.size(); i++) {
            if (holder.tweetId == videos.get(i).tweetId) {
                // recycling the playing videos layout since it is off the screen
                videos.get(i).releaseVideo();
                videos.remove(i);
                i--;
            }
        }

        final long id = cursor.getLong(TWEET_COL);
        holder.tweetId = id;
        final String profilePic = cursor.getString(PRO_PIC_COL);
        holder.proPicUrl = profilePic;
        String tweetTexts = cursor.getString(TEXT_COL);
        final String name = cursor.getString(NAME_COL);
        final String screenname = cursor.getString(SCREEN_NAME_COL);
        final String userId = cursor.getString(USER_ID);
        final String picUrl = cursor.getString(PIC_COL);
        holder.picUrl = picUrl;
        holder.imageTouchListener.setImageUrls(picUrl);
        final long longTime = cursor.getLong(TIME_COL);
        final String otherUrl = cursor.getString(URL_COL);
        final String users = cursor.getString(USER_COL);
        final String hashtags = cursor.getString(HASHTAG_COL);
        final long mediaDuration = cursor.getLong(VIDEO_DURATION_COL);
        holder.status = new StatusJSONImplMastodon(cursor);

        UiUtils.setPollRecyclerView(holder.pollRecyclerView, mContext, secondAcc, holder.status);

        //注意不能传它的父类UserMentionEntity，没有构造函数无法反序列化
        final List<UserMentionEntity> mention = JsonHelper.parseObjectList(cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS))
                , new TypeToken<List<UserMentionEntityJSONImplMastodon>>() {
                }.getType());

        final List<Emoji> emojis = JsonHelper.parseObjectList(cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI))
                , new TypeToken<List<Emoji>>() {
                }.getType());

        ReTweeterAccount retweeter = null;
        try {
            retweeter = JsonHelper.parseObject(cursor.getString(RETWEETER_COL), ReTweeterAccount.class);
        } catch (Exception e) {
        }
        final ReTweeterAccount fRetweeter = retweeter;

        final boolean muffled = isMuffled(userId, retweeter);

        holder.gifUrl = cursor.getString(GIF_COL);

        final boolean inAConversation;
        if (hasConvo) {
            inAConversation = cursor.getInt(CONVO_COL) == 1;
        } else {
            inAConversation = false;
        }
        String statusUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_STATUS_URL));
        String emoji = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI));

        if (inAConversation) {
            if (holder.isAConversation != null && holder.isAConversation.getVisibility() != View.VISIBLE) {
                holder.isAConversation.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.isAConversation != null && holder.isAConversation.getVisibility() != View.GONE) {
                holder.isAConversation.setVisibility(View.GONE);
            }
        }

        final String tweetWithReplyHandles = tweetTexts;

        final String formatText = Html.fromHtml(tweetTexts).toString();
        final String replies = settings.compressReplies ? ReplyUtils.getReplyingToHandles(formatText) : "";
        //压缩回复会将post中的@信息移除掉，然后显示回复给：人员列表
        final boolean showCompressReply = inAConversation && settings.compressReplies && replies != null && !replies.isEmpty() && mention != null && mention.size() > 0;
        if (showCompressReply) {
            tweetTexts = formatText;
            for (String atName :
                    replies.split(" ")) {
                if (atName.length() > 1) {
                    tweetTexts = tweetTexts.replace(atName, "");
                }
            }

            if (tweetTexts.endsWith("\n\n")) {
                //格式化将<p>变成了两个换行符，暂时这样处理
                int position = tweetTexts.lastIndexOf("\n\n");
                tweetTexts = tweetTexts.substring(0, position);
            }

            if (ReplyUtils.showMultipleReplyNames(replies)) {
                holder.replies.setText(replyToText + " " + ReplyUtils.getReplyingNamesToHandles(mention));
                HtmlParser.linkifyText(holder.replies, emojis, mention, false);
            } else {
                final String firstPerson = mention.get(0).getName();
                //点击事件不灵敏，字符串长一些号点击，原因未知
                othersText = context.getString(R.string.others) + " " + (mention.size() - 1) + " " + mContext.getString(R.string.noti_reply);
                holder.replies.setText(replyToText + " " + firstPerson + " & " + othersText);

                String[] nameList = new String[mention.size()];
                for (int i = 0; i < mention.size(); i++) {
                    UserMentionEntity userMentionEntity = mention.get(i);
                    nameList[i] = userMentionEntity.getName();
                }

                Link others = new Link(othersText)
                        .setUnderlined(false)
                        .setTextColor(ThemeStore.accentColor(context))
                        .setOnClickListener(clickedText -> {
                            new AccentMaterialDialog(
                                    context,
                                    R.style.MaterialAlertDialogTheme
                            ).setItems(nameList, (dialog, which) -> ProfilePager.start(context, mention.get(which).getId() + "")).show();
                        });
                Link first = new Link(firstPerson)
                        .setUnderlined(false)
                        .setTextColor(ThemeStore.accentColor(context))
                        .setOnClickListener(clickedText -> {
                            ProfilePager.start(context, mention.get(0).getId() + "");
                        });

                LinkBuilder.on(holder.replies).addLink(others).addLink(first).build();
            }

            if (holder.replies.getVisibility() != View.VISIBLE) {
                holder.replies.setVisibility(View.VISIBLE);
            }
        } else if (holder.replies.getVisibility() != View.GONE) {
            holder.replies.setVisibility(View.GONE);
        }

        final String tweetText = tweetTexts;

        if (muffled) {
            if (holder.background.getVisibility() != View.GONE) {
                holder.background.setVisibility(View.GONE);
                holder.muffledName.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.background.getVisibility() != View.VISIBLE) {
                holder.background.setVisibility(View.VISIBLE);
                holder.muffledName.setVisibility(View.GONE);
            }
        }

        if (settings.alwaysShowButtons && !(this instanceof ActivityCursorAdapter)) {
            if (likedStatuses.contains(holder.tweetId)) {
                holder.likeButton.setImageResource(R.drawable.ic_heart);
                holder.likeButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
            } else {
                holder.likeButton.clearColorFilter();
                holder.likeButton.setImageResource(R.drawable.ic_heart_outline);
            }

            if (retweetedStatuses.contains(holder.tweetId)) {
                holder.retweetButton.setImageResource(R.drawable.ic_retweet);
                holder.retweetButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
            } else {
                holder.retweetButton.clearColorFilter();
            }

            TweetButtonUtils utils = new TweetButtonUtils(holder.background.getContext());
            utils.setUpSimpleButtons(holder.status, holder.alwaysShownButtons, (newLikeState, originalStatus) -> {
                if (newLikeState) {
                    likedStatuses.add(originalStatus.getId());
                } else {
                    likedStatuses.remove(originalStatus.getId());
                }

                if (holder.tweetId == originalStatus.getId()) {
                    if (newLikeState) {
                        holder.likeButton.setImageResource(R.drawable.ic_heart);
                        holder.likeButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
                    } else {
                        holder.likeButton.clearColorFilter();
                        holder.likeButton.setImageResource(R.drawable.ic_heart_outline);
                    }
                }
            }, (newRetweetState, originalStatus) -> {
                if (newRetweetState) {
                    retweetedStatuses.add(originalStatus.getId());
                } else {
                    retweetedStatuses.remove(originalStatus.getId());
                }

                if (holder.tweetId == originalStatus.getId()) {
                    if (newRetweetState) {
                        holder.retweetButton.setImageResource(R.drawable.ic_retweet);
                        holder.retweetButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
                    } else {
                        holder.retweetButton.clearColorFilter();
                    }
                }
            }, (newBookmarkState, originalStatus) -> {
                if (newBookmarkState) {
                    bookmarkedStatuses.add(originalStatus.getId());
                } else {
                    bookmarkedStatuses.remove(originalStatus.getId());
                }

                if (holder.tweetId == originalStatus.getId()) {
                    if (newBookmarkState) {
                        holder.bookmarkButton.setImageResource(R.drawable.round_bookmark_24);
                        holder.bookmarkButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
                    } else {
                        holder.bookmarkButton.clearColorFilter();
                        holder.bookmarkButton.setImageResource(R.drawable.round_bookmark_border_24);
                    }
                }
            });
        }

        if (holder.quickActions != null) {
            holder.quickActions.setOnClickListener(view1 -> {
                QuickActionsPopup popup = new QuickActionsPopup(context, holder.status, secondAcc);
                popup.setExpansionPointForAnim(holder.quickActions);
                popup.setOnTopOfView(holder.quickActions);
                popup.show();

                debounceClick(holder.quickActions);
            });
        }

        holder.background.setOnClickListener(view12 -> {
            if (holder.preventNextClick) {
                holder.preventNextClick = false;
                return;
            }

            if (holder.expandArea.getVisibility() != View.GONE) {
                removeExpansion(holder, true);
            }

            String link;
            boolean displayPic = !holder.picUrl.equals("");
            if (displayPic) {
                link = holder.picUrl;
            } else {
                link = otherUrl.split("  ")[0];
            }

            Intent viewTweet = new Intent(context, TweetActivity.class);
            viewTweet.putExtra("name", name);
            viewTweet.putExtra("screenname", screenname);
            viewTweet.putExtra("time", longTime);
            viewTweet.putExtra("tweet", tweetWithReplyHandles);
            viewTweet.putExtra("retweeter", JsonHelper.toJSONString(fRetweeter));
            viewTweet.putExtra("webpage", link);
            viewTweet.putExtra("other_links", otherUrl);
            viewTweet.putExtra("picture", displayPic);
            viewTweet.putExtra("tweetid", holder.tweetId);
            viewTweet.putExtra("proPic", profilePic);
            viewTweet.putExtra("users", users);
            viewTweet.putExtra("hashtags", hashtags);
            viewTweet.putExtra("animated_gif", holder.gifUrl);
            viewTweet.putExtra("conversation", inAConversation);
            viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
            viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);
            viewTweet.putExtra(AppSettings.ACCOUNT_ID, userId);

            if (secondAcc) {
                String text = context.getString(R.string.using_second_account).replace("%s", "@" + settings.secondScreenName);
                TopSnackbarUtil.showSnack(context, text, Toast.LENGTH_SHORT);
                viewTweet.putExtra("second_account", true);
            }

            TweetActivity.applyDragDismissBundle(context, viewTweet);

            context.startActivity(viewTweet);

            debounceClick(holder.background);
        });

        holder.background.setOnLongClickListener(view13 -> {
            if (holder.expandArea.getVisibility() == View.GONE) {
                addExpansion(holder, id);
            } else {
                removeExpansion(holder, true);
            }

            return true;
        });

        holder.profilePic.setOnClickListener(view14 -> {
            if (isHomeTimeline) {
                sharedPrefs.edit()
                        .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                        .commit();
            }

            ProfilePager.start(context, name, userId, holder.proPicUrl);
            debounceClick(holder.profilePic);
        });

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (muffled) {
            String t = "<b>@" + screenname + "</b>: " + tweetText;
            holder.muffledName.setText(Html.fromHtml(t));
        }

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(longTime, context, true));
        } else {
            Date date = new Date(longTime);
            holder.time.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }

        boolean removeLastCharacters = false;
        boolean embeddedTweetFound = false;

        if (settings.inlinePics && tweetText.contains("pic.twitter.com/")) {
            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                removeLastCharacters = true;
            }
        } else if (settings.inlinePics && TweetView.isEmbeddedTweet(tweetText)) {
            embeddedTweetFound = true;

            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                removeLastCharacters = true;
            }
        }

        try {
            String text = removeLastCharacters ?
                    tweetText.substring(0, tweetText.length() - (embeddedTweetFound ? 33 : 25)) :
                    tweetText;
            holder.tweet.setText(text);

            if (text.isEmpty()) {
                if (holder.tweet.getVisibility() != View.GONE)
                    holder.tweet.setVisibility(View.GONE);
            } else if (holder.tweet.getVisibility() != View.VISIBLE) {
                holder.tweet.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            holder.tweet.setText(tweetText);

            if (tweetText.isEmpty()) {
                if (holder.tweet.getVisibility() != View.GONE)
                    holder.tweet.setVisibility(View.GONE);
            } else if (holder.tweet.getVisibility() != View.VISIBLE) {
                holder.tweet.setVisibility(View.VISIBLE);
            }
        }

        boolean picture = false;
        boolean picturePeek = false;
        int videoPeekLayout = -1;

        if (holder.videoView.getVisibility() == View.VISIBLE) {
            holder.videoView.setVisibility(View.GONE);
        }

        if (settings.inlinePics && holder.picUrl != null && !holder.picUrl.equals("")) {
            // there is a picture in the tweet

            if (holder.imageHolder.getVisibility() == View.GONE) {
                holder.imageHolder.setVisibility(View.VISIBLE);
            }

            if (holder.picUrl.contains("youtube") || (holder.gifUrl != null && !android.text.TextUtils.isEmpty(holder.gifUrl))) {
                // video tag on the picture

                if (holder.playButton.getVisibility() == View.GONE) {
                    holder.playButton.setVisibility(View.VISIBLE);
                }

                int videoType;
                if (VideoMatcherUtil.isTwitterGifLink(holder.gifUrl)) {
                    holder.playButton.setImageDrawable(new GifBadge(context));
                    videoPeekLayout = R.layout.peek_gif;
                    videoType = 0;
                } else {
                    holder.playButton.setImageDrawable(new VideoBadge(context, mediaDuration));
                    videoType = 1;
                    if (!holder.picUrl.contains("youtube")) {
                        videoPeekLayout = R.layout.peek_video;
                    }
                }

                holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VideoViewerActivity.startActivity(context, id, holder.gifUrl, otherUrl);
                        debounceClick(holder.imageHolder);
                    }
                });

                if (holder.gifUrl.contains(".mp4") || holder.gifUrl.contains(".m3u8")) {
                    videos.add(new Video(holder.videoView, holder.tweetId, holder.gifUrl, cursor.getPosition(), videoType));
                }

                picture = true;
            } else {
                // no video tag, just the picture
                if (holder.playButton.getVisibility() == View.VISIBLE) {
                    holder.playButton.setVisibility(View.GONE);
                }

                holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (isHomeTimeline) {
                            sharedPrefs.edit()
                                    .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                    .commit();
                        }

                        ImageViewerActivity.Companion.startActivity(context, id, holder.image, holder.imageTouchListener.getImageTouchPosition(), holder.picUrl.split(" "));
                        debounceClick(holder.imageHolder);
                    }
                });

                picturePeek = true;
                picture = true;
            }
        } else {
            if (holder.imageHolder.getVisibility() != View.GONE) {
                holder.imageHolder.setVisibility(View.GONE);
            }

            if (holder.playButton.getVisibility() == View.VISIBLE) {
                holder.playButton.setVisibility(View.GONE);
            }
        }


        if (retweeter != null && !TextUtils.isEmpty(retweeter.username) && !isDM) {
            String text = context.getResources().getString(R.string.retweeter);
            holder.retweeter.setText((settings.revampedTweets() ? "" : text) + ReTweeterAccount.getRetweeterFormatUrl(retweeter.displayName, retweeter.id));

            if (holder.retweeter.getVisibility() != View.VISIBLE) {
                holder.retweeter.setVisibility(View.VISIBLE);
            }
        } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }

        if (picture) {
            if (!settings.condensedTweets()) {
                if (settings.cropImagesOnTimeline) {
                    Glide.with(context).load(holder.picUrl).centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(null).into(holder.image);
                } else {
                    holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(context).load(holder.picUrl).fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(null).into(holder.image);
                }
            } else {
                Glide.with(context).load(holder.picUrl).fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(null).into(holder.image);
            }
        }

        if (settings.showProfilePictures) {
            Glide.with(context).load(holder.proPicUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(null).into(holder.profilePic);
        } else if (holder.profilePic.getVisibility() != View.GONE) {
            holder.profilePic.setVisibility(View.GONE);
            holder.name.setOnClickListener(v -> {
                holder.profilePic.performClick();
                debounceClick(holder.name);
            });
        }

        if (embeddedTweetFound || picture || !settings.webPreviews) {
            if (holder.webPreviewCard.getVisibility() != View.GONE) {
                holder.webPreviewCard.setVisibility(View.GONE);
            }
        } else if (!TextUtils.isEmpty(otherUrl)) {

            if (holder.webPreviewCard.getVisibility() != View.VISIBLE) {
                holder.webPreviewCard.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.webPreviewCard.getVisibility() != View.GONE) {
                holder.webPreviewCard.setVisibility(View.GONE);
            }
        }


        if (showCompressReply) {
            //压缩回复会将post中的@信息移除掉，所以需要格式化，那么用HtmlParser就不会正常解析hashtag了
            allen.town.focus.twitter.utils.text.TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
            holder.tweet.setText(CustomEmojiHelper.emojify(holder.tweet.getText(), emojis, holder.tweet, false));
        } else {
            HtmlParser.linkifyText(holder.tweet, emojis, mention, false);
        }
        HtmlParser.linkifyText(holder.retweeter, emojis, mention, false);

        if (settings.usePeek) {
            ProfilePeek.create(context, holder.profilePic, userId);

            if (picturePeek) {
                if (context instanceof PeekViewActivity) {
                    PeekViewOptions options = new PeekViewOptions();
                    options.setFullScreenPeek(true);
                    options.setBackgroundDim(1f);

                    Peek.into(R.layout.peek_image, new SimpleOnPeek() {
                        @Override
                        public void onInflated(View rootView) {
                            Glide.with(context).load(holder.picUrl.split(" ")[holder.imageTouchListener.getImageTouchPosition()])
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into((ImageView) rootView.findViewById(R.id.image));
                        }
                    }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                }
            } else if (videoPeekLayout != -1) {
                if (context instanceof PeekViewActivity) {
                    if (videoPeekLayout != 0 && !holder.gifUrl.contains("youtu")) {

                        PeekViewOptions options = new PeekViewOptions();
                        options.setFullScreenPeek(true);
                        options.setBackgroundDim(1f);

                        Peek.into(videoPeekLayout, new OnPeek() {
                            private BetterVideoPlayer videoView;

                            @Override
                            public void shown() {
                            }

                            @Override
                            public void onInflated(View rootView) {
                                videoView = (BetterVideoPlayer) rootView.findViewById(R.id.video);
                                videoView.setSource(Uri.parse(holder.gifUrl.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                videoView.setCallback(new BetterVideoCallbackWrapper() {
                                    @Override
                                    public void onCompletion(BetterVideoPlayer player) {
                                        if (VideoMatcherUtil.isTwitterGifLink(holder.gifUrl)) {
                                            videoView.seekTo(0);
                                            videoView.start();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void dismissed() {
                                videoView.release();
                            }
                        }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                    } else {
                        Peek.clear(holder.imageHolder);
                    }
                }
            } else {
                Peek.clear(holder.imageHolder);
            }
        }

        if (TweetView.isEmbeddedTweet(tweetText)) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
            if (!tryImmediateEmbeddedLoad(holder, otherUrl)) {
                loadEmbeddedTweet(holder, otherUrl);
            }
        }

        tryImmediateWebPageLoad(holder);

        setUpRevampedTweet(cursor, holder, muffled);
    }

    private boolean tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
        Long embeddedId = 0l;
        for (String u : otherUrl.split(" ")) {
            if (u.contains("/status/") && !u.contains("/i/web/")) {
                embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                break;
            }
        }

        if (embeddedId != 0l && quotedTweets.containsKey(embeddedId)) {
            Status status = quotedTweets.get(embeddedId);
            TweetView v = QuotedTweetView.create(context, status);
            v.setDisplayProfilePicture(!settings.condensedTweets());
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.addView(v.getView());

            if (settings.detailedQuotes) {
                holder.embeddedTweet.setMinimumHeight(0);
            }

            return true;
        } else {
            return false;
        }
    }

    private void tryImmediateWebPageLoad(final ViewHolder holder) {
        holder.webPreviewCard.setTag(holder.status.getCard());
        holder.webPreviewCard.displayPreview(holder.status.getCard());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        try {
            if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        } catch (Exception e) {
            ((Activity) context).recreate();
            return null;
        }

        View v;
        if (convertView == null) {
            v = newView(context, cursor, parent);
        } else {
            v = convertView;
        }

        bindView(v, context, cursor);

        return v;
    }

    public void removeExpansion(final ViewHolder holder, boolean anim) {
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(holder.expandArea.getHeight(), 0);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.expandArea.getLayoutParams();
                layoutParams.height = val;
                holder.expandArea.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorContent.setDuration(anim ? ANIMATION_DURATION : 0);
        heightAnimatorContent.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorContent);

        if (anim) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    holder.expandArea.setVisibility(View.GONE);
                    holder.expandArea.removeAllViews();
                    hasExpandedTweet = false;
                }
            }, ANIMATION_DURATION);
        } else {
            holder.expandArea.setVisibility(View.GONE);
            hasExpandedTweet = false;
        }

    }

    protected void startAnimation(Animator animator) {
        animator.start();
    }

    public static final int ANIMATION_DURATION = 100;
    public static Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    public void addExpansion(final ViewHolder holder, final long tweetId) {
        hasExpandedTweet = true;

        final View buttons = LayoutInflater.from(holder.background.getContext()).inflate(R.layout.tweet_expansion_buttons, null, false);
        final View counts = LayoutInflater.from(holder.background.getContext()).inflate(R.layout.tweet_expansion_counts, null, false);
        buttons.setPadding(0, Utils.toDP(12, context), 0, Utils.toDP(12, context));

        if (settings.darkTheme) {
            buttons.findViewById(R.id.compose_button).setAlpha(.75f);
        }

        final TweetButtonUtils utils = new TweetButtonUtils(context);
        utils.setUpShare(buttons, holder.status);

        new Thread(() -> {
            try {
                Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                counts.post(new Runnable() {
                    @Override
                    public void run() {
                        utils.setUpButtons(status, tweetId, counts, buttons, false, true);
                    }
                });
            } catch (Exception e) {

            }
        }).start();

        final int expansionSize = Utils.toDP(settings.alwaysShowButtons ? 48 : 64, context);
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(0, expansionSize);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.expandArea.getLayoutParams();
                layoutParams.height = val;
                holder.expandArea.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorContent.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
                holder.expandArea.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (holder.expandArea.getChildCount() > 0) {
                    holder.expandArea.removeAllViews();
                }

                holder.expandArea.setMinimumHeight(expansionSize);
                holder.expandArea.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.expandArea.invalidate();

                holder.expandArea.addView(counts);

                if (!settings.alwaysShowButtons) {
                    holder.expandArea.addView(buttons);
                }
            }
        });

        heightAnimatorContent.setDuration(ANIMATION_DURATION);
        heightAnimatorContent.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorContent);

    }


    public void loadEmbeddedTweet(final ViewHolder holder, final String otherUrls) {

        holder.embeddedTweet.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/") && !u.contains("/i/web/")) {
                        embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                        break;
                    }
                }

                if (embeddedId != 0l) {
                    Status status = null;
                    if (quotedTweets.containsKey(embeddedId)) {
                        status = quotedTweets.get(embeddedId);
                    } else {
                        try {
                            status = new StatusJSONImplMastodon(new GetStatusByID(embeddedId + "").execSync());
                            quotedTweets.put(embeddedId, status);
                        } catch (Exception e) {

                        }
                    }

                    final Status embedded = status;

                    if (status != null) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TweetView v = QuotedTweetView.create(context, embedded);
                                v.setDisplayProfilePicture(!settings.condensedTweets());
                                v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
                                v.setSmallImage(true);

                                holder.embeddedTweet.removeAllViews();
                                holder.embeddedTweet.addView(v.getView());

                                holder.embeddedTweet.setMinimumHeight(0);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private class Video {

        public int positionOnTimeline;
        public String url;
        public long tweetId;
        public SimpleVideoView videoView;
        public int videoType = 0; // 0 = GIF, 1 = VIDEO

        public Video(SimpleVideoView videoView, long tweetId, String url, int positionOnTimeline, int videoType) {
            this.videoView = videoView;
            this.tweetId = tweetId;
            this.url = url;
            this.positionOnTimeline = getCount() - positionOnTimeline + 1;
            this.videoType = videoType;
        }

        private boolean isPlaying = false;

        public void playCurrentVideo() {
            if (!activityPaused && videoView != null && !isDataSaver) {
                videoHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (activityPaused || videoView == null || Video.this.isPlaying) {
                            return;
                        }

                        if (settings.autoplay == AppSettings.AUTOPLAY_NEVER ||
                                (settings.autoplay == AppSettings.AUTOPLAY_WIFI && Utils.getConnectionStatus(context))) {
                            return;
                        }

                        if (videoType == 1 && settings.onlyAutoPlayGifs) {
                            return;
                        }

                        if (videoView.getVisibility() != View.VISIBLE) {
                            videoView.setVisibility(View.VISIBLE);
                        }

                        videoView.start(url);
                        Video.this.isPlaying = true;
                    }
                }, 500);
            }
        }

        public void stopOnScroll() {
            if (videoView != null && Video.this.isPlaying) {
                videoView.release();
                videoView.setVisibility(View.GONE);
            }
            resetVideoHandler();
            Video.this.isPlaying = false;
        }

        public void releaseVideo() {
            if (videoView != null) {
                videoView.setVisibility(View.GONE);
                videoView.release();
                tweetId = -1;
                videoView = null;
                url = null;
            }
        }
    }


    public Map<Long, Status> getQuotedTweets() {
        return quotedTweets;
    }

    public void setQuotedTweets(Map<Long, Status> quotedTweets) {
        this.quotedTweets = quotedTweets;
    }

    private void debounceClick(final View view) {
        view.setEnabled(false);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setEnabled(true);
            }
        }, 250);
    }

    private boolean isMuffled(String userId, ReTweeterAccount retweeter) {
        return muffledUsers.contains(userId) ||
                (retweeter != null && !android.text.TextUtils.isEmpty(retweeter.id) && muffledUsers.contains(retweeter.id));
    }

    private void setUpRevampedTweet(final Cursor cursor, ViewHolder holder, boolean muffled) {
        if (!settings.revampedTweets()) {
            return;
        }

        if (cursor.getPosition() == cursor.getCount() - 1) {
            holder.revampedTopLine.setVisibility(View.INVISIBLE);
        } else if (holder.revampedTopLine.getVisibility() != View.VISIBLE) {
            holder.revampedTopLine.setVisibility(View.VISIBLE);
        }

        View timeHolder = ((View) holder.time.getParent());
        if (muffled) {
            if (timeHolder.getVisibility() != View.GONE) timeHolder.setVisibility(View.GONE);
        } else if (timeHolder.getVisibility() != View.VISIBLE) {
            timeHolder.setVisibility(View.VISIBLE);
        }

        if (holder.retweeter.getVisibility() == View.VISIBLE) {
            if (holder.revampedRetweetIcon.getVisibility() != View.VISIBLE)
                holder.revampedRetweetIcon.setVisibility(View.VISIBLE);
        } else if (holder.revampedRetweetIcon.getVisibility() != View.GONE) {
            holder.revampedRetweetIcon.setVisibility(View.GONE);
        }
    }

    private void forceRippleAnimation(View view, MotionEvent event) {
        Drawable background = view.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && background instanceof RippleDrawable) {
            final RippleDrawable rippleDrawable = (RippleDrawable) background;

            rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});
            rippleDrawable.setHotspot(event.getX(), event.getY());

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    rippleDrawable.setState(new int[]{});
                }
            }, 300);
        }
    }
}
