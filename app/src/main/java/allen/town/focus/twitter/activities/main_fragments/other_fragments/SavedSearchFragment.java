package allen.town.focus.twitter.activities.main_fragments.other_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.search.GetSearchResults;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class SavedSearchFragment extends MainFragment {

    private String search;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        search = getArguments().getString("saved_search", "");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void setUpListScroll() {

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
    }

    public ArrayList<twitter4j.Status> tweets = new ArrayList<Status>();
    public TimelineArrayAdapter adapter;
    public boolean hasMore;

    @Override
    public void getCursorAdapter(boolean showSpinner) {
        if (showSpinner) {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        }

        new TimeoutThread(() -> {
            final long topId;
            if (tweets.size() > 0) {
                topId = tweets.get(0).getId();
            } else {
                topId = 0;
            }

            try {
                List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                        new GetSearchResults(search, GetSearchResults.Type.STATUSES, true, null, SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().statuses
                );

                tweets.clear();
                tweets.addAll(statuses);

                if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                    hasMore = true;
                } else {
                    hasMore = false;
                }

                try {
                    context.runOnUiThread(() -> {

                        if (!isAdded()) {
                            return;
                        }

                        int top = 0;
                        for (int i = 0; i < tweets.size(); i++) {
                            if (tweets.get(i).getId() == topId) {
                                top = i;
                                break;
                            }
                        }

                        adapter = new TimelineArrayAdapter(context, tweets);
                        listView.setAdapter(adapter);

                        if (adapter.getCount() == 0) {
                            if (noContent != null) noContent.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        } else {
                            if (noContent != null) noContent.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        }

                        listView.setSelection(top);

                        spinner.setVisibility(View.GONE);

                        refreshLayout.setRefreshing(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    context.runOnUiThread(() -> {
                        spinner.setVisibility(View.GONE);
                        refreshLayout.setRefreshing(false);
                    });
                } catch (Exception x) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_save_searches);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_save_searches_summary);
    }

    public boolean canRefresh = true;

    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            refreshLayout.setRefreshing(true);

            new TimeoutThread(() -> {
                try {
                    List<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(
                            new GetSearchResults(search, GetSearchResults.Type.STATUSES, true, null, SearchedTrendsActivity.TWEETS_PER_REFRESH).execSync().statuses
                    );

                    tweets.addAll(statuses);

                    if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    try {
                        context.runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }

                            refreshLayout.setRefreshing(false);
                            canRefresh = true;
                        });
                    } catch (Exception e) {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        context.runOnUiThread(() -> {
                            refreshLayout.setRefreshing(false);
                            canRefresh = true;
                        });
                    } catch (Exception x) {

                    }
                }
            }).start();
        }
    }
}
