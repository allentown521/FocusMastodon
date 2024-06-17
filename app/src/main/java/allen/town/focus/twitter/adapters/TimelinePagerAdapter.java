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
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.discover.DiscoverPagerFragment;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.TrendTags;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.HomeFragment;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.extentions.FavUsersTweetsFragment;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.extentions.LinksFragment;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.extentions.PicFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.ActivityFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.BookmarkedTweetsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.DMFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.FavoriteTweetsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.FavoriteUsersFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.ListFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.ListsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.MentionsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.RetweetFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.SavedTweetsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.SecondAccMentionsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.UserTweetsFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.public_timeline.LocalTimelineFragment;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.public_timeline.PublicTimelineFragment;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.SystemBarVisibility;

public class TimelinePagerAdapter extends FragmentPagerAdapter {

    public static final int MAX_EXTRA_PAGES = 20;

    private Context context;
    private SharedPreferences sharedPrefs;
    private boolean removeHome;
    private SystemBarVisibility watcher;

    public List<Long> listIds = new ArrayList<Long>(); // 0 is the furthest to the left
    public List<Long> userIds = new ArrayList<>();
    public List<Integer> pageTypes = new ArrayList<Integer>();
    public List<String> pageNames = new ArrayList<String>();
    public List<String> searches = new ArrayList<String>();

    public List<Fragment> frags = new ArrayList<Fragment>();
    public List<String> names = new ArrayList<String>();

    public int mentionIndex = -1;

    // remove the home fragment to swipe to, since it is on the launcher
    public TimelinePagerAdapter(FragmentManager fm, Context context, SharedPreferences sharedPreferences, boolean removeHome) {
        super(fm);
        this.context = context;
        this.sharedPrefs = sharedPreferences;
        this.removeHome = removeHome;

        int currentAccount = sharedPreferences.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        for (int i = 0; i < MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String userIdentifier = "account_" + currentAccount + "_user_tweets_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            String nameIdentifier = "account_" + currentAccount + "_name_" + (i + 1);
            String searchIdentifier = "account_" + currentAccount + "_search_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type != AppSettings.PAGE_TYPE_NONE &&
                    !(removeHome && type == AppSettings.PAGE_TYPE_HOME)) {
                pageTypes.add(type);
                listIds.add(sharedPrefs.getLong(listIdentifier, 0l));
                userIds.add(sharedPrefs.getLong(userIdentifier, 0l));
                pageNames.add(sharedPrefs.getString(nameIdentifier, ""));
                searches.add(sharedPrefs.getString(searchIdentifier, ""));
            }
        }

        for (int i = 0; i < pageTypes.size(); i++) {
            switch (pageTypes.get(i)) {
                case AppSettings.PAGE_TYPE_HOME:
                    frags.add(new HomeFragment());
                    names.add(context.getResources().getString(R.string.timeline));
                    break;
                case AppSettings.PAGE_TYPE_MENTIONS:
                    frags.add(new MentionsFragment());
                    names.add(context.getResources().getString(R.string.mentions));
                    mentionIndex = i;
                    break;
                case AppSettings.PAGE_TYPE_SECOND_MENTIONS:
                    frags.add(new SecondAccMentionsFragment());
                    names.add("@" + AppSettings.getInstance(context).secondScreenName);
                    mentionIndex = i;
                    break;
                case AppSettings.PAGE_TYPE_DMS:
                    frags.add(new DMFragment());
                    names.add(context.getResources().getString(R.string.direct_messages));
                    break;
                case AppSettings.PAGE_TYPE_WORLD_TIMELINE:
                    Bundle b = new Bundle();
                    Fragment f = new PublicTimelineFragment();
                    frags.add(f);
                    b.putBoolean(AppSettings.NEED_ADD_LIST_HEADER, true);
                    names.add(context.getString(R.string.world_timeline));
                    break;
                case AppSettings.PAGE_TYPE_LOCAL_TIMELINE:
                    f = new LocalTimelineFragment();
                    frags.add(f);
                    b = new Bundle();
                    b.putBoolean(AppSettings.NEED_ADD_LIST_HEADER, true);
                    f.setArguments(b);
                    names.add(context.getString(R.string.local_timeline));
                    break;
                case AppSettings.PAGE_TYPE_HASHTAGS:
                    f = new TrendTags();
                    b = new Bundle();
                    b.putBoolean(AppSettings.NEED_ADD_LIST_HEADER, true);
                    f.setArguments(b);
                    frags.add(f);
                    names.add(context.getString(R.string.hashtags));
                    break;
                case AppSettings.PAGE_TYPE_FAVORITE_STATUS:
                    frags.add(new FavoriteTweetsFragment());
                    names.add(context.getString(R.string.favorite_tweets));
                    break;
                case AppSettings.PAGE_TYPE_ACTIVITY:
                    frags.add(new ActivityFragment());
                    names.add(context.getString(R.string.activity));
                    mentionIndex = i;
                    break;
                case AppSettings.PAGE_TYPE_LIST_TIMELINE:
                    b = new Bundle();
                    b.putLong("list_id", listIds.get(i));
                    f = new ListFragment();
                    f.setArguments(b);

                    frags.add(f);
                    names.add(pageNames.get(i));
                    break;
                case AppSettings.PAGE_TYPE_USER_TWEETS:
                    b = new Bundle();
                    b.putLong("user_id", userIds.get(i));
                    f = new UserTweetsFragment();
                    f.setArguments(b);

                    frags.add(f);
                    names.add(pageNames.get(i));
                    break;
                case AppSettings.PAGE_TYPE_BOOKMARKED_TWEETS:
                    frags.add(new BookmarkedTweetsFragment());
                    names.add(context.getString(R.string.bookmarked_tweets));
                    break;
                case AppSettings.PAGE_TYPE_LIST:
                    frags.add(new ListsFragment());
                    names.add(context.getString(R.string.lists));
                    break;
                case AppSettings.PAGE_TYPE_RETWEET:
                    frags.add(new RetweetFragment());
                    names.add(context.getString(R.string.retweets));
                    break;
                case AppSettings.PAGE_TYPE_FAV_USERS:
                    frags.add(new FavoriteUsersFragment());
                    names.add(context.getString(R.string.favorite_users));
                    break;
                case AppSettings.PAGE_TYPE_DISCOVER:
                    frags.add(new DiscoverPagerFragment());
                    names.add(context.getString(R.string.discover));
                    break;

                default:
                    frags.add(getFrag(pageTypes.get(i)));
                    names.add(getName(pageTypes.get(i)));
                    break;
            }
        }

        for (int i = 0; i < frags.size(); i++) {
            if (frags.get(i).getArguments() == null) {
                Bundle b = new Bundle();
                b.putInt("fragment_number", i);
                frags.get(i).setArguments(b);
            } else {
                Bundle b = frags.get(i).getArguments();
                b.putInt("fragment_number", i);
                frags.get(i).setArguments(b);
            }
        }
    }

    @Override
    public Fragment getItem(int i) {
        return frags.get(i);
    }

    @Override
    public CharSequence getPageTitle(int i) {
        if (names.size() > i) {
            return names.get(i);
        } else {
            return "";
        }
    }

    @Override
    public int getCount() {
        return frags.size();
    }

    public MainFragment getFrag(int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_LINKS:
                return new LinksFragment();
            case AppSettings.PAGE_TYPE_PICS:
                return new PicFragment();
            case AppSettings.PAGE_TYPE_FAV_USERS_TWEETS:
                return new FavUsersTweetsFragment();
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                return new SavedTweetsFragment();
        }

        return null;
    }

    public String getName(int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_LINKS:
                return context.getResources().getString(R.string.links);
            case AppSettings.PAGE_TYPE_PICS:
                return context.getResources().getString(R.string.pictures);
            case AppSettings.PAGE_TYPE_FAV_USERS_TWEETS:
                return context.getString(R.string.favorite_users_tweets);
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                return context.getString(R.string.saved_tweets);
        }

        return null;
    }

    public Fragment getRealFrag(int i) {
        return frags.get(i);
    }

    @Override
    public float getPageWidth(int position) {
        if (AppSettings.dualPanels(context))
            return (.5f);
        else
            return (1.0f);
    }
}
