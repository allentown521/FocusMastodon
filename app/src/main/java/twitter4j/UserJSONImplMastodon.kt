/*
 * Copyright 2007 Yusuke Yamamoto
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
package twitter4j

import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper
import allen.town.focus.twitter.model.Account
import allen.town.focus.twitter.model.HeaderPaginationList
import android.annotation.SuppressLint
import android.database.Cursor
import android.text.TextUtils
import java.util.*

/**
 * A data class representing Basic user information element
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
/*package*/
internal class UserJSONImplMastodon : TwitterResponseImpl, User {
    private var id: Long = 0
    private var name: String? = null
    private var email: String? = null
    private var screenName: String? = null
    private var location: String? = null
    private var description: String? = null
    private var descriptionURLEntities: Array<URLEntity>? = null
    private var urlEntity: URLEntity? = null
    private var isContributorsEnabled = false
    private var profileImageUrl: String? = null
    private var profileImageUrlHttps: String? = null
    private var isDefaultProfileImage = false
    private var url: String? = null
    private var isProtected = false
    private var followersCount = 0
    private var status: Status? = null
    private var profileBackgroundColor: String? = null
    private var profileTextColor: String? = null
    private var profileLinkColor: String? = null
    private var profileSidebarFillColor: String? = null
    private var profileSidebarBorderColor: String? = null
    private var profileUseBackgroundImage = false
    private var isDefaultProfile = false
    private var showAllInlineMedia = false
    private var friendsCount = 0
    private var createdAt: Date? = null
    private var favouritesCount = 0
    private var utcOffset = 0
    private var timeZone: String? = null
    private var profileBackgroundImageUrl: String? = null
    private var profileBackgroundImageUrlHttps: String? = null
    private var profileBannerImageUrl: String? = null
    private var profileBackgroundTiled = false
    private var lang: String? = null
    private var statusesCount = 0
    private var isGeoEnabled = false
    private var isVerified = false
    private var translator = false
    private var listedCount = 0
    private var isFollowRequestSent = false
    private var withheldInCountries: Array<String>? = null

    /*package*/
    constructor(json: Account) : super() {
        init(json)
    }

    @SuppressLint("Range")
    constructor(cursor: Cursor) {
        screenName = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME))
        val idStr: String? =
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USER_ID))
        if (!TextUtils.isEmpty(idStr)) {
            id = idStr!!.toLong()
        }

        name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME))


        profileImageUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC))
        profileImageUrlHttps = profileImageUrl
    }

    /* Only for serialization purposes. */ /*package*/
    constructor() {}

    @Throws(Exception::class)
    private fun init(json: Account) {
        try {
            id = json.id.toLong()
            name = json.displayName

            screenName = json.acct //用带域名的账号
            description = json.note

            profileImageUrl = json.avatarStatic
            profileImageUrlHttps = json.avatarStatic
            url = json.url
            followersCount = json.followersCount.toInt()
            friendsCount = json.followingCount.toInt()
            createdAt = Date((json.createdAt?.epochSecond ?: 0) * 1000)
            profileBannerImageUrl = json.headerStatic
            statusesCount = json.statusesCount.toInt()
        } catch (jsone: JSONException) {
            throw Exception(jsone.message + ":" + json.toString(), jsone)
        }
    }

    override fun compareTo(that: User): Int {
        return (id - that.id).toInt()
    }

    override fun getId(): Long {
        return id
    }

    override fun getName(): String {
        return name!!
    }

    override fun getEmail(): String? {
        return email
    }

    override fun getScreenName(): String {
        return screenName!!
    }

    override fun getLocation(): String? {
        return location
    }

    override fun getDescription(): String? {
        return description
    }

    override fun isContributorsEnabled(): Boolean {
        return isContributorsEnabled
    }

    override fun getProfileImageURL(): String? {
        return profileImageUrl
    }

    override fun getBiggerProfileImageURL(): String? {
        return profileImageUrl
    }

    override fun getMiniProfileImageURL(): String? {
        return profileImageUrl
    }

    override fun getOriginalProfileImageURL(): String? {
        return profileImageUrl
    }

    override fun get400x400ProfileImageURL(): String? {
        return profileImageUrl
    }

    override fun getProfileImageURLHttps(): String? {
        return profileImageUrlHttps
    }

    override fun getBiggerProfileImageURLHttps(): String? {
        return profileImageUrlHttps
    }

    override fun getMiniProfileImageURLHttps(): String? {
        return profileImageUrlHttps
    }

    override fun getOriginalProfileImageURLHttps(): String? {
        return profileImageUrlHttps
    }

    override fun get400x400ProfileImageURLHttps(): String? {
        return profileImageUrlHttps
    }

    override fun isDefaultProfileImage(): Boolean {
        return isDefaultProfileImage
    }

    /**
     * {@inheritDoc}
     */
    override fun getURL(): String {
        return url!!
    }

    override fun isProtected(): Boolean {
        return isProtected
    }

    override fun getFollowersCount(): Int {
        return followersCount
    }

    override fun getProfileBackgroundColor(): String {
        return profileBackgroundColor!!
    }

    override fun getProfileTextColor(): String {
        return profileTextColor!!
    }

    override fun getProfileLinkColor(): String {
        return profileLinkColor!!
    }

    override fun getProfileSidebarFillColor(): String {
        return profileSidebarFillColor!!
    }

    override fun getProfileSidebarBorderColor(): String {
        return profileSidebarBorderColor!!
    }

    override fun isProfileUseBackgroundImage(): Boolean {
        return profileUseBackgroundImage
    }

    override fun isDefaultProfile(): Boolean {
        return isDefaultProfile
    }

    /**
     * {@inheritDoc}
     */
    override fun isShowAllInlineMedia(): Boolean {
        return showAllInlineMedia
    }

    override fun getFriendsCount(): Int {
        return friendsCount
    }

    override fun getStatus(): Status? {
        return status
    }

    override fun getCreatedAt(): Date {
        return createdAt!!
    }

    override fun getFavouritesCount(): Int {
        return favouritesCount
    }

    override fun getUtcOffset(): Int {
        return utcOffset
    }

    override fun getTimeZone(): String {
        return timeZone!!
    }

    override fun getProfileBackgroundImageURL(): String {
        return profileBackgroundImageUrl!!
    }

    override fun getProfileBackgroundImageUrlHttps(): String {
        return profileBackgroundImageUrlHttps!!
    }

    override fun getProfileBannerURL(): String? {
        return profileBannerImageUrl
    }

    override fun getProfileBannerRetinaURL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/web_retina" else null
    }

    override fun getProfileBannerIPadURL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/ipad" else null
    }

    override fun getProfileBannerIPadRetinaURL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/ipad_retina" else null
    }

    override fun getProfileBannerMobileURL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/mobile" else null
    }

    override fun getProfileBannerMobileRetinaURL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/mobile_retina" else null
    }

    override fun getProfileBanner300x100URL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/300x100" else null
    }

    override fun getProfileBanner600x200URL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/600x200" else null
    }

    override fun getProfileBanner1500x500URL(): String? {
        return if (profileBannerImageUrl != null) "$profileBannerImageUrl/1500x500" else null
    }

    override fun isProfileBackgroundTiled(): Boolean {
        return profileBackgroundTiled
    }

    override fun getLang(): String {
        return lang!!
    }

    override fun getStatusesCount(): Int {
        return statusesCount
    }

    override fun isGeoEnabled(): Boolean {
        return isGeoEnabled
    }

    override fun isVerified(): Boolean {
        return isVerified
    }

    override fun isTranslator(): Boolean {
        return translator
    }

    override fun getListedCount(): Int {
        return listedCount
    }

    override fun isFollowRequestSent(): Boolean {
        return isFollowRequestSent
    }

    override fun getDescriptionURLEntities(): Array<URLEntity>? {
        return descriptionURLEntities
    }

    override fun getURLEntity(): URLEntity {
        if (urlEntity == null) {
            val plainURL = if (url == null) "" else url!!
            urlEntity = URLEntityJSONImpl(0, plainURL.length, plainURL, plainURL, plainURL)
        }
        return urlEntity!!
    }

    override fun getWithheldInCountries(): Array<String>? {
        return withheldInCountries
    }

    override fun hashCode(): Int {
        return id.toInt()
    }

    override fun equals(obj: Any?): Boolean {
        if (null == obj) {
            return false
        }
        return if (this === obj) {
            true
        } else obj is User && obj.id == id
    }

    override fun toString(): String {
        return "UserJSONImpl{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", screenName='" + screenName + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", isContributorsEnabled=" + isContributorsEnabled +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", profileImageUrlHttps='" + profileImageUrlHttps + '\'' +
                ", isDefaultProfileImage=" + isDefaultProfileImage +
                ", url='" + url + '\'' +
                ", isProtected=" + isProtected +
                ", followersCount=" + followersCount +
                ", status=" + status +
                ", profileBackgroundColor='" + profileBackgroundColor + '\'' +
                ", profileTextColor='" + profileTextColor + '\'' +
                ", profileLinkColor='" + profileLinkColor + '\'' +
                ", profileSidebarFillColor='" + profileSidebarFillColor + '\'' +
                ", profileSidebarBorderColor='" + profileSidebarBorderColor + '\'' +
                ", profileUseBackgroundImage=" + profileUseBackgroundImage +
                ", isDefaultProfile=" + isDefaultProfile +
                ", showAllInlineMedia=" + showAllInlineMedia +
                ", friendsCount=" + friendsCount +
                ", createdAt=" + createdAt +
                ", favouritesCount=" + favouritesCount +
                ", utcOffset=" + utcOffset +
                ", timeZone='" + timeZone + '\'' +
                ", profileBackgroundImageUrl='" + profileBackgroundImageUrl + '\'' +
                ", profileBackgroundImageUrlHttps='" + profileBackgroundImageUrlHttps + '\'' +
                ", profileBackgroundTiled=" + profileBackgroundTiled +
                ", lang='" + lang + '\'' +
                ", statusesCount=" + statusesCount +
                ", isGeoEnabled=" + isGeoEnabled +
                ", isVerified=" + isVerified +
                ", translator=" + translator +
                ", listedCount=" + listedCount +
                ", isFollowRequestSent=" + isFollowRequestSent +
                ", withheldInCountries=" + Arrays.toString(withheldInCountries) +
                '}'
    }

    companion object {
        private const val serialVersionUID = -5448266606847617015L

        /**
         * Get URL Entities from JSON Object.
         * returns URLEntity array by entities/[category]/urls/url[]
         *
         * @param json     user json object
         * @param category entities category. e.g. "description" or "url"
         * @return URLEntity array by entities/[category]/urls/url[]
         * @throws JSONException
         * @throws Exception
         */
        @Throws(JSONException::class, Exception::class)
        private fun getURLEntitiesFromJSON(json: JSONObject, category: String): Array<URLEntity?> {
            if (!json.isNull("entities")) {
                val entitiesJSON = json.getJSONObject("entities")
                if (!entitiesJSON.isNull(category)) {
                    val descriptionEntitiesJSON = entitiesJSON.getJSONObject(category)
                    if (!descriptionEntitiesJSON.isNull("urls")) {
                        val urlsArray = descriptionEntitiesJSON.getJSONArray("urls")
                        val len = urlsArray.length()
                        val urlEntities = arrayOfNulls<URLEntity>(len)
                        for (i in 0 until len) {
                            urlEntities[i] = URLEntityJSONImpl(urlsArray.getJSONObject(i))
                        }
                        return urlEntities
                    }
                }
            }
            return arrayOfNulls(0)
        }

        /*package*/
        @Throws(Exception::class)
        @JvmStatic
        fun createPagableUserList(
            list: HeaderPaginationList<Account>?
        ): HeaderPaginationList<User> {
            return if (list == null) {
                HeaderPaginationList()
            } else {
                val size = list.size
                val users: HeaderPaginationList<User> = HeaderPaginationList.copyOnlyPage(list)
                for (i in 0 until size) {
                    val userJson = list[i]
                    val user: User = UserJSONImplMastodon(userJson)
                    users.add(user)
                }
                users
            }

        }

    }
}