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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
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
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.adapters.PeopleArrayAdapter;
import allen.town.focus.twitter.api.requests.search.GetSearchResults;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;


public class UserSearchFragment extends Fragment {

    private ListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    private boolean translucent;

    public String searchQuery;
    public boolean onlyProfile;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    public UserSearchFragment() {
        this.translucent = false;
        this.searchQuery = "";
        this.onlyProfile = false;
    }

    private BroadcastReceiver newSearch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchQuery = intent.getStringExtra("query");

            doUserSearch(searchQuery);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentConstant.NEW_SEARCH_ACTION);
        context.registerReceiver(newSearch, filter);
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

    public View noContent;
    public TextView noContentText;
    public View layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.translucent = getArguments().getBoolean("translucent", false);
        this.searchQuery = getArguments().getString("search").replaceAll("@", "");
        searchQuery = searchQuery.replace(" TOP", "");
        searchQuery = searchQuery.replace(" -RT", "");
        this.onlyProfile = getArguments().getBoolean("only_profile", false);

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);
        noContent = layout.findViewById(R.id.no_content);
        noContentText = (TextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getString(R.string.no_users));

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

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom && canRefresh) {
                            getMoreUsers(searchQuery.replace("@", ""));
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

        spinner = layout.findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        doUserSearch(searchQuery);

        if (onlyProfile) {
            ProfilePager.start(context, searchQuery.replace("@", "").replaceAll(" ", ""));
            getActivity().finish();
        }

        return layout;
    }

    public void onRefreshStarted() {
        mPullToRefreshLayout.setRefreshing(false);
    }

    public ArrayList<User> users;
    public PeopleArrayAdapter peopleAdapter;

    public void doUserSearch(final String mQuery) {
        listView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        hasMore = true;
        canRefresh = false;

        new TimeoutThread(() -> {
            try {

                List<User> result = UserJSONImplMastodon.createPagableUserList(
                        new GetSearchResults(mQuery, GetSearchResults.Type.ACCOUNTS, true, null, SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().accounts
                );

                users = new ArrayList<User>();

                if (result.size() < 18) {
                    hasMore = false;
                    canRefresh = false;
                }


                users.addAll(result);

                ((Activity) context).runOnUiThread(() -> {
                    peopleAdapter = new PeopleArrayAdapter(context, users, false);
                    listView.setAdapter(peopleAdapter);
                    listView.setVisibility(View.VISIBLE);

                    spinner.setVisibility(View.GONE);

                    if (peopleAdapter.getCount() == 0) {
                        noContent.setVisibility(View.VISIBLE);
                    } else {
                        noContent.setVisibility(View.GONE);
                    }

                    canRefresh = true;
                });
            } catch (Exception e) {
                e.printStackTrace();

                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    noContent.setVisibility(View.VISIBLE);
                });
                hasMore = false;

                canRefresh = true;
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public boolean canRefresh = true;
    public boolean hasMore;

    public void getMoreUsers(final String mQuery) {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new TimeoutThread(() -> {
                try {
                    List<User> result = UserJSONImplMastodon.createPagableUserList(
                            new GetSearchResults(mQuery, GetSearchResults.Type.ACCOUNTS, true, getMaxIdFromList(users) + "", SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().accounts
                    );

                    users.addAll(result);

                    ((Activity) context).runOnUiThread(() -> {
                        if (peopleAdapter != null) {
                            peopleAdapter.notifyDataSetChanged();
                        }
                        mPullToRefreshLayout.setRefreshing(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                    ((Activity) context).runOnUiThread(() -> mPullToRefreshLayout.setRefreshing(false));
                    hasMore = false;
                }
            }).start();
        }
    }

    public static long getMaxIdFromList(List<User> statuses) {
        return statuses.get(statuses.size() - 1).getId() - 1;
    }
}