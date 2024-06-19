package allen.town.focus.twitter.utils

import allen.town.focus.twitter.R
import allen.town.focus.twitter.api.requests.statuses.UploadAttachment
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus_common.util.BasePreferenceUtil
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.transition.ChangeTransform
import android.transition.Transition
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.Window
import code.name.monkey.retromusic.extensions.generalThemeValue
import code.name.monkey.retromusic.util.theme.ThemeMode
import twitter4j.UploadedMedia
import java.io.File
import java.util.*


object Utils {
    const val PACKAGE_NAME = "allen.town.focus.twitter"


    private const val SECOND_MILLIS = 1000
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    @JvmStatic
    fun getTimeAgo(time: Long, context: Context, allowLongFormat: Boolean): String {
        var time = time
        if (allowLongFormat && AppSettings.getInstance(context).revampedTweets()) {
            return getTimeAgoLongFormat(time, context)
        }
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000
        }
        val now = currentTime
        if (time > now || time <= 0) {
            return "+1d"
        }
        val diff = now - time
        return if (diff < MINUTE_MILLIS) {
            "${diff / SECOND_MILLIS}s"
        } else if (diff < 2 * MINUTE_MILLIS) {
            "1m"
        } else if (diff < 50 * MINUTE_MILLIS) {
            "${diff / MINUTE_MILLIS}m"
        } else if (diff < 90 * MINUTE_MILLIS) {
            "1h"
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1L) "1h" else "${diff / HOUR_MILLIS}h"
        } else if (diff < 48 * HOUR_MILLIS) {
            "1d"
        } else {
            "${diff / DAY_MILLIS}d"
        }
    }

    private fun getTimeAgoLongFormat(time: Long, context: Context): String {
        var time = time
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000
        }
        val now = currentTime
        if (time > now || time <= 0) {
            return "+1d"
        }
        val diff = now - time
        return if (diff < MINUTE_MILLIS) {
            context.getString(
                R.string.seconds_ago,
                diff / SECOND_MILLIS
            )
        } else if (diff < 2 * MINUTE_MILLIS) {
            context.getString(R.string.min_ago, 1)
        } else if (diff < 50 * MINUTE_MILLIS) {
            context.getString(
                R.string.mins_ago,
                diff / MINUTE_MILLIS
            )
        } else if (diff < 90 * MINUTE_MILLIS) {
            context.getString(R.string.hour_ago, 1)
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1L) context.getString(
                R.string.hour_ago,
                1
            ) else context.getString(
                R.string.new_hours_ago,
                diff / HOUR_MILLIS
            )
        } else if (diff < 48 * HOUR_MILLIS) {
            context.getString(R.string.day_ago, 1)
        } else {
            context.getString(
                R.string.new_days_ago,
                diff / DAY_MILLIS
            )
        }
    }

    private val c = charArrayOf('K', 'M', 'B', 'T')

    @JvmStatic
    fun coolFormat(n: Double, iteration: Int): String {
        val d = n.toLong() / 100 / 10.0
        val isRound =
            d * 10 % 10 == 0.0 //true if the decimal part is equal to 0 (then it's trimmed anyway)
        return if (d < 1000) //this determines the class, i.e. 'k', 'm' etc
            (if (d > 99.9 || isRound || !isRound && d > 9.99) //this decides whether to trim the decimals
                d.toInt() * 10 / 10 else d.toString() + "" // (int) d * 10 / 10 drops the decimal
                    ).toString() + "" + c[iteration] else coolFormat(d, iteration + 1)
    }

    fun getTranslateURL(lang: String): String {
        return "https://translate.google.com/m/translate#auto|" +
                lang +
                "|"
    }

    private val currentTime: Long
        private get() = Date().time

    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun getActionBarHeight(context: Context): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            var value = TypedValue.complexToDimensionPixelSize(
                tv.data,
                context.resources.displayMetrics
            )
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                value += toDP(6, context)
            }
            value
        } else {
            toDP(48, context)
        }
    }

    @JvmStatic
    fun getNavBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        } else if (hasNavBar(context)) {
            toDP(48, context)
        }
        return result
    }

    @JvmStatic
    fun hasNavBar(context: Context): Boolean {
        return when (AppSettings.getInstance(context).navBarOption) {
            AppSettings.NAV_BAR_AUTOMATIC -> {
                val window: Window? = (context as? Activity)?.window
                window?.run {
                    val decorView: View = window.decorView
                    val rect = Rect()
                    decorView.getWindowVisibleDisplayFrame(rect)
                    val outMetrics = DisplayMetrics()
                    val windowManager = window.windowManager
                    windowManager.defaultDisplay.getRealMetrics(outMetrics)
                    return (outMetrics.heightPixels - rect.bottom) > 0
                } ?: false

            }
            AppSettings.NAV_BAR_PRESENT -> true
            AppSettings.NAV_BAR_NONE -> false
            else -> true
        }
    }

    // true if on mobile data
    // false otherwise
    @JvmStatic
    fun getConnectionStatus(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val activeNetwork = cm.activeNetworkInfo
            if (null != activeNetwork) {
                if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) return false
                if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) return true
            }
        } catch (e: Exception) {
        }
        return false
    }

    @JvmStatic
    fun hasInternetConnection(context: Context): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting
    }

    @JvmStatic
    fun toDP(px: Int, context: Context): Int {
        return try {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                px.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        } catch (e: Exception) {
            px
        }
    }

    @JvmStatic
    fun toPx(dp: Int, context: Context): Int {
        val r = context.resources
        return try {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics)
                .toInt()
        } catch (e: Exception) {
            dp
        }
    }

    fun isPackageInstalled(context: Context, targetPackage: String?): Boolean {
        val pm = context.packageManager
        try {
            val info = pm.getPackageInfo(targetPackage!!, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return true
    }

    fun setSharedContentTransition(context: Context, trans: Transition?) {
        val activity = context as Activity

        // inside your activity (if you did not enable transitions in your theme)
        try {
            activity.window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            activity.window.allowEnterTransitionOverlap = true
            activity.window.allowReturnTransitionOverlap = true
        } catch (e: Exception) {
        }
    }

    @JvmStatic
    fun setSharedContentTransition(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSharedContentTransition(context, ChangeTransform())
        }
    }

    @JvmStatic
    fun setUpTheme(context: Context, settings: AppSettings? = null) {
        setUpMainTheme(context)
    }


    @JvmStatic
    fun setUpMainTheme(context: Context, settings: AppSettings? = null) {
        val theme = if (BasePreferenceUtil.materialYou) {
            if (context.generalThemeValue === ThemeMode.BLACK) R.style.Theme_Focus_for_Mastodon_MD3_Base_Black else R.style.Theme_Focus_for_Mastodon_MD3_Base
        } else {
            val themeMode = context.generalThemeValue
            if (themeMode === ThemeMode.LIGHT) {
                R.style.Theme_Focus_for_MastodonLight_Main
            } else if (themeMode === ThemeMode.DARK) {
                R.style.Theme_Focus_for_MastodonDark_Main
            } else if (themeMode === ThemeMode.BLACK) {
                R.style.Theme_Focus_for_MastodonBlack_Main
            } else {
                R.style.Theme_Focus_for_MastodonLight_Main
            }
        }
        context.setTheme(theme)
    }

    /**
     * 忽略主题设置，直接使用暗色主题
     */
    @JvmStatic
    fun setUpMainDarkTheme(context: Context) {
        val theme = R.style.Theme_Focus_for_MastodonBlack_Main
        context.setTheme(theme)
        //这行会导致MainActivity重启
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    @JvmStatic
    fun setUpTweetTheme(context: Context, settings: AppSettings? = null) {
        setUpMainTheme(context)
    }

    @JvmStatic
    fun setUpProfileTheme(context: Context, settings: AppSettings? = null) {
        setUpMainTheme(context)
    }

    /**
     * 用户没有设置账号的背景图片就用app的设置
     * @param settings
     * @return
     */
    @JvmStatic
    fun getBackgroundUrlForTheme(settings: AppSettings?): String {
        return ""
    }

    @JvmStatic
    fun setActionBar(context: Context) {
        val settings = AppSettings.getInstance(context)
        if (settings.actionBar != null) {
            //Drawable back = settings.actionBar;
            try {
                (context as Activity).actionBar!!.setBackgroundDrawable(settings.actionBar)
            } catch (e: Exception) {
                // on the compose there isnt an action bar
            }
        }

        // we will only do this if it is specified with the function below
        //setWallpaper(settings, context);
    }


    val isAndroidN: Boolean
        @JvmStatic
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || Build.VERSION.CODENAME == "N"
    val isAndroidO: Boolean
        @JvmStatic
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.CODENAME == "O"
    val isAndroidP: Boolean
        @JvmStatic
        get() = Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 || Build.VERSION.CODENAME == "P"

    @JvmStatic
    fun setTaskDescription(activity: Activity) {

    }

    @JvmStatic
    fun isColorDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.30
    }

    @JvmStatic
    fun withImmutability(value: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or value
        } else {
            value
        }
    }

    @JvmStatic
    fun withMutability(value: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or value
        } else {
            value
        }
    }

    @JvmStatic
    fun uploadImage(context: Context, useSecond: Boolean, url: String): UploadedMedia {
        val uri = Uri.parse(url)
        val inputStream = context.contentResolver.openInputStream(uri)
        val attachment =
            if (useSecond) UploadAttachment(
                inputStream,
                uri
            ).execSecondAccountSync() else UploadAttachment(
                inputStream,
                uri
            ).execSync()
        return UploadedMedia(attachment)
    }

    @JvmStatic
    fun uploadAttachment(context: Context, useSecond: Boolean, file: File): UploadedMedia {
        val uri = Uri.fromFile(file)
        val inputStream = context.contentResolver.openInputStream(uri)
        val attachment =
            if (useSecond) UploadAttachment(
                inputStream,
                uri
            ).execSecondAccountSync() else UploadAttachment(
                inputStream,
                uri
            ).execSync()
        return UploadedMedia(attachment)
    }

    @JvmStatic
    fun uploadVideo(context: Context, useSecond: Boolean, uri: String): UploadedMedia {
        return uploadImage(context, useSecond, uri)
    }
}