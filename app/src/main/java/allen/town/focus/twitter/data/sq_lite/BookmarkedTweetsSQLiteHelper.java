package allen.town.focus.twitter.data.sq_lite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookmarkedTweetsSQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_BOOKMARKED_TWEETS = "bookmarked_tweets";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ACCOUNT = "account";
    public static final String COLUMN_TWEET_ID = "tweet_id";
    public static final String COLUMN_UNREAD = "unread";
    public static final String COLUMN_ARTICLE = "article";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PRO_PIC = "profile_pic";
    public static final String COLUMN_SCREEN_NAME = "screen_name";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_PIC_URL = "pic_url";
    public static final String COLUMN_URL = "other_url";
    public static final String COLUMN_RETWEETER = "retweeter";
    public static final String COLUMN_HASHTAGS = "hashtags";
    public static final String COLUMN_USERS = "users";
    public static final String COLUMN_ANIMATED_GIF = "extra_one";
    public static final String COLUMN_EXTRA_TWO = "extra_two";
    public static final String COLUMN_CLIENT_SOURCE = "extra_three";
    public static final String COLUMN_CONVERSATION = "conversation";
    public static final String COLUMN_MEDIA_LENGTH = "media_length";
    public static final String COLUMN_MENTION = "mention";
    public static final String COLUMN_EMOJI = "emoji";
    public static final String COLUMN_USER_ID = "user_id";

    public static final String COLUMN_STATUS_URL = "statusUrl";
    public static final String COLUMN_REBLOGS_COUNT = "reblogsCount";
    public static final String COLUMN_FAVOURITES_COUNT = "favouritesCount";
    public static final String COLUMN_REPLIES_COUNT = "repliesCount";
    public static final String COLUMN_REBLOGGED = "reblogged";
    public static final String COLUMN_BOOKMARKED = "bookmarked";
    public static final String COLUMN_FAVOURITED = "favourited";
    public static final String COLUMN_SENSITIVE = "sensitive";
    public static final String COLUMN_SPOILER_TEXT = "spoilerText";
    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_POLL = "poll";
    public static final String COLUMN_LANGUAGE = "language";
    private static final String DATABASE_NAME = "bookmarked_tweets.db";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_BOOKMARKED_TWEETS + "(" + COLUMN_ID
            + " integer primary key, " + COLUMN_ACCOUNT
            + " integer account num, " + COLUMN_TWEET_ID
            + " integer tweet id, " + COLUMN_UNREAD
            + " integer unread, " + COLUMN_TYPE
            + " integer type of tweet, " + COLUMN_ARTICLE
            + " text article text, " + COLUMN_TEXT
            + " text not null, " + COLUMN_NAME
            + " text users name, " + COLUMN_PRO_PIC
            + " text url of pic, " + COLUMN_SCREEN_NAME
            + " text user screen, " + COLUMN_TIME
            + " integer time, " + COLUMN_URL
            + " text other url, " + COLUMN_PIC_URL
            + " text pic url, " + COLUMN_HASHTAGS
            + " text hashtags, " + COLUMN_STATUS_URL
            + " text statusUrl, " + COLUMN_REBLOGS_COUNT
            + " text reblogsCount, " + COLUMN_FAVOURITES_COUNT
            + " text favouritesCount, " + COLUMN_REPLIES_COUNT
            + " text repliesCount, " + COLUMN_REBLOGGED
            + " text reblogged, " + COLUMN_BOOKMARKED
            + " text bookmarked, " + COLUMN_FAVOURITED
            + " text favourited, " + COLUMN_SENSITIVE
            + " text sensitive, " + COLUMN_SPOILER_TEXT
            + " text spoilerText, " + COLUMN_VISIBILITY
            + " text visibility, " + COLUMN_POLL
            + " text poll, " + COLUMN_LANGUAGE
            + " text language, " + COLUMN_EMOJI
            + " text emoji, " + COLUMN_MENTION
            + " text mention, " + COLUMN_USER_ID
            + " text user_id, " + COLUMN_USERS
            + " text users, " + COLUMN_RETWEETER
            + " text original name, " + COLUMN_ANIMATED_GIF
            + " text extra one, " + COLUMN_EXTRA_TWO
            + " text extra two, " + COLUMN_CLIENT_SOURCE
            + " text extra three);";

    private static final String DATABASE_ADD_CONVO_FIELD =
            "ALTER TABLE " + TABLE_BOOKMARKED_TWEETS + " ADD COLUMN " + COLUMN_CONVERSATION + " INTEGER DEFAULT 0";

    private static final String DATABASE_ADD_MEDIA_LENGTH_FIELD =
            "ALTER TABLE " + TABLE_BOOKMARKED_TWEETS + " ADD COLUMN " + COLUMN_MEDIA_LENGTH + " INTEGER DEFAULT -1";

    public BookmarkedTweetsSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(DATABASE_ADD_CONVO_FIELD);
        database.execSQL(DATABASE_ADD_MEDIA_LENGTH_FIELD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(DATABASE_ADD_MEDIA_LENGTH_FIELD);
        }
    }

}