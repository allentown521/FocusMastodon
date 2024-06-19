package allen.town.focus.twitter.data.sq_lite;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;

import allen.town.focus.twitter.utils.TweetLinkUtils;

import java.util.List;

import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.Timber;
import twitter4j.Status;

public class SavedTweetsDataSource {

    // provides access to the database
    public static SavedTweetsDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static SavedTweetsDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new SavedTweetsDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private SavedTweetSQLiteHelper dbHelper;
    public static String[] allColumns = {
            SavedTweetSQLiteHelper.COLUMN_ID,
            SavedTweetSQLiteHelper.COLUMN_TWEET_ID,
            SavedTweetSQLiteHelper.COLUMN_ACCOUNT,
            SavedTweetSQLiteHelper.COLUMN_TEXT,
            SavedTweetSQLiteHelper.COLUMN_NAME,
            SavedTweetSQLiteHelper.COLUMN_PRO_PIC,
            SavedTweetSQLiteHelper.COLUMN_SCREEN_NAME,
            SavedTweetSQLiteHelper.COLUMN_TIME,
            SavedTweetSQLiteHelper.COLUMN_PIC_URL,
            SavedTweetSQLiteHelper.COLUMN_RETWEETER,
            SavedTweetSQLiteHelper.COLUMN_URL,
            SavedTweetSQLiteHelper.COLUMN_USERS,
            SavedTweetSQLiteHelper.COLUMN_HASHTAGS,
            SavedTweetSQLiteHelper.COLUMN_ANIMATED_GIF,
            SavedTweetSQLiteHelper.COLUMN_MEDIA_LENGTH,
            SavedTweetSQLiteHelper.COLUMN_MENTION,
            SavedTweetSQLiteHelper.COLUMN_EMOJI,
            SavedTweetSQLiteHelper.COLUMN_USER_ID,
            HomeSQLiteHelper.COLUMN_STATUS_URL,HomeSQLiteHelper.COLUMN_REBLOGS_COUNT,HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT,
            HomeSQLiteHelper.COLUMN_REPLIES_COUNT,HomeSQLiteHelper.COLUMN_REBLOGGED,HomeSQLiteHelper.COLUMN_BOOKMARKED,
            HomeSQLiteHelper.COLUMN_FAVOURITED,HomeSQLiteHelper.COLUMN_SENSITIVE,HomeSQLiteHelper.COLUMN_SPOILER_TEXT,
            HomeSQLiteHelper.COLUMN_VISIBILITY,HomeSQLiteHelper.COLUMN_POLL,HomeSQLiteHelper.COLUMN_LANGUAGE,
            HomeSQLiteHelper.COLUMN_CLIENT_SOURCE
    };

    public SavedTweetsDataSource(Context context) {
        dbHelper = new SavedTweetSQLiteHelper(context);
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

    public SavedTweetSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createTweet(Status status, int account) {
        ContentValues values = HomeDataSource.getContentValues(status, account);

        try {
            database.insert(SavedTweetSQLiteHelper.TABLE_HOME, null, values);
        } catch (Exception e) {
            open();
            database.insert(SavedTweetSQLiteHelper.TABLE_HOME, null, values);
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
            database.update(SavedTweetSQLiteHelper.TABLE_HOME, values,
                    HomeSQLiteHelper.COLUMN_TWEET_ID + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?",
                    new String[]{status.getId() + "", account + ""});
        } catch (Exception e) {
            Timber.e(e, "updateTweet");
        }
    }

    public int insertTweets(List<Status> statuses, int account) {

        ContentValues[] valueses = new ContentValues[statuses.size()];

        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);

            ContentValues values = HomeDataSource.getContentValues(status, account);

            valueses[i] = values;
        }

        return insertMultiple(valueses);
    }

    private synchronized int insertMultiple(ContentValues[] allValues) {
        int rowsAdded = 0;
        long rowId;
        ContentValues values;

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.beginTransaction();

            for (ContentValues initialValues : allValues) {
                values = initialValues == null ? new ContentValues() : new ContentValues(initialValues);
                try {
                    rowId = database.insert(SavedTweetSQLiteHelper.TABLE_HOME, null, values);
                } catch (IllegalStateException e) {
                    return rowsAdded;
                }
                if (rowId > 0)
                    rowsAdded++;
            }

            database.setTransactionSuccessful();
        } catch (NullPointerException e)  {
            e.printStackTrace();
            return rowsAdded;
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
            return rowsAdded;
        } catch (IllegalStateException e) {
            // caught setting up the transaction I guess, shouldn't ever happen now.
            e.printStackTrace();
            return rowsAdded;
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
            database.delete(SavedTweetSQLiteHelper.TABLE_HOME, SavedTweetSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(SavedTweetSQLiteHelper.TABLE_HOME, SavedTweetSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllTweets(String accountId) {

        try {
            database.delete(SavedTweetSQLiteHelper.TABLE_HOME, null, null);
        } catch (Exception e) {
            open();
            database.delete(SavedTweetSQLiteHelper.TABLE_HOME, null, null);
        }
    }

    public synchronized Cursor getCursor(int accountId) {
        Cursor cursor;
        try {
            cursor = database.query(SavedTweetSQLiteHelper.TABLE_HOME,
                    allColumns, SavedTweetSQLiteHelper.COLUMN_ACCOUNT + " = ?", new String[] { "" + accountId },
                    null, null, SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(SavedTweetSQLiteHelper.TABLE_HOME,
                    allColumns, SavedTweetSQLiteHelper.COLUMN_ACCOUNT + " = ?", new String[] { "" + accountId },
                    null, null, SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized boolean isTweetSaved(long tweetId, int accountId) {
        Cursor cursor;
        try {
            cursor = database.query(SavedTweetSQLiteHelper.TABLE_HOME,
                    allColumns, SavedTweetSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " = ?",
                    new String[] { "" + accountId, "" + tweetId }, null, null,
                    SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(SavedTweetSQLiteHelper.TABLE_HOME,
                    allColumns, SavedTweetSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " = ?",
                    new String[] { "" + accountId, "" + tweetId }, null, null,
                    SavedTweetSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        boolean saved = cursor != null && cursor.getCount() > 0;

        try {
            cursor.close();
        } catch (Exception e) {
        }

        return saved;
    }
}
