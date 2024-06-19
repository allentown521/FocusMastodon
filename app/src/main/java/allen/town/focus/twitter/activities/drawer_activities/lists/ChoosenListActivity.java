package allen.town.focus.twitter.activities.drawer_activities.lists;
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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.timelines.GetListTimeline;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import code.name.monkey.appthemehelper.ThemeStore;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class ChoosenListActivity extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ListView listView;

    private long listId;
    private String listName;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setSharedContentTransition(this);

        settings = AppSettings.getInstance(this);


//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Utils.setUpMainTheme(this, settings);

        setContentView(R.layout.ptr_list_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
//        toolbar.setPadding(0, Utils.getStatusBarHeight(this), 0, 0);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(null);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            View kitkatStatusBar = findViewById(R.id.kitkat_status_bar);

            if (kitkatStatusBar != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) kitkatStatusBar.getLayoutParams();
                params.height = Utils.getStatusBarHeight(context);
                kitkatStatusBar.setLayoutParams(params);

                kitkatStatusBar.setVisibility(View.VISIBLE);
                kitkatStatusBar.setBackgroundColor(getResources().getColor(android.R.color.black));
            }
        }

        mPullToRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        spinner = findViewById(R.id.list_progress);
        listView = findViewById(R.id.listView);

        mPullToRefreshLayout.setOnRefreshListener(() -> onRefreshStarted());

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int size = Utils.getActionBarHeight(context) + (landscape ? 0 : Utils.getStatusBarHeight(context));
        mPullToRefreshLayout.setProgressViewOffset(false, 0, size + toDP(25));
        mPullToRefreshLayout.setColorSchemeColors(ThemeStore.accentColor(context));

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getNavBarHeight(context) + Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        View header = new View(context);
        header.setOnClickListener(null);
        header.setOnLongClickListener(null);
        ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
        header.setLayoutParams(params);
        listView.addHeaderView(header);
        listView.setHeaderDividersEnabled(false);

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }
        //listView.setTranslationY(Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context));

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                switch (scrollState) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom) {
                            getLists();
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        listName = getIntent().getStringExtra("list_name");

        listId = Long.parseLong(getIntent().getStringExtra("list_id"));
        actionBar.setTitle(listName);

        getLists();

        Utils.setActionBar(context);

    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    boolean justRefreshed = false;

    public void onRefreshStarted() {
        new TimeoutThread(() -> {
            try {

                justRefreshed = true;

                HeaderPaginationList<StatusJSONImplMastodon> lists = StatusJSONImplMastodon.createStatusList(
                        new GetListTimeline(listId + "", currCursor + "", null, 40, "")
                                .execSync());
                currCursor = lists.getNextCursor();

                statuses.clear();
                statuses.addAll(lists);
                stripDuplicates();

                ((Activity) context).runOnUiThread(() -> {
                    adapter = new TimelineArrayAdapter(context, statuses);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);

                    spinner.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();

            } catch (OutOfMemoryError e) {
                e.printStackTrace();

            }

            ((Activity) context).runOnUiThread(() -> mPullToRefreshLayout.setRefreshing(false));
        }).start();
    }

    private ArrayList<Status> statuses = new ArrayList<Status>();
    private TimelineArrayAdapter adapter;
    private boolean canRefresh = false;
    private String currCursor = "";

    public void getLists() {
        canRefresh = false;

        new TimeoutThread(() -> {
            try {

                HeaderPaginationList<StatusJSONImplMastodon> lists = StatusJSONImplMastodon.createStatusList(
                        new GetListTimeline(listId + "", currCursor + "", null, 40, "")
                                .execSync());
                currCursor = lists.getNextCursor();

                statuses.addAll(lists);
                stripDuplicates();

                ((Activity) context).runOnUiThread(() -> {
                    if (adapter == null) {
                        adapter = new TimelineArrayAdapter(context, statuses);
                        listView.setAdapter(adapter);
                        listView.setVisibility(View.VISIBLE);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    spinner.setVisibility(View.GONE);
                    canRefresh = true;
                });
            } catch (Exception | OutOfMemoryError e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        spinner.setVisibility(View.GONE);
                        canRefresh = false;
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.choosen_list, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_to_first:
                try {
                    listView.setSelectionFromTop(0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;

            default:
                return true;
        }
    }

    private void stripDuplicates() {
        Map<Long, Status> map = new LinkedHashMap<>();
        for (Status status : statuses) {
            map.put(status.getId(), status);
        }
        statuses.clear();
        statuses.addAll(map.values());
    }
}
