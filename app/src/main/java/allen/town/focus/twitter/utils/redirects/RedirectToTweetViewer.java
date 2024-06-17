package allen.town.focus.twitter.utils.redirects;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.activities.tweet_viewer.TweetActivity;

public class RedirectToTweetViewer extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            MentionsDataSource.getInstance(this).markRead(getIntent().getLongExtra("tweetid", 1));
        } catch (Exception e) { }

        Intent tweet = new Intent(this, TweetActivity.class);
        tweet.putExtras(getIntent());

        long forcedTweetId = getIntent().getLongExtra("forced_tweet_id", -1);
        if (forcedTweetId != -1) {
            tweet.putExtra("tweetid", forcedTweetId);
        }

        tweet.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        TweetActivity.applyDragDismissBundle(this, tweet);

        finish();
        overridePendingTransition(0,0);

        startActivity(tweet);
    }
}
