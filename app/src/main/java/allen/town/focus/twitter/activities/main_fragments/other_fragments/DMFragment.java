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

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.data.sq_lite.DMDataSource;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.background_refresh.DirectMessageRefreshService;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.Timber;

public class DMFragment extends MainFragment {

    public static final int DM_REFRESH_ID = 125;

    public int unread = 0;

    public BroadcastReceiver updateDM = new BroadcastReceiver() {
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
        new AsyncTask<Void, Void, Boolean>() {

            private boolean update;
            private int numberNew;

            @Override
            protected void onPreExecute() {
                DrawerActivity.canSwitch = false;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    DMDataSource dataSource = DMDataSource.getInstance(context);
                    List<twitter4j.Status> statuses = dataSource.getNewestPageFromRemote(isSecondAccount(), currentAccount);

                    if (statuses.size() != 0) {
                        update = true;
                        numberNew = statuses.size();
                    } else {
                        update = false;
                        numberNew = 0;
                    }

                    try {
                        dataSource.markAllRead(currentAccount);
                    } catch (Throwable e) {

                    }

                    numberNew = dataSource.insertTweets(statuses, currentAccount);
                    unread = numberNew;

                } catch (Exception e) {
                    // Error in updating status
                    Log.e("DM Update Error", e.getMessage());
                }

                DirectMessageRefreshService.scheduleRefresh(context);


                return numberNew > 0;
            }

            @Override
            protected void onPostExecute(Boolean result) {

                stopCurrentVideos();
                getCursorAdapter(false);

                try {
                    if (numberNew > 0) {
                        CharSequence text = numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_direct_message) : numberNew + " " + getResources().getString(R.string.new_direct_message);
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
                        CharSequence text = getResources().getString(R.string.no_new_direct_messages);
                        showToastBar(text + "", allRead, 400, true, toTopListener);
                    }
                } catch (Exception e) {
                    // user closed the app before it was done
                }

                refreshLayout.setRefreshing(false);

                DrawerActivity.canSwitch = true;


            }
        }.execute();
    }


    public TimeLineCursorAdapter setAdapter(Cursor c) {
        return new TimeLineCursorAdapter(context, c, true, DMFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean(AppSettings.REFRESH_ME_DM, false)) {
            getCursorAdapter(false);
            //未读数量清空
            sharedPrefs.edit().putInt(AppSettings.DM_UNREAD_STARTER + currentAccount, 0).commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_DM, false).commit();
                }
            }, 1000);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.UPDATE_DM_ACTION);
        context.registerReceiver(updateDM, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        try {
            DMDataSource.getInstance(context).markAllRead(currentAccount);
        } catch (Exception e) {

        }
        super.onStop();
    }

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_dms);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_dms_summary);
    }

    public void getCursorAdapter(boolean showSpinner) {
        if (showSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) {
            }
        }

        new TimeoutThread(() -> {
            final Cursor cursor;
            try {
                cursor = DMDataSource.getInstance(context).getCursor(currentAccount);
            } catch (Exception e) {
                DMDataSource.dataSource = null;
                getCursorAdapter(true);
                return;
            }

            try {
                Timber.v("dm cursor size: " + cursor.getCount());
            } catch (Exception e) {
                DMDataSource.dataSource = null;
                getCursorAdapter(true);
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

                stopCurrentVideos();
                if (cursorAdapter != null) {
                    TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, DMFragment.this);
                    cursorAdapter.setQuotedTweets(DMFragment.this.cursorAdapter.getQuotedTweets());
                    DMFragment.this.cursorAdapter = cursorAdapter;
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, DMFragment.this);
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
            });
        }).start();
    }

    @Override
    public void onPause() {
        if (unread > 0) {
            DMDataSource.getInstance(context).markAllRead(currentAccount);
            unread = 0;
        }

        context.unregisterReceiver(updateDM);

        super.onPause();
    }


    public void attachCursor() {
        try {
            applyAdapter();
        } catch (Exception e) {

        }

        int newTweets;

        try {
            newTweets = DMDataSource.getInstance(context).getUnreadCount(currentAccount);
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