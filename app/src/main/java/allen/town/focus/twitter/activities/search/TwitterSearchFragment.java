package allen.town.focus.twitter.activities.search;
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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.search.GetSearchResults;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Expandable;
import allen.town.focus.twitter.utils.ExpansionViewHelper;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import allen.town.focus_common.util.Timber;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class TwitterSearchFragment extends Fragment implements Expandable {

    private ListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    private boolean translucent;

    public String searchQuery;
    public boolean onlyStatus;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    public TwitterSearchFragment() {

    }


    private BroadcastReceiver newSearch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchQuery = intent.getStringExtra("query");

            doSearch(searchQuery);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.NEW_SEARCH_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(newSearch, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(newSearch, filter);
        }
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(newSearch);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public View layout;
    public View noContent;
    public TextView noContentText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        this.translucent = getArguments().getBoolean("translucent", false);
        this.searchQuery = getArguments().getString("search");
        this.onlyStatus = getArguments().getBoolean("only_status", false);

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);
        noContent = layout.findViewById(R.id.no_content);
        noContentText = (TextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getString(R.string.no_tweets));

        mPullToRefreshLayout = (MaterialSwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout);
        mPullToRefreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int size = Utils.getActionBarHeight(context) + (landscape ? 0 : Utils.getStatusBarHeight(context));
        mPullToRefreshLayout.setProgressViewOffset(false, -1 * toDP(64), toDP(25));
        mPullToRefreshLayout.setColorSchemeColors(ThemeStore.accentColor(context));

        listView = (ListView) layout.findViewById(R.id.listView);

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom) {
                            getMore();
                        }
                        break;
                }

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (getResources().getBoolean(R.bool.has_drawer) ? Utils.getNavBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        if (onlyStatus) {
            try {
                findStatus(Long.parseLong(searchQuery));
            } catch (Exception e) {

            }
        } else {
            doSearch(searchQuery);
        }

        return layout;
    }

    public void onRefreshStarted() {
        new TimeoutThread(() -> {
            final long topId;
            if (tweets.size() > 0) {
                topId = tweets.get(0).getId();
            } else {
                topId = 0;
            }

            try {

                tweets.clear();

                List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                        new GetSearchResults(searchQuery, GetSearchResults.Type.STATUSES, true, null, SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().statuses
                );

                tweets.addAll(statuses);

                if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                    hasMore = true;
                } else {
                    hasMore = false;
                }

                ((Activity) context).runOnUiThread(() -> {
                    int top = 0;
                    for (int i = 0; i < tweets.size(); i++) {
                        if (tweets.get(i).getId() == topId) {
                            top = i;
                            break;
                        }
                    }

                    adapter = new TimelineArrayAdapter(context, tweets, onlyStatus);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);
                    listView.setSelection(top);

                    spinner.setVisibility(View.GONE);

                    mPullToRefreshLayout.setRefreshing(false);

                    if (adapter.getCount() == 0) {
                        noContent.setVisibility(View.VISIBLE);
                    } else {
                        noContent.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                    mPullToRefreshLayout.setRefreshing(false);
                });
            }
        }).start();
    }

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        if (listView.getVisibility() != View.GONE) {
            listView.setVisibility(View.GONE);
        }

        new TimeoutThread(() -> {
            try {
                Timber.v("query in frag: " + mQuery);

                tweets.clear();

                List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                        new GetSearchResults(mQuery, GetSearchResults.Type.STATUSES, true, null, SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().statuses
                );
                tweets.addAll(statuses);

                if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                    hasMore = true;
                } else {
                    hasMore = false;
                }

                ((Activity) context).runOnUiThread(() -> {
                    adapter = new TimelineArrayAdapter(context, tweets, onlyStatus);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);

                    spinner.setVisibility(View.GONE);


                    if (adapter.getCount() == 0) {
                        noContent.setVisibility(View.VISIBLE);
                    } else {
                        noContent.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                });
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    public void findStatus(final long statusid) {
        listView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        hasMore = false;

        new TimeoutThread(() -> {
            try {
                Status status = new StatusJSONImplMastodon(new GetStatusByID(statusid + "").execSync());

                final ArrayList<Status> statuses = new ArrayList<Status>();

                statuses.add(status);

                ((Activity) context).runOnUiThread(() -> {
                    TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses, onlyStatus);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.GONE);

                    if (adapter.getCount() == 0) {
                        noContent.setVisibility(View.VISIBLE);
                    } else {
                        noContent.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();

                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                });
                hasMore = false;
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public boolean canRefresh = true;
    public boolean hasMore;
    public TimelineArrayAdapter adapter;
    public ArrayList<Status> tweets = new ArrayList<Status>();

    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new TimeoutThread(() -> {
                try {
                    List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                            new GetSearchResults(searchQuery, GetSearchResults.Type.STATUSES, true, SearchedTrendsActivity.getMaxIdFromList(tweets) + "", SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().statuses
                    );

                    tweets.addAll(statuses);

                    if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    ((Activity) context).runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        mPullToRefreshLayout.setRefreshing(false);
                        canRefresh = true;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() -> {
                        mPullToRefreshLayout.setRefreshing(false);
                        canRefresh = true;
                    });
                }
            }).start();
        }
    }

    private int expandedDistanceFromTop = 0;

    @Override
    public void expandViewOpen(final int distanceFromTop, int position, View root, ExpansionViewHelper helper) {
        expandedDistanceFromTop = distanceFromTop;

        listView.smoothScrollBy(distanceFromTop - Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context), TimeLineCursorAdapter.ANIMATION_DURATION);
    }

    @Override
    public void expandViewClosed(int currentDistanceFromTop) {

        if (currentDistanceFromTop != -1) {
            listView.smoothScrollBy(-1 * expandedDistanceFromTop + currentDistanceFromTop, TimeLineCursorAdapter.ANIMATION_DURATION);
        }
    }
}