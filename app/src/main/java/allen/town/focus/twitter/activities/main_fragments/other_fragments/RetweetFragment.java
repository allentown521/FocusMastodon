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

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.PixelScrollDetector;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class RetweetFragment extends MainFragment {

    private boolean landscape;

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_retweets);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_retweets_summary);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        sharedPrefs = AppSettings.getSharedPreferences(context);


        listView.setHeaderDividersEnabled(false);
        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }


        listView.setOnScrollListener(new PixelScrollDetector(new MainFragmentPixelScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom && canRefresh) {
                            getRetweets();
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        }));

        getRetweets();
    }


    public boolean canRefresh = false;
    private String lastSinceNotiId = "";
    public TimelineArrayAdapter adapter;
    public ArrayList<Status> statuses = new ArrayList<Status>();
    public boolean hasMore = true;

    public void getRetweets() {
        if (!hasMore) {
            return;
        }

        canRefresh = false;

        new TimeoutThread(() -> {
            try {

                List<Notification> list = new GetNotifications("", lastSinceNotiId, 30, EnumSet.of(Notification.Type.REBLOG)).execSync();

                List<StatusJSONImplMastodon> favs = new ArrayList<>();
                if (list != null && list.size() > 0) {
                    if (list.get(0).status != null) {
                        lastSinceNotiId = list.get(0).getID() + "";
                    }
                    for (Notification noti :
                            list) {
                        if (noti.status != null) {
                            favs.add(new StatusJSONImplMastodon(noti.status));
                        }
                    }
                }
                List<StatusJSONImplMastodon> filteredList = favs.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                if (favs.size() < 17) {
                    hasMore = false;
                }


                for (Status s : filteredList) {
                    statuses.add(s);
                }

                ((Activity) context).runOnUiThread(() -> {

                    if (adapter == null) {
                        if (statuses.size() > 0) {
                            adapter = new TimelineArrayAdapter(context, statuses, TimelineArrayAdapter.RETWEET);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            try {
                                noContent.setVisibility(View.VISIBLE);
                            } catch (Exception e) {

                            }
                            listView.setVisibility(View.GONE);
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    spinner.setVisibility(View.GONE);
                    canRefresh = true;
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                    canRefresh = false;
                });
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                    canRefresh = false;
                });
            }
        }).start();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    private boolean changedConfig = false;
    private boolean activityActive = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activityActive) {
        } else {
            changedConfig = true;
        }
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

    }

    @Override
    public void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changedConfig) {
        }

        activityActive = true;
        changedConfig = false;
    }

}