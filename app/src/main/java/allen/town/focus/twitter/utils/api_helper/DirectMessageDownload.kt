package allen.town.focus.twitter.utils.api_helper

import allen.town.focus.twitter.data.sq_lite.DMDataSource
import allen.town.focus.twitter.receivers.IntentConstant
import allen.town.focus.twitter.services.background_refresh.SecondDMRefreshService
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.NotificationUtils
import allen.town.focus.twitter.utils.Utils
import android.content.Context
import android.content.Intent
import android.util.Log
import twitter4j.Status

object DirectMessageDownload {

    @JvmStatic
    fun download(context: Context?, useSecondAccount: Boolean, alwaysSync: Boolean): Int {
        if (context == null) {
            return 0
        }

        val sharedPrefs = AppSettings.getSharedPreferences(context)
        val settings = AppSettings.getInstance(context)

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile && !alwaysSync) {
            return 0
        }

        try {
            var currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1)

            if (useSecondAccount) {
                currentAccount = if (currentAccount == 1) 2 else 1
            }


            val dataSource = DMDataSource.getInstance(context)
            val statuses: MutableList<Status> = dataSource.getNewestPageFromRemote(useSecondAccount, currentAccount);
            dataSource.insertTweets(statuses, currentAccount)

            val inserted = statuses.size

            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit()
            sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_DM, true).commit()

            if (settings.notifications && settings.dmsNot && inserted > 0 && !alwaysSync) {
                val currentUnread =
                    sharedPrefs.getInt("${AppSettings.DM_UNREAD_STARTER}$currentAccount", 0)
                sharedPrefs.edit().putInt(
                    "${AppSettings.DM_UNREAD_STARTER}$currentAccount",
                    inserted + currentUnread
                ).commit()

                if (useSecondAccount) {
                    NotificationUtils.notifySecondDMs(context, currentAccount)
                } else {
                    NotificationUtils.refreshNotification(context)
                }
            }


            if (!useSecondAccount && settings.syncSecondMentions) {
                SecondDMRefreshService.startNow(context)
            }

            if (!useSecondAccount) {
                context.sendBroadcast(Intent(IntentConstant.NEW_DIRECT_MESSAGE_ACTION))
            }

            return inserted
        } catch (e: Exception) {
            // Error in updating status
            Log.d("Twitter Update Error", "${e.message}")
        }

        return 0

    }
}
