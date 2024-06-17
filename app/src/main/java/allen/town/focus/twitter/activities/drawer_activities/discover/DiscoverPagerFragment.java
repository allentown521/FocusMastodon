package allen.town.focus.twitter.activities.drawer_activities.discover;
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.adapters.TrendsPagerAdapter;

import allen.town.focus_common.util.StatusBarUtils;

public class DiscoverPagerFragment extends MainFragment {

    private TrendsPagerAdapter mSectionsPagerAdapter;
    public static ViewPager mViewPager;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.trends_activity, null);

        //getChildFragmentManager很重要
        mSectionsPagerAdapter = new TrendsPagerAdapter(getChildFragmentManager(), context);

        mViewPager = (ViewPager) layout.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        TabLayout strip = (TabLayout) layout.findViewById(R.id.pager_tab_strip);
        strip.setupWithViewPager(mViewPager);


        StatusBarUtils.setPaddingStatusBarTop(getActivity(),strip);

        mViewPager.setOffscreenPageLimit(3);
        return layout;
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
