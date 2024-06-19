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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;

import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.TweetLinkUtils;

import java.util.List;

import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.Timber;
import twitter4j.Status;

public class MentionsDataSource {

    // provides access to the database
    public static MentionsDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static MentionsDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new MentionsDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private MentionsSQLiteHelper dbHelper;
    private SharedPreferences sharedPrefs;
    public String[] allColumns = {MentionsSQLiteHelper.COLUMN_ID, MentionsSQLiteHelper.COLUMN_UNREAD, MentionsSQLiteHelper.COLUMN_TWEET_ID, MentionsSQLiteHelper.COLUMN_ACCOUNT, MentionsSQLiteHelper.COLUMN_TYPE,
            MentionsSQLiteHelper.COLUMN_TEXT, MentionsSQLiteHelper.COLUMN_NAME, MentionsSQLiteHelper.COLUMN_PRO_PIC,
            MentionsSQLiteHelper.COLUMN_SCREEN_NAME, MentionsSQLiteHelper.COLUMN_TIME, MentionsSQLiteHelper.COLUMN_PIC_URL,
            MentionsSQLiteHelper.COLUMN_RETWEETER, MentionsSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS, MentionsSQLiteHelper.COLUMN_ANIMATED_GIF,
            MentionsSQLiteHelper.COLUMN_CONVERSATION, MentionsSQLiteHelper.COLUMN_MEDIA_LENGTH,
            HomeSQLiteHelper.COLUMN_STATUS_URL, HomeSQLiteHelper.COLUMN_REBLOGS_COUNT, HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT,
            HomeSQLiteHelper.COLUMN_REPLIES_COUNT, HomeSQLiteHelper.COLUMN_REBLOGGED, HomeSQLiteHelper.COLUMN_BOOKMARKED,
            HomeSQLiteHelper.COLUMN_FAVOURITED, HomeSQLiteHelper.COLUMN_SENSITIVE, HomeSQLiteHelper.COLUMN_SPOILER_TEXT,
            HomeSQLiteHelper.COLUMN_VISIBILITY, HomeSQLiteHelper.COLUMN_POLL, HomeSQLiteHelper.COLUMN_LANGUAGE,
            HomeSQLiteHelper.COLUMN_CLIENT_SOURCE, HomeSQLiteHelper.COLUMN_USER_ID, HomeSQLiteHelper.COLUMN_EMOJI, HomeSQLiteHelper.COLUMN_MENTION,
            HomeSQLiteHelper.COLUMN_NOTIFICATION_ID
    };

    public MentionsDataSource(Context context) {
        dbHelper = new MentionsSQLiteHelper(context);
        sharedPrefs = AppSettings.getSharedPreferences(context);

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

    public MentionsSQLiteHelper getHelper() {
        return dbHelper;
    }


    public synchronized void createTweet(Status status, int account) {
        ContentValues values = HomeDataSource.getContentValues(status, account);
        try {
            database.insert(MentionsSQLiteHelper.TABLE_MENTIONS, null, values);
        } catch (Exception e) {
            open();
            database.insert(MentionsSQLiteHelper.TABLE_MENTIONS, null, values);
        }
    }

    public synchronized int insertTweets(List<Status> statuses, int account) {

        ContentValues[] valueses = new ContentValues[statuses.size()];

        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);

            ContentValues values = HomeDataSource.getContentValues(status, account);

            valueses[i] = values;
        }

        return insertMultiple(valueses);
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
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, values,
                    HomeSQLiteHelper.COLUMN_TWEET_ID + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?",
                    new String[]{status.getId() + "", account + ""});
        } catch (Exception e) {
            Timber.e(e, "updateTweet");
        }
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
                    rowId = database.insert(MentionsSQLiteHelper.TABLE_MENTIONS, null, values);
                } catch (IllegalStateException e) {
                    return rowsAdded;
                }
                if (rowId > 0)
                    rowsAdded++;
            }

            database.setTransactionSuccessful();
        } catch (NullPointerException e) {
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
            database.delete(MentionsSQLiteHelper.TABLE_MENTIONS, MentionsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(MentionsSQLiteHelper.TABLE_MENTIONS, MentionsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllTweets(int account) {

        try {
            database.delete(MentionsSQLiteHelper.TABLE_MENTIONS,
                    MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        } catch (Exception e) {
            open();
            database.delete(MentionsSQLiteHelper.TABLE_MENTIONS,
                    MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        }
    }

    public synchronized Cursor getCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized Cursor getWidgetCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " DESC", "150");
        } catch (Exception e) {
            open();
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " DESC", "150");
        }

        return cursor;
    }

    public synchronized Cursor getTrimmingCursor(int account) {

        String where = MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        Cursor cursor;
        try {
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, null, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, null, null, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized Cursor getUnreadCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = MentionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + MentionsSQLiteHelper.COLUMN_UNREAD + " = ?";

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + MentionsSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, new String[]{account + "", "1"}, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, where, new String[]{account + "", "1"}, MentionsSQLiteHelper.COLUMN_TWEET_ID, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized int getUnreadCount(int account) {

        Cursor cursor = getUnreadCursor(account);

        int count = cursor.getCount();

        cursor.close();

        return count;
    }

    public synchronized void markRead(long tweetId) {

        ContentValues cv = new ContentValues();
        cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

        try {
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        } catch (Exception e) {
            open();
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        }
    }

    // true is unread
    // false have been read
    public synchronized void markMultipleRead(int current, int account) {

        Cursor cursor = getUnreadCursor(account);

        try {
            if (cursor.moveToPosition(current)) {
                do {

                    long tweetId = cursor.getLong(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TWEET_ID));

                    ContentValues cv = new ContentValues();
                    cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

                    try {
                        database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
                    } catch (Exception e) {
                        open();
                        database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
                    }

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // there is nothing in the unread array
        }

        cursor.close();
    }

    public synchronized void markAllRead(int account) {

        ContentValues cv = new ContentValues();
        cv.put(MentionsSQLiteHelper.COLUMN_UNREAD, 0);

        try {
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + MentionsSQLiteHelper.COLUMN_UNREAD + " = ?", new String[]{account + "", "1"});
        } catch (Exception e) {
            open();
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + MentionsSQLiteHelper.COLUMN_UNREAD + " = ?", new String[]{account + "", "1"});
        }
    }

    public synchronized String getNewestName(int account) {

        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToLast()) {
                name = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_SCREEN_NAME));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return name;
    }

    public synchronized String getNewestUserId(int account) {

        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToLast()) {
                name = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_USER_ID));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return name;
    }

    public synchronized String getNewestNames(int account) {
        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToLast()) {
                name = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_USERS));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return name;
    }

    public synchronized String getNewestMessage(int account) {

        Cursor cursor = getCursor(account);
        String message = "";

        try {
            if (cursor.moveToLast()) {
                message = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TEXT));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return message;
    }

    public synchronized String getNewestPictureUrl(int account) {

        Cursor cursor = getCursor(account);
        String message = "";

        try {
            if (cursor.moveToLast()) {
                message = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_PIC_URL));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return message;
    }

    public synchronized long[] getLastIds(int account) {
        long[] ids = new long[]{0, 0};

        Cursor cursor;
        try {
            cursor = getTrimmingCursor(account);
        } catch (Exception e) {
            return ids;
        }

        try {
            if (cursor.moveToLast()) {
                ids[0] = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NOTIFICATION_ID));
            }

            if (cursor.moveToPrevious()) {
                ids[1] = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NOTIFICATION_ID));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return ids;
    }

    public synchronized boolean tweetExists(long tweetId, int account) {

        Cursor cursor;
        try {
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + MentionsSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                    allColumns, MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + MentionsSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, MentionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public synchronized void deleteDups(int account) {

        try {
            database.execSQL("DELETE FROM " + MentionsSQLiteHelper.TABLE_MENTIONS + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + MentionsSQLiteHelper.TABLE_MENTIONS + " GROUP BY " + MentionsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
        } catch (Exception e) {
            open();
            database.execSQL("DELETE FROM " + MentionsSQLiteHelper.TABLE_MENTIONS + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + MentionsSQLiteHelper.TABLE_MENTIONS + " GROUP BY " + MentionsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
        }
    }

    public synchronized void removeHTML(long tweetId, String text) {
        ContentValues cv = new ContentValues();
        cv.put(MentionsSQLiteHelper.COLUMN_TEXT, text);

        try {
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        } catch (Exception e) {
            close();
            open();
            database.update(MentionsSQLiteHelper.TABLE_MENTIONS, cv, MentionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        }

    }

    public synchronized void trimDatabase(int account, int trimSize) {
        Cursor cursor = getTrimmingCursor(account);
        if (cursor.getCount() > trimSize) {
            if (cursor.moveToPosition(cursor.getCount() - trimSize)) {
                try {
                    database.delete(
                            MentionsSQLiteHelper.TABLE_MENTIONS,
                            MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    MentionsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
                            null);
                } catch (Exception e) {
                    open();
                    database.delete(
                            MentionsSQLiteHelper.TABLE_MENTIONS,
                            MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    MentionsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
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
