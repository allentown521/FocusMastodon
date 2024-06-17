package allen.town.focus.twitter.utils;
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

import android.text.Html;
import android.text.TextUtils;

import allen.town.focus.twitter.model.Card;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.JsonHelper;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

public class TweetLinkUtils {

    public static class TweetMediaInformation {
        public String url;
        public long duration;

        public TweetMediaInformation(String url, long duration) {
            this.url = url;
            this.duration = duration;
        }
    }

    public static String[] getLinksInStatus(Status status) {
        return getLinksInStatus(status.getText(), status.getUserMentionEntities(),
                status.getHashtagEntities(), status.getURLEntities(), status.getMediaEntities(), status.getCard());
    }


    private static String[] getLinksInStatus(String tweetTexts, UserMentionEntity[] users, HashtagEntity[] hashtags,
                                             URLEntity[] urls, MediaEntity[] medias, Card card) {
        String mUsers = JsonHelper.toJSONString(users);
        String mHashtags = "";
        String imageUrl = "";
        String otherUrl = JsonHelper.toJSONString(card);

        //这里只支持最多4个图片预览，并且不支持图片和视频/gif同时存在，其实是可以的
        if (medias != null && medias.length > 0) {
            for (MediaEntity m : medias) {
                if (m.getType().equals("video") || m.getType().equals("gif")) {
                    if (!imageUrl.contains(m.getDisplayURL())) {
                        imageUrl += m.getDisplayURL() + " ";
                    }
                } else if (m.getType().equals("photo")) {
                    if (!imageUrl.contains(m.getMediaURL())) {
                        imageUrl += m.getMediaURL() + " ";
                    }
                }

            }
        }


        if (hashtags != null && hashtags.length > 0) {
            for (HashtagEntity h : hashtags) {
                if (!TextUtils.isEmpty(h.getUrl())) {
                    mHashtags += h.getUrl() + " ";
                }
            }
        }


        return new String[]{tweetTexts, imageUrl, otherUrl, mHashtags, mUsers};
    }


    public static String removeColorHtml(String text, AppSettings settings) {
        return Html.fromHtml(text).toString();
    }

    public static TweetMediaInformation getGIFUrl(Status status, String otherUrls) {
        return getGIFUrl(status.getMediaEntities(), otherUrls);
    }

    public static TweetMediaInformation getGIFUrl(MediaEntity[] entities, String otherUrls) {

        for (MediaEntity e : entities) {
            if (e.getType().contains("gif")) {
                return new TweetMediaInformation(e.getMediaURL(), e.getVideoDurationMillis());
            } else if (e.getType().equals("surfaceView") || e.getType().equals("video")) {
                return new TweetMediaInformation(e.getMediaURL(), e.getVideoDurationMillis());
            }
        }

        // otherwise, lets just go with a blank string
        return new TweetMediaInformation("", -1);
    }

    public static long getTweetIdFromLink(String link) {
        if (!link.contains("/status/")) {
            return 0l;
        } else {
            int index = link.indexOf("/status/") + "/status/".length();

            String id = link.substring(index);

            if (id.contains("?")) {
                try {
                    id = id.substring(id.indexOf("?"));
                    id = id.replace("?", "");
                } catch (Exception e) {

                }
            }

            try {
                return Long.parseLong(id);
            } catch (Exception e) {
                return 0l;
            }
        }
    }
}
