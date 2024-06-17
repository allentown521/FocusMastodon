package allen.town.focus.twitter.views;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import allen.town.focus.twitter.R;

import twitter4j.Status;

public class QuotedTweetViewRevamped extends TweetView {

    public QuotedTweetViewRevamped(Context context, Status status) {
        super(context, status);
    }

    protected View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet_quoted_revamped, null, false);
        return tweetView;
    }
}
