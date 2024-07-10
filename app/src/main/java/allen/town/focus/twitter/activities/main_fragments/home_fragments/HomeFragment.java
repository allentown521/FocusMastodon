package allen.town.focus.twitter.activities.main_fragments.home_fragments;
/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.api.requests.timelines.GetHomeTimeline;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.PreCacheService;
import allen.town.focus.twitter.services.background_refresh.TimelineRefreshService;
import allen.town.focus.twitter.services.background_refresh.WidgetRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.widget.WidgetProvider;
import allen.town.focus_common.util.Timber;
import allen.town.focus_common.util.TopSnackbarUtil;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

/**
 * 主页
 */
public class HomeFragment extends MainFragment {

    public static final int HOME_REFRESH_ID = 121;

    public int unread;

    public boolean initial = true;
    public boolean newTweets = false;

    @Override
    public void setHome() {
        isHome = true;
        setStrings();
    }

    public void resetTimeline(boolean spinner) {
        getCursorAdapter(spinner);
    }

    private View.OnClickListener toMentionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
            int page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);

            int extraPages = 0;
            if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                extraPages++;
            }

            if (page2Type != AppSettings.PAGE_TYPE_NONE) {
                extraPages++;
            }

            MainActivity.mViewPager.setCurrentItem(
                    ((TimelinePagerAdapter) MainActivity.mViewPager.getAdapter()).mentionIndex, true);
            hideToastBar(400);
        }
    };

    protected View.OnClickListener liveStreamRefresh = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            newTweets = false;
            viewPressed = true;
            trueLive = true;
            manualRefresh = false;
            resetTimeline(false);
            listView.setSelectionFromTop(0, 0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    infoBar = false;
                }
            }, 500);

            context.sendBroadcast(new Intent("allen.town.focus.twitter.CLEAR_PULL_UNREAD"));
        }
    };


    public int liveUnread = 0;
    public boolean loadToTop = false;

    public BroadcastReceiver pullReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!isLauncher()) {
                if (listView.getFirstVisiblePosition() == 0) {
                    // we want to automatically show the new one if the user is at the top of the list
                    // so we set the current position to the id of the top tweet

                    context.sendBroadcast(new Intent("allen.town.focus.twitter.CLEAR_PULL_UNREAD"));

                    sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, false).commit();
                    final long id = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 0l);
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                    new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            // sleep so that everyting loads correctly
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {

                            }
                            try {
                                HomeDataSource.getInstance(context).markPosition(currentAccount, id);
                            } catch (Exception e) {

                            }
                            //HomeContentProvider.updateCurrent(currentAccount, context, id);

                            trueLive = true;
                            loadToTop = true;

                            resetTimeline(false);
                        }
                    }).start();

                } else {
                    liveUnread++;
                    sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, false).commit();

                    newTweets = true;
                }
            }
        }
    };

    public BroadcastReceiver markRead = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            markReadForLoad();
        }
    };

    public BroadcastReceiver homeClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Timber.v("home closed broadcast received on home fragment");
            if (!dontGetCursor) {
                resetTimeline(true);
            }
            dontGetCursor = false;
        }
    };

    public TimeLineCursorAdapter returnAdapter(Cursor c) {
        TimeLineCursorAdapter adapter = new TimeLineCursorAdapter(context, c, false, true, this);
        if (this.cursorAdapter != null)
            adapter.setQuotedTweets(this.cursorAdapter.getQuotedTweets());

        return adapter;
    }

    public boolean isLauncher() {
        return false;
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

        TimeoutThread getCursor = new TimeoutThread(new Runnable() {
            @Override
            public void run() {

                if (!trueLive && !initial) {
                    markReadForLoad();
                }

                final Cursor cursor;
                try {
                    cursor = HomeDataSource.getInstance(context).getCursor(currentAccount);
                } catch (Exception e) {
                    Timber.v("caught getting the cursor on the home timeline, sending reset home");
                    HomeDataSource.dataSource = null;
                    context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
                    return;
                }

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (!isAdded()) {
                            return;
                        }

                        Cursor c = null;
                        if (cursorAdapter != null) {
                            c = cursorAdapter.getCursor();
                        }

                        stopCurrentVideos();
                        cursorAdapter = returnAdapter(cursor);

                        try {
                            Timber.v("size of adapter cursor on home fragment: " + cursor.getCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
                            return;
                        }

                        initial = false;

                        long id = sharedPrefs.getLong("current_position_" + currentAccount, 0l);
                        boolean update = true;
                        int numTweets;
                        if (id == 0 || loadToTop) {
                            numTweets = 0;
                            loadToTop = false;
                        } else {
                            numTweets = getPosition(cursor);

                            // if it would set it to the end, then we will get the position by the id instead
                            if (numTweets > cursor.getCount() - 5) {
                                numTweets = getPosition(cursor, id);
                                if (numTweets == -1) {
                                    return;
                                }
                            }

                            sharedPrefs.edit().putBoolean("just_muted", false).commit();
                        }

                        final int tweets = numTweets;

                        if (spinner.getVisibility() == View.VISIBLE) {
                            spinner.setVisibility(View.GONE);
                        }

                        if (cursorAdapter.getCount() == 0) {
                            if (noContent != null) noContent.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        } else {
                            if (noContent != null) noContent.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        }

                        try {
                            applyAdapter();
                        } catch (Exception e) {
                            // happens when coming from the launcher sometimes because database has been closed
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
                            return;
                        }

                        if (viewPressed && !settings.topDown) {
                            int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                            try {
                                listView.setSelectionFromTop(liveUnread + listView.getHeaderViewsCount() -
                                                //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                (settings.jumpingWorkaround ? 1 : 0),
                                        size);
                            } catch (Exception e) {
                                // not attached
                            }
                        } else if (tweets != 0) {
                            unread = tweets;
                            int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                            try {
                                if (!settings.topDown) {
                                    listView.setSelectionFromTop(tweets + listView.getHeaderViewsCount() -
                                                    //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                    (settings.jumpingWorkaround ? 1 : 0),
                                            size);
                                }
                            } catch (Exception e) {
                                // not attached
                            }
                        } else {
                            try {
                                listView.setSelectionFromTop(0, 0);
                            } catch (Exception e) {
                                // not attached
                            }
                        }

                        try {
                            c.close();
                        } catch (Exception e) {

                        }

                        liveUnread = 0;
                        viewPressed = false;

                        refreshLayout.setRefreshing(false);

                        isRefreshing = false;

                        try {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    newTweets = false;
                                }
                            }, 500);
                        } catch (Exception e) {
                            newTweets = false;
                        }
                    }
                });
            }
        });

        getCursor.setPriority(8);
        getCursor.start();


    }

    public void toTop() {
        // used so the content observer doesn't change the shared pref we just put in
        trueLive = true;
        super.toTop();
    }

    public boolean manualRefresh = false;
    public boolean dontGetCursor = false;
    public boolean rateLimited = false;

    public int insertTweets(List<Status> statuses, long[] lastId) {
        return HomeDataSource.getInstance(context).insertTweets(statuses, currentAccount, lastId);
    }

    public int doRefresh() {
        TimelineRefreshService.scheduleRefresh(context);

        int numberNew = 0;

        if (TimelineRefreshService.isRunning || WidgetRefreshService.isRunning) {
            // quit if it is running in the background
            return 0;
        }

        try {
            Cursor cursor = cursorAdapter.getCursor();
            if (cursor.moveToLast()) {
                long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
                HomeDataSource.getInstance(context).markPosition(currentAccount, id);
                //HomeContentProvider.updateCurrent(currentAccount, context, id);
            }
        } catch (Exception e) {

        }

        boolean needClose = false;

        context.sendBroadcast(new Intent("allen.town.focus.twitter.CLEAR_PULL_UNREAD"));


        final List<twitter4j.Status> statuses = new ArrayList<>();

        boolean foundStatus = false;


        long[] lastId = null;
        long sinceId;
        try {
            lastId = HomeDataSource.getInstance(context).getLastIds(currentAccount);
            sinceId = lastId[1];
        } catch (Exception e) {
            sinceId = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 1l);
        }
        Timber.v("since_id=" + sinceId);

        long beforeDownload = Calendar.getInstance().getTimeInMillis();


        AppSettings settings = new AppSettings(context);


        for (int i = 0; i < settings.maxTweetsRefresh; i++) {

            try {
                if (!foundStatus) {

                    HeaderPaginationList<StatusJSONImplMastodon> list = StatusJSONImplMastodon.createStatusList(
                            new GetHomeTimeline(null, null, 40, sinceId + "")
                                    .execSync());
                    sinceId = list.get(0).getId();

                    List filteredList = list.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.HOME)).collect(Collectors.toList());
                    statuses.addAll(filteredList);


                    if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                        Timber.v("found status");
                        foundStatus = true;
                    } else {
                        Timber.v("haven't found status");
                        foundStatus = false;
                    }
                }
            } catch (Exception e) {
                // the page doesn't exist
                e.printStackTrace();
                Timber.v("Focus_for_Mastodon_error", "error with refresh");
                foundStatus = true;
            } catch (OutOfMemoryError o) {
                // don't know why...
            }
        }

        long afterDownload = Calendar.getInstance().getTimeInMillis();
        Timber.v("downloaded " + statuses.size() + " tweets in " + (afterDownload - beforeDownload));

        if (statuses.size() > 0) {
            statuses.remove(statuses.size() - 1);
        }

        HashSet<Status> hs = new HashSet<>();
        hs.addAll(statuses);
        statuses.clear();
        statuses.addAll(hs);

        Timber.v("tweets after hashset: " + statuses.size());

        manualRefresh = false;

        if (needClose) {
            HomeDataSource.dataSource = null;
            Timber.v("sending the reset home broadcase in needclose section");
            dontGetCursor = true;
            context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
        }

        if (lastId == null) {
            try {
                lastId = HomeDataSource.getInstance(context).getLastIds(currentAccount);
            } catch (Exception e) {
                // let the
                lastId = new long[]{0, 0, 0, 0, 0};
            }
        }

        try {
            numberNew = insertTweets(statuses, lastId);
        } catch (NullPointerException e) {
            return 0;
        }

        if (numberNew > statuses.size()) {
            numberNew = statuses.size();
        }

        if (numberNew > 0 && statuses.size() > 0) {
            sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
        }

        Timber.v("inserted " + numberNew + " tweets in " + (Calendar.getInstance().getTimeInMillis() - afterDownload));

        //numberNew = statuses.size();
        unread = numberNew;

        statuses.clear();

        int unreadCount;
        try {
            unreadCount = HomeDataSource.getInstance(context).getUnreadCount(currentAccount);
        } catch (Exception e) {
            unreadCount = numberNew;
        }

        return unreadCount;
    }

    public String sNewTweet;
    public String sNewTweets;
    public String sNoNewTweets;
    public String sNewMention;
    public String sNewMentions;

    public void setStrings() {
        sNewTweet = getResources().getString(R.string.new_tweet);
        sNewTweets = getResources().getString(R.string.new_tweets);
        sNoNewTweets = getResources().getString(R.string.no_new_tweets);
        sNewMention = getResources().getString(R.string.new_mention);
        sNewMentions = getResources().getString(R.string.new_mentions);
    }

    public int numberNew;

    public boolean isRefreshing = false;

    @Override
    public void onRefreshStarted() {
        if (isRefreshing) {
            return;
        } else {
            isRefreshing = true;
        }

        DrawerActivity.canSwitch = false;

        TimeoutThread refresh = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (!onStartRefresh) {
                    numberNew = doRefresh();
                } else {
                    onStartRefresh = false;
                }


                HomeFragment.starting = false;

                final boolean result = numberNew > 0;

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setStrings();

                            if (result) {
                                Timber.v("getting cursor adapter in onrefreshstarted");
                                resetTimeline(false);

                                if (unread > 0) {
                                    final CharSequence text;

                                    numberNew = HomeDataSource.getInstance(context).getUnreadCount(currentAccount);

                                    text = numberNew == 1 ? numberNew + " " + sNewTweet : numberNew + " " + sNewTweets;

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Looper.prepare();
                                            } catch (Exception e) {
                                                // just in case
                                            }
                                            isToastShowing = false;
                                            overrideSnackbarSetting = true;
                                            showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                                        }
                                    }, 500);
                                }
                            } else if (rateLimited) {

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                                rateLimited = false;

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Looper.prepare();
                                            isToastShowing = false;
                                            overrideSnackbarSetting = true;
                                            TopSnackbarUtil.showSnack(HomeFragment.this.getActivity(), R.string.rate_limit_reached, Toast.LENGTH_LONG);
                                        } catch (Exception e) {
                                            // just in case
                                        }
                                    }
                                }, 500);

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                            } else {
                                final CharSequence text = sNoNewTweets;

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Looper.prepare();
                                        } catch (Exception e) {
                                            // just in case
                                        }
                                        isToastShowing = false;
                                        showToastBar(text + "", allRead, 400, true, toTopListener);
                                    }
                                }, 500);

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                            }

                            DrawerActivity.canSwitch = true;

                            newTweets = false;

                            if (!isLauncher()) {
                                new RefreshMentions().execute();
                            }
                        } catch (Exception e) {
                            DrawerActivity.canSwitch = true;

                            try {
                                refreshLayout.setRefreshing(false);
                            } catch (Exception x) {
                                // not attached to the activity i guess, don't know how or why that would be though
                            }
                            isRefreshing = false;
                        }
                    }
                });
            }
        });

        refresh.setPriority(7);
        refresh.start();
    }

    class RefreshMentions extends AsyncTask<Void, Void, Boolean> {

        private boolean update = false;
        private int numberNew = 0;

        @Override
        protected void onPreExecute() {
            DrawerActivity.canSwitch = false;
        }

        protected Boolean doInBackground(Void... args) {

            try {

                MentionsDataSource mentions = MentionsDataSource.getInstance(context);
                try {
                    mentions.markAllRead(settings.currentAccount);
                } catch (Throwable e) {

                }
                long[] lastNotiId = mentions.getLastIds(currentAccount);
                List<Notification> list = new GetNotifications("", lastNotiId[0] > 0 ? lastNotiId[0] + "" : "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();


                List<StatusJSONImplMastodon> statuses = new ArrayList<>();
                if (list != null && list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).status != null) {
                            statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                        }
                    }
                }
                List<StatusJSONImplMastodon> filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                if (filteredList.size() != 0) {
                    update = true;
                    numberNew = filteredList.size();
                } else {
                    update = false;
                    numberNew = 0;
                }

                for (StatusJSONImplMastodon status : filteredList) {
                    try {
                        mentions.createTweet(status, currentAccount);
                    } catch (Exception e) {
                        break;
                    }
                }

                sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_MENTIONS, true).commit();

            } catch (Exception e) {
                // Error in updating status
                Timber.e(e,"Home Update mentions Error");
            } catch (OutOfMemoryError e) {
                // why do you do this?!?!
                update = false;
            }

            return update;
        }

        protected void onPostExecute(Boolean updated) {

            try {
                if (updated) {
                    setStrings();
                    context.sendBroadcast(new Intent(IntentConstant.REFRESH_MENTIONS_ACTION));
                    sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_MENTIONS, true).commit();
                    final CharSequence text = numberNew == 1 ? numberNew + " " + sNewMention : numberNew + " " + sNewMentions;
                    isToastShowing = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showToastBar(text + "", toMentions, 400, true, toMentionsListener);
                        }
                    }, 1500);
                } else {

                }
            } catch (Exception e) {
                // might happen when switching accounts from the notification for second accounts mentions
            }

            DrawerActivity.canSwitch = true;
        }

    }

    public boolean justStarted = false;
    public Handler waitOnRefresh = new Handler();
    public Runnable applyRefresh = new Runnable() {
        @Override
        public void run() {
            sharedPrefs.edit().putBoolean(AppSettings.SHOULD_REFRESH, true).commit();
        }
    };

    @Override
    public void onPause() {

        markReadForLoad();

        context.unregisterReceiver(pullReceiver);
        context.unregisterReceiver(markRead);
        context.unregisterReceiver(homeClosed);

        super.onPause();
    }

    @Override
    public void onStop() {

        context.sendBroadcast(new Intent("allen.town.focus.twitter.CLEAR_PULL_UNREAD"));

        WidgetProvider.updateWidget(getActivity());
        //context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.NEW_TWEET_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(pullReceiver, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(pullReceiver, filter);
        }

        filter = new IntentFilter();
        filter.addAction(IntentConstant.RESET_HOME_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(homeClosed, filter , RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(homeClosed, filter);
            }

        filter = new IntentFilter();
        filter.addAction(AppSettings.BROADCAST_MARK_POSITION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(markRead, filter , RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(markRead, filter);
                }

        if (isLauncher()) {
            return;
        }

        if (sharedPrefs.getBoolean(AppSettings.REFRESH_ME, false)) { // this will restart the loader to display the new tweets
            //getLoaderManager().restartLoader(0, null, HomeFragment.this);
            Timber.v("getting cursor adapter in on resume");
            resetTimeline(true);
            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, false).commit();
        }
    }

    public boolean onStartRefresh = false;
    public static Handler refreshHandler;

    @Override
    public void onStart() {
        super.onStart();

        if (HomeFragment.refreshHandler == null) {
            HomeFragment.refreshHandler = new Handler();
        }

        if (MainActivity.caughtstarting) {
            MainActivity.caughtstarting = false;
            return;
        }

        initial = true;
        justStarted = true;

        if (sharedPrefs.getBoolean(AppSettings.REFRESH_ME, false)) { // this will restart the loader to display the new tweets
            Timber.v("getting cursor adapter in on start");
            resetTimeline(false);
            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, false).commit();
        } else if (!sharedPrefs.getBoolean(AppSettings.DONT_REFRESH, false)) { // otherwise, if there are no new ones, it should start the refresh
            HomeFragment.refreshHandler.removeCallbacksAndMessages(null);
            HomeFragment.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ((settings.refreshOnStart) &&
                            (listView.getFirstVisiblePosition() == 0) &&
                            !MainActivity.isPopup &&
                            sharedPrefs.getBoolean(AppSettings.SHOULD_REFRESH, true)) {
                        if (actionBar != null && !actionBar.isShowing() && !isLauncher()) {
                            showStatusBar();
                            actionBar.show();
                        }

                        refreshOnStart();
                    }

                    waitOnRefresh.removeCallbacks(applyRefresh);
                    waitOnRefresh.postDelayed(applyRefresh, 30000);
                    sharedPrefs.edit().putBoolean(AppSettings.DONT_REFRESH, false).commit();

                }
            }, 600);
        }


        context.sendBroadcast(new Intent("allen.town.focus.twitter.CLEAR_PULL_UNREAD"));
    }

    public static boolean starting = false;

    private void refreshOnStart() {
        if (HomeFragment.starting) {
            return;
        } else {
            new Handler().postDelayed(() -> HomeFragment.starting = false, 10000);
        }

        refreshLayout.setRefreshing(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.TIMELINE_REFRESHE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(new InnerBroadcastReceiver(), filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(new InnerBroadcastReceiver(), filter);
        }

        new Thread(() -> {
            TimelineRefreshService.refresh(context, true);
            PreCacheService.cache(context);
        }).start();
    }

    class InnerBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.v("here");
            numberNew = intent.getIntExtra(AppSettings.NUMBER_NEW, 0);
            unread = numberNew;
            onStartRefresh = true;
            onRefreshStarted();
            try {
                context.unregisterReceiver(this);
            } catch (Exception e) {
                // not registered
            }
        }
    };

    public boolean trueLive = false;
    public boolean viewPressed = false;

    // use the cursor to find which one has "1" in current position column
    public int getPosition(Cursor cursor) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                String s;
                do {
                    s = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CURRENT_POS));
                    if (s != null && !s.isEmpty()) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Timber.v("caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
            return -1;
        }

        return pos;
    }

    // find the id from the cursor to get the position
    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                do {
                    if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) < id) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Timber.v("caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
            return -1;
        }

        return pos;
    }

    public Handler handler = new Handler();

    public void markReadForLoad() {
        try {
            final Cursor cursor = cursorAdapter.getCursor();
            final int current = listView.getFirstVisiblePosition();

            if (cursor.isClosed()) {
                return;
            }

            HomeDataSource.getInstance(context).markAllRead(currentAccount);

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                Timber.v("Focus_for_Mastodon_marking_read", cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) + "");
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                new TimeoutThread(new Runnable() {
                    @Override
                    public void run() {
                        markRead(currentAccount, context, id);
                    }
                }).start();
            } else {
                if (cursor.moveToLast()) {
                    final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                    new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            markRead(currentAccount, context, id);
                        }
                    }).start();
                }
            }
        } catch (IllegalStateException e) {
            // Home datasource is not open, so we manually close it to null out values and reset it
            e.printStackTrace();
            try {
                HomeDataSource.dataSource = null;
            } catch (Exception x) {

            }
        } catch (NullPointerException | StaleDataException | SQLiteDiskIOException e) {
            e.printStackTrace();
            // the cursoradapter is null
        }
    }

    public void markRead(int currentAccount, Context context, long id) {
        try {
            HomeDataSource.getInstance(context).markPosition(currentAccount, id);
        } catch (Throwable t) {

        }
    }

}