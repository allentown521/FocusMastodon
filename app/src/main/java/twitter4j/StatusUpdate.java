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

package twitter4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import allen.town.focus.twitter.api.requests.statuses.CreateStatus;
import allen.town.focus.twitter.model.StatusPrivacy;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.1
 */
public final class StatusUpdate implements java.io.Serializable {

    private static final long serialVersionUID = 7422094739799350035L;
    private final String status;
    private String inReplyToStatusId = null;
    private GeoLocation location = null;
    private String placeId = null;
    private boolean displayCoordinates = true;
    private boolean possiblySensitive;
    private String mediaName;
    private transient InputStream mediaBody;
    private File mediaFile;
    private long[] mediaIds;
    private boolean autoPopulateReplyMetadata;
    private String attachmentUrl = null;
    private String language;
    public String spoilerText;
    public StatusPrivacy visibility;
    public List<MediaToSend> media;

    public void setInReplyToStatusId(String inReplyToStatusId) {
        this.inReplyToStatusId = inReplyToStatusId;
    }


    public CreateStatus.Request.Poll getPoll() {
        return poll;
    }

    public void setPoll(CreateStatus.Request.Poll poll) {
        this.poll = poll;
    }

    public CreateStatus.Request.Poll poll;

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public InputStream getMediaBody() {
        return mediaBody;
    }

    public void setMediaBody(InputStream mediaBody) {
        this.mediaBody = mediaBody;
    }

    public File getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(File mediaFile) {
        this.mediaFile = mediaFile;
    }

    public long[] getMediaIds() {
        return mediaIds;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSpoilerText() {
        return spoilerText;
    }

    public void setSpoilerText(String spoilerText) {
        this.spoilerText = spoilerText;
    }

    public StatusPrivacy getVisibility() {
        return visibility;
    }

    public void setVisibility(StatusPrivacy visibility) {
        this.visibility = visibility;
    }

    public StatusUpdate(String status) {
        this.status = status;
    }

    public StatusUpdate(String status, StatusPrivacy visibility) {
        this.status = status;
        setVisibility(visibility);
    }

    public String getStatus() {
        return status;
    }

    public String getInReplyToStatusId() {
        return inReplyToStatusId;
    }

    public void setInReplyToStatusId(long inReplyToStatusId) {
        this.inReplyToStatusId = inReplyToStatusId + "";
    }

    public StatusUpdate inReplyToStatusId(long inReplyToStatusId) {
        setInReplyToStatusId(inReplyToStatusId);
        return this;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public void setLocation(GeoLocation location) {
        this.location = location;
    }

    public StatusUpdate location(GeoLocation location) {
        setLocation(location);
        return this;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public StatusUpdate placeId(String placeId) {
        setPlaceId(placeId);
        return this;
    }

    public boolean isDisplayCoordinates() {
        return displayCoordinates;
    }

    public void setDisplayCoordinates(boolean displayCoordinates) {
        this.displayCoordinates = displayCoordinates;
    }

    public StatusUpdate displayCoordinates(boolean displayCoordinates) {
        setDisplayCoordinates(displayCoordinates);
        return this;
    }

    /**
     * @param file media file
     * @since Twitter4J 2.2.5
     */
    public void setMedia(File file) {
        this.mediaFile = file;
    }

    /**
     * @param file media file
     * @return this instance
     * @since Twitter4J 2.2.5
     */
    public StatusUpdate media(File file) {
        setMedia(file);
        return this;
    }

    /**
     * @param name name
     * @param body media body as stream
     * @since Twitter4J 2.2.5
     */
    public void setMedia(String name, InputStream body) {
        this.mediaName = name;
        this.mediaBody = body;
    }

    /**
     * @param mediaIds media ids
     * @since Twitter4J 4.0.2
     */
    public void setMediaIds(long... mediaIds) {
        this.mediaIds = mediaIds;
        this.media = new ArrayList<>();
        for (long id :
                mediaIds) {
            media.add(new MediaToSend(id + "", false));
        }
    }

    /**
     * @return attachment url
     * @since Twitter4J 4.0.7
     */
    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    /**
     * @param attachmentUrl attachment url
     * @since Twitter4J 4.0.7
     */
    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    /**
     * @param attachmentUrl attachment url
     * @return status update
     * @since Twitter4J 4.0.7
     */
    public StatusUpdate attachmentUrl(String attachmentUrl) {
        setAttachmentUrl(attachmentUrl);
        return this;
    }

    /*package*/ boolean isForUpdateWithMedia() {
        return mediaFile != null || mediaName != null;
    }

    /**
     * @param name media name
     * @param body media body
     * @return this instance
     * @since Twitter4J 2.2.5
     */
    public StatusUpdate media(String name, InputStream body) {
        setMedia(name, body);
        return this;
    }

    /**
     * @param possiblySensitive possibly sensitive
     * @since Twitter4J 2.2.5
     */
    public void setPossiblySensitive(boolean possiblySensitive) {
        this.possiblySensitive = possiblySensitive;
    }

    /**
     * @param possiblySensitive possibly sensitive
     * @return this instance
     * @since Twitter4J 2.2.5
     */
    public StatusUpdate possiblySensitive(boolean possiblySensitive) {
        setPossiblySensitive(possiblySensitive);
        return this;
    }

    /**
     * @return possibly sensitive
     * @since Twitter4J 2.2.5
     */
    public boolean isPossiblySensitive() {
        return possiblySensitive;
    }

    /**
     * @return autoPopulateReplyMetadata
     * @since Twitter4J 4.0.7
     */
    public boolean isAutoPopulateReplyMetadata() {
        return autoPopulateReplyMetadata;
    }

    /**
     * @param autoPopulateReplyMetadata auto reply meta data
     * @since Twitter4J 4.0.7
     */
    public void setAutoPopulateReplyMetadata(boolean autoPopulateReplyMetadata) {
        this.autoPopulateReplyMetadata = autoPopulateReplyMetadata;
    }

    /**
     * @param autoPopulateReplyMetadata auto reply meta data
     * @return this instance
     * @since Twitter4J 4.0.7
     */
    public StatusUpdate autoPopulateReplyMetadata(boolean autoPopulateReplyMetadata) {
        setAutoPopulateReplyMetadata(autoPopulateReplyMetadata);
        return this;
    }





    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatusUpdate that = (StatusUpdate) o;

        if (inReplyToStatusId != that.inReplyToStatusId) return false;
        if (displayCoordinates != that.displayCoordinates) return false;
        if (possiblySensitive != that.possiblySensitive) return false;
        if (autoPopulateReplyMetadata != that.autoPopulateReplyMetadata) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        if (placeId != null ? !placeId.equals(that.placeId) : that.placeId != null) return false;
        if (mediaName != null ? !mediaName.equals(that.mediaName) : that.mediaName != null)
            return false;
        if (mediaBody != null ? !mediaBody.equals(that.mediaBody) : that.mediaBody != null)
            return false;
        if (mediaFile != null ? !mediaFile.equals(that.mediaFile) : that.mediaFile != null)
            return false;
        if (!Arrays.equals(mediaIds, that.mediaIds)) return false;
        return attachmentUrl != null ? attachmentUrl.equals(that.attachmentUrl) : that.attachmentUrl == null;
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (placeId != null ? placeId.hashCode() : 0);
        result = 31 * result + (displayCoordinates ? 1 : 0);
        result = 31 * result + (possiblySensitive ? 1 : 0);
        result = 31 * result + (mediaName != null ? mediaName.hashCode() : 0);
        result = 31 * result + (mediaBody != null ? mediaBody.hashCode() : 0);
        result = 31 * result + (mediaFile != null ? mediaFile.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(mediaIds);
        result = 31 * result + (autoPopulateReplyMetadata ? 1 : 0);
        result = 31 * result + (attachmentUrl != null ? attachmentUrl.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StatusUpdate{" +
                "status='" + status + '\'' +
                ", inReplyToStatusId=" + inReplyToStatusId +
                ", location=" + location +
                ", placeId='" + placeId + '\'' +
                ", displayCoordinates=" + displayCoordinates +
                ", possiblySensitive=" + possiblySensitive +
                ", mediaName='" + mediaName + '\'' +
                ", mediaBody=" + mediaBody +
                ", mediaFile=" + mediaFile +
                ", mediaIds=" + Arrays.toString(mediaIds) +
                ", autoPopulateReplyMetadata=" + autoPopulateReplyMetadata +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                '}';
    }
}
