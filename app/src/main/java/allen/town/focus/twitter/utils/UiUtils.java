package allen.town.focus.twitter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import allen.town.focus.twitter.adapters.DisplayItemsAdapter;
import allen.town.focus.twitter.api.requests.accounts.SetAccountFollowed;
import allen.town.focus.twitter.data.sq_lite.BookmarkedTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.DMDataSource;
import allen.town.focus.twitter.data.sq_lite.FavoriteTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.data.sq_lite.SavedTweetsDataSource;
import allen.town.focus.twitter.data.sq_lite.UserTweetsDataSource;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.Relationship;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.ui.displayitems.PollOptionStatusDisplayItem;
import allen.town.focus_common.util.JsonHelper;
import code.name.monkey.appthemehelper.util.ATHUtil;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import okhttp3.MediaType;
import rx.Observable;
import rx.schedulers.Schedulers;
import twitter4j.Status;

public class UiUtils {
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    private UiUtils() {
    }

    public static void launchWebBrowser(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException x) {
        }
    }

    public static String formatTimeLeft(Context context, Instant instant) {
        long t = instant.toEpochMilli();
        long now = System.currentTimeMillis();
        long diff = t - now;
        if (diff < 60_000L) {
            int secs = (int) (diff / 1000L);
            return context.getResources().getQuantityString(R.plurals.x_seconds_left, secs, secs);
        } else if (diff < 3600_000L) {
            int mins = (int) (diff / 60_000L);
            return context.getResources().getQuantityString(R.plurals.x_minutes_left, mins, mins);
        } else if (diff < 3600_000L * 24L) {
            int hours = (int) (diff / 3600_000L);
            return context.getResources().getQuantityString(R.plurals.x_hours_left, hours, hours);
        } else {
            int days = (int) (diff / (3600_000L * 24L));
            return context.getResources().getQuantityString(R.plurals.x_days_left, days, days);
        }
    }

    @SuppressLint("DefaultLocale")
    public static String abbreviateNumber(int n) {
        if (n < 1000) {
            return String.format("%,d", n);
        } else if (n < 1_000_000) {
            float a = n / 1000f;
            return a > 99f ? String.format("%,dK", (int) Math.floor(a)) : String.format("%,.1fK", a);
        } else {
            float a = n / 1_000_000f;
            return a > 99f ? String.format("%,dM", (int) Math.floor(a)) : String.format("%,.1fM", n / 1_000_000f);
        }
    }

    @SuppressLint("DefaultLocale")
    public static String abbreviateNumber(long n) {
        if (n < 1_000_000_000L)
            return abbreviateNumber((int) n);

        double a = n / 1_000_000_000.0;
        return a > 99f ? String.format("%,dB", (int) Math.floor(a)) : String.format("%,.1fB", n / 1_000_000_000.0);
    }

    /**
     * Android 6.0 has a bug where start and end compound drawables don't get tinted.
     * This works around it by setting the tint colors directly to the drawables.
     *
     * @param textView
     */
    public static void fixCompoundDrawableTintOnAndroid6(TextView textView) {
        Drawable[] drawables = textView.getCompoundDrawablesRelative();
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                Drawable tinted = drawables[i].mutate();
                tinted.setTintList(textView.getTextColors());
                drawables[i] = tinted;
            }
        }
        textView.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    public static void runOnUiThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public static void runOnUiThread(Runnable runnable, long delay) {
        mainHandler.postDelayed(runnable, delay);
    }

    public static void setRelationshipToActionButtonM3(Relationship relationship, Button button) {
        boolean secondaryStyle;
        if (relationship.blocking) {
            button.setText(R.string.button_blocked);
            secondaryStyle = true;
        } else if (relationship.blockedBy) {
            button.setText(R.string.button_follow);
            secondaryStyle = false;
        } else if (relationship.requested) {
            button.setText(R.string.button_follow_pending);
            secondaryStyle = true;
        } else if (!relationship.following) {
            button.setText(relationship.followedBy ? R.string.follow_back : R.string.button_follow);
            secondaryStyle = false;
        } else {
            button.setText(R.string.button_following);
            secondaryStyle = true;
        }

        button.setEnabled(!relationship.blockedBy);
//		int styleRes=secondaryStyle ? R.style.Widget_Mastodon_M3_Button_Tonal : R.style.Widget_Mastodon_M3_Button_Filled;
//		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
        if (secondaryStyle) {
            button.setBackgroundColor(ATHUtil.resolveColor(button.getContext(), R.attr.colorOnSurfaceVariant));
        }

//		ta.recycle();
//		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
//		button.setTextColor(ta.getColorStateList(0));
//		ta.recycle();
    }

    public static void removeCallbacks(Runnable runnable) {
        mainHandler.removeCallbacks(runnable);
    }

    /**
     * Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}.
     */
    public static int lerp(int startValue, int endValue, float fraction) {
        return startValue + Math.round(fraction * (endValue - startValue));
    }

    public static String getFileName(Uri uri) {
        return uri.getLastPathSegment();
    }


    public static MediaType getFileMediaType(File file) {
        String name = file.getName();
        return MediaType.parse(MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(name.lastIndexOf('.') + 1)));
    }


    public static int getThemeColor(Context context, @AttrRes int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int color = ta.getColor(0, 0xff00ff00);
        ta.recycle();
        return color;
    }

    public static void performAccountAction(Activity activity, Account account, String accountID, Relationship relationship, Button button, Consumer<Boolean> progressCallback, Consumer<Relationship> resultCallback) {
        progressCallback.accept(true);
        new SetAccountFollowed(account.id, !relationship.following && !relationship.requested, true)
                .setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(Relationship result) {
                        resultCallback.accept(result);
                        progressCallback.accept(false);
                        if (!result.following && !result.requested) {
//								E.post(new RemoveAccountPostsEvent(accountID, account.id, true));
                        }
                    }

                    @Override
                    public void onError(ErrorResponse error) {
                        error.showToast(activity);
                        progressCallback.accept(false);
                    }
                })
                .exec(accountID);
    }

    public static void performMuteAction(Activity activity, SharedPreferences sharedPrefs, String accountId, boolean recreate) {
        String current = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        sharedPrefs.edit().putString(AppSettings.MUTED_USERS_ID, current + accountId.replaceAll(" ", "").replaceAll("@", "") + " ").commit();
        sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
        sharedPrefs.edit().putBoolean("just_muted", true).commit();
        if (recreate) {
            activity.recreate();
        } else {
            activity.finish();
        }
    }

    public static void performUnMuteAction(Activity activity, SharedPreferences sharedPrefs, String accountId) {
        String muted = sharedPrefs.getString(AppSettings.MUTED_USERS_ID, "");
        muted = muted.replace(accountId + " ", "");
        sharedPrefs.edit().putString(AppSettings.MUTED_USERS_ID, muted).commit();
        sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
        sharedPrefs.edit().putBoolean("just_muted", true).commit();
        activity.finish();
    }

    public static void openProfileByID(Context context, String selfID, String id) {
        Bundle args = new Bundle();
        args.putString("account", selfID);
        args.putString("profileAccountID", id);
    }

    public static void openHashtagTimeline(Context context, String hashtag) {
        // found a hashtag, so open the hashtag search
        Intent search = new Intent(context, SearchedTrendsActivity.class);
        search.setAction(Intent.ACTION_SEARCH);
        search.putExtra(SearchManager.QUERY, hashtag);
        context.startActivity(search);
    }


    public static <T> void updateList(List<T> oldList, List<T> newList, RecyclerView list, RecyclerView.Adapter<?> adapter, BiPredicate<T, T> areItemsSame) {
        // Save topmost item position and offset because for some reason RecyclerView would scroll the list to weird places when you insert items at the top
        int topItem, topItemOffset;
        if (list.getChildCount() == 0) {
            topItem = topItemOffset = 0;
        } else {
            View child = list.getChildAt(0);
            topItem = list.getChildAdapterPosition(child);
            topItemOffset = child.getTop();
        }
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsSame.test(oldList.get(oldItemPosition), newList.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return true;
            }
        }).dispatchUpdatesTo(adapter);
        list.scrollToPosition(topItem);
        list.scrollBy(0, topItemOffset);
    }


    public static void openURL(Context context, boolean extBrowser, String url) {
        new WebIntentBuilder(context)
                .setUrl(url)
                .setShouldForceExternal(extBrowser)
                .build().start();
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> props = Class.forName("android.os.SystemProperties");
            Method get = props.getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (Exception ignore) {
        }
        return null;
    }

    public static boolean isMIUI() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"));
    }

    public static int alphaBlendColors(int color1, int color2, float alpha) {
        float alpha0 = 1f - alpha;
        int r = Math.round(((color1 >> 16) & 0xFF) * alpha0 + ((color2 >> 16) & 0xFF) * alpha);
        int g = Math.round(((color1 >> 8) & 0xFF) * alpha0 + ((color2 >> 8) & 0xFF) * alpha);
        int b = Math.round((color1 & 0xFF) * alpha0 + (color2 & 0xFF) * alpha);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Check to see if Android platform photopicker is available on the device\
     *
     * @return whether the device supports photopicker intents.
     */
    @SuppressLint("NewApi")
    public static boolean isPhotoPickerAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2;
        } else
            return false;
    }

    @SuppressLint("InlinedApi")
    public static Intent getMediaPickerIntent(String[] mimeTypes, int maxCount) {
        Intent intent;
        if (isPhotoPickerAvailable()) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            if (maxCount > 1)
                intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxCount);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        if (mimeTypes.length > 1) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        } else if (mimeTypes.length == 1) {
            intent.setType(mimeTypes[0]);
        } else {
            intent.setType("*/*");
        }
        if (maxCount > 1)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        return intent;
    }

    /**
     * Wraps a View.OnClickListener to filter multiple clicks in succession.
     * Useful for buttons that perform some action that changes their state asynchronously.
     *
     * @param l
     * @return
     */
    public static View.OnClickListener rateLimitedClickListener(View.OnClickListener l) {
        return new View.OnClickListener() {
            private long lastClickTime;

            @Override
            public void onClick(View v) {
                if (SystemClock.uptimeMillis() - lastClickTime > 500L) {
                    lastClickTime = SystemClock.uptimeMillis();
                    l.onClick(v);
                }
            }
        };
    }

    public static HashMap<String, String> getMuffledUsers(SharedPreferences sharedPrefs) {
        HashMap list = JsonHelper.parseObjectList(sharedPrefs.getString(AppSettings.MUFFLED_USERS_ID, ""), new TypeToken<HashMap<String, String>>() {
        }.getType());
        if (list == null) {
            list = new HashMap();
        }
        return list;
    }

    public static Set<String> getMuffledUsersKyes(SharedPreferences sharedPrefs) {
        HashMap list = JsonHelper.parseObjectList(sharedPrefs.getString(AppSettings.MUFFLED_USERS_ID, ""), new TypeToken<HashMap<String, String>>() {
        }.getType());
        if (list == null) {
            list = new HashMap();
        }
        return list.keySet();
    }

    public static void setPollRecyclerView(RecyclerView pollRecyclerView, Context context, boolean secondAcc, Status status) {
        if (status.getPoll() != null) {
            pollRecyclerView.setVisibility(View.VISIBLE);
            pollRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            final DisplayItemsAdapter displayItemsAdapter = new DisplayItemsAdapter();
            displayItemsAdapter.setData(PollOptionStatusDisplayItem.buildItems(secondAcc, status, context, (statusId, poll) -> {
                if (statusId == status.getId()) {
                    status.setPoll(poll);
                    int currentAccount = AppSettings.getSharedPreferences(context).getInt(AppSettings.CURRENT_ACCOUNT, 1);
                    if (secondAcc) {
                        if (currentAccount == 1) {
                            currentAccount = 2;
                        } else {
                            currentAccount = 1;
                        }
                    }

                    int finalCurrentAccount = currentAccount;
                    Observable.just(0).subscribeOn(Schedulers.io()).subscribe(i -> {
                        //poll投票了，但是不知道从哪个列表进来的，所以就全部更新一次
                        HomeDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        MentionsDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        DMDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        FavoriteTweetsDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        BookmarkedTweetsDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        SavedTweetsDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                        UserTweetsDataSource.getInstance(context).updateTweetPollField(status, finalCurrentAccount);
                    });


                }
                //提交了投票，需要刷新UI
                displayItemsAdapter.setData(PollOptionStatusDisplayItem.buildItems(secondAcc, status, context, null));
                displayItemsAdapter.notifyDataSetChanged();
            }));
            pollRecyclerView.setAdapter(displayItemsAdapter);
        } else {
            pollRecyclerView.setVisibility(View.GONE);
        }
    }
}
