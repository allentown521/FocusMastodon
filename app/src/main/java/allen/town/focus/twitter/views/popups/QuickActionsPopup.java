package allen.town.focus.twitter.views.popups;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.compose.ComposeSecAccActivity;
import allen.town.focus.twitter.api.requests.statuses.SetStatusBookmarked;
import allen.town.focus.twitter.api.requests.statuses.SetStatusFavorited;
import allen.town.focus.twitter.api.requests.statuses.SetStatusReblogged;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import allen.town.focus_common.util.TopSnackbarUtil;
import twitter4j.Status;


public class QuickActionsPopup extends PopupLayout {

    public enum Type {RETWEET, LIKE, BOOKMARK}

    ;

    Context context;

    long tweetId;
    String screenName;
    String retweeter;
    String tweetText;
    String replyText;
    Status mStatus;

    boolean secondAccount = false;

    public QuickActionsPopup(Context context, Status status) {
        this(context, status, false);
    }

    public QuickActionsPopup(Context context, Status status, boolean secondAccount) {
        super(context);
        this.context = context;
        mStatus = status;
        this.tweetId = status.getId();
        this.screenName = status.getUser().getScreenName();
        if (status.getReTweeterAccount() != null) {
            this.retweeter = status.getReTweeterAccount().username;
        }
        this.tweetText = status.getText();


        this.secondAccount = secondAccount;

        setReplyText();

        setTitle(getResources().getString(R.string.quick_actions));
        setWidth(Utils.toDP(332, context));
        setHeight(Utils.toDP(106, context));
        setAnimationScale(.5f);
    }

    View root;
    ImageButton like;
    ImageButton retweet;
    ImageButton reply;
    ImageButton quote;
    ImageButton share;
    ImageButton bookmark;

    @Override
    public View setMainLayout() {
        root = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.quick_actions, null, false);

        like = (ImageButton) root.findViewById(R.id.favorite_button);
        retweet = (ImageButton) root.findViewById(R.id.retweet_button);
        reply = (ImageButton) root.findViewById(R.id.reply_button);
        quote = (ImageButton) root.findViewById(R.id.quote_button);
        share = (ImageButton) root.findViewById(R.id.share_button);
        bookmark = (ImageButton) root.findViewById(R.id.bookmark_button);

        like.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new Action(context, Type.LIKE, tweetId).execute();
                hide();
            }
        });

        bookmark.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new Action(context, Type.BOOKMARK, tweetId).execute();
                hide();
            }
        });

        retweet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new Action(context, Type.RETWEET, tweetId).execute();
                hide();
            }
        });

        reply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent compose;

                if (!secondAccount) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }

                compose.putExtra("user", replyText);
                compose.putExtra("id", tweetId);
                compose.putExtra("reply_to_text", "@" + screenName + ": " + tweetText);
                compose.putExtra("reply_to_status", mStatus);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                        view.getMeasuredWidth(), view.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());

                hide();
            }
        });

        quote.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent compose;

                if (!secondAccount) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }

                compose.putExtra("user", " " + mStatus.getStatusUrl());
                compose.putExtra("id", tweetId);
                compose.putExtra("reply_to_text", "@" + screenName + ": " + tweetText);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                        view.getMeasuredWidth(), view.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());

                hide();
            }
        });

        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //快捷按钮的分享item的分享内容还不一样
                String shareText = "Tweet from @" + screenName + ": " + mStatus.getStatusUrl();
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, shareText);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((Activity) context).getWindow().setExitTransition(null);
                }

                context.startActivity(Intent.createChooser(share, "Share with:"));

                hide();
            }
        });

        return root;
    }

    class Action extends AsyncTask<String, Void, Void> {
        private Type type;
        private Context context;
        private long tweetId;

        public Action(Context context, Type type, long tweetId) {
            this.context = context;
            this.type = type;
            this.tweetId = tweetId;
        }

        @Override
        protected Void doInBackground(String... urls) {

            try {
                switch (type) {
                    case LIKE:
                        if (secondAccount) {
                            new SetStatusFavorited(tweetId + "", true).execSecondAccountSync();
                        } else {
                            new SetStatusFavorited(tweetId + "", true).execSync();
                        }
                        break;
                    case RETWEET:
                        if (secondAccount) {
                            new SetStatusReblogged(tweetId + "", true).execSecondAccountSync();
                        } else {
                            new SetStatusReblogged(tweetId + "", true).execSync();
                        }
                        break;
                    case BOOKMARK:
                        if (secondAccount) {
                            new SetStatusBookmarked(tweetId + "", true).execSecondAccountSync();
                        } else {
                            new SetStatusBookmarked(tweetId + "", true).execSync();
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        public void onPostExecute(Void nothing) {
            switch (type) {
                case LIKE:
                    TopSnackbarUtil.showSnack(context, R.string.liked_status, Toast.LENGTH_SHORT);
                    break;
                case RETWEET:
                    TopSnackbarUtil.showSnack(context, R.string.retweet_success, Toast.LENGTH_SHORT);
                    break;
                case BOOKMARK:
                    TopSnackbarUtil.showSnack(context, R.string.bookmark_success, Toast.LENGTH_SHORT);
                    break;
            }
        }
    }

    private void setReplyText() {
        AppSettings settings = AppSettings.getInstance(getContext());
        String extraNames = "";
        String replyStuff = "";
        String formattedText = Html.fromHtml(tweetText).toString();
        String screenNameToUse;
        if (secondAccount) {
            screenNameToUse = settings.secondScreenName;
        } else {
            screenNameToUse = settings.myScreenName;
        }

        if (!TextUtils.isEmpty(formattedText) && formattedText.contains("@")) {
            for (String s : formattedText.split(" ")) {
                if (s.contains("@") && !s.equals(screenNameToUse) && !extraNames.contains(s) && !s.equals(screenName)) {
                    extraNames += s + " ";
                }
            }
        }

        if (!screenName.equals(screenNameToUse)) {
            replyStuff = "@" + screenName + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        if (retweeter != null && !retweeter.isEmpty()) {
            replyStuff += "@" + retweeter.replace("@", "");
        }

        replyText = replyStuff.replace(" @" + screenNameToUse, "");
    }

    @Override
    public void show() {
        super.show();
    }
}