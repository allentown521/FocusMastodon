package allen.town.focus.twitter.activities.main_fragments.other_fragments;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.api.requests.statuses.GetBookmarkedStatuses;
import allen.town.focus.twitter.data.sq_lite.BookmarkedTweetsDataSource;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus_common.util.Timber;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class BookmarkedTweetsFragment extends MainFragment {

    public boolean newTweets = false;

    public BroadcastReceiver resetLists = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(true);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.RESET_BOOKMARKS_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(resetLists, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(resetLists, filter);
        }
    }

    public boolean manualRefresh = false;
    private int COUNT_PER_PAGE = 40;
    public int doRefresh() {
        int numberNew = 0;

        try {

            long lastId = sharedPrefs.getLong("last_bookmarked_tweet_id_" + currentAccount, 0);

            final List<Status> statuses = new ArrayList<Status>();

            boolean foundStatus = false;
            long sinceId = 0;

            if (lastId > 0) {
                sinceId = lastId;
            }

            for (int i = 0; i < DrawerActivity.settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        HeaderPaginationList<StatusJSONImplMastodon> list = StatusJSONImplMastodon.createStatusList(new GetBookmarkedStatuses(null, sinceId + "", COUNT_PER_PAGE).execSync());
                        String preIndex = list.getPreviousCursor();
                        //从header获取的id
                        if (!TextUtils.isEmpty(preIndex)) {
                            sharedPrefs.edit()
                                    .putLong("last_bookmarked_tweet_id_" + currentAccount, Long.parseLong(preIndex))
                                    .commit();
                            sinceId = Long.parseLong(preIndex);
                        }
                        statuses.addAll(list);

                        if (list.size() < COUNT_PER_PAGE) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                }
            }

            manualRefresh = false;

            BookmarkedTweetsDataSource dataSource = BookmarkedTweetsDataSource.getInstance(context);
            numberNew = dataSource.insertTweets(statuses, currentAccount);

            return numberNew;

        } catch (Exception e) {
            // Error in updating status
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onRefreshStarted() {
        new AsyncTask<Void, Void, Boolean>() {

            private int numberNew;

            @Override
            protected void onPreExecute() {
                try {
                    DrawerActivity.canSwitch = false;
                } catch (Exception e) {

                }

            }

            @Override
            protected Boolean doInBackground(Void... params) {

                numberNew = doRefresh();

                return numberNew > 0;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                try {
                    super.onPostExecute(result);

                    if (result) {
                        getCursorAdapter(false);

                        if (numberNew > 0) {
                            final CharSequence text;

                            text = numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_tweet) : numberNew + " " + getResources().getString(R.string.new_tweets);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Looper.prepare();
                                    } catch (Exception e) {
                                        // just in case
                                    }
                                    showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                                }
                            }, 500);
                        }
                    } else {
                        final CharSequence text = context.getResources().getString(R.string.no_new_tweets);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Looper.prepare();
                                } catch (Exception e) {
                                    // just in case
                                }
                                showToastBar(text + "", allRead, 400, true, toTopListener);
                            }
                        }, 500);

                        refreshLayout.setRefreshing(false);
                    }

                    DrawerActivity.canSwitch = true;

                    newTweets = false;
                } catch (Exception e) {
                    DrawerActivity.canSwitch = true;

                    try {
                        refreshLayout.setRefreshing(false);
                    } catch (Exception x) {
                        // not attached to the activity i guess, don't know how or why that would be though
                    }
                }
            }
        }.execute();
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(resetLists);
        super.onPause();
    }

    public long listId;

    public void getCursorAdapter(final boolean bSpinner) {

        if (bSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) {
            }
        }

        new TimeoutThread(() -> {
            final Cursor cursor;
            try {
                cursor = BookmarkedTweetsDataSource.getInstance(context).getCursor(currentAccount);
            } catch (Exception e) {
                BookmarkedTweetsDataSource.dataSource = null;
                return;
            }

            context.runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                Cursor c = null;
                if (cursorAdapter != null) {
                    c = cursorAdapter.getCursor();
                }

                try {
                    Timber.v( "number of tweets in bookmarked: " + cursor.getCount());
                } catch (Exception e) {
                    Timber.e("getCursorAdapter fro bookmarked",e);
                    // the cursor or database is closed, so we will null out the datasource and restart the get cursor method
                    BookmarkedTweetsDataSource.dataSource = null;
                    return;
                }

                stopCurrentVideos();
                if (cursorAdapter != null) {
                    TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, BookmarkedTweetsFragment.this);
                    cursorAdapter.setQuotedTweets(BookmarkedTweetsFragment.this.cursorAdapter.getQuotedTweets());
                    BookmarkedTweetsFragment.this.cursorAdapter = cursorAdapter;
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, BookmarkedTweetsFragment.this);
                }

                applyAdapter();

                try {
                    spinner.setVisibility(View.GONE);
                } catch (Exception e) {
                }

                try {
                    if (cursorAdapter.getCount() == 0) {
                        if (noContent != null) noContent.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        if (noContent != null) noContent.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {

                }

                if (c != null) {
                    try {
                        c.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                refreshLayout.setRefreshing(false);
            });
        }).start();
    }

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_bookmarked_tweets);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_bookmarked_tweets_summary);
    }

    public Handler handler = new Handler();
    public Runnable hideToast = new Runnable() {
        @Override
        public void run() {
            hideToastBar(mLength);
            infoBar = false;
        }
    };
    public long mLength;

    public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        if (quit) {
            infoBar = true;
        } else {
            infoBar = false;
        }

        if (!settings.useSnackbar && !overrideSnackbarSetting) {
            return;
        }

        mLength = length;

        toastDescription.setText(description);
        toastButton.setText(buttonText);
        toastButton.setOnClickListener(listener);

        if (!isToastShowing) {
            handler.removeCallbacks(hideToast);
            isToastShowing = true;
            toastBar.setVisibility(View.VISIBLE);

            Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (quit) {
                        handler.postDelayed(hideToast, 3000);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            anim.setDuration(length);
            toastBar.startAnimation(anim);
        }
    }

    public void hideToastBar(long length) {
        if (!isToastShowing || (!settings.useSnackbar && !overrideSnackbarSetting)) {
            return;
        }

        mLength = length;

        isToastShowing = false;

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toastBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    public void updateToastText(String text, String button) {
        if (isToastShowing && !(text.equals("0 " + fromTop) || text.equals("1 " + fromTop) || text.equals("2 " + fromTop))) {
            infoBar = false;
            toastDescription.setText(text);
            toastButton.setText(button);
        } else if (text.equals("0 " + fromTop) || text.equals("1 " + fromTop) || text.equals("2 " + fromTop)) {
            hideToastBar(400);
        }
    }

}