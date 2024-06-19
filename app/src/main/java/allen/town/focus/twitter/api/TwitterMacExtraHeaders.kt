/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package allen.town.focus.twitter.api

import allen.town.focus.twitter.data.App
import java.util.*

/**
 * Created by mariotaku on 2017/2/25.
 */
object TwitterMacExtraHeaders : ExtraHeaders{
    var apiKeys: APIKeys = APIKeys(App.instance.applicationContext,-1)

    override fun get(): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>()
        val language = Locale.getDefault().bcp47Tag
        result.add(Pair("User-Agent", userAgent))
        result.add(Pair("Accept-Language", language))
        result.add(Pair("X-Twitter-Client", apiKeys.clientName!!))
        result.add(Pair("X-Twitter-Client-Version", apiKeys.versionName!!))
        return result
    }

    val userAgent: String get() {
        return "${apiKeys.clientName}/(${apiKeys.internalVersionName}) ${apiKeys.platformName}/${apiKeys.platformVersion} (;${apiKeys.platformArchitecture})"
    }

}
