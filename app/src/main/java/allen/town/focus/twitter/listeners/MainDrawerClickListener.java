package allen.town.focus.twitter.listeners;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.adapters.MainDrawerArrayAdapter;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.settings.configure_pages.ConfigurePagerActivity;
import allen.town.focus.twitter.views.widgets.NotificationDrawerLayout;

import java.util.HashSet;
import java.util.Set;

public class MainDrawerClickListener {

    private Context context;
    private NotificationDrawerLayout drawer;
    private ViewPager viewPager;
    private boolean noWait;
    private int swipablePages = 0;

    private String[] shownElements;
    private Set<String> set;

    private SharedPreferences sharedPreferences;

    public MainDrawerClickListener(Context context, NotificationDrawerLayout drawer, ViewPager viewPager) {
        this.context = context;
        this.drawer = drawer;
        this.viewPager = viewPager;
        this.noWait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ||
                context.getResources().getBoolean(R.bool.isTablet);
        sharedPreferences = AppSettings.getSharedPreferences(context);


        int currentAccount = sharedPreferences.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPreferences.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type != AppSettings.PAGE_TYPE_NONE) {
                swipablePages++;
            }
        }

        set = sharedPreferences.getStringSet(AppSettings.DRAWER_SHOWN_ITEMS + currentAccount, new HashSet<String>());
        shownElements = new String[set.size()];
        int i = 0;
        for (Object o : set.toArray()) {
            shownElements[i] = (String) o;
            i++;
        }
    }

    int realPages = 0;

    public void onClick(View v, int i) {
        context.sendBroadcast(new Intent(AppSettings.BROADCAST_MARK_POSITION));

        // we will increment until we find one that is in the set of shown elements
        for (int index = 0; index <= i; index++) {
            if (!set.contains(index + "")) {
                i++;
            }
        }

        if (i >= ConfigurePagerActivity.MAX_FREE_PAGE_COUNT
                &&
                !App.getInstance().checkSupporter(context, true)) {
            return;

        }

        if (i < swipablePages) {
            if (MainDrawerArrayAdapter.current < swipablePages) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            drawer.closeDrawer(Gravity.START);
                        } catch (Exception e) {
                            // landscape mode
                        }
                    }
                }, noWait ? 0 : 300);

                viewPager.setCurrentItem(i, true);
            } else {
                final int pos = i;
                try {
                    drawer.closeDrawer(Gravity.START);
                } catch (Exception e) {
                    // landscape mode
                }

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra(AppSettings.PAGE_TO_OPEN, pos);
                intent.putExtra(AppSettings.FROM_DRAW, true);

                sharedPreferences.edit().putBoolean(AppSettings.SHOULD_REFRESH, false).commit();

                final Intent fIntent = intent;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            context.startActivity(fIntent);
                            ((Activity) context).overridePendingTransition(0, 0);
                            ((Activity) context).finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, noWait ? 0 : 400);

            }
        } else {
            final int pos = i;
            try {
                drawer.closeDrawer(Gravity.START);
            } catch (Exception e) {
                // landscape mode
            }
            Intent intent = null;


            final Intent fIntent = intent;
            fIntent.putExtra(AppSettings.FROM_DRAW, true);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        context.startActivity(fIntent);
                        ((Activity) context).overridePendingTransition(0, 0);
                        ((Activity) context).finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, noWait ? 0 : 400);

        }

    }

}
