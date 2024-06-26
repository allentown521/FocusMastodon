package allen.town.focus.twitter.views.popups.profile;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.views.widgets.PopupLayout;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;
import twitter4j.User;

public class ProfileTimelinePopupLayout extends PopupLayout {

    protected ProfilePager profilePager;

    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public boolean canRefresh = false;
    public List<Status> tweets = new ArrayList<>();
    public TimelineArrayAdapter adapter;

    public ProfileTimelinePopupLayout(ProfilePager context, View main, User user) {
        super(context);

        this.profilePager = context;

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        if (AppSettings.getInstance(context).revampedTweets()) {
            list.setDivider(null);
        }

        showTitle(false);
        setFullScreen();

        if (getResources().getBoolean(R.bool.isTablet)) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setWidthByPercent(.6f);
                setHeightByPercent(.8f);
            } else {
                setWidthByPercent(.85f);
                setHeightByPercent(.68f);
            }
            setCenterInScreen();
        }

        content.addView(main);
        setUpList();
    }

    @Override
    public View setMainLayout() {
        return null;
    }

    public void setUpList() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        params.width = width;
        spinner.setLayoutParams(params);

        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom && canRefresh) {
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

    public void findTweets() {
        canRefresh = true;

        tweets = profilePager.filterTweets();
        adapter = new TimelineArrayAdapter(getContext(), tweets);
        adapter.setCanUseQuickActions(false);

        list.setAdapter(adapter);
        list.setVisibility(View.VISIBLE);

        spinner.setVisibility(View.GONE);
    }

    public void getMore() {
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean gotMoreTweets = profilePager.fetchTweets();
                    boolean gotMoreMentions = profilePager.fetchMentions();
                    boolean gotMoreLikes = profilePager.fetchFavorites();

                    canRefresh = gotMoreLikes || gotMoreMentions || gotMoreTweets;

                    if (canRefresh) {
                        tweets.clear();
                        tweets.addAll(profilePager.filterTweets());

                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    canRefresh = false;
                }

            }
        }).start();
    }

    @Override
    public void show() {
        super.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findTweets();
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME );

    }
}