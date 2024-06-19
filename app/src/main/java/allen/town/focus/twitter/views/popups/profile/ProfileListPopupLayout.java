package allen.town.focus.twitter.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimelineArrayAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;

public abstract class ProfileListPopupLayout extends PopupLayout {

    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public ArrayList<Status> tweets = new ArrayList<>();
    public String nextPage = "";
    public boolean canRefresh = false;
    public TimelineArrayAdapter adapter;

    protected boolean hasLoaded = false;

    public ProfileListPopupLayout(Context context, View main, User user) {
        super(context);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        if (AppSettings.getInstance(context).revampedTweets()) {
            list.setDivider(null);
        }

        //setTitle(getTitle());
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

        this.user = user;

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
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        TimeoutThread data = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {

                    final List<StatusJSONImplMastodon> result = getData();

                    if (result == null || result.size() == 0) {
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                                canRefresh = false;
                            }
                        });
                    }

                    tweets.clear();

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(getContext(), tweets);
                            adapter.setCanUseQuickActions(false);

                            list.setAdapter(adapter);
                            list.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);

                            if (!(ProfileListPopupLayout.this instanceof ProfileMentionsPopup)) {
                                if (result.size() > 17) {
                                    canRefresh = true;
                                } else {
                                    canRefresh = false;
                                }
                            } else {
                                canRefresh = true;
                            }

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            canRefresh = false;
                        }
                    });

                }
            }
        });

        data.setPriority(8);
        data.start();
    }

    public void getMore() {
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {

                    final boolean more = incrementQuery();

                    if (!more) {
                        canRefresh = false;
                        return;
                    }

                    final List<StatusJSONImplMastodon> result = getData();

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();

                            if (!(ProfileListPopupLayout.this instanceof ProfileMentionsPopup)) {
                                if (result.size() > 17) {
                                    canRefresh = true;
                                } else {
                                    canRefresh = false;
                                }
                            } else {
                                canRefresh = true;
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = false;
                        }
                    });
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
                if (!hasLoaded) {
                    hasLoaded = true;
                    findTweets();
                }
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);

    }

    public abstract boolean incrementQuery();

    public abstract String getTitle();

    public abstract List<StatusJSONImplMastodon> getData();

}
