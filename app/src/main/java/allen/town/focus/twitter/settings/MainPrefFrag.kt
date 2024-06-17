package allen.town.focus.twitter.settings

import allen.town.focus.twitter.BuildConfig
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.main_fragments.other_fragments.AboutFragment
import allen.town.focus.twitter.data.App
import allen.town.focus.twitter.event.PurchaseEvent
import allen.town.focus_common.ui.customtabs.BrowserLauncher
import allen.town.focus_common.util.Constants
import allen.town.focus_common.util.LogUtils.delegateFeedback
import allen.town.focus_common.util.PackageUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.bytehamster.lib.preferencesearch.SearchPreference
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
 */   class MainPrefFrag : AbsSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)
        findPreference<Preference>(PREF_BUY)!!.isVisible = !App.instance.checkSupporter(context,false)
        findPreference<Preference>(PREF_DONATE)!!.isVisible = App.instance.isDroid
        setClicks()
        setupSearch()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPurchaseChange(purchaseEvent: PurchaseEvent?) {
        findPreference<Preference>(PREF_BUY)!!.isVisible = false
    }

    override fun onStart() {
        super.onStart()
        (activity as SettingsActivity?)!!.setTitle(R.string.Focus_for_Mastodon_settings)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    var titles = arrayOf(
        "app_style",
        "widget_customization",
        "swipable_pages_and_app_drawer",
        "background_refreshes",
        "notifications",
        "data_saving_options",
        "location",
        "mute_management",
        "app_memory",
        "other_options",
        "pref_report_bug",
        "pref_donate",
        "pref_about"
    )

    fun setClicks() {
        for (i in titles.indices) {
            val p = findPreference<Preference>(titles[i])
            p!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                if (titles[i] == "pref_report_bug") {
                    delegateFeedback(
                        "Focus_for_Mastodon", BuildConfig.VERSION_NAME, Constants.PRODUCT_EMAIL,
                        activity!!, PackageUtils.getPackageName(context) + ".provider"
                    )
                } else if (titles[i] == "pref_about") {
                    AboutFragment().show(activity!!.supportFragmentManager, null)
                } else if (titles[i] == "pref_donate") {
                    BrowserLauncher.openUrl(getActivity(), "https://ko-fi.com/focusapps")
                } else {
                    (activity as SettingsActivity?)!!.openScreen(i, preference.title.toString())
                }
                false
            }
        }
    }

    private fun setupSearch() {
        val searchPreference = findPreference<SearchPreference>("searchPreference")
        val config = searchPreference!!.searchConfiguration
        config.setActivity((activity as AppCompatActivity?)!!)
        config.setFragmentContainerViewId(R.id.settings_content)
        config.setBreadcrumbsEnabled(true)
        config.index(R.xml.settings_app_style)
            .addBreadcrumb(getString(R.string.app_style))
        config.index(R.xml.settings_widget_customization)
            .addBreadcrumb(getString(R.string.widget_settings))
        config.index(R.xml.settings_advanced_app_style)
            .addBreadcrumb(getString(R.string.app_style))
            .addBreadcrumb(getString(R.string.advanced_options))
        config.index(R.xml.settings_advanced_background_refreshes)
            .addBreadcrumb(getString(R.string.sync_settings))
            .addBreadcrumb(getString(R.string.advanced_options))
        config.index(R.xml.settings_swipable_pages_and_app_drawer)
            .addBreadcrumb(getString(R.string.app_drawer))
        config.index(R.xml.settings_background_refreshes)
            .addBreadcrumb(getString(R.string.sync_settings))
        config.index(R.xml.settings_notifications)
            .addBreadcrumb(getString(R.string.notification_settings))
        config.index(R.xml.settings_data_savings)
            .addBreadcrumb(getString(R.string.data_saving_settings))
        config.index(R.xml.settings_location)
            .addBreadcrumb(getString(R.string.location))
        config.index(R.xml.settings_mutes)
            .addBreadcrumb(getString(R.string.manage_mutes))
        config.index(R.xml.settings_app_memory)
            .addBreadcrumb(getString(R.string.memory_manage))
        config.index(R.xml.settings_other_options)
            .addBreadcrumb(getString(R.string.other_options))
    }

    override fun invalidateSettings() {}

    companion object {
        private const val PREF_BUY = "buyPreference"
        private const val PREF_DONATE = "pref_donate"
    }
}