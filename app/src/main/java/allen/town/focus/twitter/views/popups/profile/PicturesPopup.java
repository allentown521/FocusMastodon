package allen.town.focus.twitter.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.PicturesGridAdapter;
import allen.town.focus.twitter.api.requests.accounts.GetAccountStatuses;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.utils.TweetLinkUtils;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import allen.town.focus_common.views.AccentMaterialDialog;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;

public class PicturesPopup extends PopupLayout {

    GridView listView;
    LinearLayout spinner;

    private User user;

    public PicturesPopup(Context context, User user) {
        super(context);

        this.user = user;

        setUp();
    }

    @Override
    public View setMainLayout() {
        return null;
    }

    public ArrayList<String> pics = new ArrayList<String>();
    public ArrayList<Status> tweetsWithPics = new ArrayList<Status>();
    public String tweetsPaging = "";
    public boolean canRefresh = false;
    public PicturesGridAdapter adapter;

    private void setUp() {
        setFullScreen();
        showTitle(false);
        //setTitle(getContext().getString(R.string.pictures));


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

        View root = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.picture_popup_layout, this, false);

        listView = (GridView) root.findViewById(R.id.gridView);
        spinner = (LinearLayout) root.findViewById(R.id.spinner);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        params.width = width;
        spinner.setLayoutParams(params);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
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

        spinner.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        pics.add(user.getOriginalProfileImageURL());
        tweetsWithPics.add(null);

        if (!TextUtils.isEmpty(user.getProfileBannerURL())) {
            pics.add(user.getProfileBannerURL());
            tweetsWithPics.add(null);
        }

        doSearch();

        content.addView(root);
    }

    @Override
    public void show() {
        super.show();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPreferences.getBoolean("show_profile_pictures_helper_dialog", true)) {
            new AccentMaterialDialog(
                    getContext(),
                    R.style.MaterialAlertDialogTheme
            )
                    .setTitle(R.string.tip_title)
                    .setMessage(R.string.profile_pictures_helper_message)
                    .setPositiveButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPreferences.edit().putBoolean("show_profile_pictures_helper_dialog", false).commit();
                        }
                    })
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create().show();
        }
    }

    public void doSearch() {
        spinner.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {

                    try {
                        HeaderPaginationList<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(new GetAccountStatuses(user.getId() + "", tweetsPaging, null, null, 20, GetAccountStatuses.Filter.MEDIA).execSync());
                        tweetsPaging = statuses.getNextCursor();

                        for (Status s : statuses) {
                            String[] links = TweetLinkUtils.getLinksInStatus(s);
                            if (!links[1].equals("")) {
                                pics.add(links[1]);
                                tweetsWithPics.add(s);
                            }
                        }

                    } catch (OutOfMemoryError e) {
                        return;
                    }


                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            int numColumns;

                            int currentOrientation = getResources().getConfiguration().orientation;
                            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                                numColumns = 5;
                            } else {
                                numColumns = 3;
                            }

                            adapter = new PicturesGridAdapter(getContext(), pics, tweetsWithPics, width / numColumns);
                            listView.setNumColumns(numColumns);
                            listView.setAdapter(adapter);

                            if (tweetsWithPics.size() > 0) {
                                listView.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);
                            } else {
                                getMore();
                            }
                            canRefresh = true;

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
        }).start();
    }

    public void getMore() {
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {

                    boolean update = false;
                    if (!TextUtils.isEmpty(tweetsPaging)) {
                        HeaderPaginationList<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(new GetAccountStatuses(user.getId() + "", tweetsPaging, null, null, 20, GetAccountStatuses.Filter.MEDIA).execSync());
                        tweetsPaging = statuses.getNextCursor();

                        for (Status s : statuses) {
                            String[] links = TweetLinkUtils.getLinksInStatus(s);
                            if (!links[1].equals("")) {
                                pics.add(links[1]);
                                tweetsWithPics.add(s);
                                update = true;
                            }
                        }
                    }


                    if (update) {
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                canRefresh = true;

                                if (tweetsWithPics.size() == 0) {
                                    getMore();
                                } else {
                                    listView.setVisibility(View.VISIBLE);
                                    spinner.setVisibility(View.GONE);
                                }
                            }
                        });
                    } else {
                        canRefresh = true;
                    }

                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = false;

                            try {
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {

                            }

                            spinner.setVisibility(View.GONE);
                        }
                    });

                }

            }
        }).start();
    }

}
