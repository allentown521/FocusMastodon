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
import allen.town.focus.twitter.model.*
import allen.town.focus.twitter.utils.HtmlParser
import allen.town.focus_common.util.JsonHelper
import android.annotation.SuppressLint
import android.database.Cursor
import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * A data class representing one single status of a user.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
/*package*/
internal class StatusJSONImplMastodon : TwitterResponseImpl, Status {
    private var createdAt: Date? = null
    private var id: Long = 0
    private var text: String? = null
    private var displayTextRangeStart = -1
    private var displayTextRangeEnd = -1
    private var source: String? = null
    private var isTruncated = false
    private var inReplyToStatusId: Long = 0
    private var inReplyToUserId: Long = 0
    private var isFavorited = false
    private var isRetweeted = false
    private var favoriteCount = 0
    private var inReplyToScreenName: String? = null
    private var geoLocation: GeoLocation? = null
    private var place: Place? = null

    @Transient
    private var strippedText: String? = null

    // this field should be int in theory, but left as long for the serialized form compatibility - TFJ-790
    private var retweetCount: Long = 0
    private var repliesCount: Long = 0
    private var isPossiblySensitive = false
    private var lang: String? = null
    private var contributorsIDs: LongArray? = null
    private var retweetedStatus: Status? = null
    private var userMentionEntities: Array<UserMentionEntity>? = null
    private lateinit var originalStatus: allen.town.focus.twitter.model.Status
    private var urlEntities: Array<URLEntity?>? = null
    private var hashtagEntities: Array<HashtagEntity?>? = null
    private var emojis: List<Emoji>? = null
    private var mediaEntities: Array<MediaEntity?>? = null
    private var symbolEntities: Array<SymbolEntity?>? = null
    private var currentUserRetweetId = -1L
    private var scopes: Scopes? = null
    private var user: User? = null
    private var withheldInCountries: Array<String>? = null
    private var quotedStatus: Status? = null
    private var quotedStatusId = -1L
    private var quotedStatusPermalink: URLEntity? = null

    private var statusUrl: String? = null
    private var spoilerText: String? = null
    private var visibility: StatusPrivacy? = null
    private var poll: Poll? = null
    private var isBookmarked = false
    private var retweeterAccount: ReTweeterAccount? = null
    private var notiId: String? = null
    private var card: Card? = null


    @SuppressLint("Range")
    constructor(cursor: Cursor) : super() {
        id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID))
        source = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CLIENT_SOURCE))
        createdAt = Date(cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME)))
        isFavorited = cursor.getInt(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_FAVOURITED)) == 1
        isRetweeted = cursor.getInt(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_REBLOGGED)) == 1
        isBookmarked = cursor.getInt(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_BOOKMARKED)) == 1
        statusUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_STATUS_URL))
        poll = JsonHelper.parseObject(
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_POLL)),
            Poll::class.java
        )
        visibility =
            byString(cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_VISIBILITY)))
        spoilerText = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SPOILER_TEXT))
        val retweeterStr: String? =
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER))
        retweeterAccount = JsonHelper.parseObject(
            retweeterStr,
            ReTweeterAccount::class.java
        )
        card = JsonHelper.parseObject(
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL)),
            Card::class.java
        )
        user = UserJSONImplMastodon(cursor)

        val hashTagStr: String? =
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS))
        if (!TextUtils.isEmpty(hashTagStr)) {
            val hashList = hashTagStr!!.split(" ")
            val len: Int = hashList.size
            if (len > 0) {
                hashtagEntities = arrayOfNulls(len)
                for (i in 0 until len) {
                    if (!TextUtils.isEmpty(hashList[i])) {
                        hashtagEntities!![i] = HashtagEntityJSONImplMastodon(
                            hashList[i].substring(
                                hashList[i].lastIndexOf("/")
                            ), hashList[i]
                        )
                    }
                }
            }
        }

        val mentionStr: String? =
            cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS))
        if (!TextUtils.isEmpty(mentionStr)) {
            userMentionEntities = JsonHelper.parseObjectList(
                mentionStr,
                object : TypeToken<Array<UserMentionEntityJSONImplMastodon>>() {
                }.type
            )
        }


        repliesCount = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_REPLIES_COUNT))
        retweetCount = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_REBLOGS_COUNT))
        favoriteCount =
            cursor.getInt(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT))
        isPossiblySensitive =
            cursor.getInt(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SENSITIVE)) == 1


        text = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT))
        lang = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_LANGUAGE))
    }

    /**
     * 有些需要存notiId,conversationId
     */
    constructor(json: allen.town.focus.twitter.model.Status, notiId: String) : super() {
        init(json)
        this.notiId = notiId
    }

    constructor(json: allen.town.focus.twitter.model.Status) : super() {
        init(json)
    }

    /* Only for serialization purposes. */ /*package*/
    constructor() {}

    @Throws(Exception::class)
    private fun init(json: allen.town.focus.twitter.model.Status) {
        id = json.id.toLong()
        source = json.application?.name ?: ""
        createdAt = Date(((json.createdAt?.epochSecond) ?: 0) * 1000)
        inReplyToStatusId = json.inReplyToId?.run {
            toLong()
        } ?: -1
        inReplyToUserId = json.inReplyToAccountId?.run {
            toLong()
        } ?: -1
        isFavorited = json.favourited
        isRetweeted = json.reblogged
        isBookmarked = json.bookmarked
        statusUrl = json.url
        poll = json.poll
        visibility = json.visibility
        spoilerText = json.spoilerText
        card = json.card

        // Try to complete mastodon `in_reply_to` info
        val inReplyToMention = json.mentions?.firstOrNull {
            it.id == json.inReplyToAccountId
        }
        if (inReplyToMention != null) {
            inReplyToScreenName = inReplyToMention.username
        }

        repliesCount = json.repliesCount
        retweetCount = json.reblogsCount
        favoriteCount = json.favouritesCount.toInt()
        isPossiblySensitive = json.sensitive
        user = UserJSONImplMastodon(json.account)
        retweetedStatus = json.reblog?.run {
            StatusJSONImplMastodon(this)
        }

        emojis = json.emojis
        hashtagEntities = EntitiesParseUtil.getHashtagsMastodon(json.tags)
        userMentionEntities = EntitiesParseUtil.getUserMentionsMastodon(json.mentions)

        mediaEntities = EntitiesParseUtil.getMedia(json.mediaAttachments)
        urlEntities = if (urlEntities == null) arrayOfNulls(0) else urlEntities
        hashtagEntities = if (hashtagEntities == null) arrayOfNulls(0) else hashtagEntities
        symbolEntities = if (symbolEntities == null) arrayOfNulls(0) else symbolEntities
        mediaEntities = if (mediaEntities == null) arrayOfNulls(0) else mediaEntities
        text = json.content
        lang = json.language
        originalStatus = json
    }


    override fun compareTo(that: Status): Int {
        val delta = id - that.id
        if (delta < Int.MIN_VALUE) {
            return Int.MIN_VALUE
        } else if (delta > Int.MAX_VALUE) {
            return Int.MAX_VALUE
        }
        return delta.toInt()
    }

    override fun getCreatedAt(): Date? {
        return createdAt
    }

    override fun getId(): Long {
        return id
    }

    override fun getText(): String? {
        return text
    }

    override fun getDisplayTextRangeStart(): Int {
        return displayTextRangeStart
    }

    override fun getDisplayTextRangeEnd(): Int {
        return displayTextRangeEnd
    }

    override fun getSource(): String? {
        return source
    }

    override fun isTruncated(): Boolean {
        return isTruncated
    }

    override fun getInReplyToStatusId(): Long {
        return inReplyToStatusId
    }

    override fun getInReplyToUserId(): Long {
        return inReplyToUserId
    }

    override fun getInReplyToScreenName(): String? {
        return inReplyToScreenName
    }

    override fun getGeoLocation(): GeoLocation? {
        return geoLocation
    }

    override fun getPlace(): Place? {
        return place
    }

    override fun getContributors(): LongArray? {
        return contributorsIDs
    }

    override fun isFavorited(): Boolean {
        return isFavorited
    }

    /**
     * 被我转推
     */
    override fun isRetweeted(): Boolean {
        return isRetweeted
    }

    override fun getFavoriteCount(): Int {
        return favoriteCount
    }

    override fun getUser(): User? {
        return user
    }

    /**
     * 是转推
     */
    override fun isRetweet(): Boolean {
        return retweetedStatus != null
    }

    override fun getRetweetedStatus(): Status? {
        return retweetedStatus
    }

    override fun getRetweetCount(): Int {
        return retweetCount.toInt()
    }

    override fun isRetweetedByMe(): Boolean {
        return isRetweeted
    }

    override fun getCurrentUserRetweetId(): Long {
        return currentUserRetweetId
    }

    override fun isPossiblySensitive(): Boolean {
        return isPossiblySensitive
    }

    override fun getUserMentionEntities(): Array<UserMentionEntity>? {
        return userMentionEntities
    }

    override fun getUserMentionEntitiesList(): List<UserMentionEntity> {
        val list = ArrayList<UserMentionEntity>()
        if (userMentionEntities != null && userMentionEntities!!.size > 0) {
            for (mention in userMentionEntities!!)
                list.add(mention)
        }
        return list
    }

    override fun getURLEntities(): Array<URLEntity?>? {
        return urlEntities
    }

    override fun getHashtagEntities(): Array<HashtagEntity?>? {
        return hashtagEntities
    }

    override fun getMediaEntities(): Array<MediaEntity?>? {
        return mediaEntities
    }

    override fun getSymbolEntities(): Array<SymbolEntity?>? {
        return symbolEntities
    }

    override fun getCard(): Card? {
        return card
    }

    override fun getScopes(): Scopes? {
        return scopes
    }

    override fun getWithheldInCountries(): Array<String>? {
        return withheldInCountries
    }

    override fun getQuotedStatusId(): Long {
        return quotedStatusId
    }

    override fun getQuotedStatus(): Status? {
        return quotedStatus
    }

    override fun getEmoji(): List<Emoji>? {
        return emojis
    }

    override fun getQuotedStatusPermalink(): URLEntity? {
        return quotedStatusPermalink
    }

    override fun getStrippedText(): String? {
        if (strippedText == null) strippedText = text?.run {
            HtmlParser.strip(text)
        }
        return strippedText
    }

    override fun setPoll(poll: Poll?) {
        this.poll = poll
    }

    override fun getRetwitterFormatUrl(): String? {
        var url: String? = null
        if (isRetweet && getUser() != null) {
            val originalName = getUser()!!.screenName
            val id = getUser()!!.id
            url = ReTweeterAccount.getRetweeterFormatUrl(originalName, "$id")

        }
        return url
    }

    override fun isBookmarked(): Boolean {
        return isBookmarked
    }

    override fun getSpoilerText(): String? {
        return spoilerText
    }

    override fun getVisibility(): StatusPrivacy? {
        return visibility
    }

    override fun getPoll(): Poll? {
        return poll
    }

    override fun getStatusUrl(): String? {
        return statusUrl
    }

    override fun getRepliesCount(): Long {
        return repliesCount
    }

    override fun getReTweeterAccount(): ReTweeterAccount? {
        return retweeterAccount
    }

    override fun getNotiId(): String? {
        return notiId
    }

    override fun getLang(): String? {
        return lang
    }

    fun getOrginalStatus(): allen.town.focus.twitter.model.Status {
        return originalStatus
    }

    fun getContentStatus(): Status? {
        return if (retweetedStatus != null) retweetedStatus else this
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
        } else obj is Status && obj.id == id
    }

    override fun toString(): String {
        return "StatusJSONImpl{" +
                "createdAt=" + createdAt +
                ", id=" + id +
                ", text='" + text + '\'' +
                ", source='" + source + '\'' +
                ", isTruncated=" + isTruncated +
                ", inReplyToStatusId=" + inReplyToStatusId +
                ", inReplyToUserId=" + inReplyToUserId +
                ", isFavorited=" + isFavorited +
                ", isRetweeted=" + isRetweeted +
                ", favoriteCount=" + favoriteCount +
                ", inReplyToScreenName='" + inReplyToScreenName + '\'' +
                ", geoLocation=" + geoLocation +
                ", place=" + place +
                ", retweetCount=" + retweetCount +
                ", isPossiblySensitive=" + isPossiblySensitive +
                ", lang='" + lang + '\'' +
                ", contributorsIDs=" + Arrays.toString(contributorsIDs) +
                ", retweetedStatus=" + retweetedStatus +
                ", userMentionEntities=" + Arrays.toString(userMentionEntities) +
                ", urlEntities=" + Arrays.toString(urlEntities) +
                ", hashtagEntities=" + Arrays.toString(hashtagEntities) +
                ", mediaEntities=" + Arrays.toString(mediaEntities) +
                ", symbolEntities=" + Arrays.toString(symbolEntities) +
                ", currentUserRetweetId=" + currentUserRetweetId +
                ", user=" + user +
                ", withHeldInCountries=" + Arrays.toString(withheldInCountries) +
                ", quotedStatusId=" + quotedStatusId +
                ", quotedStatus=" + quotedStatus +
                '}'
    }

    companion object {
        private const val serialVersionUID = -6461195536943679985L

        @JvmStatic
        fun byString(s: String?): StatusPrivacy {
            return when (s) {
                "public" -> StatusPrivacy.PUBLIC
                "unlisted" -> StatusPrivacy.UNLISTED
                "private" -> StatusPrivacy.PRIVATE
                "direct" -> StatusPrivacy.DIRECT
                else -> StatusPrivacy.PUBLIC
            }
        }

        /*package*/
        @JvmStatic
        fun createStatusList(statuses: HeaderPaginationList<allen.town.focus.twitter.model.Status>?): HeaderPaginationList<StatusJSONImplMastodon> {
            return if (statuses == null) {
                return HeaderPaginationList()
            } else {
                val results: HeaderPaginationList<StatusJSONImplMastodon> =
                    HeaderPaginationList.copyOnlyPage(statuses)

                for (status in statuses) {
                    results.add(StatusJSONImplMastodon(status))
                }
                results
            }
        }
    }
}