package allen.town.focus.twitter.activities

import allen.town.focus.twitter.R
import allen.town.focus.twitter.di.Injectable
import allen.town.focus.twitter.utils.Utils
import allen.town.focus_common.activity.ClearAllActivityInterface
import allen.town.focus_common.extensions.*
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.LanguageContextWrapper
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.os.ConfigurationCompat
import code.name.monkey.appthemehelper.ATH
import code.name.monkey.appthemehelper.constants.ThemeConstants
import code.name.monkey.appthemehelper.util.ATHUtil.resolveColor
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.util.theme.ThemeManager
import com.klinker.android.peekview.PeekViewActivity
import java.lang.ref.WeakReference
import java.util.*

/**
 * A helper class to facilitate the usage of very light colored Toolbars, where the text will need to be
 * changed to dark.
 */
@SuppressLint("Registered")
open class WhiteToolbarActivity : PeekViewActivity(),ClearAllActivityInterface {
    private var toolbar: Toolbar? = null
    private var updateTime: Long = -1
    private val entranceActivityNames = HashSet<String>()

    open fun getToolbarBackgroundColor(toolbar: Toolbar?): Int {
        return if (toolbar != null) {
            resolveColor(toolbar.context, R.attr.colorSurface)
        } else Color.BLACK
    }

    private fun updateTheme() {
        Utils.setUpTheme(this)
        //2023/1/16测试 android13:
        // 不加setDefaultNightMode这行，系统是浅色主题，手动选择app深色主题有问题，系统是浅色，手动选择app深色主题无效，FollowSystem的深色主题是正常的
        // 加setDefaultNightMode这行如果没有这个判断，系统是深色主题，app从浅色主题修改为FollowSystem无效，并且当app为FollowSystem时并且不是md3，从浅色切换到深色有问题;加了判断有效但是返回主界面不正常，退出再进就正常
        if(ThemeConstants.THEME_AUTO_VALUE != BasePreferenceUtil.getGeneralThemeValueOriginal()){
        //结合SettingsActivity返回后重新创建MainActivity就一切正常
            AppCompatDelegate.setDefaultNightMode(ThemeManager.getNightMode(application))
        }
        //不确定影响，暂时不添加这个分支。唯一的问题是当系统为深色时，如果app从浅色修改到FollowSystem无效，必须杀进程
            else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val toolbar = getATHToolbar()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        this.toolbar = toolbar
        super.setSupportActionBar(toolbar)
    }

    protected open fun getATHToolbar(): Toolbar? {
        return toolbar
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        updateTheme()
        super.onCreate(savedInstanceState)
        setEdgeToEdgeOrImmersive(R.id.status_bar, false)
        setLightNavigationBarAuto()
        setLightStatusBarAuto(surfaceColor())
        if (VersionUtils.hasQ()) {
            window.decorView.isForceDarkAllowed = false
        }

        updateTime = System.currentTimeMillis()
        activities[hashCode()] =
            WeakReference(this)
    }


    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (ATH.didThemeValuesChange(this, updateTime)) {
            onThemeChanged()
        }
    }

    private fun onThemeChanged() {
        postRecreate()
    }

    fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler().post { recreate() }
    }

    /**
     * 设置主activity，退出时将关闭所有activity
     *
     * @param entranceActivityName
     */
    protected fun addEntranceActivityName(entranceActivityName: String) {
        entranceActivityNames.add(entranceActivityName)
    }

    protected fun removeEntranceActivityName(entranceActivityName: String) {
        entranceActivityNames.remove(entranceActivityName)
    }

    override fun onDestroy() {
        super.onDestroy()
        val removed = activities.remove(this.hashCode())!!
    }

    override fun finish() {
        super.finish()
//        if (entranceActivityNames.contains(javaClass.simpleName)) {
//            clearAllAppcompactActivities(false)
//        }
    }

    /**
     * finish all activitys
     */
    override fun clearAllAppcompactActivities(recreate: Boolean) {
        val leftActivities = HashMap(activities)
        val N = leftActivities.size
        if (DEBUG) {
            Log.d(THIS_FILE, "left activities: $N")
        }
        val iter: Iterator<WeakReference<AppCompatActivity>> = leftActivities.values.iterator()
        var leftActivity: AppCompatActivity?
        var ref: WeakReference<AppCompatActivity>
        var isFinishing = false
        while (iter.hasNext()) {
            ref = iter.next()
            leftActivity = if (ref != null) ref.get() else null
            if (leftActivity != null) {
                isFinishing = leftActivity.isFinishing
                if (!isFinishing) {
                    if (recreate) {
                        ActivityCompat.recreate(leftActivity)
                    } else {
                        leftActivity.finish()
                    }
                }
                if (DEBUG) {
                    Log.d(THIS_FILE, "left activity: $leftActivity is finishing? $isFinishing")
                }
            }
        }
        if (DEBUG) {
            Log.d(THIS_FILE, "clearAllBasicAppComapctActivites DONE!!!")
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val code = BasePreferenceUtil.languageCode
        val locale = if (code == "auto") {
            // Get the device default locale
            ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
        } else {
            Locale.forLanguageTag(code)
        }
        super.attachBaseContext(LanguageContextWrapper.wrap(newBase, locale))
        //和Android APP Bundle有关，加载资源用的，对apk方式有没有影响
        installSplitCompat()
    }

    companion object {
        private const val DEBUG = false
        private const val THIS_FILE = "BasicAppComapctActivity"
        private val activities = HashMap<Int, WeakReference<AppCompatActivity>>()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}