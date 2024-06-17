package allen.town.focus.twitter.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.compose.ComposeSecAccActivity;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.SavedTweetsFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.api.requests.statuses.SetStatusBookmarked;
import allen.town.focus.twitter.api.requests.statuses.SetStatusFavorited;
import allen.town.focus.twitter.api.requests.statuses.SetStatusReblogged;
import allen.town.focus.twitter.data.sq_lite.BookmarkedTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.FavoriteTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.SavedTweetsDataSource;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.UserMentionEntity;

public class TweetButtonUtils {

    private ExpansionViewHelper.TweetLoaded tweetLoaded;

    //原始的status
    private Status originalStatus;
    //如果是转推那么就是转推的内容
    private Status status;
    private Context context;
    private AppSettings settings;

    private TextView tweetCounts;
    private TextView tweetVia;
    private ImageButton retweetButton;
    private ImageButton likeButton;
    private ImageButton bookmarkButton;

    private boolean secondAcc;
    private String replyText;

    public TweetButtonUtils(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);
    }

    public void setIsSecondAcc(boolean secondAcc) {
        this.secondAcc = secondAcc;
    }

    public void setUpShare(View buttonsRoot, Status status) {
        setUpShare(buttonsRoot, status.getText(), status.getUser().getScreenName(), status.getStatusUrl());
    }

    public void setUpShare(View buttonsRoot, String statusText, String screenname, String statusUrl) {
        final ImageButton shareButton = buttonsRoot.findViewById(R.id.share_button);

        shareButton.setOnClickListener(view -> {
            String text = statusUrl + "\n\n@" + screenname + ": " + Html.fromHtml(statusText);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Tweet from @" + screenname);
            share.putExtra(Intent.EXTRA_TEXT, text);

            context.startActivity(Intent.createChooser(share, "Share with:"));
        });
    }

    /**
     * @param s                       需要转推(如果是转推)后的内容
     * @param tweetId
     * @param countsRoot
     * @param buttonsRoot
     * @param showOverflow
     * @param tweetLoadedSuccessfully
     */
    public void setUpButtons(Status s, final long tweetId, View countsRoot, View buttonsRoot, boolean showOverflow, boolean tweetLoadedSuccessfully) {
        final ImageButton overflowButton = (ImageButton) buttonsRoot.findViewById(R.id.overflow_button);
        if (s == null) {
            if (showOverflow) {
                overflowButton.setVisibility(View.VISIBLE);
                final boolean tweetIsSaved = SavedTweetsDataSource.getInstance(context).isTweetSaved(tweetId, settings.currentAccount);

                if (!tweetIsSaved) {
                    overflowButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        }
                    });
                } else {
                    overflowButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final PopupMenu menu = new PopupMenu(context, overflowButton);
                            final int SAVE_TWEET = 0;

                            menu.getMenu().add(Menu.NONE, SAVE_TWEET, Menu.NONE, context.getString(R.string.remove_from_saved_tweets));
                            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    switch (menuItem.getItemId()) {
                                        case SAVE_TWEET:
                                            SavedTweetsDataSource.getInstance(context).deleteTweet(tweetId);
                                            context.sendBroadcast(new Intent(SavedTweetsFragment.REFRESH_ACTION));

                                            if (context instanceof Activity) {
                                                ((Activity) context).finish();
                                            }
                                            break;

                                    }
                                    return false;
                                }
                            });

                            menu.show();
                        }
                    });
                }
            }

            return;
        }
        this.originalStatus = s;
        if (s.isRetweet()) {
            s = s.getRetweetedStatus();
        }

        this.status = s;
        this.replyText = generateReplyText();

        tweetCounts = countsRoot.findViewById(R.id.tweet_counts);
        tweetVia = countsRoot.findViewById(R.id.tweet_source);
        likeButton = buttonsRoot.findViewById(R.id.like_button);
        retweetButton = buttonsRoot.findViewById(R.id.retweet_button);
        bookmarkButton = buttonsRoot.findViewById(R.id.bookmark_button);
        final ImageButton composeButton = buttonsRoot.findViewById(R.id.compose_button);
        final ImageButton quoteButton = buttonsRoot.findViewById(R.id.quote_button);
        final ImageButton shareButton = buttonsRoot.findViewById(R.id.share_button);

        if (showOverflow) {
            overflowButton.setVisibility(View.VISIBLE);
        }

        if (!tweetLoadedSuccessfully) {
            return;
        }

        likeButton.setOnClickListener(view -> {
            if (status.isFavorited() || !settings.crossAccActions) {
                favoriteStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
            } else if (settings.crossAccActions) {
                // dialog for favoriting
                String[] options = new String[2];
//                    String[] options = new String[3];

                options[0] = "@" + settings.myScreenName;
                options[1] = "@" + settings.secondScreenName;
//                    options[2] = context.getString(R.string.both_accounts);

                new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme).setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int item) {
                        favoriteStatus(item + 1);
                    }
                }).create().show();
            }
        });
        bookmarkButton.setOnClickListener(view -> {
            if (status.isBookmarked() || !settings.crossAccActions) {
                bookmarkStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
            } else if (settings.crossAccActions) {
                // dialog for favoriting
                String[] options = new String[2];
//                    String[] options = new String[3];

                options[0] = "@" + settings.myScreenName;
                options[1] = "@" + settings.secondScreenName;
//                    options[2] = context.getString(R.string.both_accounts);

                new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme).setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int item) {
                        bookmarkStatus(item + 1);
                    }
                }).create().show();
            }
        });

        retweetButton.setOnClickListener(view -> {
            if (status.isRetweetedByMe() || !settings.crossAccActions) {
                retweetStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
            } else {
                // dialog for favoriting
                String[] options = new String[2];
//                    String[] options = new String[3];

                options[0] = "@" + settings.myScreenName;
                options[1] = "@" + settings.secondScreenName;
//                    options[2] = context.getString(R.string.both_accounts);

                new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme).setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int item) {
                        retweetStatus(item + 1);
                    }
                }).create().show();
            }
        });

        quoteButton.setOnClickListener(v -> {
            String text = restoreLinks(status.getText());

            switch (AppSettings.getInstance(context).quoteStyle) {
                case AppSettings.QUOTE_STYLE_TWITTER:
                    text = " " + status.getStatusUrl();
                    break;
                case AppSettings.QUOTE_STYLE_Focus_for_Mastodon:
                    text = restoreLinks(text);
                    text = "\"@" + status.getUser().getScreenName() + ": " + text + "\" ";
                    break;
                case AppSettings.QUOTE_STYLE_RT:
                    text = restoreLinks(text);
                    text = " RT @" + status.getUser().getScreenName() + ": " + text;
                    break;
                case AppSettings.QUOTE_STYLE_VIA:
                    text = restoreLinks(text);
                    text = text + " via @" + status.getUser().getScreenName();
            }

            Intent quote;
            if (!secondAcc) {
                quote = new Intent(context, ComposeActivity.class);
            } else {
                quote = new Intent(context, ComposeSecAccActivity.class);
            }
            quote.putExtra("user", text);
            quote.putExtra("id", status.getId());
            quote.putExtra("reply_to_text", "@" + status.getUser().getScreenName() + ": " + status.getText());

            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            quote.putExtra("already_animated", true);

            context.startActivity(quote, opts.toBundle());
        });

        composeButton.setOnClickListener(v -> {
            Intent compose;
            if (!secondAcc) {
                compose = new Intent(context, ComposeActivity.class);
            } else {
                compose = new Intent(context, ComposeSecAccActivity.class);
            }
            compose.putExtra("user", replyText);
            compose.putExtra("id", status.getId());
            compose.putExtra("reply_to_text", "@" + status.getUser().getScreenName() + ": " + status.getText());
            compose.putExtra("reply_to_status", status);

            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            compose.putExtra("already_animated", true);

            context.startActivity(compose, opts.toBundle());
        });

        if (status.getUser().isProtected()) {
            retweetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TopSnackbarUtil.showSnack(context, R.string.protected_account_retweet, Toast.LENGTH_SHORT);
                }
            });

            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TopSnackbarUtil.showSnack(context, R.string.protected_account_quote, Toast.LENGTH_SHORT);
                }
            });
        }

        setUpShare(buttonsRoot, status);
        updateTweetCounts(status);
    }

    /**
     * @param statusOriginal  从数据库读取出来的
     * @param buttonsRoot
     * @param likeCallback
     * @param retweetCallback
     */
    public void setUpSimpleButtons(final Status statusOriginal, View buttonsRoot, final LikeCallback likeCallback, final RetweetCallback retweetCallback, final BookmarkCallback bookmarkCallback) {

        long tweetId = statusOriginal.getId();
        String screenName = statusOriginal.getUser().getScreenName();
        String text = statusOriginal.getText();
        likeButton = buttonsRoot.findViewById(R.id.always_like_button);
        retweetButton = buttonsRoot.findViewById(R.id.always_retweet_button);
        bookmarkButton = buttonsRoot.findViewById(R.id.always_bookmark_button);
        final ImageButton composeButton = buttonsRoot.findViewById(R.id.always_compose_button);
        final ImageButton quoteButton = buttonsRoot.findViewById(R.id.always_quote_button);
        final ImageButton shareButton = buttonsRoot.findViewById(R.id.always_share_button);
        likeButton.setOnClickListener(view -> {
            new Thread(() -> {
                try {
                    Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                    this.originalStatus = s;
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    this.status = status;

                    buttonsRoot.post(() -> {
                        likeCallback.onLikeChanged(!status.isFavorited(), status);
                        favoriteStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                    });
                } catch (Exception e) {

                }
            }).start();
        });
        bookmarkButton.setOnClickListener(view -> {
            new Thread(() -> {
                try {
                    Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                    this.originalStatus = s;
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    this.status = status;

                    buttonsRoot.post(() -> {
                        bookmarkCallback.onBookmarkChanged(!status.isBookmarked(), status);
                        bookmarkStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                    });
                } catch (Exception e) {

                }
            }).start();
        });

        retweetButton.setOnClickListener(view -> {
            new Thread(() -> {
                try {
                    Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                    this.originalStatus = s;
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    this.status = status;

                    buttonsRoot.post(() -> {
                        retweetCallback.onRetweetChanged(!status.isRetweetedByMe(), status);
                        retweetStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                    });
                } catch (Exception e) {

                }
            }).start();
        });

        quoteButton.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                    this.originalStatus = s;
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    this.status = status;
                    this.replyText = generateReplyText();

                    buttonsRoot.post(() -> {
                        String text1 = restoreLinks(status.getText());

                        switch (AppSettings.getInstance(context).quoteStyle) {
                            case AppSettings.QUOTE_STYLE_TWITTER:
                                text1 = " " + status.getStatusUrl();
                                break;
                            case AppSettings.QUOTE_STYLE_Focus_for_Mastodon:
                                text1 = restoreLinks(text1);
                                text1 = "\"@" + status.getUser().getScreenName() + ": " + text1 + "\" ";
                                break;
                            case AppSettings.QUOTE_STYLE_RT:
                                text1 = restoreLinks(text1);
                                text1 = " RT @" + status.getUser().getScreenName() + ": " + text1;
                                break;
                            case AppSettings.QUOTE_STYLE_VIA:
                                text1 = restoreLinks(text1);
                                text1 = text1 + " via @" + status.getUser().getScreenName();
                        }

                        Intent quote;
                        if (!secondAcc) {
                            quote = new Intent(context, ComposeActivity.class);
                        } else {
                            quote = new Intent(context, ComposeSecAccActivity.class);
                        }
                        quote.putExtra("user", text1);
                        quote.putExtra("id", status.getId());
                        quote.putExtra("reply_to_text", "@" + status.getUser().getScreenName() + ": " + status.getText());

                        ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
                        quote.putExtra("already_animated", true);

                        context.startActivity(quote, opts.toBundle());
                    });
                } catch (Exception e) {

                }
            }).start();
        });

        composeButton.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    Status s = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    this.status = status;
                    this.replyText = generateReplyText();

                    buttonsRoot.post(() -> {
                        Intent compose;
                        if (!secondAcc) {
                            compose = new Intent(context, ComposeActivity.class);
                        } else {
                            compose = new Intent(context, ComposeSecAccActivity.class);
                        }
                        compose.putExtra("user", replyText);
                        compose.putExtra("id", tweetId);
                        compose.putExtra("reply_to_text", "@" + screenName + ": " + text);
                        compose.putExtra("reply_to_status", status);

                        ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
                        compose.putExtra("already_animated", true);

                        context.startActivity(compose, opts.toBundle());
                    });
                } catch (Exception e) {

                }
            }).start();
        });

        shareButton.setOnClickListener(view -> {
            String text12 = statusOriginal.getStatusUrl() + "\n\n@" + screenName + ": " + Html.fromHtml(text);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Tweet from @" + screenName);
            share.putExtra(Intent.EXTRA_TEXT, text12);

            context.startActivity(Intent.createChooser(share, "Share with:"));
        });
        updateTweetCounts(statusOriginal);
    }


    private final int TYPE_ACC_ONE = 1;
    private final int TYPE_ACC_TWO = 2;

    private void updateTweetCounts(twitter4j.Status status) {
        this.originalStatus = status;
        if (status.isRetweet()) {
            status = status.getRetweetedStatus();
        }

        this.status = status;

        AppSettings settings = AppSettings.getInstance(context);

        String retweets = status.getRetweetCount() == 1 ? context.getString(R.string.retweet).toLowerCase() : context.getString(R.string.new_retweets);
        String likes = status.getFavoriteCount() == 1 ? context.getString(R.string.favorite).toLowerCase() : context.getString(R.string.new_favorites);
        String tweetCount = status.getFavoriteCount() + " <b>" + likes + "</b>  " + (!status.getUser().isProtected() ? status.getRetweetCount() + " <b>" + retweets + "</b> " : "");
        if (tweetCounts != null) {
            tweetCounts.setText(Html.fromHtml(tweetCount));
        }

        if (status.isRetweetedByMe()) {
            retweetButton.setImageResource(R.drawable.ic_retweet);
            retweetButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
        } else {
            retweetButton.clearColorFilter();

        }

        if (status.isFavorited()) {
            likeButton.setImageResource(R.drawable.ic_heart);
            likeButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
        } else {
            likeButton.clearColorFilter();
            likeButton.setImageResource(R.drawable.ic_heart_outline);
        }
        if (status.isBookmarked()) {
            bookmarkButton.setImageResource(R.drawable.round_bookmark_24);
            bookmarkButton.setColorFilter(ThemeStore.accentColor(context), PorterDuff.Mode.MULTIPLY);
        } else {
            bookmarkButton.clearColorFilter();
            bookmarkButton.setImageResource(R.drawable.round_bookmark_border_24);
        }

        if (!TextUtils.isEmpty(status.getSource()) && tweetVia != null) {
            String via = context.getResources().getString(R.string.via) + " <b>" + android.text.Html.fromHtml(status.getSource()).toString() + "</b>";
            tweetVia.setText(Html.fromHtml(via));
        }

    }

    public void favoriteStatus(final int type) {
        final long id = status.getId();
        new TimeoutThread(() -> {
            try {
                boolean useAccount1 = false;
                boolean useAccount2 = false;
                if (type == TYPE_ACC_ONE) {
                    useAccount1 = true;
                } else if (type == TYPE_ACC_TWO) {
                    useAccount2 = true;
                } else {
                    useAccount1 = true;
                    useAccount2 = true;
                }

                if (status.isFavorited() && useAccount1) {
                    ((Activity) context).runOnUiThread(() -> TopSnackbarUtil.showSnack(context, R.string.removing_favorite, Toast.LENGTH_SHORT));
                    new SetStatusFavorited(id + "", false).execSync();
                    try {
                        FavoriteTweetsDataSource.getInstance(context).deleteTweet(id);
                        context.sendBroadcast(new Intent(IntentConstant.RESET_FAVORITES_ACTION));
                    } catch (Exception e) {
                    }
                } else if (useAccount1) {
                    ((Activity) context).runOnUiThread(() -> TopSnackbarUtil.showSnack(context, R.string.favoriting_status, Toast.LENGTH_SHORT));
                    try {
                        new SetStatusFavorited(id + "", true).execSync();
                    } catch (Exception e) {
                        // already been favorited by this account
                    }
                }

                if (useAccount2) {
                    try {
                        new SetStatusFavorited(id + "", true).execSecondAccountSync();
                    } catch (Exception e) {

                    }
                }

                final Status originalStatus = new StatusJSONImplMastodon(new GetStatusByID(TweetButtonUtils.this.originalStatus.getId() + "").execSync());
                final Status status = originalStatus.isRetweet() ? originalStatus.getRetweetedStatus() : originalStatus;
                HomeDataSource.getInstance(context).updateTweet(originalStatus, AppSettings.getSharedPreferences(context).getInt(AppSettings.CURRENT_ACCOUNT, 1));

                ((Activity) context).runOnUiThread(() -> {
                    try {
                        updateTweetCounts(status);
                    } catch (Throwable t) {
                        // won't work with the simple tweet buttons (no count shown)
                    }
                });
            } catch (Exception e) {

            }
        }).start();
    }

    public void bookmarkStatus(final int type) {
        final long id = status.getId();
        new TimeoutThread(() -> {
            try {
                boolean useAccount1 = false;
                boolean useAccount2 = false;
                if (type == TYPE_ACC_ONE) {
                    useAccount1 = true;
                } else if (type == TYPE_ACC_TWO) {
                    useAccount2 = true;
                } else {
                    useAccount1 = true;
                    useAccount2 = true;
                }

                if (status.isBookmarked() && useAccount1) {
                    ((Activity) context).runOnUiThread(() -> TopSnackbarUtil.showSnack(context, R.string.removing_bookmark, Toast.LENGTH_SHORT));
                    new SetStatusBookmarked(id + "", false).execSync();
                    try {
                        BookmarkedTweetsDataSource.getInstance(context).deleteTweet(id);
                        context.sendBroadcast(new Intent(IntentConstant.RESET_BOOKMARKS_ACTION));
                    } catch (Exception e) {
                    }
                } else if (useAccount1) {
                    ((Activity) context).runOnUiThread(() -> TopSnackbarUtil.showSnack(context, R.string.bookmarking_status, Toast.LENGTH_SHORT));
                    try {
                        new SetStatusBookmarked(id + "", true).execSync();
                    } catch (Exception e) {
                        // already been bookmarked by this account
                    }
                }

                if (useAccount2) {
                    try {
                        new SetStatusBookmarked(id + "", true).execSecondAccountSync();
                    } catch (Exception e) {

                    }
                }

                final Status originalStatus = new StatusJSONImplMastodon(new GetStatusByID(TweetButtonUtils.this.originalStatus.getId() + "").execSync());
                final Status status = originalStatus.isRetweet() ? originalStatus.getRetweetedStatus() : originalStatus;
                HomeDataSource.getInstance(context).updateTweet(originalStatus, AppSettings.getSharedPreferences(context).getInt(AppSettings.CURRENT_ACCOUNT, 1));

                ((Activity) context).runOnUiThread(() -> {
                    try {
                        updateTweetCounts(status);
                    } catch (Throwable t) {
                        // won't work with the simple tweet buttons (no count shown)
                    }
                });
            } catch (Exception e) {

            }
        }).start();
    }

    public void retweetStatus(final int type) {
        final long id = status.getId();
        new TimeoutThread(() -> {
            try {
                // if they have a protected account, we want to still be able to retweet their retweets
                long idToRetweet = id;
                if (status != null && status.isRetweet()) {
                    idToRetweet = status.getRetweetedStatus().getId();
                }

                boolean useAccount1 = false;
                boolean useAccount2 = false;
                if (type == TYPE_ACC_ONE) {
                    useAccount1 = true;
                } else if (type == TYPE_ACC_TWO) {
                    useAccount2 = true;
                } else {
                    useAccount1 = true;
                    useAccount2 = true;
                }

                if (status.isRetweetedByMe() && useAccount1) {
                    ((Activity) context).runOnUiThread(() -> {
                        TopSnackbarUtil.showSnack(context, R.string.removing_retweet, Toast.LENGTH_SHORT);
                    });
                    new SetStatusReblogged(idToRetweet + "", false).execSync();
                } else if (useAccount1) {
                    ((Activity) context).runOnUiThread(() -> TopSnackbarUtil.showSnack(context, R.string.retweeting_status, Toast.LENGTH_SHORT));
                    try {
                        new SetStatusReblogged(idToRetweet + "", true).execSync();
                    } catch (Exception e) {

                    }
                }

                if (useAccount2) {
                    new SetStatusReblogged(idToRetweet + "", true).execSecondAccountSync();
                }

                final Status originalStatus = new StatusJSONImplMastodon(new GetStatusByID(TweetButtonUtils.this.originalStatus.getId() + "").execSync());
                final Status status = originalStatus.isRetweet() ? originalStatus.getRetweetedStatus() : originalStatus;
                HomeDataSource.getInstance(context).updateTweet(originalStatus, AppSettings.getSharedPreferences(context).getInt(AppSettings.CURRENT_ACCOUNT, 1));
                ((Activity) context).runOnUiThread(() -> {
                    try {
                        updateTweetCounts(status);
                    } catch (Throwable t) {
                        // won't work with the simple tweet buttons (no count shown)
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String generateReplyText() {
        String text = Html.fromHtml(status.getText()).toString();
        String screenName = status.getUser().getScreenName();
        String extraNames = "";

        String screenNameToUse;

        if (secondAcc) {
            screenNameToUse = settings.secondScreenName;
        } else {
            screenNameToUse = settings.myScreenName;
        }

        if (text.contains("@")) {
            for (UserMentionEntity user : status.getUserMentionEntities()) {
                String s = user.getScreenName();
                if (!s.equals(screenNameToUse) && !extraNames.contains(s) && !s.equals(screenName)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        String replyStuff = "";
        if (!screenName.equals(screenNameToUse)) {
            replyStuff = "@" + screenName + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        if (settings.autoInsertHashtags && text.contains("#")) {
            for (HashtagEntity entity : status.getHashtagEntities()) {
                replyStuff += "#" + entity.getText() + " ";
            }
        }

        return replyStuff;
    }

    String restoreLinks(String text) {
        String imageUrl = TweetLinkUtils.getLinksInStatus(status)[1];
        if (imageUrl.contains(" ")) {
            imageUrl = imageUrl.split(" ")[0];
        }

        int urlEntitiesSize = status.getURLEntities().length;
        int length = imageUrl != null && !imageUrl.isEmpty() ? urlEntitiesSize + 1 : urlEntitiesSize;

        String[] otherLinks = new String[length];
        for (int i = 0; i < otherLinks.length; i++) {
            if (i < urlEntitiesSize) {
                otherLinks[i] = status.getURLEntities()[i].getExpandedURL();
            } else {
                otherLinks[i] = imageUrl;
            }
        }

        String webLink = null;

        ArrayList<String> webpages = new ArrayList<>();
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

        String full = text;

        String[] split = text.split("\\s");
        String[] otherLink = new String[otherLinks.length];

        for (int i = 0; i < otherLinks.length; i++) {
            otherLink[i] = "" + otherLinks[i];
        }

        for (String s : otherLink) {
            Log.v("Focus_for_Mastodon_links", ":" + s + ":");
        }

        boolean changed = false;
        int otherIndex = 0;

        if (otherLink.length > 0) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                //if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                    String f = s.replace("...", "").replace("http", "");

                    f = stripTrailingPeriods(f);

                    try {
                        if (otherIndex < otherLinks.length) {
                            if (otherLink[otherIndex].substring(otherLink[otherIndex].length() - 1, otherLink[otherIndex].length()).equals("/")) {
                                otherLink[otherIndex] = otherLink[otherIndex].substring(0, otherLink[otherIndex].length() - 1);
                            }
                            f = otherLink[otherIndex].replace("http://", "").replace("https://", "").replace("www.", "");
                            otherLink[otherIndex] = "";
                            otherIndex++;

                            changed = true;
                        }
                    } catch (Exception e) {

                    }

                    if (changed) {
                        split[i] = f;
                    } else {
                        split[i] = s;
                    }
                } else {
                    split[i] = s;
                }

            }
        }

        int replacementIndex = 0;
        if (webLink != null && !webLink.equals("")) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (Patterns.WEB_URL.matcher(s).find() && replacementIndex < otherLinks.length) {
                    String replace = otherLinks[replacementIndex];
                    replacementIndex += 1;
                    if (replace.replace(" ", "").equals("")) {
                        replace = webLink;
                    }
                    split[i] = replace;
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            full = "";
            for (String p : split) {
                full += p + " ";
            }

            full = full.substring(0, full.length() - 1);
        }

        return full.replaceAll("  ", " ");
    }

    private static String stripTrailingPeriods(String url) {
        try {
            if (url.substring(url.length() - 1, url.length()).equals(".")) {
                return stripTrailingPeriods(url.substring(0, url.length() - 1));
            } else {
                return url;
            }
        } catch (Exception e) {
            return url;
        }
    }



    public interface LikeCallback {
        void onLikeChanged(boolean newLikeState, Status originalStatus);
    }

    public interface BookmarkCallback {
        void onBookmarkChanged(boolean newBookmarkState, Status originalStatus);
    }

    public interface RetweetCallback {
        void onRetweetChanged(boolean newRetweetState, Status originalStatus);
    }
}
