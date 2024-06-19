package allen.town.focus.twitter.activities.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.PeopleArrayAdapter;
import allen.town.focus.twitter.api.requests.statuses.GetStatusReblogs;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class RetweetersFragment extends Fragment {

    private static final String ARG_TWEET_ID = "arg_tweet_id";

    public static RetweetersFragment getInstance(long tweetId) {
        RetweetersFragment fragment = new RetweetersFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_TWEET_ID, tweetId);

        fragment.setArguments(args);
        return fragment;
    }

    private long tweetId;

    private ListView listView;
    private LinearLayout spinner;
    private LinearLayout noContent;
    private FontPrefTextView noContentText;

    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        context = getActivity();
        tweetId = getArguments().getLong(ARG_TWEET_ID);

        View layout = inflater.inflate(R.layout.no_ptr_list_layout, null);

        listView = (ListView) layout.findViewById(R.id.listView);
        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        noContent = (LinearLayout) layout.findViewById(R.id.no_content);
        noContentText = (FontPrefTextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getActivity().getResources().getString(R.string.no_retweets));
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom) {
                            startSearch();
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        reset();

        startSearch();

        return layout;
    }

    /**
     * 弹窗会复用fragment，状态都需要重置
     */
    private void reset() {
        hasMore = true;
        peopleArrayAdapter = null;
        nextPage = null;
        users.clear();
    }

    public boolean hasMore;
    String nextPage = null;
    final ArrayList<User> users = new ArrayList<>();
    PeopleArrayAdapter peopleArrayAdapter;

    private void startSearch() {
        if (hasMore) {
            new TimeoutThread(() -> {
                try {

                    final HeaderPaginationList<User> userList = UserJSONImplMastodon.createPagableUserList(new GetStatusReblogs(tweetId + "", null, nextPage, 80).execSync());
                    users.addAll(userList);
                    if (userList.hasNext()) {
                        nextPage = userList.getNextCursor();
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    if (getActivity() == null) {
                        return;
                    }

                    ((Activity) context).runOnUiThread(() -> {
                        if (users.size() > 0 && getActivity() != null) {
                            if (peopleArrayAdapter == null) {
                                peopleArrayAdapter = new PeopleArrayAdapter(getActivity(), users);
                                listView.setAdapter(peopleArrayAdapter);
                            } else {
                                peopleArrayAdapter.notifyDataSetChanged();
                            }
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            noContent.setVisibility(View.VISIBLE);
                        }

                        spinner.setVisibility(View.GONE);
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                    if (getActivity() == null) {
                        return;
                    }

                    ((Activity) context).runOnUiThread(() -> {
                        noContent.setVisibility(View.VISIBLE);
                        spinner.setVisibility(View.GONE);
                    });

                }
            }).start();
        }

    }

}