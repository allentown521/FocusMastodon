package allen.town.focus.twitter.activities;
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

import static code.name.monkey.appthemehelper.constants.ThemeConstants.TAB_TEXT_MODE;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;

import allen.town.focus.twitter.BuildConfig;
import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.setup.material_login.MaterialLogin;
import allen.town.focus.twitter.adapters.MainDrawerArrayAdapter;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.api.session.AccountSessionManager;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.data.sq_lite.DMDataSource;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersDataSource;
import allen.town.focus.twitter.data.sq_lite.FollowersDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.InteractionsDataSource;
import allen.town.focus.twitter.data.sq_lite.ListDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.event.AuthFailedEvent;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.services.SendScheduledTweet;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.NotificationUtils;
import allen.town.focus.twitter.utils.PermissionModelUtils;
import allen.town.focus.twitter.utils.UpdateUtils;
import allen.town.focus_common.ad.ConsentRequestManager;
import allen.town.focus_common.dialog.RatingDialog;
import allen.town.focus_common.extensions.ActivityThemeExtensionsUtils;
import allen.town.focus_common.extensions.ColorExtensionsUtils;
import allen.town.focus_common.inappupdate.InAppPlayUpdateUtil;
import allen.town.focus_common.util.BasePreferenceUtil;
import allen.town.focus_common.views.AccentMaterialDialog;


public class MainActivity extends DrawerActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static boolean isPopup;
    public static Context sContext;

    public static FloatingActionButton sendButton;
    public static boolean showIsRunning = false;
    public static boolean hideIsRunning = false;
    public static Handler sendHandler;
    public static Runnable showSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.GONE && !showIsRunning) {

                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_in);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        showIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendButton.setVisibility(View.VISIBLE);
                        showIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sendButton.startAnimation(anim);
            }
        }
    };
    public static Runnable hideSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.VISIBLE && !hideIsRunning) {
                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_out);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        hideIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendButton.setVisibility(View.GONE);
                        hideIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                anim.setDuration(ANIM_DURATION);
                sendButton.startAnimation(anim);
            }
        }
    };

    public void topCurrentFragment() {
        Intent top = new Intent(IntentConstant.TOP_TIMELINE_ACTION);
        top.putExtra("fragment_number", mViewPager.getCurrentItem());
        sendBroadcast(top);
    }

    public void showAwayFromTopToast() {
        Intent toast = new Intent(IntentConstant.SHOW_TOAST_ACTION);
        toast.putExtra("fragment_number", mViewPager.getCurrentItem());
        sendBroadcast(toast);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addEntranceActivityName(MainActivity.class.getSimpleName());
        AppSettings settings = AppSettings.getInstance(this);
        if (settings.myScreenName == null || settings.myScreenName.isEmpty()) {
            if (settings.currentAccount == 1) {
                settings.sharedPrefs.edit().putInt(AppSettings.CURRENT_ACCOUNT, 2).commit();
            } else {
                settings.sharedPrefs.edit().putInt(AppSettings.CURRENT_ACCOUNT, 1).commit();
            }

            AppSettings.invalidate();
        }
        AccountSessionManager.getInstance().maybeUpdateLocalInfo();
        UpdateUtils.checkUpdate(this);
        //不要放在onresume中，否则即使点击不更新会一直走这个回调并且弹窗
        InAppPlayUpdateUtil.checkGooglePlayInAppUpdate(this, BuildConfig.VERSION_CODE);

        MainActivity.sendHandler = new Handler();

        context = this;
        sContext = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        DrawerActivity.settings = AppSettings.getInstance(context);

        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {

        }

        sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, getIntent().getBooleanExtra("from_notification", false)).commit();

        setUpTheme();
        setUpWindow();
        setContentView(R.layout.main_activity);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        setUpDrawer(0, getResources().getString(R.string.timeline));

        ActivityThemeExtensionsUtils.setNavigationBarColor(this, ColorExtensionsUtils.surfaceColor(this));
        MainActivity.sendButton = (FloatingActionButton) findViewById(R.id.send_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementExitTransition(new ChangeBounds());
        }

        MainActivity.sendHandler.postDelayed(showSend, 1000);
        MainActivity.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent compose = new Intent(context, ComposeActivity.class);
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                compose.putExtra("already_animated", true);
                startActivity(compose, opts.toBundle());
            }
        });


        if (!settings.isTwitterLoggedIn) {
            // set the default theme for new users to be dark timeline style, with a dark app bar
            AppSettings.getInstance(this).sharedPrefs.edit()
                    .putString("timeline_pictures", "" + AppSettings.PICTURES_NORMAL)
                    .putInt("material_theme_1", AppSettings.DEFAULT_THEME)
                    .commit();
            Intent login = new Intent(context, MaterialLogin.class);
            startActivity(login);
        } else {
            if (!App.getInstance().isDroid() && App.getInstance().needOpenPurchaseWhenAppOpen()) {
                App.getInstance().checkSupporter(context, true);
            }
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(getSupportFragmentManager(), context, sharedPrefs, getIntent().getBooleanExtra("from_launcher", false));
        int currAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
        int defaultPage = sharedPrefs.getInt(AppSettings.DEFAULT_TIMELINE_PAGE + currAccount, 0);
        actionBar.setTitle(mSectionsPagerAdapter.getPageTitle(defaultPage));

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    int count = mSectionsPagerAdapter.getCount();
                    for (int i = 0; i < count; i++) {
                        MainFragment f = (MainFragment) mSectionsPagerAdapter.getRealFrag(i);
                        f.allowBackPress();
                        f.resetVideoHandler();
                    }
                }
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                showBars();
            }

            public void onPageSelected(int position) {
                Fragment f = (Fragment) mSectionsPagerAdapter.getRealFrag(position);
                if(f instanceof MainFragment){
                    ((MainFragment)f).playCurrentVideos();
                }


                String title = "" + mSectionsPagerAdapter.getPageTitle(position);

                MainDrawerArrayAdapter.setCurrent(context, position);
                adapter.setSelectedItemId(position);
                drawerList.invalidateViews();

                actionBar.setTitle(title);
            }
        });

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setCurrentItem(defaultPage);
        MainDrawerArrayAdapter.setCurrent(this, defaultPage);
        adapter.setSelectedItemId(defaultPage);

        drawerList.invalidateViews();

        if (getIntent().getBooleanExtra("from_launcher", false)) {
            actionBar.setTitle(mSectionsPagerAdapter.getPageTitle(getIntent().getIntExtra("launcher_page", 0)));
        }

        mViewPager.setOffscreenPageLimit(TimelinePagerAdapter.MAX_EXTRA_PAGES);

        final PermissionModelUtils permissionUtils = new PermissionModelUtils(this);

            if (permissionUtils.needPermissionCheck()) {
                permissionUtils.showPermissionExplanationThenAuthorization();
            }

        setLauncherPage();

        if (getIntent().getBooleanExtra(AppSettings.FROM_DRAW, false)) {
            mViewPager.setCurrentItem(getIntent().getIntExtra(AppSettings.PAGE_TO_OPEN, 1));
        }

        Log.v("Focus_for_Mastodon_starting", "ending on create");
    }

    /**
     * 检查api key 是否还有效
     */

    private void showAuthInvalidDialog() {
        new AccentMaterialDialog(
                this,
                R.style.MaterialAlertDialogTheme
        )
                .setMessage(getResources().getString(R.string.login_error))
                .setPositiveButton(R.string.menu_logout, (dialogInterface, i) -> {
                    EventBus.getDefault().post(
                            new AuthFailedEvent()
                    );
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    public void setLauncherPage() {
        // do nothing here
    }

    public void setUpWindow() {
        // nothing here, will be overrode
        MainActivity.isPopup = false;

        if ((getIntent().getFlags() & 0x00002000) != 0) {
            MainActivity.isPopup = true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    @Override
    public void onBackPressed() {

        // this will go through all the current fragments and check if one has an expanded item
        int count = mSectionsPagerAdapter.getCount();
        boolean clicked = false;
        for (int i = 0; i < count; i++) {
            MainFragment f = (MainFragment) mSectionsPagerAdapter.getRealFrag(i);

            // we only want it to quit if there is an expanded item and the view pager is currently looking at the
            // page with that expanded item. If they swipe to mentions while something is expanded on the main
            // timeline , then it should still quit if the back button is pressed

            if (!f.allowBackPress() && mViewPager.getCurrentItem() == i) {
                clicked = true;
            }
        }

        if (!clicked) {
            super.onBackPressed();
        }
    }

    private void handleOpenPage() {
        if (sharedPrefs.getBoolean(AppSettings.OPEN_A_PAGE, false)) {
            sharedPrefs.edit().putBoolean(AppSettings.OPEN_A_PAGE, false).commit();
            int page = sharedPrefs.getInt(AppSettings.OPEN_WHAT_PAGE, 1);
            String title = "" + mSectionsPagerAdapter.getPageTitle(page);
            actionBar.setTitle(title);
            mViewPager.setCurrentItem(page);
        }

        //没有用到了
        if (sharedPrefs.getBoolean("open_interactions", false)) {
            sharedPrefs.edit().putBoolean("open_interactions", false).commit();
            mDrawerLayout.openDrawer(Gravity.END);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOpenPage();
    }

    @Override
    public void onResume() {
        super.onResume();
        handleOpenPage();
        RatingDialog.check();
        new ConsentRequestManager().showConsentForm(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        try {
            int current = mViewPager.getCurrentItem();
            MainFragment currentFragment = (MainFragment) mSectionsPagerAdapter.getRealFrag(current);
            currentFragment.scrollDown();
        } catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        try {
            HomeDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            MentionsDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            DMDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            ListDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FollowersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FavoriteUsersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            InteractionsDataSource.getInstance(context).close();
        } catch (Exception e) { }

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public static boolean caughtstarting = false;

    @Override
    public void onStart() {
        super.onStart();

        MainActivity.isPopup = false;

        Log.v("Focus_for_Mastodon_starting", "main activity starting");

        sharedPrefs = AppSettings.getSharedPreferences(this);


        if (sharedPrefs.getBoolean("launcher_frag_switch", false)) {

            sharedPrefs.edit().putBoolean("launcher_frag_switch", false)
                              .putBoolean(AppSettings.DONT_REFRESH, true).commit();

            AppSettings.invalidate();

            Log.v("Focus_for_Mastodon_theme", "no action bar overlay found, recreating");

            finish();
            overridePendingTransition(0, 0);
            startActivity(getRestartIntent());
            overridePendingTransition(0, 0);

            MainActivity.caughtstarting = true;

            // return so that it doesn't start the background refresh, that is what caused the dups.
            sharedPrefs.edit().putBoolean("dont_refresh_on_start", true).commit();
            return;
        } else {
            sharedPrefs.edit().putBoolean(AppSettings.DONT_REFRESH, false)
                              .putBoolean(AppSettings.SHOULD_REFRESH, true).commit();

            MainActivity.caughtstarting = false;
        }

        UpdateUtils.checkUpdate(this);
        RatingDialog.init(this);

        if (sharedPrefs.getBoolean("force_reverse_click", true)) {
            sharedPrefs.edit().putBoolean("reverse_click_option", false)
                    .putBoolean("force_reverse_click", false)
                    .commit();
        }

        new Handler().postDelayed(() -> {
            NotificationUtils.sendTestNotification(MainActivity.this);
            SendScheduledTweet.scheduleNextRun(context);
        }, 1000);
    }

    public Intent getRestartIntent() {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (settings.floatingCompose) {
            menu.getItem(2).setVisible(false); // hide the compose button here
        }

        if (settings.tweetmarkerManualOnly) {
            menu.getItem(7).setVisible(true);
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(TAB_TEXT_MODE)){
            navigationView.setLabelVisibilityMode(BasePreferenceUtil.getTabTitleMode());
        }else if(key.equals("timeline_pictures")
                || key.equals(AppSettings.WEB_PREVIEW_TIMELINE_KEY)
        ){
            postRecreate();
        } else if(key.equals("text_size")){
            clearAllAppcompactActivities(true);
        }

    }
}
