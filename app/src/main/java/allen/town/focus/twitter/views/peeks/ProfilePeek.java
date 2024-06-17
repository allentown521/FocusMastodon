package allen.town.focus.twitter.views.peeks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.SimpleOnPeek;

import java.util.ArrayList;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.api.requests.accounts.GetAccountByID;
import allen.town.focus.twitter.api.requests.accounts.GetAccountRelationships;
import allen.town.focus.twitter.model.Relationship;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.Utils;
import code.name.monkey.appthemehelper.util.ATHUtil;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class ProfilePeek extends SimpleOnPeek {

    public static void create(Context context, View view, String userId) {
        if (context instanceof PeekViewActivity) {
            PeekViewOptions options = new PeekViewOptions()
                    .setAbsoluteWidth(225)
                    .setAbsoluteHeight(279);

            Peek.into(R.layout.peek_profile, new ProfilePeek(userId))
                    .with(options)
                    .applyTo((PeekViewActivity) context, view);
        }
    }

    private String userId;

    private ImageView profilePicture;
    private ImageView bannerImage;
    private ImageView verified;
    private TextView realName;
    private TextView screenName;
    private TextView description;
    private TextView location;
    private TextView followerCount;
    private TextView friendCount;
    private TextView tweetCount;
    private TextView followingStatus;

    private ProfilePeek(String userId) {
        this.userId = userId;
    }

    @Override
    public void onInflated(View rootView) {
        profilePicture = (ImageView) rootView.findViewById(R.id.profile_pic);
        bannerImage = (ImageView) rootView.findViewById(R.id.banner);
        verified = (ImageView) rootView.findViewById(R.id.verified);
        realName = (TextView) rootView.findViewById(R.id.real_name);
        screenName = (TextView) rootView.findViewById(R.id.screen_name);
        location = (TextView) rootView.findViewById(R.id.location);
        description = (TextView) rootView.findViewById(R.id.description);
        followerCount = (TextView) rootView.findViewById(R.id.followers_count);
        friendCount = (TextView) rootView.findViewById(R.id.following_count);
        tweetCount = (TextView) rootView.findViewById(R.id.tweet_count);
        followingStatus = (TextView) rootView.findViewById(R.id.following_status);

        final Activity activity = (Activity) rootView.getContext();
        if (!ATHUtil.isWindowBackgroundDark(activity)) {
            int color = rootView.getResources().getColor(R.color.light_text);
            location.setTextColor(color);
            description.setTextColor(color);
            followerCount.setTextColor(color);
            friendCount.setTextColor(color);
            tweetCount.setTextColor(color);
            followingStatus.setTextColor(color);

            ((TextView) rootView.findViewById(R.id.tweets_label)).setTextColor(color);
            ((TextView) rootView.findViewById(R.id.followers_label)).setTextColor(color);
            ((TextView) rootView.findViewById(R.id.following_label)).setTextColor(color);
        }

        new TimeoutThread(() -> {
            try {
                final User user = new UserJSONImplMastodon(new GetAccountByID(userId).execSync());

                activity.runOnUiThread(() -> {
                    if (activity.isDestroyed()) {
                        return;
                    }

                    Glide.with(activity).load(user.getOriginalProfileImageURL()).into(profilePicture);
                    Glide.with(activity).load(user.getProfileBannerURL()).into(bannerImage);

                    realName.setText(user.getName());
                    screenName.setText("@" + user.getScreenName());
                    description.setText(user.getDescription());
                    HtmlParser.linkifyText(description, null, null, false);

                    followerCount.setText(
                            user.getFollowersCount() < 1000 ?
                                    "" + user.getFollowersCount() :
                                    Utils.coolFormat(user.getFollowersCount(), 0));

                    friendCount.setText(
                            user.getFriendsCount() < 1000 ?
                                    "" + user.getFriendsCount() :
                                    Utils.coolFormat(user.getFriendsCount(), 0));

                    tweetCount.setText(
                            user.getStatusesCount() < 1000 ?
                                    "" + user.getStatusesCount() :
                                    Utils.coolFormat(user.getStatusesCount(), 0));


                    if (user.isVerified()) {
                        verified.setVisibility(View.VISIBLE);
                        verified.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                    }
                });

                final Relationship friendship = new GetAccountRelationships(new ArrayList<>() {
                    {
                        add(userId);
                    }
                }).execSync().get(0);
                activity.runOnUiThread(() -> {
                    if (friendship.followedBy) {
                        followingStatus.setText(activity.getString(R.string.follows_you));
                    } else {
                        followingStatus.setText(activity.getString(R.string.not_following_you));
                    }
                });
            } catch (Exception e) {

            }
        }).start();
    }
}
