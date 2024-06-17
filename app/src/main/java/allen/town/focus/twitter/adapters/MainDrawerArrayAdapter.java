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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.listeners.MainDrawerClickListener;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.views.widgets.NotificationDrawerLayout;
import code.name.monkey.appthemehelper.ThemeStore;

public class MainDrawerArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> text = new ArrayList<String>();
    public SharedPreferences sharedPrefs;
    public static int current = 0;
    public int textSize;

    public List<Long> listIds = new ArrayList<Long>(); // 0 is the furthest to the left
    public List<Long> userIds = new ArrayList<Long>(); // 0 is the furthest to the left
    public List<Integer> pageTypes = new ArrayList<Integer>();
    public List<String> pageNames = new ArrayList<String>();
    public List<String> searchPages = new ArrayList<String>();
    public List<String> searchNames = new ArrayList<String>();

    public Set<String> shownItems;

    static class ViewHolder {
        public TextView name;
        public ImageView icon;
        public MaterialCardView cardView;
    }

    private NotificationDrawerLayout drawer;
    private ViewPager viewPager;
    private MainDrawerClickListener mainDrawerClickListener;
    protected BottomNavigationView navigationView;

    public MainDrawerArrayAdapter(Context context, NotificationDrawerLayout drawer, ViewPager viewPager, BottomNavigationView navigationView) {
        super(context, 0);
        this.context = (Activity) context;
        this.sharedPrefs = AppSettings.getSharedPreferences(context);
        this.drawer = drawer;
        this.viewPager = viewPager;
        this.navigationView = navigationView;
        textSize = 15;
        mainDrawerClickListener = new MainDrawerClickListener(context, drawer, viewPager);

        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String userIdentifier = "account_" + currentAccount + "_user_tweets_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            String nameIdentifier = "account_" + currentAccount + "_name_" + (i + 1);
            String searchIdentifier = "account_" + currentAccount + "_search_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type != AppSettings.PAGE_TYPE_NONE) {
                pageTypes.add(type);
                listIds.add(sharedPrefs.getLong(listIdentifier, 0l));
                userIds.add(sharedPrefs.getLong(userIdentifier, 0l));
                pageNames.add(sharedPrefs.getString(nameIdentifier, ""));
                searchNames.add(sharedPrefs.getString(searchIdentifier, ""));
            }
        }

        for (int i = 0; i < pageTypes.size(); i++) {
            switch (pageTypes.get(i)) {
                case AppSettings.PAGE_TYPE_SECOND_MENTIONS:
                    text.add(AppSettings.getInstance(context).secondScreenName);
                    break;
                case AppSettings.PAGE_TYPE_LIST_TIMELINE:
                case AppSettings.PAGE_TYPE_USER_TWEETS:
                    text.add(pageNames.get(i));
                    break;
                default:
                    text.add(getName(pageTypes.get(i)));
                    break;
            }
        }


        shownItems = sharedPrefs.getStringSet(AppSettings.DRAWER_SHOWN_ITEMS + currentAccount, new HashSet<String>());

        for (int i = text.size() - 1; i >= 0; i--) {
            if (!shownItems.contains(i + "")) {
                text.remove(i);
                pageTypes.remove(i);
            }
        }

        navigationView.getMenu().clear();
        for (int i = 0; i < text.size(); i++) {
            if (i < navigationView.getMaxItemCount()) {
                navigationView.getMenu().add(0, i, 0, text.get(i)).setIcon(getIconResByPosition(i));
            }

        }
        if (navigationView.getMenu().size() == 1) {
            navigationView.setVisibility(View.GONE);
        }


        Menu menu = navigationView.getMenu();
        //为了让超过5个的页面不显示选中状态，需要手动维护每个tab的选中状态
        menu.setGroupCheckable(0, true, false);

        navigationView.setOnItemSelectedListener(item -> {
            item.setChecked(true);
            MenuItem menuItem;
            for (int i = 0; i < menu.size(); i++) {
                menuItem = menu.findItem(i);
                if (item.getItemId() != menuItem.getItemId()) {
                    menuItem.setChecked(false);
                }
            }
            mainDrawerClickListener.onClick(null, item.getItemId());
            return false;
        });

    }

    public void setNavigationViewItemReselectedListener(boolean flag) {
        if (flag) {
            navigationView.setOnItemReselectedListener(item -> {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).topCurrentFragment();
                }
            });
        } else {
            //从某个页面A切换到第N（>5）个的页面，再点击tab切换到页面A，当前页面和选中的页面是同一个如果这个监听不为空那么不会走itemSelectedListener的分支
            navigationView.setOnItemReselectedListener(null);
        }

    }

    @Override
    public int getCount() {
        return text.size();
    }

    @SuppressLint("ResourceType")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        String settingName = text.get(position);

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);

            ViewHolder viewHolder = new ViewHolder();

            viewHolder.name = (TextView) rowView.findViewById(R.id.title);
            viewHolder.icon = (ImageView) rowView.findViewById(R.id.icon);
            viewHolder.cardView = (MaterialCardView) rowView.findViewById(R.id.cardView);


            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        holder.name.setText(settingName);

        try {
            int iconRes = getIconResByPosition(position);
            holder.icon.setImageResource(iconRes);
        } catch (Exception e) {

        }

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.colorSurface});
        int bResource = b.getResourceId(0, 0);
        rowView.setBackgroundResource(bResource);
        ((MaterialCardView) holder.cardView).setChecked(highlightedCurrent == position);
        if (highlightedCurrent == position) {
            holder.icon.setColorFilter(ThemeStore.accentColor(context));
            holder.name.setTextColor(ThemeStore.accentColor(context));

        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            int resource = a.getResourceId(0, 0);

            holder.icon.clearColorFilter();
            holder.name.setTextColor(context.getResources().getColor(resource));

        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainDrawerClickListener.onClick(v, position);
            }
        });

        return rowView;
    }

    private int getIconResByPosition(int position) {
        int iconRes = -1;
        String pageName = text.get(position);
        int pageType = pageTypes.get(position);
        if (pageType == AppSettings.PAGE_TYPE_HOME) {
            iconRes = R.drawable.ic_round_home_24;
        } else if (pageType == AppSettings.PAGE_TYPE_MENTIONS || pageType == AppSettings.PAGE_TYPE_SECOND_MENTIONS) {
            iconRes = R.drawable.ic_mention;
        } else if (pageType == AppSettings.PAGE_TYPE_DMS) {
            iconRes = R.drawable.ic_round_message_24;
        } else if (pageType == AppSettings.PAGE_TYPE_RETWEET) {
            iconRes = R.drawable.ic_retweet;
        } else if (pageName.equals(context.getResources().getString(R.string.favorite_tweets)) || pageType == AppSettings.PAGE_TYPE_SAVED_TWEETS) {
            iconRes = R.drawable.ic_heart;
        } else if (pageType == AppSettings.PAGE_TYPE_FAV_USERS || pageType == AppSettings.PAGE_TYPE_FAV_USERS_TWEETS || pageType == AppSettings.PAGE_TYPE_USER_TWEETS) {
            iconRes = R.drawable.ic_round_person_24;
        } else if (pageName.equals(context.getString(R.string.world_timeline))) {
            iconRes = R.drawable.ic_world_24;
        } else if (pageName.equals(context.getString(R.string.local_timeline))) {
            iconRes = R.drawable.ic_local_24;
        } else if (pageType == AppSettings.PAGE_TYPE_DISCOVER) {
            iconRes = R.drawable.ic_discover;
        } else if (pageType == AppSettings.PAGE_TYPE_LIST || text.get(position).equals(context.getResources().getString(R.string.lists)) || pageType == AppSettings.PAGE_TYPE_LIST_TIMELINE) {
            iconRes = R.drawable.ic_round_list_alt_24;
        } else if (pageTypes.get(position) == AppSettings.PAGE_TYPE_HASHTAGS) {
            iconRes = R.drawable.ic_hashtag;
        } else if (pageType == AppSettings.PAGE_TYPE_LINKS) {
            iconRes = R.drawable.ic_round_insert_link_24;
        } else if (pageType == AppSettings.PAGE_TYPE_PICS) {
            iconRes = R.drawable.ic_round_image_24;
        } else if (pageType == AppSettings.PAGE_TYPE_ACTIVITY) {
            iconRes = R.drawable.ic_round_notifications_24;
        } else if (pageType == AppSettings.PAGE_TYPE_BOOKMARKED_TWEETS) {
            iconRes = R.drawable.round_bookmark_24;
        } else {
            iconRes = R.drawable.ic_round_person_24;
        }
        return iconRes;
    }

    public static int highlightedCurrent;

    /**
     * 只用来修改tab menu的选中状态
     *
     * @param itemId
     */
    public void setSelectedItemId(int itemId) {
        Menu menu = navigationView.getMenu();
        MenuItem item;
        if (menu.findItem(itemId) != null) {
            menu.findItem(itemId).setChecked(true);
            setNavigationViewItemReselectedListener(true);
        } else {
            setNavigationViewItemReselectedListener(false);
        }
        for (int i = 0; i < menu.size(); i++) {
            item = menu.findItem(i);
            if (item.getItemId() != itemId) {
                item.setChecked(false);
            }
        }

    }

    public static void setCurrent(Context context, int i) {
        current = i;
        highlightedCurrent = i;
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        Set<String> shownItems = sharedPrefs.getStringSet(AppSettings.DRAWER_SHOWN_ITEMS + currentAccount, new HashSet<String>());
        for (int index = 0; index <= highlightedCurrent; index++) {
            if (!shownItems.contains(index + "")) {
                highlightedCurrent--;
            }
        }
    }

    public String getName(int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_HOME:
                return context.getResources().getString(R.string.timeline);
            case AppSettings.PAGE_TYPE_MENTIONS:
                return context.getResources().getString(R.string.mentions);
            case AppSettings.PAGE_TYPE_DMS:
                return context.getResources().getString(R.string.direct_messages);
            case AppSettings.PAGE_TYPE_WORLD_TIMELINE:
                return context.getResources().getString(R.string.world_timeline);
            case AppSettings.PAGE_TYPE_LOCAL_TIMELINE:
                return context.getString(R.string.local_timeline);
            case AppSettings.PAGE_TYPE_ACTIVITY:
                return context.getString(R.string.activity);
            case AppSettings.PAGE_TYPE_FAVORITE_STATUS:
                return context.getString(R.string.favorite_tweets);
            case AppSettings.PAGE_TYPE_LINKS:
                return context.getResources().getString(R.string.links);
            case AppSettings.PAGE_TYPE_PICS:
                return context.getResources().getString(R.string.pictures);
            case AppSettings.PAGE_TYPE_FAV_USERS_TWEETS:
                return context.getString(R.string.favorite_users_tweets);
            case AppSettings.PAGE_TYPE_HASHTAGS:
                return context.getString(R.string.hashtags);
            case AppSettings.PAGE_TYPE_BOOKMARKED_TWEETS:
                return context.getString(R.string.bookmarked_tweets);
            case AppSettings.PAGE_TYPE_LIST:
                return context.getString(R.string.lists);
            case AppSettings.PAGE_TYPE_RETWEET:
                return context.getString(R.string.retweets);
            case AppSettings.PAGE_TYPE_FAV_USERS:
                return context.getString(R.string.favorite_users);
            case AppSettings.PAGE_TYPE_DISCOVER:
                return context.getString(R.string.discover);
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                return context.getString(R.string.saved_tweets);
        }

        return null;
    }
}