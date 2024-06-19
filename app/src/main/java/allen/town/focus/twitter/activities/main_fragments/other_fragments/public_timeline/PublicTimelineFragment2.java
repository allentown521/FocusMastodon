package allen.town.focus.twitter.activities.main_fragments.other_fragments.public_timeline;

import static allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity.getMaxIdFromList;

import android.view.View;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.api.requests.timelines.GetPublicTimeline;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public abstract class PublicTimelineFragment2 extends MainFragment {

    @Override
    public void setUpListScroll() {
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom && hasMore) {
                            getCursorAdapter(false);
                        }
                        break;
                }

                onScrollListener.onScrollStateChanged(absListView, i);
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onScrollListener.onScroll(absListView, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        });
    }

    private List<Status> statuses = new ArrayList<>();
    private boolean hasMore = true;

    @Override
    public void getCursorAdapter(boolean showSpinner) {

        if (showSpinner) {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        }

        new TimeoutThread(() -> {
            List<StatusJSONImplMastodon> timeline = getTimeline();

            if (timeline == null) {
                try {
                    getActivity().runOnUiThread(() -> {
                        listView.setVisibility(View.GONE);
                        spinner.setVisibility(View.GONE);
                    });
                } catch (Exception e) {

                }
                return;
            }

            if (timeline.size() < TWEETS_PER_REFRESH) {
                hasMore = false;
            }

            statuses.addAll(timeline);

            try {
                getActivity().runOnUiThread(() -> {
                    try {
                        if (statuses != null) {
                            listView.setAdapter(new TimelineArrayAdapter(context, statuses));
                            listView.setVisibility(View.VISIBLE);
                        }

                        spinner.setVisibility(View.GONE);

                        refreshLayout.setRefreshing(false);
                    } catch (Exception e) {
                        // not attached to activity
                    }
                });
            } catch (Exception e) {

            }


        }).start();
    }

    public static final int TWEETS_PER_REFRESH = 40;

    protected boolean isLocal() {
        return false;
    }

    protected boolean isRemote() {
        return false;
    }

    protected List<StatusJSONImplMastodon> getTimeline() {

        try {
            return StatusJSONImplMastodon.createStatusList(
                    new GetPublicTimeline(isLocal(), isRemote(), getMaxIdFromList(statuses) + "", TWEETS_PER_REFRESH, null).execSync()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
