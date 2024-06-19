package allen.town.focus.twitter.activities.main_fragments.other_fragments;
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.api.requests.timelines.GetListTimeline;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.data.sq_lite.ListDataSource;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.background_refresh.ListRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.Timber;
import twitter4j.ResponseList;
import twitter4j.StatusJSONImplMastodon;

public class ListFragment extends MainFragment {

    public static final int LIST_REFRESH_ID = 122;

    public boolean newTweets = false;

    public ListFragment() {
        this.listId = 0;
    }

    public BroadcastReceiver resetLists = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(true);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listId = getArguments().getLong("list_id", 0l);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.RESET_LISTS_ACTION);
        filter.addAction(IntentConstant.LIST_REFRESHED_ACTION + listId);
        context.registerReceiver(resetLists, filter);

        if (sharedPrefs.getBoolean(AppSettings.REFRESH_ME_LIST_STARTER + listId, false)) { // this will restart the loader to display the new tweets
            getCursorAdapter(true);
            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_LIST_STARTER + listId, false).commit();
        }
    }

    public boolean manualRefresh = false;

    private int COUNT_PER_PAGE = 40;

    public int doRefresh() {
        int numberNew = 0;

        try {


            long[] lastId = ListDataSource.getInstance(context).getLastIds(listId);
            long sinceId = 0;
            final List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

            boolean foundStatus = false;


            if (lastId[0] > 0) {
                sinceId = lastId[0];
            }

            for (int i = 0; i < DrawerActivity.settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        HeaderPaginationList<StatusJSONImplMastodon> list = StatusJSONImplMastodon.createStatusList(
                                new GetListTimeline(listId + "", null, null, COUNT_PER_PAGE, sinceId + "")
                                        .execSync());
                        sinceId = list.get(0).getId();

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

            ListDataSource dataSource = ListDataSource.getInstance(context);
            numberNew = dataSource.insertTweets(statuses, listId);

            ListRefreshService.scheduleRefresh(context);

            return numberNew;

        } catch (Exception e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
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

                            new Handler().postDelayed(() -> {
                                try {
                                    Looper.prepare();
                                } catch (Exception e) {
                                    // just in case
                                }
                                overrideSnackbarSetting = true;
                                showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                            }, 500);
                        }
                    } else {
                        final CharSequence text = context.getResources().getString(R.string.no_new_tweets);

                        new Handler().postDelayed(() -> {
                            try {
                                Looper.prepare();
                            } catch (Exception e) {
                                // just in case
                            }
                            showToastBar(text + "", allRead, 400, true, toTopListener);
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
        markReadForLoad();
        context.unregisterReceiver(resetLists);
        super.onPause();
    }

    public long listId;

    public void getCursorAdapter(final boolean bSpinner) {

        markReadForLoad();

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
                cursor = ListDataSource.getInstance(context).getCursor(listId);
            } catch (Exception e) {
                ListDataSource.dataSource = null;
                context.sendBroadcast(new Intent(IntentConstant.RESET_LISTS_ACTION));
                return;
            }

            context.runOnUiThread(() -> {

                if (!isAdded()) {
                    return;
                }

                Cursor c = null;
                if (cursorAdapter != null) {
                    c = cursorAdapter.getCursor();
                    Timber.v( c.getCount() + " tweets in old list");
                }

                try {
                    Timber.v( "number of tweets in list: " + cursor.getCount());
                } catch (Exception e) {
                    e.printStackTrace();
                    // the cursor or database is closed, so we will null out the datasource and restart the get cursor method
                    ListDataSource.dataSource = null;
                    context.sendBroadcast(new Intent(IntentConstant.RESET_LISTS_ACTION));
                    return;
                }

                stopCurrentVideos();
                if (cursorAdapter != null) {
                    TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, ListFragment.this);
                    cursorAdapter.setQuotedTweets(ListFragment.this.cursorAdapter.getQuotedTweets());
                    ListFragment.this.cursorAdapter = cursorAdapter;
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, ListFragment.this);
                }

                applyAdapter();

                int position = getPosition(cursor, sharedPrefs.getLong("current_list_" + listId + "_account_" + currentAccount, 0));

                if (position > 0 && !settings.topDown) {
                    int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                    try {
                        listView.setSelectionFromTop(position + listView.getHeaderViewsCount() -
                                        //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                        (settings.jumpingWorkaround ? 1 : 0),
                                size);
                    } catch (Exception e) {
                        // not attached
                    }
                    refreshLayout.setRefreshing(false);
                }

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
        return getString(R.string.no_content_lists);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_lists_summary);
    }

    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                do {
                    if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) == id) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {

        }

        return pos;
    }

    public void markReadForLoad() {

        try {
            Cursor cursor = cursorAdapter.getCursor();
            int current = listView.getFirstVisiblePosition();

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_list_" + listId + "_account_" + currentAccount, id).commit();
            } else {
                if (cursor.moveToLast()) {
                    long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_list_" + listId + "_account_" + currentAccount, id).commit();
                }
            }
        } catch (Exception e) {
            // cursor adapter is null because the loader was reset for some reason
            e.printStackTrace();
        }
    }

}