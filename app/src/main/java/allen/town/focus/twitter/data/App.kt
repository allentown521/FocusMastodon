package allen.town.focus.twitter.data

import allen.town.focus.twitter.BuildConfig
import allen.town.focus.twitter.di.AppInjector
import allen.town.focus_common.BaseApplication
import allen.town.focus_common.http.LeanHttpClient
import allen.town.focus_common.http.bean.LeanAdmobBean
import allen.town.focus_common.http.bean.LeanAdmobContentBean
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.JsonHelper
import allen.town.focus_common.util.Timber
import allen.town.focus_purchase.iap.SupporterManagerWrap
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.os.BuildCompat
import androidx.multidex.MultiDex
import com.github.ajalt.reprint.core.Reprint
import com.google.gson.reflect.TypeToken
import allen.town.focus.twitter.settings.Prefs
import allen.town.focus.twitter.settings.font.FontCache.cache
import allen.town.focus.twitter.utils.DynamicShortcutUtils
import allen.town.focus.twitter.utils.EmojiUtils
import allen.town.focus.twitter.utils.NavigationUtil
import allen.town.focus.twitter.utils.NotificationChannelUtil
import allen.town.focus.twitter.utils.text.EmojiInitializer.initializeEmojiCompat
import allen.town.focus_common.crash.Crashlytics
import allen.town.focus_common.http.util.LogUtil
import code.name.monkey.appthemehelper.ThemeStore
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.android.schedulers.AndroidSchedulers
import me.grishka.appkit.utils.V
import rx.schedulers.Schedulers
import xyz.klinker.android.drag_dismiss.util.AndroidVersionUtils
import javax.inject.Inject

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
 */   class App : BaseApplication() , HasAndroidInjector {
    companion object {
        @JvmField
        var DATA_USED: Long = 0
        @JvmStatic
        lateinit var instance: App
            private set
        @JvmStatic
        fun getPrefs(context: Context): Prefs? {
            val instance = getInstance(context)
            if (instance.prefs == null) {
                instance.prefs = Prefs(instance)
            }
            return instance.prefs
        }

        fun getInstance(context: Context): App {
            return context.applicationContext as App
        }


        init {
            if (AndroidVersionUtils.isAndroidQ()) {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        Reprint.initialize(this)
        instance = this
        // okhttp日志拦截 初始化Looger工具，https://blog.csdn.net/NewActivity/article/details/104321397
        LogUtil.init(BuildConfig.DEBUG)
        V.setApplicationContext(this)
        AppInjector.init(this)
        //放最前面
        checkPurchase()
        initializeEmojiCompat(this)
        runBackgroundSetup()
        //缓存第三方字体
        cache(this)
        Crashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BasePreferenceUtil.isDisableFirebase)
        getAdmobAdInfo()
        doAppUpgrade(1)

    }

    /**
     * 升级app后可以做的一些事情
     */
    private fun doAppUpgrade(version: Int){
        val lastVersion = ThemeStore.lastAppVersionCode(this)
        if(lastVersion < 1){
            //do something upgrade here
        }
        ThemeStore.setLastAppVersionCode(this,version)
    }

    /**
     * 不是订阅用户跳转付费界面
     *
     * @return true 代表订阅了
     */
    fun checkSupporter(context: Context?, gotoPro: Boolean = true): Boolean {
        if (!isSupporter && !temporarySupporter() && gotoPro) {
            context?.run {
                NavigationUtil.goToProVersion(context)
            }
        }
        return isSupporter || temporarySupporter() || isDroid
    }

    private fun getAdmobAdInfo() {
        if (!isAlipay && !isDroid && BasePreferenceUtil.needCheckAdmobInfo()) {
            Timber.d("getAdmobAdInfo")
            LeanHttpClient.getAdmob("636cfe205fee9f4325d1147e")
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { leanAdmobBean: LeanAdmobBean? ->
                        BasePreferenceUtil.lastAdmobCheckTime = System.currentTimeMillis()
                        leanAdmobBean?.run {
                            val content = leanAdmobBean.content
                            content?.run {
                                val leanAdmobContentBeanList: ArrayList<LeanAdmobContentBean>? =
                                    JsonHelper.parseObjectList(
                                        this,
                                        object : TypeToken<ArrayList<LeanAdmobContentBean>>() {
                                        }.type
                                    )
                                leanAdmobContentBeanList?.run {
                                    for (leanAdmobContentBean in this) {
                                        BasePreferenceUtil.setStringValue(
                                            leanAdmobContentBean.type,
                                            leanAdmobContentBean.id
                                        )
                                    }
                                }
                            }
                        }
                    },
                    { error: Throwable? ->
                        Timber.e(
                            "failed to getAdmobAdInfo ${Log.getStackTraceString(error)}"
                        )
                    }
                )
        }
    }

    private fun checkPurchase() {
        //查询是否是订阅用户
        val supporterManager = SupporterManagerWrap.getSupporterManger(this)
        supporterManager.isSupporter.observeOn(Schedulers.immediate())
            .subscribe({ aBoolean: Boolean? -> supporterManager.dispose() }) { throwable: Throwable? ->
                //必须实现onError方法否则会抛异常
                supporterManager.dispose()
                Timber.w(throwable, "checkPurchase")
            }
        if (!isSupporter) {
            //不是订阅用户才会查询是否去除了广告
            Timber.i("query if remove ads")
            //不要共用一个supporterManager，那样容易出问题
            val inAppSupporterManager = SupporterManagerWrap.getSupporterManger(this)
            inAppSupporterManager.isRemoveAdsSupporter.observeOn(Schedulers.immediate())
                .subscribe({ aBoolean: Boolean? -> inAppSupporterManager.dispose() }) { throwable: Throwable? ->
                    //必须实现onError方法否则会抛异常
                    inAppSupporterManager.dispose()
                    Timber.w(throwable, "checkRemoveAdsPurchase")
                }
        }
    }


    var prefs: Prefs? = null
    fun runBackgroundSetup() {
        if ("robolectric" != Build.FINGERPRINT && BuildCompat.isAtLeastNMR1()) {
            Thread {
                try {
                    NotificationChannelUtil.createNotificationChannels(this@App)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                EmojiUtils.init(this@App)
                try {
                    DynamicShortcutUtils(this@App).buildProfileShortcut()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>
    override fun androidInjector() = androidInjector

}