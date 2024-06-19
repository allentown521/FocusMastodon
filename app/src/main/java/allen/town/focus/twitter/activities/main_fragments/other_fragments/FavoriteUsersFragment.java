package allen.town.focus.twitter.activities.main_fragments.other_fragments;
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

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.adapters.FavoriteUsersCursorAdapter;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersDataSource;
import allen.town.focus.twitter.utils.Utils;

public class FavoriteUsersFragment extends MainFragment {


    @Override
    public void onDestroy() {
        try {
            people.getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_fav_users);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_fav_users_summary);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {


        listView.setHeaderDividersEnabled(false);
        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        listView.setFooterDividersEnabled(false);

        spinner.setVisibility(View.GONE);

        new GetFavUsers().execute();

    }

    private FavoriteUsersCursorAdapter people;

    public void refreshFavs() {
        new GetFavUsers().execute();
    }

    class GetFavUsers extends AsyncTask<String, Void, Cursor> {

        protected Cursor doInBackground(String... urls) {
            try {
                return FavoriteUsersDataSource.getInstance(getContext()).getCursor();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Cursor cursor) {

            if (cursor == null) {
                return;
            }


            try {
                Log.v("fav_users", cursor.getCount() + "");
            } catch (Exception e) {

                FavoriteUsersDataSource.dataSource = null;
                return;
            }

            if (cursor.getCount() > 0) {
                people = new FavoriteUsersCursorAdapter(getContext(), cursor,FavoriteUsersFragment.this);
                listView.setAdapter(people);
                listView.setVisibility(View.VISIBLE);
            } else {
                try {
                    noContent.setVisibility(View.VISIBLE);
                } catch (Exception e) {

                }
                listView.setVisibility(View.GONE);
            }

            spinner.setVisibility(View.GONE);
        }
    }

    private boolean changedConfig = false;
    private boolean activityActive = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activityActive) {
        } else {
            changedConfig = true;
        }
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

    }

    @Override
    public void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changedConfig) {
        }

        activityActive = true;
        changedConfig = false;
    }


}