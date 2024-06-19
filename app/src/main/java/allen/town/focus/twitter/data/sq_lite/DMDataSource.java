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

import java.util.List;

import allen.town.focus.twitter.api.requests.conversation.GetConversations;
import allen.town.focus.twitter.model.Conversation;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.Timber;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;

public class DMDataSource {

    // provides access to the database
    public static DMDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static DMDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new DMDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private SharedPreferences sharedPrefs;
    private DMSQLiteHelper dbHelper;
    public String[] allColumns = {DMSQLiteHelper.COLUMN_ID, DMSQLiteHelper.COLUMN_UNREAD, DMSQLiteHelper.COLUMN_TWEET_ID, DMSQLiteHelper.COLUMN_ACCOUNT, DMSQLiteHelper.COLUMN_TYPE,
            DMSQLiteHelper.COLUMN_TEXT, DMSQLiteHelper.COLUMN_NAME, DMSQLiteHelper.COLUMN_PRO_PIC,
            DMSQLiteHelper.COLUMN_SCREEN_NAME, DMSQLiteHelper.COLUMN_TIME, DMSQLiteHelper.COLUMN_PIC_URL,
            DMSQLiteHelper.COLUMN_RETWEETER, DMSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS, DMSQLiteHelper.COLUMN_ANIMATED_GIF,
            DMSQLiteHelper.COLUMN_CONVERSATION, DMSQLiteHelper.COLUMN_MEDIA_LENGTH,
            HomeSQLiteHelper.COLUMN_STATUS_URL, HomeSQLiteHelper.COLUMN_REBLOGS_COUNT, HomeSQLiteHelper.COLUMN_FAVOURITES_COUNT,
            HomeSQLiteHelper.COLUMN_REPLIES_COUNT, HomeSQLiteHelper.COLUMN_REBLOGGED, HomeSQLiteHelper.COLUMN_BOOKMARKED,
            HomeSQLiteHelper.COLUMN_FAVOURITED, HomeSQLiteHelper.COLUMN_SENSITIVE, HomeSQLiteHelper.COLUMN_SPOILER_TEXT,
            HomeSQLiteHelper.COLUMN_VISIBILITY, HomeSQLiteHelper.COLUMN_POLL, HomeSQLiteHelper.COLUMN_LANGUAGE,
            HomeSQLiteHelper.COLUMN_CLIENT_SOURCE, HomeSQLiteHelper.COLUMN_USER_ID, HomeSQLiteHelper.COLUMN_EMOJI, HomeSQLiteHelper.COLUMN_MENTION,
            HomeSQLiteHelper.COLUMN_NOTIFICATION_ID
    };

    public DMDataSource(Context context) {
        dbHelper = new DMSQLiteHelper(context);
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

    public DMSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createTweet(Status status, int account) {
        ContentValues values = HomeDataSource.getContentValues(status, account);
        try {
            database.insert(DMSQLiteHelper.TABLE_DM, null, values);
        } catch (Exception e) {
            open();
            database.insert(DMSQLiteHelper.TABLE_DM, null, values);
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
                    rowId = database.insert(DMSQLiteHelper.TABLE_DM, null, values);
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
            database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllTweets(int account) {

        try {
            database.delete(DMSQLiteHelper.TABLE_DM,
                    DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        } catch (Exception e) {
            open();
            database.delete(DMSQLiteHelper.TABLE_DM,
                    DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        }
    }

    public synchronized Cursor getCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized Cursor getWidgetCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " DESC", "150");
        } catch (Exception e) {
            open();
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " DESC", "150");
        }

        return cursor;
    }

    public synchronized Cursor getTrimmingCursor(int account) {

        String where = DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        Cursor cursor;
        try {
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, null, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, null, null, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized Cursor getUnreadCursor(int account) {

        boolean mutedMentions = sharedPrefs.getBoolean(AppSettings.SHOW_MUTED_MENTIONS, false);
        String users = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        String hashtags = sharedPrefs.getString("muted_hashtags", "");
        String expressions = sharedPrefs.getString(AppSettings.MUTED_REGEX, "");

        expressions = expressions.replaceAll("'", "''");

        String where = DMSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + DMSQLiteHelper.COLUMN_UNREAD + " = ?";

        if (!users.equals("") && !mutedMentions) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_USER_ID + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("") && !mutedMentions) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("") && !mutedMentions) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + DMSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        Cursor cursor;
        try {
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, new String[]{account + "", "1"}, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, where, new String[]{account + "", "1"}, DMSQLiteHelper.COLUMN_TWEET_ID, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
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
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        } catch (Exception e) {
            open();
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        }
    }

    // true is unread
    // false have been read
    public synchronized void markMultipleRead(int current, int account) {

        Cursor cursor = getUnreadCursor(account);

        try {
            if (cursor.moveToPosition(current)) {
                do {

                    long tweetId = cursor.getLong(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TWEET_ID));

                    ContentValues cv = new ContentValues();
                    cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

                    try {
                        database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
                    } catch (Exception e) {
                        open();
                        database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
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
        cv.put(DMSQLiteHelper.COLUMN_UNREAD, 0);

        try {
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + DMSQLiteHelper.COLUMN_UNREAD + " = ?", new String[]{account + "", "1"});
        } catch (Exception e) {
            open();
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + DMSQLiteHelper.COLUMN_UNREAD + " = ?", new String[]{account + "", "1"});
        }
    }

    public synchronized String getNewestName(int account) {

        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToLast()) {
                name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_SCREEN_NAME));
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
                name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_USER_ID));
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
                name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_USERS));
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
                message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return message;
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
            database.update(DMSQLiteHelper.TABLE_DM, values,
                    HomeSQLiteHelper.COLUMN_TWEET_ID + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?",
                    new String[]{status.getId() + "", account + ""});
        } catch (Exception e) {
            Timber.e(e, "updateTweet");
        }
    }

    public synchronized String getNewestPictureUrl(int account) {

        Cursor cursor = getCursor(account);
        String message = "";

        try {
            if (cursor.moveToLast()) {
                message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_PIC_URL));
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
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + DMSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(DMSQLiteHelper.TABLE_DM,
                    allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + DMSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, DMSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public synchronized void deleteDups(int account) {

        try {
            database.execSQL("DELETE FROM " + DMSQLiteHelper.TABLE_DM + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + DMSQLiteHelper.TABLE_DM + " GROUP BY " + DMSQLiteHelper.COLUMN_TWEET_ID + ") AND " + DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
        } catch (Exception e) {
            open();
            database.execSQL("DELETE FROM " + DMSQLiteHelper.TABLE_DM + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + DMSQLiteHelper.TABLE_DM + " GROUP BY " + DMSQLiteHelper.COLUMN_TWEET_ID + ") AND " + DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
        }
    }

    public synchronized void removeHTML(long tweetId, String text) {
        ContentValues cv = new ContentValues();
        cv.put(DMSQLiteHelper.COLUMN_TEXT, text);

        try {
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        } catch (Exception e) {
            close();
            open();
            database.update(DMSQLiteHelper.TABLE_DM, cv, DMSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
        }

    }

    public synchronized void trimDatabase(int account, int trimSize) {
        Cursor cursor = getTrimmingCursor(account);
        if (cursor.getCount() > trimSize) {
            if (cursor.moveToPosition(cursor.getCount() - trimSize)) {
                try {
                    database.delete(
                            DMSQLiteHelper.TABLE_DM,
                            DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    DMSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
                            null);
                } catch (Exception e) {
                    open();
                    database.delete(
                            DMSQLiteHelper.TABLE_DM,
                            DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                                    DMSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
                            null);
                }
            }
        }

        try {
            cursor.close();
        } catch (Exception e) {

        }
    }

    /**
     * 从服务端获取最新的一页私信
     *
     * @param isSecondAccount
     * @param currentAccount
     * @return
     */
    public List<twitter4j.Status> getNewestPageFromRemote(boolean isSecondAccount, int currentAccount) {
        long[] lastNotiId = getLastIds(currentAccount);

        HeaderPaginationList<Conversation> list = null;
        try {
            if (isSecondAccount) {
                list = new GetConversations("", "", 40).execSecondAccountSync();
            } else {
                list = new GetConversations("", "", 40).execSync();
            }
        } catch (Exception e) {
            Timber.e("getNewestPageFromRemote", e);
        }

        HeaderPaginationList<twitter4j.Status> statuses = HeaderPaginationList.copyOnlyPage(list);
        if (list != null) {
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).status != null && Long.parseLong(list.get(i).id) > lastNotiId[0]) {
                        statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                    }
                }

            }

            String preIndex = statuses.getPreviousCursor();
            //从header获取的id

        }


        return statuses;

    }
}
