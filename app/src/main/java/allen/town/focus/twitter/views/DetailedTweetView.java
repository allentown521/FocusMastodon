package allen.town.focus.twitter.views;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.api.requests.statuses.GetStatusByID;
import allen.town.focus.twitter.utils.TweetButtonUtils;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;

import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class DetailedTweetView extends TweetView {

    public static DetailedTweetView create(final Context context, final long tweetId) {
        final DetailedTweetView tweetView = new DetailedTweetView(context);
        final AppSettings settings = AppSettings.getInstance(context);

        tweetView.setCurrentUser(settings.myScreenName);

        new TimeoutThread(() -> {
            try {
                final Status status = new StatusJSONImplMastodon(new GetStatusByID(tweetId + "").execSync());
                if (status == null) {
                    return;
                }

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tweetView.setData(status);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return tweetView;
    }

    private DetailedTweetView (Context context) {
        super(context);

        createProgressView();
    }

    FrameLayout root = null;
    private void createProgressView() {
        root = (FrameLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.progress_spinner, null, false);
        root.setPadding(0,Utils.toDP(16, context),0, Utils.toDP(64, context));
    }

    @Override
    public void setData(Status status) {
        super.setData(status);

        View tweetView = super.getView();

        root.removeAllViews();
        root.addView(tweetView);
    }

    private FontPrefTextView likesText;
    private FontPrefTextView retweetsText;

    @Override
    public View getView() {
        return root;
    }

    @Override
    protected void setComponents(View v) {
        super.setComponents(v);

        // find the like and retweet buttons
        likesText = (FontPrefTextView) v.findViewById(R.id.likes);
        retweetsText = (FontPrefTextView) v.findViewById(R.id.retweets);

        likesText.setTextSize(settings.textSize);
        retweetsText.setTextSize(settings.textSize);
    }

    @Override
    protected void bindData() {
        super.bindData();

        likesText.setText(numLikes + "");
        retweetsText.setText(numRetweets + "");
    }

    @Override
    protected View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.detailed_tweet, null, false);
        return tweetView;
    }

    @Override
    protected boolean shouldShowImage() {
        return showImage;
    }

    private boolean showImage = true;

    public void setShouldShowImage(boolean showImage) {
        this.showImage = showImage;
    }
}
