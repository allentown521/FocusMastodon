package allen.town.focus.twitter.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import com.bytehamster.lib.preferencesearch.SearchPreferenceResult
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.MainActivity
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.utils.Utils.setUpMainTheme
import android.content.Intent

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
 */   class SettingsActivity : WhiteToolbarActivity(), SearchPreferenceResultListener {
    lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.invalidate()
        setUpTheme()
        setContentView(R.layout.settings_base)
        val settings = AppSettings.getInstance(this)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_content, MainPrefFrag(),FRAGMENT_TAG)
                .commit()
        }

        val ab = supportActionBar
        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.setDisplayShowHomeEnabled(true)
        ab.setTitle(R.string.Focus_for_Mastodon_settings)
        ab.setIcon(null)

        collapsingToolbarLayout = findViewById(R.id.collapsingToolbarLayout)
    }

    fun setTitle(title: String) {
        collapsingToolbarLayout.setTitle(title)
    }

    companion object {
        private const val FRAGMENT_TAG = "tag_preferences"
    }

    override fun setTitle(title: Int) {
        collapsingToolbarLayout.setTitle(getString(title))
    }

    fun setUpTheme() {
        val settings = AppSettings.getInstance(this)
        setUpMainTheme(this, settings)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        return super.onCreateOptionsMenu(menu)
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

    fun openScreen(screen: Int, title: String): PreferenceFragmentCompat? {
        val fragment = PrefFragment()
        val args = Bundle()
        args.putInt("position", screen)
        args.putString("title", title)
        fragment.arguments = args
        supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.retro_fragment_open_enter,
            R.anim.retro_fragment_open_exit,
            R.anim.retro_fragment_close_enter,
            R.anim.retro_fragment_close_exit
        ).replace(R.id.settings_content, fragment!!)
            .addToBackStack(title).commit()
        return fragment
    }

    fun openAdvanceScreen(screen: Int, title: String): PreferenceFragmentCompat? {
        val fragment = PrefFragmentAdvanced()
        val args = Bundle()
        args.putInt("position", screen)
        args.putString("title", title)
        fragment.arguments = args
        supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.retro_fragment_open_enter,
            R.anim.retro_fragment_open_exit,
            R.anim.retro_fragment_close_enter,
            R.anim.retro_fragment_close_exit
        ).replace(R.id.settings_content, fragment!!)
            .addToBackStack(title).commit()
        return fragment
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            AppSettings.invalidate()
            //important 不要删除，见WhiteToolbarActivity-setDefaultNightMode
        val main = Intent(this, MainActivity::class.java)
        startActivity(main)
            finish()
        } else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            var view = currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(this)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            supportFragmentManager.popBackStack()
        }

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.anim_activity_stay, R.anim.retro_fragment_close_exit)
    }

    override fun onSearchResultClicked(result: SearchPreferenceResult) {
        var fragment: PreferenceFragmentCompat? = null
        if (result.resourceFile == R.xml.settings_app_style) {
            fragment = openScreen(0, getString(R.string.app_style))
        } else if (result.resourceFile == R.xml.settings_widget_customization) {
            fragment = openScreen(1, getString(R.string.widget_settings))
        } else if (result.resourceFile == R.xml.settings_swipable_pages_and_app_drawer) {
            fragment = openScreen(2, getString(R.string.app_drawer))
        } else if (result.resourceFile == R.xml.settings_background_refreshes) {
            fragment = openScreen(3, getString(R.string.sync_settings))
        } else if (result.resourceFile == R.xml.settings_notifications) {
            fragment = openScreen(4, getString(R.string.notification_settings))
        } else if (result.resourceFile == R.xml.settings_data_savings) {
            fragment = openScreen(5, getString(R.string.data_saving_settings))
        } else if (result.resourceFile == R.xml.settings_location) {
            fragment = openScreen(6, getString(R.string.location_settings))
        } else if (result.resourceFile == R.xml.settings_mutes) {
            fragment = openScreen(7, getString(R.string.manage_mutes))
        } else if (result.resourceFile == R.xml.settings_app_memory) {
            fragment = openScreen(8, getString(R.string.memory_manage))
        } else if (result.resourceFile == R.xml.settings_other_options) {
            fragment = openScreen(9, getString(R.string.other_options))
        } else if (result.resourceFile == R.xml.settings_advanced_app_style) {
            fragment = openAdvanceScreen(0, getString(R.string.advanced_settings))
        } else if (result.resourceFile == R.xml.settings_advanced_background_refreshes) {
            fragment = openAdvanceScreen(3, getString(R.string.advanced_settings))
        }

        result.highlight(fragment)
    }
}