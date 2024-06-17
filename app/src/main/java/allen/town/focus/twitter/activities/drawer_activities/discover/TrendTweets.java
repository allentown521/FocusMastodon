package allen.town.focus.twitter.activities.drawer_activities.discover;
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.trends.GetTrendingStatuses;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class TrendTweets extends MainFragment {


    public Context context;
    public AppSettings settings;

    public ListView listView;
    public View layout;

    public SharedPreferences sharedPrefs;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = AppSettings.getSharedPreferences(context);


        settings = AppSettings.getInstance(context);

        layout = inflater.inflate(R.layout.profiles_list, null);

        listView = (ListView) layout.findViewById(R.id.listView);

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

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else if ((getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        if (getArguments() != null && getArguments().getBoolean(AppSettings.NEED_ADD_LIST_HEADER, false)) {
            setUpHeaders(listView);
        }


        getTweets();

        return layout;
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

    }


    public boolean hasMore = true;
    public ArrayList<Status> statuses = new ArrayList<>();
    public TimelineArrayAdapter adapter;
    protected int COUNT_PER = 40;
    protected int page = 0;

    public void getTweets() {

        canRefresh = false;

        new TimeoutThread(() -> {
            try {

                List<StatusJSONImplMastodon> statusList = getStatusList().stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.PUBLIC)).collect(Collectors.toList());

                if (statusList.size() == COUNT_PER) {
                    hasMore = true;
                    page++;
                } else {
                    hasMore = false;
                }

                statuses.addAll(statusList);

                ((Activity) context).runOnUiThread(() -> {
                    adapter = new TimelineArrayAdapter(context, statuses);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.VISIBLE);

                    LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                    spinner.setVisibility(View.GONE);
                });
            } catch (Throwable e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> {
                    LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                    spinner.setVisibility(View.GONE);

                    LinearLayout noContent = (LinearLayout) layout.findViewById(R.id.no_content);
                    noContent.setVisibility(View.VISIBLE);

                });
            }

            canRefresh = true;
        }).start();
    }

    public boolean canRefresh = true;

    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            new TimeoutThread(() -> {
                try {
                    List<StatusJSONImplMastodon> statusList = getStatusList().stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.PUBLIC)).collect(Collectors.toList());

                    if (statusList.size() == COUNT_PER) {
                        page++;
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }
                    statuses.addAll(statusList);
                    ((Activity) context).runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        canRefresh = true;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() -> canRefresh = false);
                }
            }).start();
        }
    }

    public HeaderPaginationList<StatusJSONImplMastodon> getStatusList() throws Exception {
        return StatusJSONImplMastodon.createStatusList(new GetTrendingStatuses(page * COUNT_PER, COUNT_PER).execSync());
    }

}