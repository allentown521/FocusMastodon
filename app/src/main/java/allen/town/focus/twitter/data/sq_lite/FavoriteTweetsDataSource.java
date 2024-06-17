package allen.town.focus.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.TweetLinkUtils;

import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.Timber;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

public class FavoriteTweetsDataSource {

    private static final int timelineSize = 200;

    // provides access to the database
    public static FavoriteTweetsDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static FavoriteTweetsDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new FavoriteTweetsDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private FavoriteTweetsSQLiteHelper dbHelper;
    private Context context;
    private SharedPreferences sharedPreferences;

    public static String[] allColumns = {FavoriteTweetsSQLiteHelper.COLUMN_ID, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID, FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT, FavoriteTweetsSQLiteHelper.COLUMN_TYPE,
            FavoriteTweetsSQLiteHelper.COLUMN_TEXT, FavoriteTweetsSQLiteHelper.COLUMN_NAME, FavoriteTweetsSQLiteHelper.COLUMN_PRO_PIC,
            FavoriteTweetsSQLiteHelper.COLUMN_SCREEN_NAME, FavoriteTweetsSQLiteHelper.COLUMN_TIME, FavoriteTweetsSQLiteHelper.COLUMN_PIC_URL,
            FavoriteTweetsSQLiteHelper.COLUMN_RETWEETER, FavoriteTweetsSQLiteHelper.COLUMN_URL, FavoriteTweetsSQLiteHelper.COLUMN_USERS, FavoriteTweetsSQLiteHelper.COLUMN_HASHTAGS,
            FavoriteTweetsSQLiteHelper.COLUMN_EXTRA_TWO, FavoriteTweetsSQLiteHelper.COLUMN_ANIMATED_GIF, FavoriteTweetsSQLiteHelper.COLUMN_CONVERSATION, FavoriteTweetsSQLiteHelper.COLUMN_MEDIA_LENGTH,
            FavoriteTweetsSQLiteHelper.COLUMN_MENTION, FavoriteTweetsSQLiteHelper.COLUMN_EMOJI, FavoriteTweetsSQLiteHelper.COLUMN_USER_ID,
            HomeSQLiteHelper.COLUMN_STATUS_URL, HomeSQLiteHelper.COLUMN_REBLOGS_COUNT, HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT,
            HomeSQLiteHelper.COLUMN_REPLIES_COUNT, HomeSQLiteHelper.COLUMN_REBLOGGED, HomeSQLiteHelper.COLUMN_BOOKMARKED,
            HomeSQLiteHelper.COLUMN_FAVOURITED, HomeSQLiteHelper.COLUMN_SENSITIVE, HomeSQLiteHelper.COLUMN_SPOILER_TEXT,
            HomeSQLiteHelper.COLUMN_VISIBILITY, HomeSQLiteHelper.COLUMN_POLL, HomeSQLiteHelper.COLUMN_LANGUAGE,
            HomeSQLiteHelper.COLUMN_CLIENT_SOURCE
    };

    public FavoriteTweetsDataSource(Context context) {
        dbHelper = new FavoriteTweetsSQLiteHelper(context);
        this.context = context;
        sharedPreferences = AppSettings.getSharedPreferences(context);

    }

    public void open() throws SQLException {
        try {
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            close();
        }
    }

    public void close() {
        try {
            dbHelper.close();
        } catch (Exception e) {

        }
        database = null;
        dataSource = null;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public FavoriteTweetsSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createTweet(Status status, int account) {
        ContentValues values = HomeDataSource.getContentValues(status, account);
        try {
            database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, values);
        } catch (Exception e) {
            open();
            database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, values);
        }
    }
    public synchronized void updateTweet(Status status, int account) {
        ContentValues values = HomeDataSource.getContentValues(status, account);
        updateInnerTweet(status, account, values);

    }

    public synchronized void updateTweetPollField(Status status, int account) {
        ContentValues values = new ContentValues();
        values.put(HomeSQLiteHelper.COLUMN_POLL, JsonHelper.toJSONString(status.getPoll()));
        updateInnerTweet(status, account, values);
    }

    private void updateInnerTweet(Status status, int account, ContentValues values) {
        try {
            database.update(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, values,
                    HomeSQLiteHelper.COLUMN_TWEET_ID + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?",
                    new String[]{status.getId() + "", account + ""});
        } catch (Exception e) {
            Timber.e(e, "updateTweet");
        }
    }
    public synchronized int insertTweets(List<Status> statuses, int currentAccount) {

        ContentValues[] valueses = new ContentValues[statuses.size()];

        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);
            Long id = status.getId();
            ContentValues values;

            values = HomeDataSource.getContentValues(status, currentAccount);

            valueses[i] = values;
        }

        ArrayList<ContentValues> vals = new ArrayList<ContentValues>();

        for (ContentValues v : valueses) {
            if (v != null) {
                vals.add(v);
            }
        }

        insertMultiple(valueses);

        return vals.size();
    }

    public synchronized int insertMultiple(ContentValues[] allValues) {
        int rowsAdded = 0;
        long rowId;

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.beginTransaction();

            Timber.v("starting insert, number of values: " + allValues.length);

            for (int i = 0; i < allValues.length; i++) {
                ContentValues initialValues = allValues[i];

                if (initialValues != null) {
                    try {
                        rowId = database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, initialValues);
                    } catch (Exception e) {
                        e.printStackTrace();
                        open();
                        try {
                            rowId = database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, initialValues);
                        } catch (Exception x) {
                            return rowsAdded;
                        }
                    }
                    if (rowId > 0) {
                        rowsAdded++;
                    }
                }

                allValues[i] = null;
            }

            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            open();

            if (database == null) {
                return 0;
            }

            database.beginTransaction();

            for (ContentValues initialValues : allValues) {
                if (initialValues != null) {
                    try {
                        rowId = database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, initialValues);
                    } catch (IllegalStateException x) {
                        open();
                        try {
                            rowId = database.insert(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, null, initialValues);
                        } catch (Exception m) {
                            return rowsAdded;
                        }
                    }
                    if (rowId > 0) {
                        rowsAdded++;
                    }
                }
            }

            database.setTransactionSuccessful();

        } finally {
            try {
                database.endTransaction();
            } catch (Exception e) {
                // shouldn't happen unless it gets caught above from an illegal state
            }
        }

        return rowsAdded;
    }

    public synchronized void deleteTweet(long tweetId) {
        long id = tweetId;

        try {
            database.delete(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllTweets(int account) {

        try {
            database.delete(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        } catch (Exception e) {
            open();
            database.delete(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        }
    }

    public synchronized Cursor getCursor(int account) {

        String where = FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        Cursor cursor;

        String sql = "SELECT COUNT(*) FROM " + FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS + " WHERE " + where;
        SQLiteStatement statement;
        try {
            statement = database.compileStatement(sql);
        } catch (Exception e) {
            open();
            statement = database.compileStatement(sql);
        }
        long count;
        try {
            count = statement.simpleQueryForLong();
        } catch (Exception e) {
            open();
            try {
                count = statement.simpleQueryForLong();
            } catch (Exception x) {
                return null;
            }
        }
        Log.v("Focus_for_Mastodon_database", "FavoriteTweets database has " + count + " entries");
        if (count > timelineSize) {
            try {
                cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                        allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC", (count - timelineSize) + "," + timelineSize);
            } catch (Exception e) {
                open();
                cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                        allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC", (count - timelineSize) + "," + timelineSize);
            }
        } else {
            try {
                cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                        allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            } catch (Exception e) {
                open();
                cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                        allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            }
        }

        return cursor;
    }

    public synchronized Cursor getTrimmingCursor(int account) {

        String where = FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        Cursor cursor;

        try {
            cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    allColumns, where, null, null, null, FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized long[] getLastIds(int account) {
        long id[] = new long[]{0, 0, 0, 0, 0};

        Cursor cursor;
        try {
            cursor = getCursor(account);
        } catch (Exception e) {
            return id;
        }

        try {
            if (cursor.moveToLast()) {
                int i = 0;
                do {
                    id[i] = cursor.getLong(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TWEET_ID));
                    i++;
                } while (cursor.moveToPrevious() && i < 5);
            }
        } catch (Exception e) {
        }

        if (cursor != null) {
            cursor.close();
        }

        return id;
    }

    public synchronized boolean tweetExists(long tweetId, int account) {

        Cursor cursor;
        try {
            cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    allColumns,
                    FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId,
                    null,
                    null,
                    null,
                    FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                    "1"
            );
        } catch (Exception e) {
            open();
            cursor = database.query(FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                    allColumns,
                    FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId,
                    null,
                    null,
                    null,
                    FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                    "1"
            );
        }

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public synchronized void deleteDups(int account) {

        try {
            database.execSQL("DELETE FROM " + FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS +
                    " GROUP BY " + FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
        } catch (Exception e) {
            open();
            database.execSQL("DELETE FROM " + FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS +
                    " GROUP BY " + FavoriteTweetsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account);

        }

    }

    public synchronized void trimDatabase(int account, int trimSize) {
        Cursor cursor = getTrimmingCursor(account);
        if (cursor.getCount() > trimSize) {
            if (cursor.moveToPosition(cursor.getCount() - trimSize)) {
                try {
                    database.delete(
                            FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                            FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    FavoriteTweetsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(FavoriteTweetsSQLiteHelper.COLUMN_ID)),
                            null);
                } catch (Exception e) {
                    open();
                    database.delete(
                            FavoriteTweetsSQLiteHelper.TABLE_FAVORITE_TWEETS,
                            FavoriteTweetsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    FavoriteTweetsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(FavoriteTweetsSQLiteHelper.COLUMN_ID)),
                            null);
                }
            }
        }

        try {
            cursor.close();
        } catch (Exception e) {

        }
    }
}
