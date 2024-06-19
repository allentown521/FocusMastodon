
package allen.town.focus.twitter.settings.configure_pages;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;

import allen.town.focus_common.util.Timber;


public class ConfigurePagerActivity extends WhiteToolbarActivity {

    private ConfigurationPagerAdapter chooserAdapter;
    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private androidx.appcompat.app.ActionBar actionBar;
    private ViewPager mViewPager;
    public static int MAX_FREE_PAGE_COUNT = 8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppSettings.getInstance(this).blackTheme) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        Utils.setUpTweetTheme(context, settings);
        setContentView(R.layout.configuration_activity);

        setUpDoneDiscard();

        actionBar = getSupportActionBar();
//        actionBar.setTitle(R.string.app_drawer);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        chooserAdapter = new ConfigurationPagerAdapter(getFragmentManager(), context);

        mViewPager.setAdapter(chooserAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.setOffscreenPageLimit(TimelinePagerAdapter.MAX_EXTRA_PAGES);

    }

    public void setUpDoneDiscard() {
        setSupportActionBar(findViewById(R.id.toolbar));


        findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

                        if (chooserAdapter.getCount() > MAX_FREE_PAGE_COUNT) {
                            //配置了页面的数量
                            int notNoneCount = 0;
                            for (int i = 0; i < chooserAdapter.getCount(); i++) {
                                if (((ChooserFragment) chooserAdapter.getItem(i)).type != AppSettings.PAGE_TYPE_NONE) {
                                    notNoneCount++;
                                }

                            }
                            if (notNoneCount > MAX_FREE_PAGE_COUNT &&
                                    !App.getInstance().checkSupporter(ConfigurePagerActivity.this, true)) {
                                Timber.d("not pro so limit 8 pages");
                                return;
                            }

                        }
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        for (int i = 0; i < chooserAdapter.getCount(); i++) {
                            if (chooserAdapter.getItem(i) instanceof ChooserFragment) {
                                ChooserFragment f = (ChooserFragment) chooserAdapter.getItem(i);

                                int num = i + 1;
                                editor.putInt("account_" + currentAccount + "_page_" + num, f.type);
                                editor.putLong("account_" + currentAccount + "_list_" + num + "_long", f.listId);
                                editor.putLong("account_" + currentAccount + "_user_tweets_" + num + "_long", f.userId);
                                editor.putString("account_" + currentAccount + "_name_" + num, f.name);
                                editor.putString("account_" + currentAccount + "_search_" + num, f.searchQuery);

                                if (f.check != null && f.check.isChecked()) {
                                    editor.putInt(AppSettings.DEFAULT_TIMELINE_PAGE + currentAccount, i);
                                }
                            }
                        }


                        editor.commit();

                        clearAllAppcompactActivities(true);
                        onBackPressed();
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.configuration_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return true;
        }
    }

}
