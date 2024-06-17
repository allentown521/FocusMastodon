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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.receivers.IntentConstant;

public abstract class HomeExtensionFragment extends MainFragment {

    public BroadcastReceiver homeClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.v("Focus_for_Mastodon_home_ext", "received the reset home broadcast on a home extension timeline");
            getCursorAdapter(true);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.RESET_HOME_ACTION);
        context.registerReceiver(homeClosed, filter);
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(homeClosed);

        super.onPause();
    }

    public abstract Cursor getCursor();

    @Override
    public void getCursorAdapter(final boolean bSpinner) {
        if (bSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (!bSpinner) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }
                }

                final Cursor cursor;
                try {
                    cursor = getCursor();
                }catch (Exception e) {
                    HomeDataSource.dataSource = null;
                    //getCursorAdapter(true);
                    Log.v("Focus_for_Mastodon_home_ext", "sending the reset home, caught in getcursoradapter");
                    context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
                    return;
                }

                try {
                    Log.v("Focus_for_Mastodon_database", "home extension fragment count: " + cursor.getCount());
                } catch (Exception e) {
                    HomeDataSource.dataSource = null;
                    //getCursorAdapter(true);
                    Log.v("Focus_for_Mastodon_home_ext", "sending the reset home, caught getting cursoradapter at the second spot");
                    context.sendBroadcast(new Intent(IntentConstant.RESET_HOME_ACTION));
                    return;
                }

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor c = null;
                        try {
                            c = cursorAdapter.getCursor();
                        } catch (Exception e) {

                        }

                        stopCurrentVideos();
                        if (cursorAdapter != null) {
                            TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, HomeExtensionFragment.this);
                            cursorAdapter.setQuotedTweets(HomeExtensionFragment.this.cursorAdapter.getQuotedTweets());
                            HomeExtensionFragment.this.cursorAdapter = cursorAdapter;
                        } else {
                            cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, HomeExtensionFragment.this);
                        }

                        if (bSpinner) {
                            try {
                                spinner.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            } catch (Exception e) { }
                        }

                        if(cursor.getCount() == 0){
                            noContent.setVisibility(View.VISIBLE);
                        } else {
                            noContent.setVisibility(View.GONE);
                        }
                        attachCursor();
                        refreshLayout.setRefreshing(false);

                        try {
                            c.close();
                        } catch (Exception e) {

                        }
                    }
                });
            }
        }).start();
    }

    public void attachCursor() {
        try {
            applyAdapter();
        } catch (Exception e) {

        }
    }
}