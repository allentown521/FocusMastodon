package allen.town.focus.twitter.activities.drawer_activities

import allen.town.core.service.GooglePayService
import allen.town.focus_purchase.data.db.table.GooglePlayInAppTable
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.MainActivity
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.activities.compose.ComposeActivity
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager
import allen.town.focus.twitter.activities.setup.material_login.MaterialLogin
import allen.town.focus.twitter.adapters.InteractionsCursorAdapter
import allen.town.focus.twitter.adapters.MainDrawerArrayAdapter
import allen.town.focus.twitter.adapters.TimelinePagerAdapter
import allen.town.focus.twitter.api.session.AccountSessionManager
import allen.town.focus.twitter.data.App
import allen.town.focus.twitter.data.sq_lite.*
import allen.town.focus.twitter.event.AuthFailedEvent
import allen.town.focus.twitter.event.RemoveAdsPurchaseEvent
import allen.town.focus.twitter.receivers.IntentConstant
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.settings.SettingsActivity
import allen.town.focus.twitter.utils.MySuggestionsProvider
import allen.town.focus.twitter.utils.NavigationUtil
import allen.town.focus.twitter.utils.SearchUtils
import allen.town.focus.twitter.utils.SystemBarVisibility
import allen.town.focus.twitter.utils.Utils.getActionBarHeight
import allen.town.focus.twitter.utils.Utils.getBackgroundUrlForTheme
import allen.town.focus.twitter.utils.Utils.getNavBarHeight
import allen.town.focus.twitter.utils.Utils.getStatusBarHeight
import allen.town.focus.twitter.utils.Utils.hasNavBar
import allen.town.focus.twitter.utils.Utils.setSharedContentTransition
import allen.town.focus.twitter.utils.Utils.setTaskDescription
import allen.town.focus.twitter.utils.Utils.setUpTweetTheme
import allen.town.focus.twitter.utils.Utils.toDP
import allen.town.focus.twitter.views.widgets.ActionBarDrawerToggle
import allen.town.focus.twitter.views.widgets.NotificationDrawerLayout
import allen.town.focus_common.ad.RewardedAdManager
import allen.town.focus_common.ads.OnUserEarnedRewardListener
import allen.town.focus_common.common.prefs.supportv7.dialogs.SingleListDialogFragmentCompat
import allen.town.focus_common.extensions.hide
import allen.town.focus_common.extensions.show
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.EntityDateUtils
import allen.town.focus_common.util.StatusBarUtils.setMarginStatusBarTop
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.TintedBottomNavigationView
import allen.town.focus_purchase.iap.SupporterManager
import allen.town.focus_purchase.iap.SupporterManagerWrap
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.SearchRecentSuggestions
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import code.name.monkey.appthemehelper.ThemeStore
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.SimpleLottieValueCallback
import com.android.billingclient.api.SkuDetails
import com.bumptech.glide.Glide
import com.wyjson.router.GoRouter
import de.timroes.android.listview.EnhancedListView
import me.leolin.shortcutbadger.ShortcutBadger
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
 */ abstract class DrawerActivity() : WhiteToolbarActivity(), SystemBarVisibility {
    lateinit var context: FragmentActivity

    @JvmField
    var sharedPrefs: SharedPreferences? = null

    @JvmField
    var actionBar: ActionBar? = null
    lateinit var mSectionsPagerAdapter: TimelinePagerAdapter
    lateinit var mDrawerLayout: NotificationDrawerLayout
    var notificationAdapter: InteractionsCursorAdapter? = null
    lateinit var mDrawer: LinearLayout
    lateinit var drawerList: ListView
    var notificationList: EnhancedListView? = null
    var mDrawerToggle: ActionBarDrawerToggle? = null
    var listView: ListView? = null
    var logoutVisible = false
    var kitkatStatusBar: View? = null
    var openMailResource = 0
    var closedMailResource = 0
    var readButton: ImageView? = null
    private var backgroundPic: ImageView? = null
    private var profilePic: ImageView? = null
    private var noInteractions: LinearLayout? = null
    var toolbar: Toolbar? = null
    var statusBar: View? = null

    @JvmField
    var adapter: MainDrawerArrayAdapter? = null

    protected lateinit var navigationView: TintedBottomNavigationView
    private var lottieVip: LottieAnimationView? = null
    private lateinit var removeAdIv: LottieAnimationView
    private lateinit var viewVideoAdIv: LottieAnimationView
    private lateinit var supporterManager: SupporterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = AppSettings.getInstance(this)
        super.onCreate(savedInstanceState)
        actionBar = supportActionBar
        setSharedContentTransition(this)
        setTaskDescription(this)
        supporterManager = SupporterManagerWrap.getSupporterManger(this)
        RewardedAdManager.loadRewardedAd(this)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    protected val noContentTitle: String
        protected get() = getString(R.string.no_content_home)
    protected val noContentSummary: String
        protected get() = getString(R.string.no_content_home_summary)
    private var searchUtils: SearchUtils? = null
    fun setUpDrawer(number: Int, actName: String?) {
        setUpLottieViews()
        var number = number
        var statusBarHeight = getStatusBarHeight((context)!!)
        val header = findViewById<View>(R.id.header)
        header.layoutParams.height = toDP(144, (context)!!) + statusBarHeight
        header.invalidate()
        val profilePicImage = findViewById<View>(R.id.profile_pic_contact)
        (profilePicImage.layoutParams as RelativeLayout.LayoutParams).topMargin =
            toDP(12, (context)!!) + statusBarHeight
        profilePicImage.invalidate()
        val profilePic2Image = findViewById<View>(R.id.profile_pic_contact_2)
        (profilePic2Image.layoutParams as RelativeLayout.LayoutParams).topMargin =
            toDP(12, (context)!!) + statusBarHeight
        profilePic2Image.invalidate()
        val noContentTitleTv: TextView? = findViewById<View>(R.id.no_content_title) as? TextView
        noContentTitleTv?.text = noContentTitle
        val noContentSummaryTv: TextView? = findViewById<View>(R.id.no_content_summary) as? TextView
        noContentSummaryTv?.text = noContentSummary
        searchUtils = SearchUtils(this)
        searchUtils!!.setUpSearch()
        try {
            findViewById<View>(R.id.dividerView).setOnTouchListener(OnTouchListener { v, event -> true })
        } catch (t: Throwable) {
        }
        val currentAccount = sharedPrefs!!.getInt(AppSettings.CURRENT_ACCOUNT, 1)
        for (i in 0 until TimelinePagerAdapter.MAX_EXTRA_PAGES) {
            val pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1)
            val type = sharedPrefs!!.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE)
            if (type != AppSettings.PAGE_TYPE_NONE) {
                number++
            }
        }
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField =
                ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            if (menuKeyField != null) {
                menuKeyField.isAccessible = true
                menuKeyField.setBoolean(config, false)
            }
        } catch (ex: Exception) {
            // Ignore
        }
        navigationView = findViewById(R.id.navigationView)
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (landscape) {
            //横屏隐藏tabbar
            navigationView.visibility = View.GONE
        }
        mDrawerLayout = findViewById(R.id.drawer_layout)
        adapter = MainDrawerArrayAdapter(context, mDrawerLayout, mViewPager, navigationView)
        MainDrawerArrayAdapter.setCurrent(context, number)
        adapter!!.setSelectedItemId(number)
        val a: TypedArray? = null
        val resource = R.drawable.ic_round_menu_24
        noInteractions = findViewById<View>(R.id.no_interaction) as LinearLayout
        val noInterImage = findViewById<View>(R.id.no_inter_icon) as ImageView
        noInteractions!!.visibility = View.GONE
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        if (toolbar != null) {
            try {
                setSupportActionBar(toolbar)
                hasToolbar = true
            } catch (e: Exception) {
                // already has an action bar supplied?? comes when you switch to landscape and back to portrait
                e.printStackTrace()
            }
            toolbar!!.setNavigationIcon(resource)
            toolbar!!.setNavigationOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                        mDrawerLayout.closeDrawer(Gravity.LEFT)
                    } else {
                        mDrawerLayout.openDrawer(Gravity.LEFT)
                    }
                }
            })
            toolbar!!.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    if (this@DrawerActivity is MainActivity) {
                        this@DrawerActivity.topCurrentFragment()
                    }
                    return true
                }
            })
            toolbar!!.setOnClickListener({ v: View? ->
                if (this@DrawerActivity is MainActivity) {
                    this@DrawerActivity.showAwayFromTopToast()
                }
            })

            try {
                val toolParams = toolbar!!.layoutParams as RelativeLayout.LayoutParams
                toolParams.height = getActionBarHeight((context)!!)
                //                toolParams.topMargin = Utils.getStatusBarHeight(context);
                toolbar!!.layoutParams = toolParams
            } catch (e: ClassCastException) {
                // they are linear layout here
                val toolParams = toolbar!!.layoutParams as LinearLayout.LayoutParams
                toolParams.height = getActionBarHeight((context)!!)
                //                toolParams.topMargin = Utils.getStatusBarHeight(context);
                toolbar!!.layoutParams = toolParams
            }
            setMarginStatusBarTop(this, toolbar)
        }
        actionBar = supportActionBar
        MainDrawerArrayAdapter.current = number
        openMailResource = R.drawable.ic_mail_read
        closedMailResource = R.drawable.ic_round_markunread_24
        mDrawer = findViewById<View>(R.id.left_drawer) as LinearLayout

        if (hasNavBar(context)) {
            //导航栏高度
//            (mDrawer!!.layoutParams as NotificationDrawerLayout.LayoutParams).bottomMargin =
//                getNavBarHeight(context)

            mDrawer.setPadding(
                mDrawer.paddingLeft,
                mDrawer.paddingTop,
                mDrawer.paddingRight,
                mDrawer.paddingBottom + getNavBarHeight(context)
            )
        }


        val name = mDrawer!!.findViewById<View>(R.id.name) as TextView
        val screenName = mDrawer!!.findViewById<View>(R.id.screen_name) as TextView
        backgroundPic = mDrawer!!.findViewById<View>(R.id.background_image) as ImageView
        profilePic = mDrawer!!.findViewById<View>(R.id.profile_pic_contact) as ImageView
        val profilePic2: ImageView? =
            mDrawer!!.findViewById<View>(R.id.profile_pic_contact_2) as ImageView
        val showMoreDrawer = mDrawer!!.findViewById<View>(R.id.options) as ImageButton
        val logoutLayout = mDrawer!!.findViewById<View>(R.id.logoutLayout) as LinearLayout
        val logoutDrawer = mDrawer!!.findViewById<View>(R.id.logoutButton) as Button
        drawerList = mDrawer!!.findViewById<View>(R.id.drawer_list) as ListView
        notificationList = findViewById<View>(R.id.notificationList) as EnhancedListView
        if (resources.getBoolean(R.bool.has_drawer)) {
            findViewById<View>(R.id.notification_drawer_ab).visibility = View.GONE
        }
        try {
            mDrawerLayout = findViewById<View>(R.id.drawer_layout) as NotificationDrawerLayout
            mDrawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT)
            mDrawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow_rev, Gravity.RIGHT)
            val hasDrawer = resources.getBoolean(R.bool.has_drawer)
            mDrawerToggle = object : ActionBarDrawerToggle(
                this,  /* host Activity */
                mDrawerLayout,  /* DrawerLayout object */
                resource,  /* nav drawer icon to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description */
                R.string.app_name /* "close drawer" description */
            ) {
                override fun onDrawerClosed(view: View) {
                    actionBar!!.setIcon(ColorDrawable(resources.getColor(android.R.color.transparent)))
                    if (logoutVisible) {
                        /*Animation ranim = AnimationUtils.loadAnimation(context, R.anim.drawer_rotate_back);
                        ranim.setFillAfter(true);
                        showMoreDrawer.startAnimation(ranim);*/
                        logoutLayout.visibility = View.GONE
                        drawerList!!.visibility = View.VISIBLE
                        logoutVisible = false
                    }
                    try {
                        if ((oldInteractions!!.text.toString() == resources.getString(R.string.new_interactions))) {
                            val c = InteractionsDataSource.getInstance(context).getUnreadCursor(
                                settings!!.currentAccount
                            )
                            oldInteractions!!.text = resources.getString(R.string.old_interactions)
                            readButton!!.setImageResource(openMailResource)
                            notificationList!!.enableSwipeToDismiss()
                            notificationAdapter = InteractionsCursorAdapter(context, c)
                            notificationList!!.adapter = notificationAdapter
                            try {
                                if (c.count == 0) {
                                    noInteractions!!.visibility = View.VISIBLE
                                } else {
                                    noInteractions!!.visibility = View.GONE
                                }
                            } catch (e: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                        // don't have Focus_for_Mastodon pull on
                    }
                    invalidateOptionsMenu()
                }

                override fun onDrawerOpened(drawerView: View) {
                    //actionBar.setTitle(getResources().getString(R.string.app_name));
                    //actionBar.setIcon(R.mipmap.ic_launcher);
                    searchUtils!!.hideSearch(false)
                    val c = InteractionsDataSource.getInstance(context).getUnreadCursor(
                        settings!!.currentAccount
                    )
                    try {
                        notificationAdapter = InteractionsCursorAdapter(context, c)
                        notificationList!!.adapter = notificationAdapter
                        notificationList!!.enableSwipeToDismiss()
                        oldInteractions!!.text = resources.getString(R.string.old_interactions)
                        readButton!!.setImageResource(openMailResource)
                        sharedPrefs!!.edit().putBoolean("new_notification", false).commit()
                    } catch (e: Exception) {
                        // don't have Focus_for_Mastodon pull on
                    }
                    try {
                        if (c.count == 0) {
                            noInteractions!!.visibility = View.VISIBLE
                        } else {
                            noInteractions!!.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                    }
                    invalidateOptionsMenu()
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    //super.onDrawerSlide(drawerView, slideOffset);
                    if (tranparentSystemBar == -1) {
                        tranparentSystemBar =
                            if (AppSettings.isWhiteToolbar(this@DrawerActivity)) resources.getColor(
                                R.color.light_status_bar_transparent_system_bar
                            ) else resources.getColor(R.color.transparent_system_bar)
                    }
                }

                override fun onOptionsItemSelected(item: MenuItem): Boolean {
                    Log.v("Focus_for_Mastodon_drawer", "item clicked")
                    // Toggle drawer
                    if (item.itemId == android.R.id.home) {
                        if (mDrawerLayout!!.isDrawerOpen(Gravity.LEFT)) {
                            mDrawerLayout!!.closeDrawer(Gravity.LEFT)
                        } else {
                            mDrawerLayout!!.openDrawer(Gravity.LEFT)
                        }
                        return true
                    }
                    return false
                }
            }
            mDrawerLayout!!.setDrawerListener(mDrawerToggle)
        } catch (e: Exception) {
            // landscape mode
        }
        actionBar!!.setHomeButtonEnabled(true)
        showMoreDrawer.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                if (logoutLayout.visibility == View.GONE) {
                    val anim = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            drawerList!!.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    anim.duration = 300
                    drawerList!!.startAnimation(anim)
                    val anim2 = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                    anim2.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            logoutLayout.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    anim2.duration = 300
                    logoutLayout.startAnimation(anim2)
                    logoutVisible = true
                } else {
                    /*Animation ranim = AnimationUtils.loadAnimation(context, R.anim.drawer_rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);*/
                    val anim = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            drawerList!!.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    anim.duration = 300
                    drawerList!!.startAnimation(anim)
                    val anim2 = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                    anim2.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            logoutLayout.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    anim2.duration = 300
                    logoutLayout.startAnimation(anim2)
                    logoutVisible = false
                }
            }
        })
        logoutDrawer.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                logoutFromTwitter()
            }
        })
        val sName = settings!!.myName
        val sScreenName = settings!!.myScreenName
        val sScreenId = settings!!.myId
        val backgroundUrl = settings!!.myBackgroundUrl
        val profilePicUrl = settings!!.myProfilePicUrl
        if (backgroundUrl != "") {
            Glide.with(this).load(backgroundUrl).into(backgroundPic!!)
        } else {
            Glide.with(this).load(getBackgroundUrlForTheme(settings)).into(backgroundPic!!)
        }
        backgroundPic!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                try {
                    mDrawerLayout.closeDrawer(Gravity.LEFT)
                } catch (e: Exception) {
                }
                Handler().postDelayed(object : Runnable {
                    override fun run() {
                        ProfilePager.start(context, sName, sScreenId, profilePicUrl)
                    }
                }, 400)
            }
        })
        backgroundPic!!.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                try {
                    mDrawerLayout.closeDrawer(Gravity.LEFT)
                } catch (e: Exception) {
                }
                Handler().postDelayed(object : Runnable {
                    override fun run() {
                        ProfilePager.start(context, sName, sScreenId, profilePicUrl)
                    }
                }, 400)
                return false
            }
        })
        try {
            name.text = sName
            screenName.text = "@$sScreenName"
        } catch (e: Exception) {
            // 7 inch tablet in portrait
        }
        Glide.with(this).load(profilePicUrl).into(profilePic!!)
        drawerList!!.adapter = adapter


        // set up for the second account
        var count = 0 // number of accounts logged in
        if (sharedPrefs!!.getBoolean("is_logged_in_1", false)) {
            count++
        }
        if (sharedPrefs!!.getBoolean("is_logged_in_2", false)) {
            count++
        }
        val secondAccount = findViewById<View>(R.id.second_profile) as RelativeLayout
        val name2 = findViewById<View>(R.id.name_2) as TextView
        val screenname2 = findViewById<View>(R.id.screen_name_2) as TextView
        val proPic2 = findViewById<View>(R.id.profile_pic_2) as ImageView
        name2.textSize = 15f
        screenname2.textSize = 15f
        val current = sharedPrefs!!.getInt(AppSettings.CURRENT_ACCOUNT, 1)

        // make a second account
        if (count == 1) {
            name2.text = resources.getString(R.string.new_account)
            proPic2.setImageResource(R.drawable.ic_round_add_circle_24)
            screenname2.visibility = View.GONE
            secondAccount.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (canSwitch) {
                        if (current == 1) {
                            sharedPrefs!!.edit().putInt(AppSettings.CURRENT_ACCOUNT, 2).commit()
                        } else {
                            sharedPrefs!!.edit().putInt(AppSettings.CURRENT_ACCOUNT, 1).commit()
                        }
                        context!!.sendBroadcast(Intent(AppSettings.BROADCAST_MARK_POSITION))
                        val login = Intent(context, MaterialLogin::class.java)
                        AppSettings.invalidate()
                        finish()
                        startActivity(login)
                    }
                }
            })
        } else { // switch accounts
            profilePic2?.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    secondAccount.performClick()
                }
            })
            if (current == 1) {
                name2.text = sharedPrefs!!.getString("twitter_users_name_2", "")
                screenname2.text = "@" + sharedPrefs!!.getString("twitter_screen_name_2", "")
                if (profilePic2 != null) {
                    Glide.with(this).load(sharedPrefs!!.getString("profile_pic_url_2", ""))
                        .into(profilePic2)
                }
                Glide.with(this).load(sharedPrefs!!.getString("profile_pic_url_2", ""))
                    .into(proPic2)
                secondAccount.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (canSwitch) {
                            context!!.sendBroadcast(
                                Intent(AppSettings.BROADCAST_MARK_POSITION).putExtra(
                                    AppSettings.CURRENT_ACCOUNT,
                                    current
                                )
                            )
//                            showSnack(context, "Preparing to switch", Toast.LENGTH_SHORT)

                            // we want to wait a second so that the mark position broadcast will work
                            Handler().postDelayed(object : Runnable {
                                override fun run() {
                                    sharedPrefs!!.edit().putInt(AppSettings.CURRENT_ACCOUNT, 2)
                                        .commit()
                                    sharedPrefs!!.edit().remove("new_notifications")
                                        .remove("new_retweets").remove("new_favorites")
                                        .remove("new_followers").commit()
                                    AppSettings.invalidate()
                                    finish()
                                    val next = Intent(context, MainActivity::class.java)
                                    startActivity(next)
                                }
                            }, 500)
                        }
                    }
                })
            } else {
                name2.text = sharedPrefs!!.getString("twitter_users_name_1", "")
                screenname2.text = "@" + sharedPrefs!!.getString("twitter_screen_name_1", "")
                if (profilePic2 != null) {
                    Glide.with(this).load(sharedPrefs!!.getString("profile_pic_url_1", ""))
                        .into(profilePic2)
                }
                Glide.with(this).load(sharedPrefs!!.getString("profile_pic_url_1", ""))
                    .into(proPic2)
                secondAccount.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (canSwitch) {
                            context!!.sendBroadcast(
                                Intent(AppSettings.BROADCAST_MARK_POSITION).putExtra(
                                    AppSettings.CURRENT_ACCOUNT,
                                    current
                                )
                            )
//                            showSnack(context, "Switching account now", Toast.LENGTH_SHORT)
                            Handler().postDelayed(object : Runnable {
                                override fun run() {
                                    sharedPrefs!!.edit().putInt(AppSettings.CURRENT_ACCOUNT, 1)
                                        .commit()
                                    sharedPrefs!!.edit().remove("new_notifications")
                                        .remove("new_retweets").remove("new_favorites")
                                        .remove("new_followers").commit()
                                    AppSettings.invalidate()
                                    finish()
                                    val next = Intent(context, MainActivity::class.java)
                                    startActivity(next)
                                }
                            }, 500)
                        }
                    }
                })
            }
        }

        statusBar = findViewById(R.id.activity_status_bar)

        if (MainActivity.isPopup) {
            statusBarHeight = 0
        }

        statusBar?.run {
            try {
                val statusParams = layoutParams
                statusParams.height = statusBarHeight
                layoutParams = statusParams
            } catch (e: java.lang.Exception) {
                try {
                    val statusParams = layoutParams
                    statusParams.height = statusBarHeight
                    layoutParams = statusParams
                } catch (x: java.lang.Exception) {
                    // in the trends
                }
            }
        }

        if (translucent) {
            if (hasNavBar((context)!!)) {
                val footer = View(context)
                footer.setOnClickListener(null)
                footer.setOnLongClickListener(null)
                val params = AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT, getNavBarHeight(
                        (context)!!
                    )
                )
                footer.layoutParams = params
                drawerList!!.addFooterView(footer)
                drawerList!!.setFooterDividersEnabled(false)
            }
        }
        try {
            mDrawerLayout.setDrawerLockMode(
                NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                Gravity.RIGHT
            )
        } catch (e: Exception) {
            // no drawer?
        }
    }

    private fun setUpLottieViews() {
        lottieVip = findViewById(R.id.already_vip_lottie)
        removeAdIv = findViewById(R.id.remove_ad_iv)
        viewVideoAdIv = findViewById(R.id.view_ad_video_iv)
        lottieVip!!.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            SimpleLottieValueCallback {
                PorterDuffColorFilter(
                    ThemeStore.accentColor(
                        context
                    ), PorterDuff.Mode.SRC_ATOP
                )
            }
        )

        lottieVip!!.setOnClickListener { NavigationUtil.goToProVersion(context) }
    }

    fun onSettingsClicked(v: View?) {
        Handler().postDelayed(object : Runnable {
            override fun run() {
                try {
//                    mDrawerLayout!!.closeDrawer(Gravity.START)
                    finish()
                } catch (e: Exception) {
                    // landscape mode
                }
            }
        }, 600)
        context!!.sendBroadcast(Intent(AppSettings.BROADCAST_MARK_POSITION))
        val settings = Intent(context, SettingsActivity::class.java)

        sharedPrefs!!.edit().putBoolean(AppSettings.SHOULD_REFRESH, false).commit()
        //        overridePendingTransition(R.anim.retro_fragment_open_enter, R.anim.anim_activity_stay);
        startActivity(settings)
    }

    fun setUpTweetTheme() {
        setUpTheme()
        setUpTweetTheme((context)!!, settings)
    }

    open fun setUpTheme() {
        if (settings!!.uiExtras && (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE ||
                    resources.getBoolean(R.bool.isTablet)) && !MainActivity.isPopup
        ) {
            translucent = true
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            try {
                val immersive = Settings.System.getInt(contentResolver, "immersive_mode")
                if (immersive == 1) {
                    translucent = false
                }
            } catch (e: Exception) {
            }
        } else if (settings!!.uiExtras && (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    !resources.getBoolean(R.bool.isTablet)) && !MainActivity.isPopup
        ) {
            translucent = true
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            try {
                val immersive = Settings.System.getInt(contentResolver, "immersive_mode")
                if (immersive == 1) {
                    translucent = false
                }
            } catch (e: Exception) {
            }
        } else {
            translucent = false
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        try {
            mDrawerToggle!!.syncState()
        } catch (e: Exception) {
            // landscape mode
        }
    }

    /**
     * 退出登录
     * @param useCurrent 是否只是退出当前账号
     */
    fun logoutFromTwitter(useCurrent: Boolean = true, currentIndex: Int = 1) {
        val currentAccount = if (useCurrent)
            sharedPrefs!!.getInt(AppSettings.CURRENT_ACCOUNT, 1)
        else currentIndex
        val login1 = sharedPrefs!!.getBoolean("is_logged_in_1", false)
        val login2 = sharedPrefs!!.getBoolean("is_logged_in_2", false)

        AccountSessionManager.getInstance()
            .removeAccount(sharedPrefs!!.getString("session_id_$currentAccount", null))
        // Delete the data for the logged out account
        val e = sharedPrefs!!.edit()
        e.remove("is_logged_in_$currentAccount")
        e.remove("new_notification")
        e.remove("new_retweets")
        e.remove("new_favorites")
        e.remove("new_followers")
        e.remove("new_quotes")
        e.remove("current_position_$currentAccount")
        e.remove("last_activity_refresh_$currentAccount")
        e.remove("original_activity_refresh_$currentAccount")
        e.remove("activity_follower_count_$currentAccount")
        e.remove("activity_latest_followers_$currentAccount")
        e.remove("last_direct_message_id_$currentAccount")
        e.remove("last_bookmarked_tweet_id_$currentAccount")
        e.remove("last_favorited_tweet_id_$currentAccount")

        e.commit()
        val homeSources = HomeDataSource.getInstance(context)
        homeSources.deleteAllTweets(currentAccount)
        val mentionsSources = MentionsDataSource.getInstance(context)
        mentionsSources.deleteAllTweets(currentAccount)
        val dmSource = DMDataSource.getInstance(context)
        dmSource.deleteAllTweets(currentAccount)
        val inters = InteractionsDataSource.getInstance(context)
        inters.deleteAllInteractions(currentAccount)
        val activity = ActivityDataSource.getInstance(context)
        activity.deleteAll(currentAccount)
        val favTweets = FavoriteTweetsDataSource.getInstance(context)
        favTweets.deleteAllTweets(currentAccount)
        val bookmarkedTweets = BookmarkedTweetsDataSource.getInstance(context)
        bookmarkedTweets.deleteAllTweets(currentAccount)
        try {
            val account1List1 = sharedPrefs!!.getLong("account_" + currentAccount + "_list_1", 0L)
            val account1List2 = sharedPrefs!!.getLong("account_" + currentAccount + "_list_2", 0L)
            val list = ListDataSource.getInstance(context)
            list.deleteAllTweets(account1List1)
            list.deleteAllTweets(account1List2)
        } catch (x: Exception) {
        }
        val suggestions = SearchRecentSuggestions(
            this,
            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE
        )
        suggestions.clearHistory()
        AppSettings.invalidate()
        if (currentAccount == 1 && login2) {
            e.putInt(AppSettings.CURRENT_ACCOUNT, 2).commit()
            finish()
            val next = Intent(context, MainActivity::class.java)
            startActivity(next)
        } else if (currentAccount == 2 && login1) {
            e.putInt(AppSettings.CURRENT_ACCOUNT, 1).commit()
            finish()
            val next = Intent(context, MainActivity::class.java)
            startActivity(next)
        } else { // only the one account
            e.putInt(AppSettings.CURRENT_ACCOUNT, 1).commit()
            finish()
            val login = Intent(context, MaterialLogin::class.java)
            startActivity(login)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthFailed(authFailedEvent: AuthFailedEvent?) {
        logoutFromTwitter(false, 1)
        logoutFromTwitter(false, 2)
    }

    public override fun onStart() {
        super.onStart()
        if (sharedPrefs!!.getBoolean("remake_me", false) && !MainActivity.isPopup) {
            sharedPrefs!!.edit().putBoolean("remake_me", false).commit()
            recreate()
            sharedPrefs!!.edit().putBoolean("launcher_frag_switch", false)
                .putBoolean(AppSettings.DONT_REFRESH, true).commit()
            return
        }

        // clear interactions notifications
        sharedPrefs!!.edit().putInt("new_retweets", 0).commit()
        sharedPrefs!!.edit().putInt("new_favorites", 0).commit()
        sharedPrefs!!.edit().putInt("new_followers", 0).commit()
        sharedPrefs!!.edit().putInt("new_quotes", 0).commit()
        sharedPrefs!!.edit().putString("old_interaction_text", "").commit()
        invalidateOptionsMenu()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onMultiWindowModeChanged(isMultiWindow: Boolean) {
        super.onMultiWindowModeChanged(isMultiWindow)
        Timber.v("is multi window: $isMultiWindow")
        if (isMultiWindow) {
        } else {
            recreate()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // cancels the notifications when the app is opened
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancelAll()
    }

    public override fun onResume() {
        super.onResume()
        cancelTeslaUnread()

        lottieVip!!.visibility =
            if (App.instance.checkSupporter(context, false) && !App.instance.isDroid) View.VISIBLE else View.GONE
        setRemoveAdButton()
        setViewVideoAdButton()

        // cancels the notifications when the app is opened
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancelAll()
        val e = sharedPrefs!!.edit()
        e.putInt("new_followers", 0)
        e.putInt("new_favorites", 0)
        e.putInt("new_retweets", 0)
        e.putString("old_interaction_text", "")
        e.commit()
        settings = AppSettings.getInstance(context)
        try {
            mDrawerLayout!!.setDrawerLockMode(
                NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                Gravity.RIGHT
            )
        } catch (x: Exception) {
            // no drawer?
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val DISMISS = 0
        val SEARCH = 1
        val COMPOSE = 2
        val NOTIFICATIONS = 3
        val DM = 4
        val SETTINGS = 5
        val TOFIRST = 6
        val TWEETMARKER = 7
        menu!!.getItem(TWEETMARKER).isVisible = false
        if (mDrawerLayout!!.isDrawerOpen(Gravity.RIGHT) || sharedPrefs!!.getBoolean(
                "open_interactions",
                false
            )
        ) {
            menu.getItem(DISMISS).isVisible = true
            menu.getItem(SEARCH).isVisible = false
            menu.getItem(COMPOSE).isVisible = false
            menu.getItem(DM).isVisible = false
            menu.getItem(TOFIRST).isVisible = false
            menu.getItem(NOTIFICATIONS).isVisible = false
        } else {
            menu.getItem(DISMISS).isVisible = false
            menu.getItem(SEARCH).isVisible = true
            menu.getItem(COMPOSE).isVisible = true
            menu.getItem(DM).isVisible = true
            menu.getItem(NOTIFICATIONS).isVisible = false
        }

        // to first button in overflow instead of the toast
        if (MainDrawerArrayAdapter.current > adapter!!.pageTypes.size || (settings!!.uiExtras && settings!!.useToast)) {
            menu.getItem(TOFIRST).isVisible = false
        } else {
            menu.getItem(TOFIRST).isVisible = true
        }
        if (MainActivity.isPopup) {
            menu.getItem(SETTINGS).isVisible = false // hide the settings button if the popup is up
            menu.getItem(SEARCH).isVisible = false // hide the search button in popup

            // disable the left drawer so they can't switch activities in the popup.
            // causes problems with the layouts
            mDrawerLayout!!.setDrawerLockMode(
                NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                Gravity.LEFT
            )
            actionBar!!.setDisplayShowHomeEnabled(false)
            actionBar!!.setDisplayHomeAsUpEnabled(false)
            actionBar!!.setHomeButtonEnabled(false)
            if (toolbar != null) {
                toolbar!!.navigationIcon = null
            }
        }
        noti = menu.getItem(NOTIFICATIONS)
        if (resources.getBoolean(R.bool.options_drawer)) {
            menu.getItem(SETTINGS).isVisible = false
        }
        if (InteractionsDataSource.getInstance(context)
                .getUnreadCount(settings!!.currentAccount) > 0
        ) {
            setNotificationFilled(true)
        } else {
            setNotificationFilled(false)
        }
        menu.getItem(DM).isVisible = false
        menu.getItem(DISMISS).isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }

    var noti: MenuItem? = null
    fun setNotificationFilled(isFilled: Boolean) {
        if (isFilled) {
            noti!!.icon = AppCompatResources.getDrawable(
                this,
                R.drawable.ic_round_notifications_24
            )
        } else {
            noti!!.icon = AppCompatResources.getDrawable(
                this,
                R.drawable.ic_round_notifications_none_24
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (mDrawerLayout!!.isDrawerOpen(Gravity.LEFT)) {
                    mDrawerLayout!!.closeDrawer(Gravity.LEFT)
                } else {
                    mDrawerLayout!!.openDrawer(Gravity.LEFT)
                }
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_search -> {
                searchUtils!!.showSearchView()
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_compose -> {
                val compose = Intent(context, ComposeActivity::class.java)
                sharedPrefs!!.edit().putBoolean("from_notification_bool", false).commit()
                startActivity(compose)
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_direct_message -> {
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_settings -> {
                context!!.sendBroadcast(Intent(AppSettings.BROADCAST_MARK_POSITION))
                val settings = Intent(context, SettingsActivity::class.java)
                //                finish();
                sharedPrefs!!.edit().putBoolean(AppSettings.SHOULD_REFRESH, false).commit()
                //overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);
                startActivity(settings)
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_dismiss -> {
                dismissNotifications()
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_notifications -> {
                if (mDrawerLayout!!.isDrawerOpen(Gravity.LEFT)) {
                    mDrawerLayout!!.closeDrawer(Gravity.LEFT)
                }
                if (mDrawerLayout!!.isDrawerOpen(Gravity.RIGHT)) {
                    mDrawerLayout!!.closeDrawer(Gravity.RIGHT)
                } else {
                    mDrawerLayout!!.openDrawer(Gravity.RIGHT)
                }
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_to_first -> {
                context!!.sendBroadcast(Intent(IntentConstant.TOP_TIMELINE_ACTION))
                return super.onOptionsItemSelected(item)
            }
            R.id.menu_tweetmarker -> {
                context!!.sendBroadcast(Intent(IntentConstant.TWEETMARKER_ACTION))
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * 激励广告
     */
    private fun setViewVideoAdButton() {
        if ((App.instance.checkSupporter(this, false)
                    && !App.instance.temporarySupporter())
            || !BasePreferenceUtil.isRewardCanShowToday()
            || App.instance.isDroid
        ) {
            viewVideoAdIv.visibility = View.GONE
        } else {
            viewVideoAdIv.visibility = View.VISIBLE
            viewVideoAdIv.setOnClickListener {

                if (BasePreferenceUtil.firstToViewVideoAd) {
                    AccentMaterialDialog(
                        this,
                        R.style.MaterialAlertDialogTheme
                    )
                        .setTitle(R.string.rewarded_title)
                        .setMessage(R.string.rewarded_ad_one_hour_tip)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setNeutralButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                            showRewardedAd()
                        }
                        .show()
                    BasePreferenceUtil.firstToViewVideoAd = false
                } else {
                    showRewardedAd()
                }


            }
        }
    }

    private fun showRewardedAd() {

        RewardedAdManager.showRewardedVideo(this, object : OnUserEarnedRewardListener {
            override fun onUserEarnedReward() {
                BasePreferenceUtil.rewardAdValidTime = System.currentTimeMillis()
                setRemoveAdButton()
                setViewVideoAdButton()
                EventBus.getDefault().post(
                    RemoveAdsPurchaseEvent()
                )
            }

            override fun onClosed(isEarned: Boolean) {
                if (isEarned) {
                    TopSnackbarUtil.showSnack(
                        context,
                        getString(
                            R.string.rewarded_locked, EntityDateUtils.timeStamp2Date(
                                BasePreferenceUtil
                                    .rewardAdValidTime, "yyyy-MM-dd HH:mm"
                            )
                        ),
                        Toast.LENGTH_LONG
                    )
                }
            }

        })

    }

    private fun setRemoveAdButton() {
        if (App.instance.isAdBlockUser()) {
            removeAdIv.visibility = View.GONE
        } else {
            removeAdIv.visibility = View.VISIBLE
            removeAdIv.setOnClickListener {
                val strList = arrayListOf(
                    getString(R.string.remove_ads_modest),
                    getString(R.string.remove_ads_suggested),
                    getString(R.string.remove_ads_generous)
                )
                val skuList = GoRouter.getInstance().getService(GooglePayService::class.java)!!.getRemoveAdsId()
                val selectedIndex = if (skuList.size > 2) skuList.size - 2 else 0

                SingleListDialogFragmentCompat.getInstance(
                    selectedIndex,
                    strList.toArray(arrayOf<String>()),
                    { dialog, which ->
                        val selectedRemoveAdSku = skuList[which]
                        supporterManager.supporterInAppItem.subscribeOn(rx.schedulers.Schedulers.io())
                            .observeOn(
                                rx.android.schedulers.AndroidSchedulers.mainThread()
                            ).subscribe(
                                { skuDetails: List<SkuDetails> ->
                                    mDrawerLayout.closeDrawer(Gravity.LEFT)
                                    for (detail in skuDetails) {
                                        if (selectedRemoveAdSku == detail.sku) {
                                            supporterManager.becomeInAppSubSupporter(
                                                context,
                                                detail,
                                                GooglePlayInAppTable.TYPE_REMOVE_ADS
                                            ).subscribeOn(rx.schedulers.Schedulers.io()).observeOn(
                                                rx.android.schedulers.AndroidSchedulers.mainThread()
                                            )
                                                .subscribe({ aBoolean: Boolean ->
                                                    if (aBoolean) {
                                                        TopSnackbarUtil.showSnack(
                                                            context,
                                                            R.string.thanks_purchase,
                                                            Toast.LENGTH_LONG
                                                        )
                                                        setRemoveAdButton()
                                                        EventBus.getDefault().post(
                                                            RemoveAdsPurchaseEvent()
                                                        )
                                                    }
                                                }) { throwable: Throwable? ->
                                                    Timber.d(
                                                        throwable,
                                                        "There was an error while purchasing remove ads supporter item"
                                                    )
                                                }
                                        } else {
                                            Timber.e("unknown remove ads sku %s", detail.sku)
                                        }
                                    }
                                }) { throwable: Throwable? ->
                                Timber.e(
                                    throwable, "There was an error while retrieving " +
                                            "remove ads supporter sub item"
                                )
                            }
                    }, getString(R.string.remove_ads_tip)
                ).show(supportFragmentManager, null)
            }
        }
    }

    private fun dismissNotifications() {
        val data = InteractionsDataSource.getInstance(context)
        data.markAllRead(settings!!.currentAccount)
        mDrawerLayout!!.closeDrawer(Gravity.RIGHT)
        val c = data.getUnreadCursor(settings!!.currentAccount)
        notificationAdapter = InteractionsCursorAdapter(context, c)
        notificationList!!.adapter = notificationAdapter
        try {
            if (c.count == 0 && noInteractions!!.visibility != View.VISIBLE) {
                noInteractions!!.visibility = View.VISIBLE
                noInteractions!!.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.fade_in
                    )
                )
            } else if (noInteractions!!.visibility != View.GONE) {
                noInteractions!!.visibility = View.GONE
                noInteractions!!.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.fade_out
                    )
                )
            }
        } catch (e: Exception) {
        }
    }

    fun toDP(px: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            px.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    fun cancelTeslaUnread() {
        ShortcutBadger.removeCount(context)
        TimeoutThread(object : Runnable {
            override fun run() {
                try {
                    val cv = ContentValues()
                    cv.put(
                        "tag",
                        "allen.town.focus.twitter/allen.town.focus.twitter.ui.MainActivity"
                    )
                    cv.put("count", 0) // back to zero
                    context!!.contentResolver.insert(
                        Uri
                            .parse("content://com.teslacoilsw.notifier/unread_count"),
                        cv
                    )
                } catch (ex: IllegalArgumentException) {
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }).start()
    }

    var abOffset = -1
    override fun showBars() {
        navigationView.show()
        if (abOffset == -1) {
            abOffset = getStatusBarHeight((context)!!) + getActionBarHeight(
                (context)!!
            )
        }
        if (toolbar == null || toolbar!!.visibility == View.VISIBLE) {
            return
        }
        if (toolbar != null && !MainActivity.isPopup) {
            if (toolBarVis == null) {
                toolBarVis = Handler()
            }
            toolBarVis!!.removeCallbacksAndMessages(null)
            toolbar!!.visibility = View.VISIBLE
            val showToolbar = ValueAnimator.ofInt(-1 * abOffset, 0)
            showToolbar.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    val `val` = animation.animatedValue as Int
                    toolbar!!.translationY = `val`.toFloat()
                }
            })
            val showToolbarAlpha = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 0f, 1f)
            showToolbar.duration = ANIM_DURATION.toLong()
            showToolbarAlpha.duration = ANIM_DURATION.toLong()
            showToolbar.interpolator = INTERPOLATOR
            showToolbarAlpha.interpolator = INTERPOLATOR

            //showToolbarAlpha.setEvaluator(EVALUATOR);
            showToolbar.start()
            showToolbarAlpha.start()
        }
    }

    private var tranparentSystemBar = -1
    private val statusColor = -1
    private val EVALUATOR = ArgbEvaluator()
    var toolBarVis: Handler? = null
    override fun hideBars() {
        if (settings!!.swipeHideNavigation) {
            navigationView.hide()
        }
        if (toolbar == null || toolbar!!.visibility == View.GONE) {
            Log.v("talon_app_bar", "toolbar is null")
            return
        }

        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            toolbar.setElevation(0);
        }*/if (toolbar != null && !MainActivity.isPopup) {
            val hideToolbar = ValueAnimator.ofInt(0, -1 * abOffset)
            hideToolbar.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    val `val` = animation.animatedValue as Int
                    toolbar!!.translationY = `val`.toFloat()
                }
            })
            val hideToolbarAlpha = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 1f, 0f)
            hideToolbar.duration = ANIM_DURATION.toLong()
            hideToolbarAlpha.duration = ANIM_DURATION.toLong()
            hideToolbar.interpolator = INTERPOLATOR
            hideToolbarAlpha.interpolator = INTERPOLATOR

            //hideToolbarAlpha.setEvaluator(EVALUATOR);
            hideToolbar.start()
            hideToolbarAlpha.start()
            if (toolBarVis == null) {
                toolBarVis = Handler()
            }
            toolBarVis!!.removeCallbacksAndMessages(null)
            toolBarVis!!.postDelayed(object : Runnable {
                override fun run() {
                    toolbar!!.visibility = View.GONE
                }
            }, ANIM_DURATION.toLong())
        }
    }

    override fun onBackPressed() {
        try {
            if (searchUtils!!.isShowing) {
                searchUtils!!.hideSearch(true)
            } else if (mDrawerLayout!!.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout!!.closeDrawer(GravityCompat.START)
            } else if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout!!.closeDrawer(GravityCompat.END)
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmField
        var settings: AppSettings? = null

        @JvmField
        var mViewPager: ViewPager? = null

        @JvmField
        var translucent = false

        @JvmField
        var canSwitch = true
        var navBarHeight = 0

        @JvmField
        var oldInteractions: TextView? = null

        @JvmField
        var hasToolbar = false
        val SETTINGS_RESULT = 101

        @JvmField
        val ANIM_DURATION = 350
        var INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()
    }
}