package allen.town.focus.twitter.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.activities.tweet_viewer.TweetActivity;
import allen.town.focus.twitter.data.sq_lite.ActivityDataSource;
import allen.town.focus.twitter.data.sq_lite.ActivitySQLiteHelper;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.model.Emoji;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.text.TextUtils;
import allen.town.focus.twitter.utils.text.TouchableMovementMethod;
import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.views.AccentMaterialDialog;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.UserMentionEntity;
import twitter4j.UserMentionEntityJSONImplMastodon;

public class ActivityCursorAdapter extends TimeLineCursorAdapter {

    public ActivityCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
    }

    private int TYPE_COL;
    private int TITLE_COL;

    @Override
    public void init(boolean cont) {
        super.init(cont);

        TYPE_COL = cursor.getColumnIndex(ActivitySQLiteHelper.COLUMN_TYPE);
        TITLE_COL = cursor.getColumnIndex(ActivitySQLiteHelper.COLUMN_TITLE);
    }

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.webPreviewCard != null && holder.embeddedTweet.getVisibility() != View.GONE) {
            holder.embeddedTweet.setVisibility(View.GONE);
        }

        if (holder.webPreviewCard != null && holder.webPreviewCard.getVisibility() != View.GONE) {
            holder.webPreviewCard.setVisibility(View.GONE);
        }


        final String title = cursor.getString(TITLE_COL);
        final long id = cursor.getLong(TWEET_COL);
        holder.tweetId = id;
        final String profilePic = cursor.getString(PRO_PIC_COL);
        holder.proPicUrl = profilePic;
        final String tweetText = cursor.getString(TEXT_COL);
        final String name = cursor.getString(NAME_COL);
        final String screenname = cursor.getString(SCREEN_NAME_COL);
        final String picUrl = cursor.getString(PIC_COL);
        holder.picUrl = picUrl;
        final long longTime = cursor.getLong(TIME_COL);
        final String otherUrl = cursor.getString(URL_COL);
        final String users = cursor.getString(USER_COL);
        final String hashtags = cursor.getString(HASHTAG_COL);
        holder.gifUrl = cursor.getString(GIF_COL);
        int type = cursor.getInt(TYPE_COL);
        final String userId = cursor.getString(USER_ID);
        String statusUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_STATUS_URL));
        String emoji = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI));

        holder.status = new StatusJSONImplMastodon(cursor);

        UiUtils.setPollRecyclerView(holder.pollRecyclerView, mContext, secondAcc, holder.status);

        //注意不能传它的父类UserMentionEntity，没有构造函数无法反序列化
        final List<UserMentionEntity> usersList = JsonHelper.parseObjectList(users
                , new TypeToken<List<UserMentionEntityJSONImplMastodon>>() {
                }.getType());

        final List<Emoji> emojis = JsonHelper.parseObjectList(cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI))
                , new TypeToken<List<Emoji>>() {
                }.getType());


        String retweeter;
        try {
            retweeter = cursor.getString(RETWEETER_COL);
        } catch (Exception e) {
            retweeter = "";
        }

        if (retweeter == null) {
            retweeter = "";
        }

        if (!settings.absoluteDate) {
            holder.screenTV.setText(Utils.getTimeAgo(longTime, context, true));
        } else {
            Date date = new Date(longTime);
            holder.screenTV.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }


        holder.name.setSingleLine(true);

        switch (type) {
            case ActivityDataSource.TYPE_NEW_FOLLOWER:
                holder.background.setOnClickListener(view1 -> {

                    if (usersList.size() == 1) {
                        ProfilePager.start(context, usersList.get(0).getId() + "");
                    } else {
                        displayUserDialog(usersList);
                    }
                });
                holder.profilePic.setOnClickListener(v -> holder.background.performClick());
                break;
            case ActivityDataSource.TYPE_FAVORITES:
            case ActivityDataSource.TYPE_RETWEETS:
                final String fRetweeter = retweeter;
                holder.background.setOnClickListener(view17 -> {
                    if (holder.preventNextClick) {
                        holder.preventNextClick = false;
                        return;
                    }
                    String link = "";

                    boolean displayPic = holder.picUrl != null && !holder.picUrl.equals("") && !holder.picUrl.contains("youtu");
                    if (displayPic) {
                        link = holder.picUrl;
                    } else {
                        link = otherUrl.split("  ")[0];
                    }

                    String text = tweetText;
                    String[] split = tweetText.split(" ");
                    if (split.length > 2 && split[1].endsWith(":")) {
                        text = "";
                        for (int i = 2; i < split.length; i++) {
                            text += split[i] + " ";
                        }
                    }

                    Intent viewTweet = new Intent(context, TweetActivity.class);
                    viewTweet.putExtra("name", settings.myName);
                    viewTweet.putExtra("screenname", settings.myScreenName);
                    viewTweet.putExtra("time", longTime);
                    viewTweet.putExtra("tweet", text);
                    viewTweet.putExtra("retweeter", fRetweeter);
                    viewTweet.putExtra("webpage", link);
                    viewTweet.putExtra("picture", displayPic);
                    viewTweet.putExtra("other_links", otherUrl);
                    viewTweet.putExtra("tweetid", holder.tweetId);
                    viewTweet.putExtra("proPic", settings.myProfilePicUrl);
                    viewTweet.putExtra("users", users);
                    viewTweet.putExtra("hashtags", hashtags);
                    viewTweet.putExtra("animated_gif", holder.gifUrl);
                    viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
                    viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);

                    TweetActivity.applyDragDismissBundle(context, viewTweet);

                    context.startActivity(viewTweet);
                });

                holder.profilePic.setOnClickListener(view16 -> holder.background.performClick());

                break;
            case ActivityDataSource.TYPE_MENTION:
                final String finalRetweeter = retweeter;
                holder.background.setOnClickListener(view15 -> {
                    if (holder.preventNextClick) {
                        holder.preventNextClick = false;
                        return;
                    }
                    String link = "";

                    boolean displayPic = holder.picUrl != null && !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
                    if (displayPic) {
                        link = holder.picUrl;
                    } else {
                        link = otherUrl.split("  ")[0];
                    }

                    Intent viewTweet = new Intent(context, TweetActivity.class);
                    viewTweet.putExtra("name", name);
                    viewTweet.putExtra("screenname", screenname);
                    viewTweet.putExtra("time", longTime);
                    viewTweet.putExtra("tweet", tweetText);
                    viewTweet.putExtra("retweeter", finalRetweeter);
                    viewTweet.putExtra("webpage", link);
                    viewTweet.putExtra("picture", displayPic);
                    viewTweet.putExtra("other_links", otherUrl);
                    viewTweet.putExtra("tweetid", holder.tweetId);
                    viewTweet.putExtra("proPic", profilePic);
                    viewTweet.putExtra("users", users);
                    viewTweet.putExtra("hashtags", hashtags);
                    viewTweet.putExtra("animated_gif", holder.gifUrl);
                    viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
                    viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);

                    TweetActivity.applyDragDismissBundle(context, viewTweet);

                    context.startActivity(viewTweet);
                });

                holder.profilePic.setOnClickListener(view14 -> ProfilePager.start(context, userId));

                break;
        }

        holder.name.setText(title);
        holder.tweet.setText(tweetText);
        HtmlParser.linkifyText(holder.tweet, emojis, usersList, false);
        HtmlParser.linkifyText(holder.replies, emojis, usersList, false);

        if (settings.showProfilePictures) {
            Glide.with(context).load(holder.proPicUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(null).into(holder.profilePic);
        } else if (holder.profilePic.getVisibility() != View.GONE) {
            holder.profilePic.setVisibility(View.GONE);
        }

        holder.tweet.setSoundEffectsEnabled(false);
        holder.tweet.setOnClickListener(view13 -> {
            if (!TouchableMovementMethod.touched) {
                // we need to manually set the background for click feedback because the spannable
                // absorbs the click on the background
                if (!holder.preventNextClick && holder.background != null && holder.background.getBackground() != null) {
                    holder.background.getBackground().setState(new int[]{android.R.attr.state_pressed});
                    new Handler().postDelayed(() -> holder.background.getBackground().setState(new int[]{android.R.attr.state_empty}), 25);
                }

                holder.background.performClick();
            }
        });

        holder.tweet.setOnLongClickListener(view12 -> {
            if (!TouchableMovementMethod.touched) {
                holder.background.performLongClick();
                holder.preventNextClick = true;
            }
            return false;
        });

        TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
        TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

    }

    public void displayUserDialog(final List<UserMentionEntity> users) {
        String[] nameList = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            UserMentionEntity userMentionEntity = users.get(i);
            nameList[i] = userMentionEntity.getName();
        }

        new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        )
                .setItems(nameList, (dialog, which) -> ProfilePager.start(context, users.get(which).getId() + ""))
                .create()
                .show();
    }
}
