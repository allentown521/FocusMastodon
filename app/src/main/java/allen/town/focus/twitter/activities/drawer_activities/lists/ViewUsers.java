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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.adapters.UserListMembersArrayAdapter;
import allen.town.focus.twitter.api.requests.list.GetListAccounts;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.PagableResponseList;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class ViewUsers extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ListView listView;
    private LinearLayout spinner;

    private boolean canRefresh = true;

    private long listId;
    private String listName;

    private String currCursor = "";

    private boolean bigEnough = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(this);


//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        listName = getIntent().getStringExtra("list_name");

        setContentView(R.layout.list_view_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle(listName);


        spinner = (LinearLayout) findViewById(R.id.list_progress);

        listView = (ListView) findViewById(R.id.listView);

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

//        listView.setTranslationY(Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context));

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        boolean toBottom = absListView.getLastVisiblePosition() == absListView.getCount() - 1;
                        if (toBottom) {
                            // Last item is fully visible.
                            if (canRefresh && bigEnough) {
                                new GetUsers().execute();
                            }

                            canRefresh = false;

                            new Handler().postDelayed(() -> canRefresh = true, 4000);
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        listId = getIntent().getLongExtra("list_id", 0);

        new GetUsers().execute();

        Utils.setActionBar(context);
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    ArrayList<User> array;
    UserListMembersArrayAdapter people;

    class GetUsers extends AsyncTask<String, Void, ArrayList<User>> {

        protected ArrayList<User> doInBackground(String... urls) {

            if (array == null) {
                array = new ArrayList<User>();
            }

            try {

                //默认为 40 个帐户。最多 80 个帐户。设置为 0 以获取所有不分页的帐户
                HeaderPaginationList list = new GetListAccounts(listId + "", currCursor + "", "", "", 20).execSync();

                HeaderPaginationList<User> users = UserJSONImplMastodon.createPagableUserList(list);

                currCursor = users.getNextCursor();

                for (User user : users) {
                    array.add(user);
                }

                bigEnough = users.size() > 16;

                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<User> users) {
            if (users != null) {
                if (people == null) {
                    people = new UserListMembersArrayAdapter(context, users, listId);
                    listView.setAdapter(people);
                } else {
                    people.notifyDataSetChanged();
                }
            }

            spinner.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

}