package allen.town.focus.twitter.activities.drawer_activities.discover.trends;
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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.widget.Toolbar;
import androidx.legacy.app.ActionBarDrawerToggle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.timelines.GetHashtagTimeline;
import allen.town.focus.twitter.data.sq_lite.HashtagDataSource;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.MySuggestionsProvider;
import allen.town.focus.twitter.utils.SearchUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import allen.town.focus_common.util.Timber;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class SearchedTrendsActivity extends WhiteToolbarActivity {
    public static final int TWEETS_PER_REFRESH = 20;

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ActionBarDrawerToggle mDrawerToggle;

    private ListView listView;
    private LinearLayout spinner;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    private SearchUtils searchUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setSharedContentTransition(this);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);


        Utils.setUpMainTheme(context, settings);
        setContentView(R.layout.searched_trends_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        searchUtils = new SearchUtils(this);
        searchUtils.setUpSearch(false);

        mPullToRefreshLayout = (MaterialSwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mPullToRefreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int size = Utils.getActionBarHeight(context) + (landscape ? 0 : Utils.getStatusBarHeight(context));
        mPullToRefreshLayout.setProgressViewOffset(false, 0, size + toDP(25));
        mPullToRefreshLayout.setColorSchemeColors(ThemeStore.accentColor(context));

        listView = (ListView) findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        View header = new View(context);
        header.setOnClickListener(null);
        header.setOnLongClickListener(null);
        ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                Utils.getActionBarHeight(context));
        header.setLayoutParams(params);
        listView.addHeaderView(header);
        listView.setHeaderDividersEnabled(false);

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

        spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        removeKeyboard();
    }

    public void removeKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        try {
            imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
        } catch (Exception e) {
            // they closed i guess
        }
    }

    public String searchQuery = "";

    public void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY).replaceAll("\"", "");

            //去掉收尾的空格，解析可能会是 #test这样
            searchQuery = searchQuery.trim();

            //专门用来搜索hashtag
            if (!searchQuery.startsWith("#")) {
                searchQuery = "#" + searchQuery;

            }
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

            suggestions.saveRecentQuery(searchQuery.replaceAll("\"", "").replaceAll(" -RT", ""), null);

            // we want to add it to the userAutoComplete
            HashtagDataSource source = HashtagDataSource.getInstance(context);

            if (source != null) {
                source.deleteTag(searchQuery.replaceAll("\"", ""));
                source.createTag(searchQuery.replaceAll("\"", ""));
            }


            getSupportActionBar().setTitle(searchQuery);

            doSearch(searchQuery);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception e) {

        }

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return super.onOptionsItemSelected(item);

            case R.id.menu_search:
                searchUtils.showSearchView();
                return super.onOptionsItemSelected(item);

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", searchQuery.replaceAll("\"", "") + " ");
                startActivity(compose);
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        super.onActivityResult(requestCode, resultCode, returnIntent);
        recreate();
    }


    @Override
    public void onBackPressed() {
        if (searchUtils.isShowing()) {
            searchUtils.hideSearch(true);
        } else {
            super.onBackPressed();
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
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
                List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                        new GetHashtagTimeline(searchQuery, null, null, TWEETS_PER_REFRESH, null).execSync()
                );

                tweets.clear();

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

                    adapter = new TimelineArrayAdapter(context, tweets);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);
                    listView.setSelection(top);

                    spinner.setVisibility(View.GONE);

                    mPullToRefreshLayout.setRefreshing(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    mPullToRefreshLayout.setRefreshing(false);
                });
            }
        }).start();
    }

    public ArrayList<twitter4j.Status> tweets = new ArrayList<Status>();
    public TimelineArrayAdapter adapter;
    public boolean hasMore;

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        new TimeoutThread(() -> {
            try {

                Timber.v("search mentionsQuery: " + mQuery);

                tweets.clear();

                List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                        new GetHashtagTimeline(mQuery, null, null, TWEETS_PER_REFRESH, null).execSync()
                );
                tweets.addAll(statuses);

                if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                    hasMore = true;
                } else {
                    hasMore = false;
                }

                ((Activity) context).runOnUiThread(() -> {
                    adapter = new TimelineArrayAdapter(context, tweets);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);

                    spinner.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> spinner.setVisibility(View.GONE));

            }
        }).start();
    }

    public boolean canRefresh = true;

    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new TimeoutThread(() -> {
                try {
                    List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                            new GetHashtagTimeline(searchQuery, getMaxIdFromList(tweets) + "", null, TWEETS_PER_REFRESH, null).execSync()
                    );
                    tweets.addAll(statuses);

                    if (statuses.size() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
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
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPullToRefreshLayout.setRefreshing(false);
                            canRefresh = true;
                        }
                    });
                }
            }).start();
        }
    }

    public static long getMaxIdFromList(List<Status> statuses) {
        return statuses.get(statuses.size() - 1).getId() - 1;
    }
}