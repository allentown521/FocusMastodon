package allen.town.focus.twitter.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.FollowersArrayAdapter;
import allen.town.focus.twitter.adapters.PeopleArrayAdapter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import twitter4j.User;

public abstract class ProfileUsersPopup extends PopupLayout {
    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public ArrayList<User> users = new ArrayList<User>();
    public ArrayList<Long> followingIds = new ArrayList<Long>();

    public String cursor = null;
    public boolean canRefresh = false;
    public ArrayAdapter<User> adapter;

    protected boolean hasLoaded = false;

    public ProfileUsersPopup(Context context, User user) {
        super(context);

        View main = ((Activity) context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(getTitle());

        setWidthByPercent(.7f);
        setHeightByPercent(.7f);

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

    public void findUsers() {
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        TimeoutThread data = new TimeoutThread(() -> {
            try {

                final HeaderPaginationList<User> result = getData(cursor);

                if (result == null) {
                    ((Activity) getContext()).runOnUiThread(() -> {
                        spinner.setVisibility(View.GONE);
                        canRefresh = false;
                    });

                    return;
                }

                users.clear();

                users.addAll(result);

                if (result.hasNext()) {
                    cursor = result.getNextCursor();
                    canRefresh = true;
                } else {
                    canRefresh = false;
                }

                ((Activity) getContext()).runOnUiThread(() -> {
                    if (followingIds == null) {
                        adapter = new PeopleArrayAdapter(getContext(), users);
                    } else {
                        adapter = new FollowersArrayAdapter(getContext(), users, followingIds);
                    }

                    list.setAdapter(adapter);

                    list.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.GONE);

                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) getContext()).runOnUiThread(() -> {
                    spinner.setVisibility(View.GONE);
                    canRefresh = false;
                });

            }
        });

        data.setPriority(8);
        data.start();
    }

    public void getMore() {
        canRefresh = false;

        new TimeoutThread(() -> {
            try {

                final HeaderPaginationList<User> result = getData(cursor);

                users.addAll(result);

                if (result.hasNext()) {
                    cursor = result.getNextCursor();
                    canRefresh = true;
                } else {
                    canRefresh = false;
                }

                ((Activity) getContext()).runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) getContext()).runOnUiThread(() -> canRefresh = false);
            }

        }).start();
    }

    @Override
    public void show() {
        super.show();

        new Handler().postDelayed(() -> {
            if (!hasLoaded) {
                hasLoaded = true;
                findUsers();
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);

    }

    public abstract String getTitle();

    public abstract HeaderPaginationList<User> getData(String cursor);

}
