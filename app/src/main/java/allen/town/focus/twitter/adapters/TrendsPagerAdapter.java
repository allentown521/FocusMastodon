package allen.town.focus.twitter.adapters;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.discover.TrendTweets;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.TrendLinks;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.TrendTags;

public class TrendsPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public TrendsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                TrendTags world = new TrendTags();
                return world;
            case 1:
                TrendTweets nearby = new TrendTweets();
                return nearby;
            case 2:
                TrendLinks local = new TrendLinks();
                return local;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.hashtags);
            case 1:
                return context.getResources().getString(R.string.posts);
            case 2:
                return context.getResources().getString(R.string.news);
            case 3:
                return context.getResources().getString(R.string.discover_people);
        }
        return null;
    }
}
