package allen.town.focus.twitter.settings

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.AccountListActivity
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.data.App
import allen.town.focus.twitter.data.App.Companion.getPrefs
import allen.town.focus.twitter.settings.AppSettings.INTERCEPT_TWITTER
import allen.town.focus.twitter.settings.configure_pages.ConfigurePagerActivity
import allen.town.focus.twitter.settings.font.FontsAdapter
import allen.town.focus.twitter.utils.IOUtils
import allen.town.focus.twitter.utils.LocalTrendsUtils
import allen.town.focus.twitter.utils.MySuggestionsProvider
import allen.town.focus.twitter.utils.ServiceUtils
import allen.town.focus.twitter.utils.UiUtils
import allen.town.focus.twitter.utils.Utils.isAndroidO
import allen.town.focus.twitter.utils.text.EmojiInitializer.initializeEmojiCompat
import allen.town.focus.twitter.utils.text.EmojiInitializer.isAlreadyUsingGoogleAndroidO
import allen.town.focus.twitter.views.preference.TweetStylePreviewPreference
import allen.town.focus.twitter.widget.WidgetProvider
import allen.town.focus_common.activity.ClearAllActivityInterface
import allen.town.focus_common.common.prefs.supportv7.ATEColorPreference
import allen.town.focus_common.common.prefs.supportv7.ATESwitchPreference
import allen.town.focus_common.common.prefs.supportv7.dialogs.PreferenceListDialog
import allen.town.focus_common.extensions.installLanguageAndRecreate
import allen.town.focus_common.theme.CustomLauncherIconMakerDialog
import allen.town.focus_common.ui.customtabs.BrowserLauncher
import allen.town.focus_common.util.BasePreferenceUtil
import allen.town.focus_common.util.JsonHelper
import allen.town.focus_common.util.PackageUtils
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.focus_common.views.AccentProgressDialog
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.SearchRecentSuggestions
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.ACCENT_COLORS
import code.name.monkey.appthemehelper.ACCENT_COLORS_SUB
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.constants.ThemeConstants
import code.name.monkey.appthemehelper.constants.ThemeConstants.LANGUAGE_NAME
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.extensions.materialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.color.DynamicColors
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import allen.town.focus.twitter.activities.filters.FiltersActivity
import java.io.File

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
 */ open class PrefFragment : AbsSettingsFragment(), OnSharedPreferenceChangeListener {
    var position = 0
    var title = ""
    var mListStyled = false
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val args = arguments
        position = args!!.getInt("position")
        title = args!!.getString("title") ?: ""
        setPreferences(position)
    }

    open fun setPreferences(position: Int) {
        when (position) {
            0 -> {
                addPreferencesFromResource(R.xml.settings_app_style)
                setupAppStyle()
            }

            1 -> {
                addPreferencesFromResource(R.xml.settings_widget_customization)
                setUpWidgetCustomization()
            }

            2 -> {
                addPreferencesFromResource(R.xml.settings_swipable_pages_and_app_drawer)
                setUpSwipablePages()
            }

            3 -> {
                addPreferencesFromResource(R.xml.settings_background_refreshes)
                setUpBackgroundRefreshes()
            }

            4 -> {
                addPreferencesFromResource(R.xml.settings_notifications)
                setUpNotificationSettings()
            }

            5 -> {
                addPreferencesFromResource(R.xml.settings_data_savings)
                setUpDataSaving()
            }

            6 -> {
                addPreferencesFromResource(R.xml.settings_location)
                setUpLocationSettings()
            }

            7 -> {
                addPreferencesFromResource(R.xml.settings_mutes)
                setUpMuteSettings()
            }

            8 -> {
                addPreferencesFromResource(R.xml.settings_app_memory)
                setUpAppMemorySettings()
            }

            9 -> {
                addPreferencesFromResource(R.xml.settings_other_options)
                setUpOtherOptions()
            }
        }
        val advanced = findPreference<Preference>("advanced")
        if (advanced != null) {
            advanced.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                (activity as SettingsActivity?)!!.openAdvanceScreen(
                    position,
                    getResources().getString(R.string.advanced_options)
                )
                true
            }
        }
    }

    fun setUpWidgetCustomization() {
        val settings = AppSettings.getInstance(activity)
        val account = findPreference<Preference>("account")
        account!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val items =
                if (settings.numberOfAccounts == 1) arrayOf<CharSequence>("@" + settings.myScreenName) else arrayOf<CharSequence>(
                    "@" + settings.myScreenName,
                    "@" + settings.secondScreenName
                )
            AccentMaterialDialog(
                requireActivity(),
                R.style.MaterialAlertDialogTheme
            )
                .setItems(items) { dialog, which ->
                    settings.sharedPrefs.edit()
                        .putString("widget_account", items[which].toString() + "").commit()
                    account.summary = items[which]
                }
                .create().show()
            true
        }
        account.summary = settings.sharedPrefs.getString("widget_account", "")
    }

    fun setUpDataSaving() {}

    fun setUpSwipablePages() {
        val pages = findPreference<Preference>("pages")
        pages!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val configurePages = Intent(context, ConfigurePagerActivity::class.java)
            startActivity(configurePages)
            false
        }
    }

    fun getName(listName: String?, type: Int): String? {
        when (type) {
            AppSettings.PAGE_TYPE_USER_TWEETS, AppSettings.PAGE_TYPE_LIST_TIMELINE -> return listName
            AppSettings.PAGE_TYPE_LINKS -> return requireContext().resources.getString(R.string.links)
            AppSettings.PAGE_TYPE_PICS -> return requireContext().resources.getString(R.string.pictures)
            AppSettings.PAGE_TYPE_FAV_USERS_TWEETS -> return requireContext().getString(R.string.favorite_users)
            AppSettings.PAGE_TYPE_SAVED_TWEETS -> return requireContext().getString(R.string.saved_tweets)
        }
        return null
    }

    fun setUpAppMemorySettings() {
        val sharedPrefs = AppSettings.getSharedPreferences(context)
        val clearSearch = findPreference<Preference>("clear_searches")
        clearSearch!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val suggestions = SearchRecentSuggestions(
                context,
                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE
            )
            suggestions.clearHistory()
            TopSnackbarUtil.showSnack(context, R.string.done, Toast.LENGTH_SHORT)
            false
        }
        val backup = findPreference<Preference>("backup")
        backup!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AccentMaterialDialog(
                requireContext(),
                R.style.MaterialAlertDialogTheme
            )
                .setTitle(requireContext().resources.getString(R.string.backup_settings_dialog))
                .setMessage(requireContext().resources.getString(R.string.backup_settings_dialog_summary))
                .setPositiveButton(R.string.ok) { dialogInterface, i ->
                    val des = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toString() + "/Focus_for_Mastodon/backup.prefs"
                    )
                    IOUtils.saveSharedPreferencesToFile(des, context)
                    TopSnackbarUtil.showSnack(
                        context,
                        requireContext().resources.getString(R.string.backup_success),
                        Toast.LENGTH_LONG
                    )
                }
                .setNegativeButton(R.string.no) { dialogInterface, i -> }
                .create()
                .show()
            false
        }
        val restore = findPreference<Preference>("restore")
        restore!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val des =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .toString() + "/Focus_for_Mastodon/backup.prefs"
                )
            val authenticationToken1 = sharedPrefs.getString("authentication_token_1", "none")
            val authenticationTokenSecret1 =
                sharedPrefs.getString("authentication_token_secret_1", "none")
            val myScreenName1 = sharedPrefs.getString("twitter_screen_name_1", "")
            val myName1 = sharedPrefs.getString("twitter_users_name_1", "")
            val myBackgroundUrl1 = sharedPrefs.getString("twitter_background_url_1", "")
            val myProfilePicUrl1 = sharedPrefs.getString("profile_pic_url_1", "")
            val lastTweetId1 = sharedPrefs.getLong("last_tweet_id_1", 0)
            val secondLastTweetId1 = sharedPrefs.getLong("second_last_tweet_id_1", 0)
            val lastMentionId1 = sharedPrefs.getLong("last_mention_id_1", 0)
            val lastDMId1 = sharedPrefs.getLong("last_dm_id_1", 0)
            val twitterId1 = sharedPrefs.getLong("twitter_id_1", 0)
            val isloggedin1 = sharedPrefs.getBoolean("is_logged_in_1", false)
            val keyVersion1 = sharedPrefs.getInt("key_version_1", 1)
            val authenticationToken2 = sharedPrefs.getString("authentication_token_2", "none")
            val authenticationTokenSecret2 =
                sharedPrefs.getString("authentication_token_secret_2", "none")
            val myScreenName2 = sharedPrefs.getString("twitter_screen_name_2", "")
            val myName2 = sharedPrefs.getString("twitter_users_name_2", "")
            val myBackgroundUrl2 = sharedPrefs.getString("twitter_background_url_2", "")
            val myProfilePicUrl2 = sharedPrefs.getString("profile_pic_url_2", "")
            val lastTweetId2 = sharedPrefs.getLong("last_tweet_id_2", 0)
            val secondLastTweetId2 = sharedPrefs.getLong("second_last_tweet_id_2", 0)
            val lastMentionId2 = sharedPrefs.getLong("last_mention_id_2", 0)
            val lastDMId2 = sharedPrefs.getLong("last_dm_id_2", 0)
            val twitterId2 = sharedPrefs.getLong("twitter_id_2", 0)
            val isloggedin2 = sharedPrefs.getBoolean("is_logged_in_2", false)
            val keyVersion2 = sharedPrefs.getInt("key_version_2", 1)
            val key = sharedPrefs.getString("consumer_key_2", "")
            IOUtils.loadSharedPreferencesFromFile(des, context)
            TopSnackbarUtil.showSnack(
                context,
                requireContext().resources.getString(R.string.restore_success),
                Toast.LENGTH_LONG
            )
            val e = sharedPrefs.edit()
            e.putString("authentication_token_1", authenticationToken1)
            e.putString("authentication_token_secret_1", authenticationTokenSecret1)
            e.putString("twitter_screen_name_1", myScreenName1)
            e.putString("twitter_users_name_1", myName1)
            e.putString("twitter_background_url_1", myBackgroundUrl1)
            e.putString("profile_pic_url_1", myProfilePicUrl1)
            e.putLong("last_tweet_id_1", lastTweetId1)
            e.putLong("second_last_tweet_id_1", secondLastTweetId1)
            e.putLong("last_mention_id_1", lastMentionId1)
            e.putLong("last_dm_id_1", lastDMId1)
            e.putLong("twitter_id_1", twitterId1)
            e.putBoolean("is_logged_in_1", isloggedin1)
            e.putInt("key_version_1", keyVersion1)
            e.putString("authentication_token_2", authenticationToken2)
            e.putString("authentication_token_secret_2", authenticationTokenSecret2)
            e.putString("twitter_screen_name_2", myScreenName2)
            e.putString("twitter_users_name_2", myName2)
            e.putString("twitter_background_url_2", myBackgroundUrl2)
            e.putString("profile_pic_url_2", myProfilePicUrl2)
            e.putLong("last_tweet_id_2", lastTweetId2)
            e.putLong("second_last_tweet_id_2", secondLastTweetId2)
            e.putLong("last_mention_id_2", lastMentionId2)
            e.putLong("last_dm_id_2", lastDMId2)
            e.putLong("twitter_id_2", twitterId2)
            e.putBoolean("is_logged_in_2", isloggedin2)
            e.putInt("key_version_2", keyVersion2)
            e.putString("consumer_key_2", key)
            e.remove("new_notifications")
            e.remove("new_retweets")
            e.remove("new_favorites")
            e.remove("new_followers")
            e.remove("new_quotes")
            val currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1)
            e.remove("last_activity_refresh_$currentAccount")
            e.remove("original_activity_refresh_$currentAccount")
            e.remove("activity_follower_count_$currentAccount")
            e.remove("activity_latest_followers_$currentAccount")
            e.commit()
            false
        }
        val cache = findPreference<Preference>("delete_cache")
        val size = IOUtils.dirSize(requireContext().cacheDir)
        cache!!.summary =
            resources.getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB"
        cache.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AccentMaterialDialog(
                requireContext(),
                R.style.MaterialAlertDialogTheme
            )
                .setTitle(requireContext().resources.getString(R.string.cache_dialog))
                .setMessage(requireContext().resources.getString(R.string.cache_dialog_summary))
                .setPositiveButton(R.string.ok) { dialogInterface, i ->
                    try {
                        TrimCache(cache).execute()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton(R.string.no) { dialogInterface, i -> }
                .create()
                .show()
            false
        }
        val trim = findPreference<Preference>("trim_now")
        trim!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AccentMaterialDialog(
                requireContext(),
                R.style.MaterialAlertDialogTheme
            )
                .setTitle(requireContext().resources.getString(R.string.trim_dialog))
                .setMessage(requireContext().resources.getString(R.string.cache_dialog_summary))
                .setPositiveButton(R.string.ok) { dialogInterface, i ->
                    try {
                        TrimDatabase().execute()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton(R.string.no) { dialogInterface, i -> }
                .create()
                .show()
            false
        }
    }

    fun setUpMuteSettings() {
        val sharedPrefs = AppSettings.getSharedPreferences(activity)
        val showHandle = findPreference<Preference>("display_screen_name")
        if (sharedPrefs.getBoolean("both_handle_name", false) && showHandle != null) {
            showHandle.isEnabled = false
        }
        val muffle = findPreference<Preference>("manage_muffles")
        muffle!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val users = UiUtils.getMuffledUsers(sharedPrefs)
            val names = arrayOfNulls<String>(users.size)
            val keys = arrayOfNulls<String>(users.size)
            if (names.isNotEmpty()) {
                var i = 0
                for (key in users!!.iterator()) {
                    names[i] = key.value
                    keys[i] = key.key
                    i++
                }
            }

            val builder = AccentMaterialDialog(
                requireContext(),
                R.style.MaterialAlertDialogTheme
            )
            builder.setItems(names) { dialog, item ->
                users!!.remove(keys[item])
                sharedPrefs.edit()
                    .putString(AppSettings.MUFFLED_USERS_ID, JsonHelper.toJSONString(users))
                    .commit()
                dialog.dismiss()
            }
            val alert = builder.create()
            if (keys.isEmpty()) {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.no_users),
                    Toast.LENGTH_SHORT
                )
            } else {
                alert.show()
            }
            false
        }
        val newRegexMute = findPreference<Preference>("filter")
        newRegexMute!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(context, FiltersActivity::class.java)
            activity?.startActivity(intent)
            true
        }
        val mutedRegex = findPreference<Preference>("manage_regex_mute")
        mutedRegex!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val exps = sharedPrefs.getString(AppSettings.MUTED_REGEX, "")!!
                .split("   ").toTypedArray()
            if (exps.size == 0 || exps.size == 1 && exps[0] == "") {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.no_expression),
                    Toast.LENGTH_SHORT
                )
            } else {
                val builder = AccentMaterialDialog(
                    requireContext(),
                    R.style.MaterialAlertDialogTheme
                )
                builder.setItems(exps) { dialog, item ->
                    var newExps = ""
                    for (i in exps.indices) {
                        if (i != item) {
                            newExps += exps[i] + "   "
                        }
                    }
                    sharedPrefs.edit().putString(AppSettings.MUTED_REGEX, newExps).commit()
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
            }
            false
        }
        val muted = findPreference<Preference>("manage_mutes")
        muted!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(context, AccountListActivity::class.java)
            intent.putExtra("type", AccountListActivity.Type.MUTES)
            activity?.startActivity(intent)
            true
        }
        val mutedRT = findPreference<Preference>("manage_blocks")
        mutedRT!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(context, AccountListActivity::class.java)
            intent.putExtra("type", AccountListActivity.Type.BLOCKS)
            activity?.startActivity(intent)
            true
        }

        val hashtags = findPreference<Preference>("manage_mutes_hashtags")
        hashtags!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val tags = sharedPrefs.getString("muted_hashtags", "")!!
                .split(" ").toTypedArray()
            for (i in tags.indices) {
                tags[i] = "#" + tags[i]
            }
            if (tags.size == 0 || tags.size == 1 && tags[0] == "#") {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.no_hashtags),
                    Toast.LENGTH_SHORT
                )
            } else {
                val builder = AccentMaterialDialog(
                    requireContext(),
                    R.style.MaterialAlertDialogTheme
                )
                builder.setItems(tags) { dialog, item ->
                    var newTags = ""
                    for (i in tags.indices) {
                        if (i != item) {
                            newTags += tags[i].replace("#", "") + " "
                        }
                    }
                    sharedPrefs.edit().putString("muted_hashtags", newTags).commit()
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
            }
            false
        }

        val clients = findPreference<Preference>("manage_muted_clients")
        clients!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val tags = sharedPrefs.getString("muted_clients", "")!!
                .split("   ").toTypedArray()
            if (tags.size == 0 || tags.size == 1 && tags[0] == "") {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.no_clients),
                    Toast.LENGTH_SHORT
                )
            } else {
                val builder = AccentMaterialDialog(
                    requireContext(),
                    R.style.MaterialAlertDialogTheme
                )
                builder.setItems(tags) { dialog, item ->
                    var newClients = ""
                    for (i in tags.indices) {
                        if (i != item) {
                            newClients += tags[i] + "   "
                        }
                    }
                    sharedPrefs.edit().putString("muted_clients", newClients).commit()
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
            }
            false
        }
    }

    fun setArticleListFont() {
        val listArticleFontPre = findPreference<Preference>(AppSettings.FONT_TYPE)
        if (listArticleFontPre != null) {
            val listFont = getPrefs(
                requireActivity()
            )!!.articleListFontType
            val fontsAdapter = FontsAdapter(activity)
            val font = fontsAdapter.getItem(listFont.get().index)
            listArticleFontPre.summary = font.label
        }
    }

    open fun setupAppStyle() {
        val sharedPrefs = AppSettings.getSharedPreferences(activity)
        if (isAlreadyUsingGoogleAndroidO()) {
            preferenceScreen.removePreference(findPreference(AppSettings.EMOJI_STYLE)!!)
        } else {
            findPreference<Preference>(AppSettings.EMOJI_STYLE)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, o ->
                    Handler().postDelayed({
                        AppSettings.invalidate()
                        initializeEmojiCompat(requireActivity())
                    }, 500)
                    true
                }
        }
        setArticleListFont()
    }

    fun getTime(hours: Int, mins: Int, militaryTime: Boolean): String {
        val hour: String
        val min: String
        var pm = false
        return if (!militaryTime) {
            if (hours > 12) {
                pm = true
                val x = hours - 12
                hour = x.toString() + ""
            } else {
                hour = hours.toString() + ""
            }
            min = if (mins < 10) {
                "0$mins"
            } else {
                mins.toString() + ""
            }
            hour + ":" + min + if (pm) " PM" else " AM"
        } else {
            hour = if (hours < 10) "0$hours" else hours.toString() + ""
            min = if (mins < 10) {
                "0$mins"
            } else {
                mins.toString() + ""
            }
            "$hour:$min"
        }
    }

    fun setUpNotificationSettings() {
        val sharedPrefs = AppSettings.getSharedPreferences(activity)
        val quietHours = findPreference<Preference>("quiet_hours")
        if (sharedPrefs.getBoolean("quiet_hours", false)) {
            quietHours!!.summary = getTime(
                sharedPrefs.getInt("quiet_start_hour", 22),
                sharedPrefs.getInt("quiet_start_min", 0),
                sharedPrefs.getBoolean("military_time", false)
            ) +
                    " - " +
                    getTime(
                        sharedPrefs.getInt("quiet_end_hour", 6),
                        sharedPrefs.getInt("quiet_end_min", 0),
                        sharedPrefs.getBoolean("military_time", false)
                    )
        } else {
            quietHours!!.summary = ""
        }
        quietHours.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, o ->
                if (!(quietHours as CheckBoxPreference?)!!.isChecked) {
                    val startDialog = MaterialTimePicker.Builder()
                        .setTimeFormat(
                            if (sharedPrefs.getBoolean(
                                    "military_time",
                                    false
                                )
                            ) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
                        )
                        .setHour(22)
                        .setMinute(0)
                        .setTitleText(getString(R.string.night_mode_night))
                        .build()
                    startDialog.addOnPositiveButtonClickListener {
                        sharedPrefs.edit().putInt("quiet_start_hour", startDialog.hour)
                            .putInt("quiet_start_min", startDialog.minute).commit()
                        val endDialog = MaterialTimePicker.Builder()
                            .setTimeFormat(
                                if (sharedPrefs.getBoolean(
                                        "military_time",
                                        false
                                    )
                                ) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
                            )
                            .setHour(6)
                            .setMinute(0)
                            .setTitleText(getString(R.string.night_mode_day))
                            .build()
                        endDialog.addOnPositiveButtonClickListener {
                            sharedPrefs.edit().putInt("quiet_end_hour", endDialog.hour)
                                .putInt("quiet_end_min", endDialog.minute).commit()
                            quietHours.summary = getTime(
                                sharedPrefs.getInt("quiet_start_hour", 22),
                                sharedPrefs.getInt("quiet_start_min", 0),
                                sharedPrefs.getBoolean("military_time", false)
                            ) +
                                    " - " +
                                    getTime(
                                        sharedPrefs.getInt("quiet_end_hour", 6),
                                        sharedPrefs.getInt("quiet_end_min", 0),
                                        sharedPrefs.getBoolean("military_time", false)
                                    )
                        }
                        endDialog.show(requireFragmentManager(), "quiet_hours_end")
                    }
                    startDialog.show(requireFragmentManager(), "quiet_hours_start")
                } else {
                    quietHours.summary = ""
                }
                true
            }
        if (isAndroidO) {
//            ringtone.setVisible(false);
            (findPreference<Preference>("advanced-notifications") as PreferenceCategory?)!!.removePreference(
                findPreference("ringtone")!!
            )
        }
        findPreference<Preference>("notification_channels")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                startActivity(intent)
                false
            }
        if (isAndroidO) {
            (findPreference<Preference>("advanced-notifications") as PreferenceCategory?)!!.removePreference(
                findPreference("alert_types")!!
            )
        } else {
            (findPreference<Preference>("advanced-notifications") as PreferenceCategory?)!!.removePreference(
                findPreference("notification_channels")!!
            )
        }
    }

    open fun setUpBackgroundRefreshes() {}
    private fun setUpOtherOptions() {
        findPreference<Preference>("prefViewForum")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                BrowserLauncher.openUrl(getContext(), "https://focusformastodon.canny.io")
                true
            }

        findPreference<Preference>("pref_changelog")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                BrowserLauncher.openUrl(getContext(), "https://focusformastodon.canny.io/changelog")
                true
            }
    }

    fun setUpLocationSettings() {
        val context: Context? = activity
        val sharedPrefs = AppSettings.getSharedPreferences(context)
        val cities = findPreference<Preference>("city")
        if (sharedPrefs.getBoolean("manually_config_location", false)) {
            cities!!.summary = sharedPrefs.getString("location", "Chicago")
        }
        cities!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val country = sharedPrefs.getString("country", "United States")
            val full = LocalTrendsUtils.getArray(country)
            val names = arrayOfNulls<String>(full.size)
            for (i in names.indices) {
                val s = full[i]
                names[i] = s[0]
            }
            val builder = AccentMaterialDialog(
                requireContext(),
                R.style.MaterialAlertDialogTheme
            )
            builder.setItems(names) { dialog, item ->
                val id = full[item][1]
                val name = full[item][0]
                sharedPrefs.edit().putInt("woeid", id.toInt()).commit()
                sharedPrefs.edit().putString("location", name).commit()
                cities.summary = name
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as SettingsActivity?)?.setTitle(title)
        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences!!
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences!!
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences, key: String) {
        val worldPrefs = AppSettings.getSharedPreferences(activity)

        // get the values and write them to our world prefs
        try {
            val s = sharedPrefs.getString(key, "")
            worldPrefs.edit().putString(key, s).commit()
        } catch (e: Exception) {
            try {
                val i = sharedPrefs.getInt(key, -100)
                worldPrefs.edit().putInt(key, i).commit()
            } catch (x: Exception) {
                try {
                    val b = sharedPrefs.getBoolean(key, false)
                    worldPrefs.edit().putBoolean(key, b).commit()
                } catch (m: Exception) {
                }
            }
        }
        AppSettings.invalidate()
        ServiceUtils.rescheduleAllServices(context)
        if (key == INTERCEPT_TWITTER) {
            if (sharedPrefs.getBoolean(INTERCEPT_TWITTER, false)) {
                AccentMaterialDialog(
                    requireContext(),
                    R.style.MaterialAlertDialogTheme
                )
                    .setMessage(R.string.intercept_twitter_push_description)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog: DialogInterface?, which: Int ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } else {
                            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }
                    }.show()
            }
        } else if (key == "layout") {
            TrimCache(null).execute()
        } else if (key == "alert_types") {
            Log.v("notification_set", "alert being set")
            val set = sharedPrefs.getStringSet("alert_types", null) ?: return
            if (set.contains("1")) {
                sharedPrefs.edit().putBoolean("vibrate", true).commit()
                worldPrefs.edit().putBoolean("vibrate", true).commit()
            } else {
                sharedPrefs.edit().putBoolean("vibrate", false).commit()
                worldPrefs.edit().putBoolean("vibrate", false).commit()
            }
            if (set.contains("2")) {
                sharedPrefs.edit().putBoolean("led", true).commit()
                worldPrefs.edit().putBoolean("led", true).commit()
            } else {
                sharedPrefs.edit().putBoolean("led", false).commit()
                worldPrefs.edit().putBoolean("led", false).commit()
            }
            if (set.contains("3")) {
                sharedPrefs.edit().putBoolean("wake", true).commit()
                worldPrefs.edit().putBoolean("wake", true).commit()
            } else {
                sharedPrefs.edit().putBoolean("wake", false).commit()
                worldPrefs.edit().putBoolean("wake", false).commit()
            }
            if (set.contains("4")) {
                sharedPrefs.edit().putBoolean("sound", true).commit()
                worldPrefs.edit().putBoolean("sound", true).commit()
            } else {
                sharedPrefs.edit().putBoolean("sound", false).commit()
                worldPrefs.edit().putBoolean("sound", false).commit()
            }
            if (set.contains("5")) {
                sharedPrefs.edit().putBoolean("heads_up", true).commit()
                worldPrefs.edit().putBoolean("heads_up", true).commit()
            } else {
                sharedPrefs.edit().putBoolean("heads_up", false).commit()
                worldPrefs.edit().putBoolean("heads_up", false).commit()
            }
        } else if (key == "widget_theme" || key == "text_size") {
            WidgetProvider.updateWidget(context)
        } else if (key == "timeline_pictures") {
            (findPreference<Preference>("tweet_style_preview") as TweetStylePreviewPreference?)!!.refreshUI()
        }
    }

    override fun invalidateSettings() {
        val webPreview: Preference? = findPreference(AppSettings.WEB_PREVIEW_TIMELINE_KEY)
        webPreview?.setOnPreferenceChangeListener { _, value ->
            if (value as Boolean && !App.instance.checkSupporter(requireContext())) {
                return@setOnPreferenceChangeListener false
            }
            true
        }

        val generalTheme: Preference? = findPreference(ThemeConstants.GENERAL_THEME)
        generalTheme?.let {
            setSummary(it)
            it.setOnPreferenceChangeListener { _, newValue ->
                val theme = newValue as String
                setSummary(it, newValue)
                ThemeStore.markChanged(requireContext())

                /*                if (VersionUtils.hasNougatMR()) {
                                    DynamicShortcutManager(
                                        requireContext(),
                                        ShortcutsDefaultList(requireContext()).defaultShortcuts
                                    ).updateDynamicShortcuts()
                                }*/
                AppSettings.invalidate()
                restartActivity()
                true
            }
        }

        val accentColorPref: ATEColorPreference? = findPreference(ThemeConstants.ACCENT_COLOR)
        val accentColor = ThemeStore.accentColor(requireContext())
        accentColorPref?.setColor(accentColor, ColorUtil.darkenColor(accentColor))
        accentColorPref?.setOnPreferenceClickListener {
            materialDialog().show {
                colorChooser(
                    initialSelection = accentColor,
                    showAlphaSelector = false,
                    colors = ACCENT_COLORS,
                    subColors = ACCENT_COLORS_SUB, allowCustomArgb = true
                ) { _, color ->
                    if (!App.instance.checkSupporter(requireContext())) {
                        return@colorChooser
                    }
                    ThemeStore.editTheme(requireContext()).accentColor(color).commit()
                    /*                    if (VersionUtils.hasNougatMR())
                                            DynamicShortcutManager(
                                                requireContext(),
                                                ShortcutsDefaultList(requireContext()).defaultShortcuts
                                            ).updateDynamicShortcuts()*/
                    restartActivity()
                }
            }
            return@setOnPreferenceClickListener true
        }
        val blackTheme: ATESwitchPreference? = findPreference(ThemeConstants.BLACK_THEME)
        blackTheme?.setOnPreferenceChangeListener { _, value ->
/*            if(value as Boolean && !MyApp.instance.checkSupporter(requireContext())){
                return@setOnPreferenceChangeListener false
            }*/
            ThemeStore.markChanged(requireContext())
            /*            if (VersionUtils.hasNougatMR()) {
                            DynamicShortcutManager(
                                requireContext(),
                                ShortcutsDefaultList(requireContext()).defaultShortcuts
                            ).updateDynamicShortcuts()
                        }*/
            restartActivity()
            true
        }

        val desaturatedColor: ATESwitchPreference? =
            findPreference(ThemeConstants.DESATURATED_COLOR)
        desaturatedColor?.setOnPreferenceChangeListener { _, value ->
            val desaturated = value as Boolean
            ThemeStore.prefs(requireContext())
                .edit()
                .putBoolean(ThemeConstants.DESATURATED_COLOR, desaturated)
                .commit()
            BasePreferenceUtil.isDesaturatedColor = desaturated
            restartActivity()
            true
        }
//
//        val colorAppShortcuts: TwoStatePreference? = findPreference(ThemeConstants.SHOULD_COLOR_APP_SHORTCUTS)
//        if (!VersionUtils.hasNougatMR()) {
//            colorAppShortcuts?.isVisible = false
//        } else {
//            colorAppShortcuts?.isChecked = BasePreferenceUtil.isColoredAppShortcuts
//            colorAppShortcuts?.setOnPreferenceChangeListener { _, newValue ->
//                BasePreferenceUtil.isColoredAppShortcuts = newValue as Boolean
//                DynamicShortcutManager(
//                    requireContext(),
//                    ShortcutsDefaultList(requireContext()).defaultShortcuts
//                ).updateDynamicShortcuts()
//                true
//            }
//        }
//
        val materialYou: ATESwitchPreference? = findPreference(ThemeConstants.MATERIAL_YOU)
        materialYou?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                DynamicColors.applyToActivitiesIfAvailable(App.getInstance(requireContext()) as Application)
            }
            restartActivity()
            true
        }
        val wallpaperAccent: ATESwitchPreference? = findPreference(ThemeConstants.WALLPAPER_ACCENT)
        wallpaperAccent?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }

        val customLauncher: Preference? = findPreference(ThemeConstants.CUSTOM_LAUNCHER)
        if (VersionUtils.hasOreo()) {
            customLauncher?.onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        CustomLauncherIconMakerDialog(
                            R.string.pref_custom_launcher_title,
                            PackageUtils.getPackageName(context),
                            "allen.town.focus.twitter.activities.MainActivity",
                            R.color.launcher_bg,
                            R.mipmap.ic_launcher_foreground,
                            R.color.launcher_bg,
                            PackageUtils.getAppName(context)
                        ).show(
                            activity!!.supportFragmentManager,
                            null
                        )
                        return true
                    }

                }
        } else {
            customLauncher?.isVisible = false
        }

        (findPreference(LANGUAGE_NAME) as? ListPreference)?.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference, newValue ->
            setSummary(preference, newValue.toString())
            requireActivity().installLanguageAndRecreate(
                newValue.toString(),
                activity as? ClearAllActivityInterface
            )
            true
        })
    }

    internal inner class TrimCache(private val cache: Preference?) :
        AsyncTask<String?, Void?, Boolean>() {
        private var pDialog: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            pDialog = AccentProgressDialog.show(
                context,
                resources.getString(R.string.trimming),
                null,
                false,
                false
            )
        }

        override fun doInBackground(vararg urls: String?): Boolean {
            IOUtils.trimCache(context)
            return true
        }

        override fun onPostExecute(deleted: Boolean) {
            val size = IOUtils.dirSize(requireContext().cacheDir)
            var fin = false
            try {
                if (cache != null) {
                    cache.summary =
                        resources.getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB"
                    //if (deleted) {
                    TopSnackbarUtil.showSnack(
                        context,
                        requireContext().resources.getString(R.string.trim_success),
                        Toast.LENGTH_SHORT
                    )
                    /*} else {
                    TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT);
                }*/
                } else {
                    fin = true
                }
                pDialog!!.dismiss()
            } catch (e: IllegalStateException) {
            }
        }

    }

    private inner class TrimDatabase : AsyncTask<String?, Void?, Boolean>() {
        var pDialog: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            pDialog = AccentProgressDialog.show(
                context,
                resources.getString(R.string.trimming),
                null,
                false,
                false
            )
        }

        override fun doInBackground(vararg urls: String?): Boolean {
            return IOUtils.trimDatabase(context, 1) && IOUtils.trimDatabase(context, 2)
        }

        override fun onPostExecute(deleted: Boolean) {
            try {
                pDialog!!.dismiss()
            } catch (e: Exception) {
                // not attached
            }
            if (deleted) {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.trim_success),
                    Toast.LENGTH_SHORT
                )
            } else {
                TopSnackbarUtil.showSnack(
                    context,
                    requireContext().resources.getString(R.string.trim_fail),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

    private val REQUEST_CODE_ALERT_RINGTONE = 100
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return if (preference.key == "ringtone") {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI
            )
            val existingValue = PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            ).getString("ringtone", "")
            if (existingValue != null) {
                if (existingValue.length == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        Uri.parse(existingValue)
                    )
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )
            }
            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
            true
        } else if (preference.key == AppSettings.FONT_TYPE) {
            val listFont = getPrefs(
                requireActivity()
            )!!.articleListFontType
            val preferenceListDialog = PreferenceListDialog(
                getContext(),
                getString(R.string.font_type)
            )
            preferenceListDialog.setSelection(listFont.get().index)
            val fontsAdapter = FontsAdapter(getContext())
            preferenceListDialog.openDialog(fontsAdapter)
            preferenceListDialog.setOnPreferenceChangedListener { i ->
                if (fontsAdapter.getItem(i).isCharge
                    //第一次打开dialog会调用,所以如果当前选择的item和之前选择的item是一样的不需要检查收费
                    && !listFont.get().cssName.equals(fontsAdapter.getItem(i).cssName)
                    && !App.instance.checkSupporter(getContext(), true)
                ) {
                    Timber.d("not pro so limit free font")
                } else {
                    listFont.set(fontsAdapter.getItem(i))
                    setArticleListFont()
                    (activity as? WhiteToolbarActivity)!!.clearAllAppcompactActivities(true)
                }

            }
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            val ringtone = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (ringtone != null) {
                AppSettings.getInstance(context).sharedPrefs.edit()
                    .putString("ringtone", ringtone.toString())
                    .commit()
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                    .putString("ringtone", ringtone.toString())
                    .commit()
                AppSettings.invalidate()
            } else {
                // "Silent" was selected
                AppSettings.getInstance(context).sharedPrefs.edit()
                    .putString("ringtone", "")
                    .commit()
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                    .putString("ringtone", "")
                    .commit()
                AppSettings.invalidate()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}