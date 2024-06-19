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
package allen.town.focus.twitter.api

import allen.town.focus.twitter.BuildConfig
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus_common.http.bean.LocalTwitterApiKeyBean
import allen.town.focus_common.util.JsonHelper.parseObject
import allen.town.focus_common.util.Timber
import android.content.Context
import android.text.TextUtils
import androidx.annotation.Keep
import androidx.annotation.StringDef

class APIKeys @JvmOverloads constructor(c: Context, currentAccount: Int = -1) {
    @JvmField
    var consumerKey: String? = null

    @JvmField
    var consumerSecret: String? = null
    var callbackUrl: String?

    @JvmField
    var auth_type = Type.OAUTH
    var same_oauth_url = false
    var no_version_suffix = false
    var clientName: String? = "Twitter-Mac"
    var versionName: String? = "6.41.0"
    var twitterApiVersion: String? = "5"
    var internalVersionName = "5002734"
    var platformName: String? = "Mac"
    var platformVersion: String? = "10.12.3"
    var platformArchitecture: String? = "x86_64"
    var client_type = ClientType.MAC_OFFICIAL

    init {
        var currentAccount = currentAccount
        callbackUrl = CALLBACK_URL
        val appVersion = BuildConfig.VERSION_CODE
        var sharedPrefs = AppSettings.getSharedPreferences(c)
        if (currentAccount == -1) {
            if (sharedPrefs == null) {
                sharedPrefs = c.getSharedPreferences(
                    "allen.town.focus.twitter_world_preferences",
                    Context.MODE_PRIVATE
                )
            }
            currentAccount = sharedPrefs!!.getInt(AppSettings.CURRENT_ACCOUNT, 1)
        }
        val apiStr = sharedPrefs.getString(API_STR_KEY, null)
        val localTwitterApiKeyBean = parseObject(
            apiStr, LocalTwitterApiKeyBean::class.java
        )
        if ((localTwitterApiKeyBean != null && !TextUtils.isEmpty(localTwitterApiKeyBean.consumer_key)
                    && !TextUtils.isEmpty(localTwitterApiKeyBean.consumer_secret)) && localTwitterApiKeyBean.apiVersion > 0
        ) {
            Timber.v("local api key is not null")
            if (localTwitterApiKeyBean.apiVersion > appVersion) {
                Timber.w("need use api key from server")
                consumerKey = localTwitterApiKeyBean.consumer_key
                consumerSecret = localTwitterApiKeyBean.consumer_secret
                callbackUrl = localTwitterApiKeyBean.callbackUrl
                //如果是oob那么就是pin码模式，需要参考twidere实现，否则callbackUrl和key还是需要对应上
                auth_type = localTwitterApiKeyBean.auth_type
                same_oauth_url = localTwitterApiKeyBean.same_oauth_url
                no_version_suffix = localTwitterApiKeyBean.no_version_suffix
                localTwitterApiKeyBean.clientName?.run {
                    clientName = this
                }
                localTwitterApiKeyBean.twitterApiVersion?.run {
                    twitterApiVersion = this
                }
                localTwitterApiKeyBean.versionName?.run {
                    versionName = this
                }
                localTwitterApiKeyBean.internalVersionName?.run {
                    internalVersionName = this
                }
                localTwitterApiKeyBean.platformName?.run {
                    platformName = this
                }
                localTwitterApiKeyBean.platformVersion?.run {
                    platformVersion = this
                }
                localTwitterApiKeyBean.platformArchitecture?.run {
                    platformArchitecture = this
                }

                client_type = localTwitterApiKeyBean.client_type
            }
        }
    }

    @Keep
    class Type {
        companion object {
            @JvmField
            var OAUTH = "oauth"

            @JvmField
            var XAUTH = "xauth"
        }
    }

    @Keep
    class ClientType {
        companion object {
            @JvmField
            var MAC_OFFICIAL = "mac_official"

            @JvmField
            var IPAD_OFFICIAL = "ipad_official"

            @JvmField
            var ANDROID_OFFICIAL = "android_official"

        }
    }

    companion object {
        private const val CALLBACK_URL = "oob" //"allentownfocustwitter://"
        private const val API_STR_KEY = "API_STR_KEY"


        /**
         * These are third party service API keys for Focus_for_Mastodon.
         *
         *
         * If you wish to use these services, You will need to get a key for the ones you want to use.
         */
        const val GIPHY_API_KEY = BuildConfig.GIPHY_API_KEY
    }
}