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

package allen.town.focus.twitter.utils.text;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.BrowserActivity;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import allen.town.focus.twitter.activities.media_viewer.VideoViewerActivity;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.activities.search.SearchPager;
import allen.town.focus.twitter.data.Link;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import code.name.monkey.appthemehelper.ThemeStore;

public class TouchableSpan extends ClickableSpan {

    public TouchableSpan(Context context, Link value, boolean extBrowser) {
        mContext = context;
        mValue = value.getShort();
        full = value.getLong();
        this.extBrowser = extBrowser;

        settings = AppSettings.getInstance(context);

        mThemeColor = ThemeStore.accentColor(context);
        mColorString = Color.argb(70, Color.red(ThemeStore.accentColor(context)),
                Color.green(ThemeStore.accentColor(context)),
                Color.blue(ThemeStore.accentColor(context)));

        // getconnectionstatus() is true if on mobile data, false otherwise
        mobilizedBrowser = settings.alwaysMobilize || (settings.mobilizeOnData && Utils.getConnectionStatus(context));

        fromLauncher = false;
    }

    private AppSettings settings;
    public final Context mContext;
    private final String mValue;
    private final String full;
    private int mThemeColor;
    private int mColorString;
    private boolean extBrowser;
    private boolean mobilizedBrowser;
    private boolean fromLauncher;

    @Override
    public void onClick(View widget) {
        mContext.sendBroadcast(new Intent(AppSettings.BROADCAST_MARK_POSITION));

        if (Patterns.WEB_URL.matcher(mValue).find()) {
            String url = "http://" + full.replace("http://", "").replace("https://", "").replace("\"", "");
            UiUtils.openURL(mContext, extBrowser, url);
        } else if (Regex.HASHTAG_PATTERN.matcher(mValue).find()) {
            // found a hashtag, so open the hashtag search
            UiUtils.openHashtagTimeline(mContext, full);
        } else if (Regex.MENTION_PATTERN.matcher(mValue).find()) {
            ProfilePager.start(mContext, full.replace("@", "").replaceAll(" ", ""));
        } else if (Regex.CASHTAG_PATTERN.matcher(mValue).find()) {
            // found a cashtag, so open the search
            Intent search = new Intent(mContext, SearchedTrendsActivity.class);
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, full);
            mContext.startActivity(search);
        }

        new Handler().postDelayed(() -> TouchableMovementMethod.touched = false, 500);
    }

    public void onLongClick() {
        if (Patterns.WEB_URL.matcher(mValue).find()) {
            // open external
            // open internal
            // copy link
            // share link
            longClickWeb(mContext, full);
        } else if (Regex.HASHTAG_PATTERN.matcher(mValue).find()) {
            // search hashtag
            // mute hashtag
            // copy hashtag
            longClickHashtag(mContext, full);
        } else if (Regex.MENTION_PATTERN.matcher(mValue).find()) {
            // Open profile
            // copy handle
            // search
            // favorite user
            // mute user
            // share profile
            longClickMentions(mContext, full);
        } else if (Regex.CASHTAG_PATTERN.matcher(mValue).find()) {
            // search cashtag
            // copy cashtag
            longClickCashtag();
        }

        new Handler().postDelayed(() -> TouchableMovementMethod.touched = false, 500);
    }

    public boolean touched = false;

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);

        ds.setUnderlineText(false);
        ds.setColor(mThemeColor);
        ds.bgColor = touched ? mColorString : Color.TRANSPARENT;
    }

    public void setTouched(boolean isTouched) {
        touched = isTouched;
    }

    public static void longClickWeb(final Context mContext, final String full) {
        AlertDialog.Builder builder = getBuilder(mContext, full);

        builder.setItems(R.array.long_click_web, (dialogInterface, i) -> {
            switch (i) {
                case 0: // open external
                    String data = full.replace("http://", "").replace("https://", "").replace("\"", "");
                    Uri weburi = Uri.parse("http://" + data);
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                    launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        mContext.startActivity(launchBrowser);
                    } catch (Exception e) {
                        TopSnackbarUtil.showSnack(mContext, "No browser found.", Toast.LENGTH_SHORT);
                    }
                    break;
                case 1: // open internal
                    data = "http://" + full.replace("http://", "").replace("https://", "").replace("\"", "");

                    if (data.contains("vine.co/v/")) {
                        VideoViewerActivity.startActivity(mContext, 0l, data, "");
                    } else {
                        AppSettings settings = AppSettings.getInstance(mContext);

                        launchBrowser = new Intent(mContext, BrowserActivity.class);
                        launchBrowser.putExtra("url", data);
                        mContext.startActivity(launchBrowser);
                    }

                    break;
                case 2: // copy link
                    copy(mContext, full);
                    break;
                case 3: // share link
                    share(mContext, full);
                    break;
            }
        });

        builder.create().show();
    }

    public static void longClickHashtag(Context mContext, String full) {
        AlertDialog.Builder builder = getBuilder(mContext, full);

        builder.setItems(R.array.long_click_hashtag, (dialogInterface, i) -> {
            switch (i) {
                case 0: // search hashtag
                    UiUtils.openHashtagTimeline(mContext, full);
                    break;
                case 1: // copy hashtag
                    copy(mContext, full);
                    break;
            }
        });

        builder.create().show();
    }

    public void longClickMentions(Context mContext, String full) {
    }

    public void longClickCashtag() {
        AlertDialog.Builder builder = getBuilder(mContext, full);

        builder.setItems(R.array.long_click_cashtag, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // search cashtag
                        TouchableSpan.this.onClick(null);
                        break;
                    case 1: // copy cashtag
                        copy(mContext, full);
                        break;
                }
            }
        });

        builder.create().show();
    }

    public static AlertDialog.Builder getBuilder(Context mContext, String full) {
        return new AccentMaterialDialog(
                mContext,
                R.style.MaterialAlertDialogTheme
        )
                .setTitle(full);
    }

    public static void search(Context mContext, String text) {
        Intent search = new Intent(mContext, SearchPager.class);
        search.setAction(Intent.ACTION_SEARCH);
        search.putExtra(SearchManager.QUERY, text);
        mContext.startActivity(search);
    }

    public static void copy(Context mContext, String text) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("link", text);
        clipboard.setPrimaryClip(clip);
        //show toast, android from S_V2 on has built-in popup, as documented in
        //https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            TopSnackbarUtil.showSnack(mContext, R.string.copied, Toast.LENGTH_SHORT);
        }
    }

    public static void share(Context mContext, String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);

        //mContext.startActivity(Intent.createChooser(share, "Share with:"));
        mContext.startActivity(share);
    }
}