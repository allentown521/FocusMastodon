package allen.town.focus.twitter.data;
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

public class Tweet {
    private long id;
    private String text;
    private String name;
    private String picUrl;
    private String userId;
    private String screenName;
    private long time;
    private String retweeter;
    private String website;
    private String otherWeb;
    private String users;
    private String hashtags;
    private String animatedGif;

    public String getText() {
        return text;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    private String statusUrl;
    private String emoji;

    public String getEmoji() {
        return emoji;
    }

    public Tweet(long id, String text, String name, String picUrl, String userId, String screenName, long time,
                 String retweeter, String webpage, String otherWeb, String users, String hashtags, String animatedGif, String statusUrl, String emoji) {
        this.id = id;
        this.text = text;
        this.name = name;
        this.screenName = screenName;
        this.userId = userId;
        this.picUrl = picUrl;
        this.time = time;
        this.retweeter = retweeter;
        this.website = webpage;
        this.otherWeb = otherWeb;
        this.users = users;
        this.hashtags = hashtags;
        this.animatedGif = animatedGif;
        this.statusUrl = statusUrl;
        this.emoji = emoji;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTweet() {
        return text;
    }

    public void setTweet(String tweet) {
        this.text = tweet;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicUrl() {
        return this.picUrl;
    }

    public void setPicUrl(String url) {
        this.picUrl = url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String name) {
        this.userId = name;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String name) {
        this.screenName = name;
    }

    public long getTime() {
        return time;
    }

    public String getRetweeter() {
        return retweeter;
    }

    public String getWebsite() {
        return website;
    }

    public String getOtherWeb() {
        return otherWeb;
    }

    public String getUsers() {
        return users;
    }

    public String getHashtags() {
        return hashtags;
    }

    public String getAnimatedGif() {
        return animatedGif;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return text;
    }
}
