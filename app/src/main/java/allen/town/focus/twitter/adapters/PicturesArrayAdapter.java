package allen.town.focus.twitter.adapters;
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

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.ImageViewerActivity;
import allen.town.focus.twitter.activities.tweet_viewer.TweetActivity;
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper;
import allen.town.focus.twitter.utils.TweetLinkUtils;

import java.util.ArrayList;

import allen.town.focus_common.util.JsonHelper;
import twitter4j.Status;
import twitter4j.User;

public class PicturesArrayAdapter extends ArrayAdapter<String> {

    protected Context context;

    private ArrayList<String> text;
    private ArrayList<Status> statuses;

    private LayoutInflater inflater;

    public static class ViewHolder {
        public ImageView iv;
        public String url;
    }

    public PicturesArrayAdapter(Context context, ArrayList<String> text, ArrayList<Status> statuses) {
        super(context, R.layout.picture);

        this.context = context;
        this.text = text;
        this.statuses = statuses;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        try {
            return text.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.picture, viewGroup, false);

        holder = new ViewHolder();

        holder.iv = (ImageView) v.findViewById(R.id.picture);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final String url, final Status status) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        Status thisStatus;

        String retweeter;
        final long time = status.getCreatedAt().getTime();
        long originalTime = 0;

        if (status.isRetweet()) {
            retweeter = status.getUser().getScreenName();

            thisStatus = status.getRetweetedStatus();
            originalTime = thisStatus.getCreatedAt().getTime();
        } else {
            retweeter = "";

            thisStatus = status;
        }
        final String fRetweeter = retweeter;

        final long fOriginalTime = originalTime;

        User user = thisStatus.getUser();

        final long id = thisStatus.getId();
        final String profilePic = user.getBiggerProfileImageURL();
        String tweetTexts = thisStatus.getText();
        final String name = user.getName();
        final String screenname = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(thisStatus);
        final String tweetText = html[0];
        final String picUrl = html[1];
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];
        final String statusUrl = thisStatus.getStatusUrl();
        final String emoji = JsonHelper.toJSONString(thisStatus.getEmoji());

        holder.url = url;

        Glide.with(context).load(url).into(holder.iv);

        holder.iv.setOnClickListener(view1 -> {
            String link;

            boolean displayPic = !picUrl.equals("");
            if (displayPic) {
                link = picUrl;
            } else {
                link = otherUrl.split("  ")[0];
            }

            Intent viewTweet = new Intent(context, TweetActivity.class);
            viewTweet.putExtra("name", name);
            viewTweet.putExtra("screenname", screenname);
            viewTweet.putExtra("time", time);
            viewTweet.putExtra("tweet", tweetText);
            viewTweet.putExtra("retweeter", fRetweeter);
            viewTweet.putExtra("webpage", link);
            viewTweet.putExtra("other_links", otherUrl);
            viewTweet.putExtra("picture", displayPic);
            viewTweet.putExtra("tweetid", id);
            viewTweet.putExtra("proPic", profilePic);
            viewTweet.putExtra("users", users);
            viewTweet.putExtra("hashtags", hashtags);
            viewTweet.putExtra(HomeSQLiteHelper.COLUMN_STATUS_URL, statusUrl);
            viewTweet.putExtra(HomeSQLiteHelper.COLUMN_EMOJI, emoji);

            context.startActivity(viewTweet);
        });

    }

    public void bindView(final View view, Context mContext, final String url) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        Glide.with(context).load(url).into(holder.iv);

        holder.iv.setOnClickListener(view1 -> ImageViewerActivity.Companion.startActivity(context, url));

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();
            holder.iv.setImageDrawable(null);
        }

        if (statuses != null) {
            bindView(v, context, text.get(position), statuses.get(position));
        } else {
            bindView(v, context, text.get(position));
        }

        return v;
    }

    public String getElement(int pos) {
        return text.get(pos);
    }
}
