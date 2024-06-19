package allen.town.focus.twitter.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.halilibo.bvpkotlin.BetterVideoPlayer;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.OnPeek;
import com.klinker.android.peekview.callback.SimpleOnPeek;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.VideoViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.ImageViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.activities.tweet_viewer.TweetActivity;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.data.WebPreview;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.listeners.MultipleImageTouchListener;
import allen.town.focus.twitter.model.ReTweeterAccount;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.twittertext.CustomEmojiHelper;
import allen.town.focus.twitter.utils.BetterVideoCallbackWrapper;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.ReplyUtils;
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
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class TimelineArrayAdapter extends ArrayAdapter<Status> implements WebPreviewCard.OnLoad {

    public Map<Long, Status> quotedTweets = new HashMap();
    public Map<String, WebPreview> webPreviews = new HashMap();

    @Override
    public void onLinkLoaded(@NotNull String link, @NotNull WebPreview preview) {
        webPreviews.put(link, preview);
    }

    public boolean openFirst = false;

    public static final int NORMAL = 0;
    public static final int RETWEET = 1;
    public static final int FAVORITE = 2;

    public Context context;
    public List<Status> statuses;
    public LayoutInflater inflater;
    public AppSettings settings;

    private String othersText;
    private String replyToText;

    public int layout;
    public Resources res;

    public ColorDrawable transparent;

    public int type;
    public String username;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    private int normalPictures;
    private int smallPictures;

    private boolean canUseQuickActions = true;
    int embeddedTweetMinHeight;

    public void setCanUseQuickActions(boolean bool) {
        canUseQuickActions = bool;
    }

    public static class ViewHolder {
        public TextView name;
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
        public TextView screenTV;
        public FrameLayout imageHolder;
        public View rootView;
        public ViewGroup embeddedTweet;
        public View quickActions;
        public WebPreviewCard webPreviewCard;
        public RecyclerView pollRecyclerView;
        // revamped tweet
        public View revampedTopLine;
        public View revampedRetweetIcon;
        public View revampedTweetContent;
        public View revampedContentRipple;

        public long tweetId;
        public boolean isFavorited;
        public String screenName;
        public String picUrl;
        public String retweeterName;
        public String animatedGif;

        public MultipleImageTouchListener imageTouchListener;
        public boolean preventNextClick = false;
        public Status status;
    }

    private List<Status> removeMutes(List<Status> statuses) {
        AppSettings settings = AppSettings.getInstance(context);
        SharedPreferences sharedPrefs = settings.sharedPrefs;
        String mutedUsers = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String mutedHashtags = sharedPrefs.getString("muted_hashtags", "");

        for (int i = 0; i < statuses.size(); i++) {
            if (mutedUsers.contains(statuses.get(i).getUser().getId() + "")) {
                statuses.remove(i);
                i--;
            } else if (statuses.get(i).isRetweet() &&
                    mutedUsers.contains(statuses.get(i).getRetweetedStatus().getUser().getId() + "")) {
                statuses.remove(i);
                i--;
            } else if (!isEmpty(mutedHashtags)) {
                for (String hashTag : mutedHashtags.split(" ")) {
                    if (statuses.get(i).getText().contains(hashTag)) {
                        statuses.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        return statuses;
    }

    private boolean isEmpty(String s) {
        return android.text.TextUtils.isEmpty(s.replace(" ", ""));
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, boolean openFirst) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";
        this.openFirst = openFirst;

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, List<Status> statuses) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, int type) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = type;
        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, String username) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;
        this.username = username;

        setUpLayout();
    }

    public void setUpLayout() {

        replyToText = context.getString(R.string.reply_to);

        statuses = removeMutes(statuses);

        normalPictures = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        smallPictures = Utils.toDP(120, context);

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

        layout = R.layout.tweet;
        if (settings.revampedTweets()) {
            layout = R.layout.tweet_revamp;
        } else if (settings.condensedTweets()) {
            layout = R.layout.tweet_condensed;
        }

        embeddedTweetMinHeight = settings.condensedTweets() ? Utils.toDP(70, context) : Utils.toDP(140, context);

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
    }

    @Override
    public int getCount() {
        return statuses.size();
    }

    @Override
    public Status getItem(int position) {
        return statuses.get(position);
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.replies = (TextView) v.findViewById(R.id.reply_to);
        holder.background = (View) v.findViewById(R.id.background);
        holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        holder.isAConversation = (ImageView) v.findViewById(R.id.is_a_conversation);
        holder.embeddedTweet = (ViewGroup) v.findViewById(R.id.embedded_tweet_card);
        holder.quickActions = v.findViewById(R.id.quick_actions);
        holder.webPreviewCard = (WebPreviewCard) v.findViewById(R.id.web_preview_card);
        holder.pollRecyclerView = (RecyclerView) v.findViewById(R.id.poll_list);
        holder.pollRecyclerView.setNestedScrollingEnabled(false);
        holder.playButton = (ImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        holder.image = (ImageView) v.findViewById(R.id.image);

        // revamped tweet
        holder.revampedTopLine = v.findViewById(R.id.line_above_profile_picture);
        holder.revampedRetweetIcon = v.findViewById(R.id.retweet_icon);
        holder.revampedTweetContent = v.findViewById(R.id.content_card);
        holder.revampedContentRipple = v.findViewById(R.id.content_ripple);

        //surfaceView.profilePic.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - (settings.condensedTweets() || settings.revampedTweets() ? 1 : 2));
        holder.name.setTextSize(settings.textSize + (settings.condensedTweets() ? 1 : 4));
        holder.time.setTextSize(settings.textSize - (settings.revampedTweets() ? 2 : 3));
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 2);

        // some things we just don't need to configure every time
        holder.tweet.setSoundEffectsEnabled(false);
        holder.tweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    holder.background.performClick();
                }
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

    public void bindView(final View view, Status status, final int position) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.embeddedTweet.getChildCount() > 0 || holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.setVisibility(View.GONE);
            if (settings.detailedQuotes) {
                holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
            }
        }

        UiUtils.setPollRecyclerView(holder.pollRecyclerView, context, false, status);

        holder.webPreviewCard.clear();

        Status thisStatus;

        ReTweeterAccount retweeter;
        final long time = status.getCreatedAt().getTime();

        if (status.isRetweet()) {
            retweeter = new ReTweeterAccount(status.getUser().getId() + "", status.getUser().getScreenName(), status.getUser().getName());
            thisStatus = status.getRetweetedStatus();
        } else {
            retweeter = null;
            thisStatus = status;
        }

        User user = thisStatus.getUser();
        holder.status = thisStatus;
        holder.tweetId = thisStatus.getId();
        final long id = holder.tweetId;
        String pic = null;
        try {
            pic = user.getOriginalProfileImageURL();
        } catch (Exception e) {
            pic = user.getProfileImageURL();
        }

        final String profilePic = pic;
        final String name = user.getName();
        final String screenname = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(thisStatus);
        String tweetTexts = html[0];
        final String picUrl = html[1];
        holder.picUrl = picUrl;
        holder.imageTouchListener.setImageUrls(picUrl);
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];

        final TweetLinkUtils.TweetMediaInformation info = TweetLinkUtils.getGIFUrl(status, otherUrl);
        holder.animatedGif = info.url;
        final String statusUrl = thisStatus.getStatusUrl();
        final String emoji = JsonHelper.toJSONString(thisStatus.getEmoji());
        final boolean inAConversation = thisStatus.getInReplyToStatusId() != -1;

        final List<UserMentionEntity> mention = thisStatus.getUserMentionEntitiesList();
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
                tweetTexts = tweetTexts.substring(0, tweetTexts.lastIndexOf("\n\n"));
            }

            if (ReplyUtils.showMultipleReplyNames(replies)) {
                holder.replies.setText(replyToText + " " + ReplyUtils.getReplyingNamesToHandles(mention));
                HtmlParser.linkifyText(holder.replies, thisStatus.getEmoji(), mention, false);
            } else {

                //点击事件不灵敏，字符串长一些号点击，原因未知
                othersText = context.getString(R.string.others) + " " + (mention.size() - 1) + " " + context.getString(R.string.noti_reply);
                final String firstPerson = mention.get(0).getName();
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

        if (canUseQuickActions && holder.quickActions != null) {
            holder.quickActions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickActionsPopup popup = new QuickActionsPopup(context, status);
                    popup.setExpansionPointForAnim(holder.quickActions);
                    popup.setOnTopOfView(holder.quickActions);
                    popup.show();
                }
            });
        }

        final ReTweeterAccount fRetweeter = retweeter;
        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.preventNextClick) {
                    holder.preventNextClick = false;
                    return;
                }

                String link;

                boolean hasGif = holder.animatedGif != null && !holder.animatedGif.isEmpty();
                boolean displayPic = !holder.picUrl.equals("");
                if (displayPic) {
                    link = holder.picUrl;
                } else {
                    link = otherUrl.split("  ")[0];
                }

                Log.v("tweet_page", "clicked");
                Intent viewTweet = new Intent(context, TweetActivity.class);
                viewTweet.putExtra("name", name);
                viewTweet.putExtra("screenname", screenname);
                viewTweet.putExtra("time", time);
                viewTweet.putExtra("tweet", tweetWithReplyHandles);
                viewTweet.putExtra("retweeter", JsonHelper.toJSONString(fRetweeter));
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", holder.tweetId);
                viewTweet.putExtra("proPic", profilePic);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", holder.animatedGif);
                viewTweet.putExtra("conversation", inAConversation);
                viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
                viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);

                TweetActivity.applyDragDismissBundle(context, viewTweet);

                context.startActivity(viewTweet);
            }
        });

        ProfilePeek.create(context, holder.profilePic, user.getId() + "");

        if (!screenname.equals(username)) {
            holder.profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ProfilePager.start(context, name, user.getId() + "", profilePic);
                }
            });

            holder.profilePic.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    ProfilePager.start(context, name, user.getId() + "", profilePic);
                    return false;
                }
            });
        } else {
            // need to clear the click listener so it isn't left over from another profile
            holder.profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            holder.profilePic.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    return true;
                }
            });
        }

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(time, context, true));
        } else {
            Date date = new Date(time);
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

        if (settings.inlinePics) {
            if (holder.picUrl.equals("")) {
                if (holder.imageHolder.getVisibility() != View.GONE) {
                    holder.imageHolder.setVisibility(View.GONE);
                }

                if (holder.playButton.getVisibility() == View.VISIBLE) {
                    holder.playButton.setVisibility(View.GONE);
                }
            } else {
                if (holder.imageHolder.getVisibility() == View.GONE) {
                    holder.imageHolder.setVisibility(View.VISIBLE);
                }

                if (holder.picUrl.contains("youtube") || (holder.animatedGif != null && !android.text.TextUtils.isEmpty(holder.animatedGif))) {

                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    PeekViewOptions options = new PeekViewOptions();
                    options.setFullScreenPeek(true);
                    options.setBackgroundDim(1f);

                    int layoutRes = 0;
                    if (VideoMatcherUtil.isTwitterGifLink(holder.animatedGif)) {
                        holder.playButton.setImageDrawable(new GifBadge(context));
                        layoutRes = R.layout.peek_gif;
                    } else {
                        holder.playButton.setImageDrawable(new VideoBadge(context, info.duration));

                        if (!holder.picUrl.contains("youtu")) {
                            layoutRes = R.layout.peek_video;
                        }
                    }

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        if (layoutRes != 0) {
                            Peek.into(layoutRes, new OnPeek() {
                                private BetterVideoPlayer videoView;

                                @Override
                                public void shown() {
                                }

                                @Override
                                public void onInflated(View rootView) {
                                    videoView = (BetterVideoPlayer) rootView.findViewById(R.id.video);
                                    videoView.setSource(Uri.parse(holder.animatedGif.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                    videoView.setCallback(new BetterVideoCallbackWrapper() {
                                        @Override
                                        public void onCompletion(BetterVideoPlayer player) {
                                            if (VideoMatcherUtil.isTwitterGifLink(holder.animatedGif)) {
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
                            }).with(options).applyTo((PeekViewActivity) context, holder.image);
                        } else {
                            Peek.clear(holder.image);
                        }
                    }

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            VideoViewerActivity.startActivity(context, id, holder.animatedGif, otherUrl);
                        }
                    });

                    holder.image.setImageDrawable(transparent);

                    picture = true;

                } else {

                    holder.image.setImageDrawable(transparent);

                    picture = true;

                    if (holder.playButton.getVisibility() == View.VISIBLE) {
                        holder.playButton.setVisibility(View.GONE);
                    }

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ImageViewerActivity.Companion.startActivity(context, id, holder.image, holder.imageTouchListener.getImageTouchPosition(), holder.picUrl.split(" "));
                        }
                    });

                    if (context instanceof PeekViewActivity && settings.usePeek) {
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
                }

                if (holder.imageHolder.getVisibility() == View.GONE) {
                    holder.imageHolder.setVisibility(View.VISIBLE);
                }
            }
        }

        if (type == NORMAL) {
            if (retweeter != null) {
                holder.retweeter.setText(context.getResources().getString(R.string.retweeter) + ReTweeterAccount.getRetweeterFormatUrl(retweeter.displayName, retweeter.id));
                holder.retweeterName = retweeter.displayName;
                holder.retweeter.setVisibility(View.VISIBLE);
            } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
                holder.retweeter.setVisibility(View.GONE);
            }
        } else if (type == RETWEET) {

            int count = status.getRetweetCount();

            if (count > 1) {
                holder.retweeter.setText(status.getRetweetCount() + " " + context.getResources().getString(R.string.retweets_lower));
                holder.retweeter.setVisibility(View.VISIBLE);
            } else if (count == 1) {
                holder.retweeter.setText(status.getRetweetCount() + " " + context.getResources().getString(R.string.retweet_lower));
                holder.retweeter.setVisibility(View.VISIBLE);
            }
        }

        if (settings.showProfilePictures) {
            try {
                Glide.with(context).load(profilePic)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(null).into(holder.profilePic);
            } catch (Exception e) {

            }
        } else if (holder.profilePic.getVisibility() != View.GONE) {
            holder.profilePic.setVisibility(View.GONE);
            holder.name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.profilePic.performClick();
                }
            });
        }

        holder.image.setImageDrawable(null);
        try {
            if (picture) {
                if (!settings.condensedTweets()) {
                    if (settings.cropImagesOnTimeline) {
                        Glide.with(context).load(holder.picUrl)
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
        } catch (Exception e) {

        }

        if (embeddedTweetFound || picture || !settings.webPreviews) {
            if (holder.webPreviewCard.getVisibility() == View.VISIBLE) {
                holder.webPreviewCard.setVisibility(View.GONE);
            }
        } else if (otherUrl != null && otherUrl.length() > 0) {

            if (holder.webPreviewCard.getVisibility() == View.GONE) {
                holder.webPreviewCard.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.webPreviewCard.getVisibility() == View.VISIBLE) {
                holder.webPreviewCard.setVisibility(View.GONE);
            }
        }

        if (showCompressReply) {
            //压缩回复会将post中的@信息移除掉，所以需要格式化，那么用HtmlParser就不会正常解析hashtag了
            allen.town.focus.twitter.utils.text.TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
            holder.tweet.setText(CustomEmojiHelper.emojify(holder.tweet.getText(), thisStatus.getEmoji(), holder.tweet, false));
        } else {
            HtmlParser.linkifyText(holder.tweet, thisStatus.getEmoji(), mention, false);
        }
        HtmlParser.linkifyText(holder.retweeter, thisStatus.getEmoji(), mention, false);


        if (TweetView.isEmbeddedTweet(tweetText) && otherUrl != null && !otherUrl.contains("/photo/")) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
            if (!tryImmediateEmbeddedLoad(holder, otherUrl)) {
                loadEmbeddedTweet(holder, otherUrl);
            }
        }

        tryImmediateWebPageLoad(holder);

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity) context).finish();
        }

        setUpRevampedTweet(position, holder);
    }

    private boolean tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
        Long embeddedId = 0l;
        for (String u : otherUrl.split(" ")) {
            if (u.contains("/status/") && !u.contains("/i/web/") && !otherUrl.contains("/photo/")) {
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

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;
        }

        if (statuses.size() > position) {
            bindView(v, statuses.get(position), position);
        }

        return v;
    }

    public void loadEmbeddedTweet(final ViewHolder holder, final String otherUrls) {

        holder.embeddedTweet.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/") && !u.contains("/i/web/") && !u.contains("/photo/")) {
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

    private void setUpRevampedTweet(final int position, ViewHolder holder) {
        if (!settings.revampedTweets()) {
            return;
        }

        if (position == 0) {
            holder.revampedTopLine.setVisibility(View.INVISIBLE);
        } else if (holder.revampedTopLine.getVisibility() != View.VISIBLE) {
            holder.revampedTopLine.setVisibility(View.VISIBLE);
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
