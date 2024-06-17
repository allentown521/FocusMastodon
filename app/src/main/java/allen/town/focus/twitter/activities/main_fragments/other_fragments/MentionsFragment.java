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
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.background_refresh.MentionsRefreshService;
import allen.town.focus.twitter.services.background_refresh.SecondMentionsRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class MentionsFragment extends MainFragment {

    public static final int MENTIONS_REFRESH_ID = 127;

    public int unread = 0;

    public BroadcastReceiver refreshMentions = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(false);
        }
    };

    public boolean isSecondAccount() {
        return false;
    }

    @Override
    public void onRefreshStarted() {
        new AsyncTask<Void, Void, Cursor>() {

            private boolean update;
            private int numberNew;

            @Override
            protected void onPreExecute() {
                DrawerActivity.canSwitch = false;
            }

            @Override
            protected Cursor doInBackground(Void... params) {
                try {

                    long[] lastNotiId = MentionsDataSource.getInstance(context).getLastIds(currentAccount);


                    List<Notification> list;
                    if (isSecondAccount()) {
                        list = new GetNotifications("", lastNotiId[0] > 0 ? lastNotiId[0] + "" : "", 30, EnumSet.of(Notification.Type.MENTION)).execSecondAccountSync();
                    } else {
                        list = new GetNotifications("", lastNotiId[0] > 0 ? lastNotiId[0] + "" : "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();
                    }

                    List<StatusJSONImplMastodon> statuses = new ArrayList<>();
                    if (list != null && list.size() > 0) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).status != null) {
                                statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                            }
                        }
                    }
                    List<twitter4j.Status> filteredList = statuses.stream().filter(new StatusFilterPredicate(isSecondAccount() ? AppSettings.getInstance(context).secondSessionId : AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                    if (filteredList.size() != 0) {
                        update = true;
                        numberNew = filteredList.size();
                    } else {
                        update = false;
                        numberNew = 0;
                    }

                    MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

                    try {
                        dataSource.markAllRead(currentAccount);
                    } catch (Throwable e) {

                    }

                    numberNew = dataSource.insertTweets(filteredList, currentAccount);
                    unread = numberNew;

                } catch (Exception e) {
                    // Error in updating status
                    Log.d("Mentions Update Error", e.getMessage());
                }

                MentionsRefreshService.scheduleRefresh(context);

                if (DrawerActivity.settings.syncSecondMentions) {
                    syncSecondMentions();
                }

                return MentionsDataSource.getInstance(context).getCursor(currentAccount);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {

                Cursor c = null;
                try {
                    c = cursorAdapter.getCursor();
                } catch (Exception e) {

                }

                stopCurrentVideos();
                cursorAdapter = setAdapter(cursor);
                attachCursor();

                try {
                    if (update) {
                        CharSequence text = numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_mention) : numberNew + " " + getResources().getString(R.string.new_mentions);
                        overrideSnackbarSetting = true;
                        showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                        int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                        try {
                            if (!settings.topDown) {
                                listView.setSelectionFromTop(numberNew + listView.getHeaderViewsCount() -
                                                //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                (settings.jumpingWorkaround ? 1 : 0),
                                        size);
                            }
                        } catch (Exception e) {
                            // not attached
                        }
                    } else {
                        CharSequence text = getResources().getString(R.string.no_new_mentions);
                        showToastBar(text + "", allRead, 400, true, toTopListener);
                    }
                } catch (Exception e) {
                    // user closed the app before it was done
                }

                refreshLayout.setRefreshing(false);

                DrawerActivity.canSwitch = true;

                try {
                    c.close();
                } catch (Exception e) {

                }
            }
        }.execute();
    }

    public void syncSecondMentions() {
        // refresh the second account
        SecondMentionsRefreshService.startNow(context);
    }

    public TimeLineCursorAdapter setAdapter(Cursor c) {
        return new TimeLineCursorAdapter(context, c, false, MentionsFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean(AppSettings.REFRESH_ME_MENTIONS, false)) {
            getCursorAdapter(false);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_MENTIONS, false).commit();
                }
            }, 1000);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.REFRESH_MENTIONS_ACTION);
        filter.addAction(IntentConstant.NEW_MENTION_ACTION);
        context.registerReceiver(refreshMentions, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        try {
            MentionsDataSource.getInstance(context).markAllRead(currentAccount);
        } catch (Exception e) {

        }
        super.onStop();
    }

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_mentions);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_mentions_summary);
    }

    public void getCursorAdapter(boolean showSpinner) {
        if (showSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) {
            }
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor;
                try {
                    cursor = MentionsDataSource.getInstance(context).getCursor(currentAccount);
                } catch (Exception e) {
                    MentionsDataSource.dataSource = null;
                    getCursorAdapter(true);
                    return;
                }

                try {
                    Log.v("Focus_for_Mastodon_databases", "mentions cursor size: " + cursor.getCount());
                } catch (Exception e) {
                    MentionsDataSource.dataSource = null;
                    getCursorAdapter(true);
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
                        if (cursorAdapter != null) {
                            TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, MentionsFragment.this);
                            cursorAdapter.setQuotedTweets(MentionsFragment.this.cursorAdapter.getQuotedTweets());
                            MentionsFragment.this.cursorAdapter = cursorAdapter;
                        } else {
                            cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, MentionsFragment.this);
                        }

                        try {
                            spinner.setVisibility(View.GONE);

                            if (cursorAdapter.getCount() == 0) {
                                if (noContent != null) noContent.setVisibility(View.VISIBLE);
                                listView.setVisibility(View.GONE);
                            } else {
                                if (noContent != null) noContent.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                        }

                        attachCursor();

                        if (c != null) {
                            try {
                                c.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onPause() {
        if (unread > 0) {
            MentionsDataSource.getInstance(context).markAllRead(currentAccount);
            unread = 0;
        }

        context.unregisterReceiver(refreshMentions);

        super.onPause();
    }


    public void attachCursor() {
        try {
            applyAdapter();
        } catch (Exception e) {

        }

        int newTweets;

        try {
            newTweets = MentionsDataSource.getInstance(context).getUnreadCount(currentAccount);
        } catch (Exception e) {
            newTweets = 0;
        }

        if (newTweets > 0) {
            unread = newTweets;
            int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
            try {
                if (!settings.topDown) {
                    listView.setSelectionFromTop(newTweets + listView.getHeaderViewsCount() -
                                    //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                    (settings.jumpingWorkaround ? 1 : 0),
                            size);
                }
            } catch (Exception e) {
                // not attached
            }
        }
    }

}