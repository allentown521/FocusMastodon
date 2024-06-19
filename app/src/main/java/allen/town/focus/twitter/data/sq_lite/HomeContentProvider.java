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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.net.Uri;
import android.os.Binder;
import android.provider.Settings.System;
import android.util.Log;

import allen.town.focus.twitter.utils.TweetLinkUtils;

import java.util.List;

import twitter4j.Status;

public class HomeContentProvider extends ContentProvider {
    static final String TAG = "HomeTimeline";

    public static final String AUTHORITY = "allen.town.focus.mastodon.home.provider";
    static final String BASE_PATH = "tweet_id";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final Uri STREAM_NOTI = Uri.parse("content://" + AUTHORITY + "/" + "stream");

    private Context context;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        context = getContext();

        return true;
    }

    @Override
    public String getType(Uri uri) {
        String ret = getContext().getContentResolver().getType(System.CONTENT_URI);
        Log.d(TAG, "getType returning: " + ret);
        return ret;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri: " + uri.toString());

        if (!checkUID(context)) {
            return null;
        }

        SQLiteDatabase db = HomeDataSource.getInstance(getContext()).getDatabase();
        long rowID;
        try {
            rowID = db.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
        } catch (IllegalStateException e) {
            // shouldn't happen here, but might i guess
            HomeDataSource.dataSource = null;
            db = HomeDataSource.getInstance(context).getDatabase();
            rowID = db.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
        }

        getContext().getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        return Uri.parse(BASE_PATH + "/" + rowID);
    }

    @Override
    public synchronized int bulkInsert(Uri uri, ContentValues[] allValues) {

        if (checkUID(context)) {
            HomeDataSource dataSource = HomeDataSource.getInstance(context);
            int inserted = dataSource.insertMultiple(allValues);
            context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);
            return inserted;
        } else {
            return 0;
        }

    }

    private boolean checkUID(Context context) {
        int callingUid = Binder.getCallingUid();

        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(
                PackageManager.GET_META_DATA);

        int launcherUid = 0;
        int pageUid = 0;
        int twitterUid = 0;

        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals("allen.town.focus.twitter")){
                //get the UID for the selected app
                twitterUid = packageInfo.uid;
            }
            if(packageInfo.packageName.equals("com.klinker.android.launcher")){
                //get the UID for the selected app
                launcherUid = packageInfo.uid;
            }
            if (packageInfo.packageName.equals("com.klinker.android.launcher.twitter_page")) {
                //get the UID for the selected app
                pageUid = packageInfo.uid;
            }
        }

        if (callingUid == launcherUid || callingUid == twitterUid || callingUid == pageUid) {
            return true;
        } else {
            return false;
        }
    }

    private int insertMultiple(ContentValues[] allValues) {
        int rowsAdded = 0;
        long rowId;
        ContentValues values;

        SQLiteDatabase db = HomeDataSource.getInstance(getContext()).getDatabase();

        try {
            db.beginTransaction();

            for (ContentValues initialValues : allValues) {
                values = initialValues == null ? new ContentValues() : new ContentValues(initialValues);
                try {
                    rowId = db.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
                } catch (IllegalStateException e) {
                    return rowsAdded;
                    //db = HomeDataSource.getInstance(context).getDatabase();
                    //rowId = 0;
                }
                if (rowId > 0)
                    rowsAdded++;
            }

            db.setTransactionSuccessful();
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
                db.endTransaction();
            } catch (Exception e) {
                // shouldn't happen unless it gets caught above from an illegal state
            }
        }

        return rowsAdded;
    }

    // arg[0] is the account
    // arg[1] is the position
    // arg[2] is true if it is position sent, false if it is id number
    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        if (!checkUID(context)) {
            return 0;
        }

        boolean positionSent = Boolean.parseBoolean(selectionArgs[2]);

        if (positionSent) {
            int pos = Integer.parseInt(selectionArgs[1]);
            int account = Integer.parseInt(selectionArgs[0]);

            HomeDataSource dataSource = HomeDataSource.getInstance(context);
            SQLiteDatabase db = dataSource.getDatabase();

            Cursor cursor = dataSource.getCursor(account);

            if (cursor.moveToPosition(pos)) {

                dataSource.removeCurrent(account);

                long tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

                ContentValues cv = new ContentValues();
                cv.put(HomeSQLiteHelper.COLUMN_CURRENT_POS, "1");

                ContentValues unread = new ContentValues();
                unread.put(HomeSQLiteHelper.COLUMN_CURRENT_POS, "");

                try {
                    db.update(HomeSQLiteHelper.TABLE_HOME, unread, HomeSQLiteHelper.COLUMN_CURRENT_POS + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?", new String[]{"1", account + ""});
                    db.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{tweetId + ""});
                } catch (Exception e) {

                }
            }
        } else {
            long id = Long.parseLong(selectionArgs[1]);
            int account = Integer.parseInt(selectionArgs[0]);
            Log.v("Focus_for_Mastodon_launcher_stuff", "id: " + id);

            HomeDataSource dataSource = HomeDataSource.getInstance(context);
            SQLiteDatabase db = dataSource.getDatabase();

            ContentValues cv = new ContentValues();
            cv.put(HomeSQLiteHelper.COLUMN_CURRENT_POS, "1");

            ContentValues unread = new ContentValues();
            unread.put(HomeSQLiteHelper.COLUMN_CURRENT_POS, "");

            try {
                db.update(HomeSQLiteHelper.TABLE_HOME, unread, HomeSQLiteHelper.COLUMN_CURRENT_POS + " = ? AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = ?", new String[]{"1", account + ""});
                db.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{id + ""});
            } catch (Exception e) {

            }
        }

        context.getContentResolver().notifyChange(uri, null);

        return 1;
    }

    @Override
    public synchronized int delete(Uri uri, String id, String[] selectionArgs) {

        if (!checkUID(context)) {
            return 0;
        }
        Log.d(TAG, "delete uri: " + uri.toString());
        SQLiteDatabase db = HomeDataSource.getInstance(getContext()).getDatabase();
        int count;

        String segment = uri.getLastPathSegment();
        count = db.delete(HomeSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);

        if (count > 0) {
            // Notify the Context's ContentResolver of the change
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Log.v("Focus_for_Mastodon_wearable", "querying");

        if (!checkUID(context)) {
            //return null;
        }

        HomeDataSource data = HomeDataSource.getInstance(context);
        Cursor c;

        if (selection != null) {
            c = data.getWearCursor(Integer.parseInt(selectionArgs[0]));
            Log.v("Focus_for_Mastodon_wearable", "getting the wearable cursor, size: " + c.getCount());
        } else {
            c = data.getCursor(Integer.parseInt(selectionArgs[0]));
            Log.v("Focus_for_Mastodon_wearable", "getting the normal cursor, size: " + c.getCount());
        }

        c.setNotificationUri(context.getContentResolver(), uri);

        return c;
    }

    public static void updateCurrent(int currentAccount, Context context, int position) {
        context.getContentResolver().update(HomeContentProvider.CONTENT_URI, new ContentValues(), "",
                new String[] {currentAccount + "", position + "", "true"});
    }

    public static void updateCurrent(int currentAccount, Context context, long id) {
        context.getContentResolver().update(HomeContentProvider.CONTENT_URI, new ContentValues(), "",
                new String[] {currentAccount + "", id + "", "false"});
    }

    public static int insertTweets(List<Status> statuses, int currentAccount, Context context) {
        ContentValues[] valueses = new ContentValues[statuses.size()];

        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);

            ContentValues values = HomeDataSource.getContentValues(status, currentAccount);

            valueses[i] = values;
        }

        return context.getContentResolver().bulkInsert(HomeContentProvider.CONTENT_URI, valueses);
    }
}