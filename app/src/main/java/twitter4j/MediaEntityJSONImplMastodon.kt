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

import allen.town.focus.twitter.model.Attachment

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.2.3
 */
class MediaEntityJSONImplMastodon : EntityIndex, MediaEntity {
    @JvmField protected var id: Long = 0
    protected var url: String? = null
    private var mediaURL: String? = null
    private var mediaURLHttps: String? = null
    private var expandedURL: String? = null
    private var displayURL: String? = null
    private var sizes: MutableMap<Int, MediaEntity.Size>? = null
    @JvmField protected var type: String? = null
    private var videoAspectRatioWidth = 0
    private var videoAspectRatioHeight = 0
    private var videoDurationMillis: Long = 0
    private var videoVariants: Array<MediaEntity.Variant?>? = null
    private var extAltText: String? = null

    constructor(json: Attachment) {
        try {
            type = when (json.type) {
                Attachment.Type.IMAGE -> "photo"
                Attachment.Type.VIDEO -> "video"
                Attachment.Type.GIFV -> "gif"
                else -> "unknown"
            }
            url = json.url ?: json.remoteUrl
            mediaURL = url
            mediaURLHttps = url
            displayURL = json.previewUrl
            sizes = HashMap(1)
            sizes!!.put(
                MediaEntity.Size.LARGE,
                MediaEntityJSONImpl.Size(json.meta?.original?.width ?: 0,json.meta?.original?.height ?: 0))
        } catch (jsone: JSONException) {
            throw Exception(jsone)
        }
    }


    /* For serialization purposes only. */ /* package */
    internal constructor() {}

    override fun getId(): Long {
        return id
    }

    override fun getMediaURL(): String? {
        return mediaURL
    }

    override fun getMediaURLHttps(): String? {
        return mediaURLHttps
    }

    override fun getText(): String? {
        return url
    }

    override fun getURL(): String? {
        return url
    }

    override fun getDisplayURL(): String? {
        return displayURL
    }

    override fun getExpandedURL(): String? {
        return expandedURL
    }

    override fun getSizes(): Map<Int, MediaEntity.Size>? {
        return sizes
    }

    override fun getType(): String {
        return type!!
    }

    override fun getStart(): Int {
        return super.getStart()
    }

    override fun getEnd(): Int {
        return super.getEnd()
    }

    override fun getVideoAspectRatioWidth(): Int {
        return videoAspectRatioWidth
    }

    override fun getVideoAspectRatioHeight(): Int {
        return videoAspectRatioHeight
    }

    override fun getVideoDurationMillis(): Long {
        return videoDurationMillis
    }

    override fun getExtAltText(): String {
        return extAltText!!
    }

    override fun getVideoVariants(): Array<MediaEntity.Variant?>? {
        return videoVariants
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is MediaEntityJSONImplMastodon) return false
        return if (id != o.id) false else true
    }

    override fun hashCode(): Int {
        return (id xor (id ushr 32)).toInt()
    }

    override fun toString(): String {
        return "MediaEntityJSONImpl{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", mediaURL='" + mediaURL + '\'' +
                ", mediaURLHttps='" + mediaURLHttps + '\'' +
                ", expandedURL='" + expandedURL + '\'' +
                ", displayURL='" + displayURL + '\'' +
                ", sizes=" + sizes +
                ", type='" + type + '\'' +
                ", videoAspectRatioWidth=" + videoAspectRatioWidth +
                ", videoAspectRatioHeight=" + videoAspectRatioHeight +
                ", videoDurationMillis=" + videoDurationMillis +
                ", videoVariants=" + videoVariants?.size +
                ", extAltText='" + extAltText + '\'' +
                '}'
    }

    companion object {
        private const val serialVersionUID = 1571961225214439778L
    }
}