package allen.town.focus.twitter.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.ListsArrayAdapter;
import allen.town.focus.twitter.api.requests.accounts.GetAccountInLists;
import allen.town.focus.twitter.api.requests.list.GetLists;
import allen.town.focus.twitter.model.MastoList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import twitter4j.User;

public class ProfileUsersListsPopup extends PopupLayout {

    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public List<MastoList> lists = new ArrayList<>();
    public ListsArrayAdapter adapter;

    protected boolean hasLoaded = false;

    public ProfileUsersListsPopup(Context context, User user) {
        super(context);

        View main = LayoutInflater.from(context).inflate(R.layout.convo_popup_layout, null, false);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(context.getString(R.string.lists));
        showTitle(true);
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
    }

    public void findLists() {
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        TimeoutThread data = new TimeoutThread(() -> {
            try {

                boolean isMyProfile = false;
                if ((user.getId() + "").equalsIgnoreCase(AppSettings.getInstance(getContext()).myId)) {
                    isMyProfile = true;
                }

                //是自己看的是自己创建的list，否则看见的是自己所在的list
                final List<MastoList> result = isMyProfile ? new GetLists().execSync() : new GetAccountInLists(user.getId() + "").execSync();

                if (result == null) {
                    ((Activity) getContext()).runOnUiThread(() -> spinner.setVisibility(View.GONE));
                }

                lists.clear();
                lists.addAll(result);

                ((Activity) getContext()).runOnUiThread(() -> {
                    adapter = new ListsArrayAdapter(getContext(), lists);

                    list.setAdapter(adapter);
                    list.setVisibility(View.VISIBLE);

                    spinner.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) getContext()).runOnUiThread(() -> spinner.setVisibility(View.GONE));

            }
        });

        data.setPriority(8);
        data.start();
    }

    @Override
    public void show() {
        super.show();

        new Handler().postDelayed(() -> {
            if (!hasLoaded) {
                hasLoaded = true;
                findLists();
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);

    }

}
