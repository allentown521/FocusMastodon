package allen.town.focus.twitter.data.sq_lite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ActivitySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_ACTIVITY = "activity";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TWEET_ID = "tweet_id";
    public static final String COLUMN_ACCOUNT = "account";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PRO_PIC = "profile_pic";
    public static final String COLUMN_SCREEN_NAME = "screen_name";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_URL = "other_url";
    public static final String COLUMN_PIC_URL = "pic_url";
    public static final String COLUMN_RETWEETER = "retweeter";
    public static final String COLUMN_HASHTAGS = "hashtags";
    public static final String COLUMN_USERS = "users";
    public static final String COLUMN_ANIMATED_GIF = "extra_one";
    public static final String COLUMN_EXTRA_TWO = "extra_two";
    public static final String COLUMN_EXTRA_THREE = "extra_three";
    public static final String COLUMN_CONVERSATION = "conversation";
    public static final String COLUMN_FAV_COUNT = "fav_count";
    public static final String COLUMN_RETWEET_COUNT = "retweet_count";
    public static final String COLUMN_MEDIA_LENGTH = "media_length";



    private static final String DATABASE_NAME = "activity.db";
    private static final int DATABASE_VERSION = 4;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_ACTIVITY + "(" + COLUMN_ID
            + " integer primary key, " + COLUMN_TWEET_ID
            + " integer tweet id, " + COLUMN_TITLE
            + " text interaction title, " + COLUMN_ACCOUNT
            + " integer account num, " + COLUMN_TYPE
            + " integer type of tweet, " + COLUMN_TEXT
            + " text not null, " + COLUMN_NAME
            + " text users name, " + COLUMN_PRO_PIC
            + " text url of pic, " + COLUMN_SCREEN_NAME
            + " text user screen, " + COLUMN_TIME
            + " integer time, " + COLUMN_URL
            + " text other url, " + COLUMN_PIC_URL
            + " text pic url, " + COLUMN_HASHTAGS
            + " text hashtags, " + HomeSQLiteHelper.COLUMN_STATUS_URL
            + " text statusUrl, " + HomeSQLiteHelper.COLUMN_REBLOGS_COUNT
            + " text reblogsCount, " + HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT
            + " text favouritesCount, " + HomeSQLiteHelper.COLUMN_REPLIES_COUNT
            + " text repliesCount, " + HomeSQLiteHelper.COLUMN_REBLOGGED
            + " text reblogged, " + HomeSQLiteHelper.COLUMN_BOOKMARKED
            + " text bookmarked, " + HomeSQLiteHelper.COLUMN_FAVOURITED
            + " text favourited, " + HomeSQLiteHelper.COLUMN_SENSITIVE
            + " text sensitive, " + HomeSQLiteHelper.COLUMN_SPOILER_TEXT
            + " text spoilerText, " + HomeSQLiteHelper.COLUMN_VISIBILITY
            + " text visibility, " + HomeSQLiteHelper.COLUMN_POLL
            + " text poll, " + HomeSQLiteHelper.COLUMN_LANGUAGE
            + " text language, " + HomeSQLiteHelper.COLUMN_EMOJI
            + " text emoji, " + HomeSQLiteHelper.COLUMN_MENTION
            + " text mention, " + HomeSQLiteHelper.COLUMN_USER_ID
            + " text user_id, " + COLUMN_USERS
            + " text users, " + COLUMN_FAV_COUNT
            + " integer favorite count, " + COLUMN_RETWEET_COUNT
            + " integer retweet count, " + COLUMN_RETWEETER
            + " text original name, " + COLUMN_ANIMATED_GIF
            + " text extra one, " + COLUMN_EXTRA_TWO
            + " text extra two, " + COLUMN_EXTRA_THREE
            + " text extra three);";

    private static final String DATABASE_ADD_CONVO_FIELD =
            "ALTER TABLE " + TABLE_ACTIVITY + " ADD COLUMN " + COLUMN_CONVERSATION + " INTEGER DEFAULT 0";

    private static final String DATABASE_ADD_MEDIA_LENGTH_FIELD =
            "ALTER TABLE " + TABLE_ACTIVITY + " ADD COLUMN " + COLUMN_MEDIA_LENGTH + " INTEGER DEFAULT -1";

    public ActivitySQLiteHelper(Context context) {
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

        if (oldVersion < 4) {
        }

    }

}