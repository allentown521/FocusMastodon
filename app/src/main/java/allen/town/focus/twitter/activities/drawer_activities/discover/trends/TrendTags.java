package allen.town.focus.twitter.activities.drawer_activities.discover.trends;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TrendsArrayAdapter;
import allen.town.focus.twitter.api.requests.trends.GetTrendingHashtags;
import allen.town.focus.twitter.data.sq_lite.HashtagDataSource;
import allen.town.focus.twitter.model.Hashtag;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.Timber;

public class TrendTags extends MainFragment {

    protected Context context;
    protected SharedPreferences sharedPrefs;
    protected AppSettings settings;

    protected ListView listView;
    protected View layout;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(context);

        layout = inflater.inflate(R.layout.trends_list_view, null);

        listView = (ListView) layout.findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else if ((getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        if (getArguments() != null && getArguments().getBoolean(AppSettings.NEED_ADD_LIST_HEADER, false)) {
            setUpHeaders(listView);
        }

        getTrends();

        return layout;
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

    }

    public void getTrends() {

        new TimeoutThread(() -> {
            try {
                final List<Hashtag> currentTrends = new GetTrendingHashtags(20).execSync();

                ((Activity) context).runOnUiThread(() -> {
                    if (currentTrends != null) {
                        listView.setAdapter(new TrendsArrayAdapter(context, currentTrends));
                    }

                    listView.setVisibility(View.VISIBLE);

                    LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                    spinner.setVisibility(View.GONE);
                });

                HashtagDataSource source = HashtagDataSource.getInstance(context);

                String hashTagWith;
                for (Hashtag hashtag : currentTrends) {
                    //返回的hashtag没有#
                    hashTagWith = "#" + hashtag.name;
                    // we want to add it to the userAutoComplete
                    Timber.v("adding: " + hashTagWith);

                    // could be much more efficient by querying and checking first, but I
                    // just didn't feel like it when there is only ever 10 of them here
                    source.deleteTag(hashTagWith);

                    // add it to the userAutoComplete database
                    source.createTag(hashTagWith);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}
