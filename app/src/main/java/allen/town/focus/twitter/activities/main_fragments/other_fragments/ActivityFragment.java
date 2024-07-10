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
import android.util.Log;
import android.view.View;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.ActivityCursorAdapter;
import allen.town.focus.twitter.data.sq_lite.ActivityDataSource;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.background_refresh.ActivityRefreshService;
import allen.town.focus.twitter.services.background_refresh.SecondActivityRefreshService;
import allen.town.focus.twitter.utils.ActivityUtils;

public class ActivityFragment extends MainFragment {

    public static final int ACTIVITY_REFRESH_ID = 131;

    public int unread = 0;

    public BroadcastReceiver refreshActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(false);
        }
    };

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_activity_yet);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_activity_yet_desc);
    }

    @Override
    public void setUpListScroll() {

    }


    @Override
    public void onRefreshStarted() {
        new AsyncTask<Void, Void, Cursor>() {

            private boolean update = false;
            @Override
            protected void onPreExecute() {
                DrawerActivity.canSwitch = false;
            }

            @Override
            protected Cursor doInBackground(Void... params) {

                try {
                    ActivityUtils utils = new ActivityUtils(getActivity());

                    update = utils.refreshActivity();

                    ActivityRefreshService.scheduleRefresh(context);
                    if (settings.syncSecondMentions) {
                        SecondActivityRefreshService.startNow(context);
                    }
                } catch (Exception e) {

                }
                
                return ActivityDataSource.getInstance(context).getCursor(currentAccount);
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

                try {
                    applyAdapter();
                } catch (Exception e) {

                }

                if (cursor.getCount() == 0) {
                    noContent.setVisibility(View.VISIBLE);
                } else {
                    noContent.setVisibility(View.GONE);
                }

                try {
                    if (update) {
                        showToastBar(getString(R.string.new_activity), getString(R.string.ok), 400, true, toTopListener);
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

    public ActivityCursorAdapter setAdapter(Cursor c) {
        return new ActivityCursorAdapter(context, c);
    }

    @Override
    public void onResume() {
        super.onResume();
        //隐藏加载进度
        spinner.setVisibility(View.GONE);
        if (sharedPrefs.getBoolean("refresh_me_activity", false)) {
            getCursorAdapter(false);

            new Handler().postDelayed(() -> sharedPrefs.edit().putBoolean("refresh_me_activity", false).commit(),1000);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.REFRESH_ACTIVITY_ACTION);
        filter.addAction(IntentConstant.NEW_ACTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(refreshActivity, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(refreshActivity, filter);
        }
    }

    public void getCursorAdapter(boolean shownoContent) {
        if (shownoContent) {
            try {
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        new TimeoutThread(() -> {
            final Cursor cursor;
            try {
                cursor = ActivityDataSource.getInstance(context).getCursor(currentAccount);
            } catch (Exception e) {
                ActivityDataSource.dataSource = null;
                getCursorAdapter(true);
                return;
            }

            try {
                Log.v("Focus_for_Mastodon_databases", "mentions cursor size: " + cursor.getCount());
            } catch (Exception e) {
                ActivityDataSource.dataSource = null;
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
                cursorAdapter = new ActivityCursorAdapter(context, cursor);

                try {
                    listView.setVisibility(View.VISIBLE);
                } catch (Exception e) { }

                try {
                    listView.setAdapter(cursorAdapter);
                } catch (Exception e) {

                }

                if (cursor.getCount() == 0) {
                    noContent.setVisibility(View.VISIBLE);
                } else {
                    noContent.setVisibility(View.GONE);
                }

                try {
                    c.close();
                } catch (Exception e) {

                }
            });
        }).start();
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(refreshActivity);
        super.onPause();
    }
}
