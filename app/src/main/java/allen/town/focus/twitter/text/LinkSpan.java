package allen.town.focus.twitter.text;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.Set;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.DrawerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.api.requests.accounts.GetAccountByID;
import allen.town.focus.twitter.api.requests.accounts.SetAccountMuted;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersDataSource;
import allen.town.focus.twitter.model.Relationship;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.text.TouchableSpan;
import allen.town.focus_common.util.JsonHelper;
import code.name.monkey.appthemehelper.ThemeStore;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class LinkSpan extends CharacterStyle {

    private int color = 0xFF00FF00;
    private OnLinkClickListener listener;
    private String link;
    private Type type;
    private String accountID;
    private AppSettings settings;
    private int mThemeColor;
    private int mColorString;
    private boolean extBrowser;
    private String originalHref;
    private String content;

    public LinkSpan(String link, OnLinkClickListener listener, Type type, String accountID, boolean extBrowser, String originalHref, String content) {
        this.listener = listener;
        this.link = link;
        this.type = type;
        this.accountID = accountID;
        this.originalHref = originalHref;
        this.content = content;

        Context context = App.getInstance();
        this.extBrowser = extBrowser;

        settings = AppSettings.getInstance(context);

        mThemeColor = ThemeStore.accentColor(context);
        mColorString = Color.argb(70, Color.red(ThemeStore.accentColor(context)),
                Color.green(ThemeStore.accentColor(context)),
                Color.blue(ThemeStore.accentColor(context)));

        //点击后的颜色
        color = mColorString;
    }

    public boolean touched = false;

    public int getColor() {
        return color;
    }

    public void setTouched(boolean isTouched) {
        touched = isTouched;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setUnderlineText(false);
        tp.setColor(mThemeColor);
//        tp.bgColor = touched ? mColorString : Color.TRANSPARENT;
    }

    public void onClick(Context context) {
        switch (getType()) {
            case URL -> UiUtils.openURL(context, false, link);
            case MENTION -> ProfilePager.start(context, link);
            case HASHTAG -> UiUtils.openHashtagTimeline(context, link);
            case CUSTOM -> listener.onLinkClick(this);
        }
    }

    public void onLongClick(Context context) {
        switch (getType()) {
            case URL -> TouchableSpan.longClickWeb(context, link);
            case MENTION -> longClickMentions(context);
            case HASHTAG -> TouchableSpan.longClickHashtag(context, link);
            case CUSTOM -> listener.onLinkClick(this);
        }
    }

    public void longClickMentions(Context context) {
        AlertDialog.Builder builder = TouchableSpan.getBuilder(context, content);

        builder.setItems(R.array.long_click_mentions, (dialogInterface, i) -> {
            final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

            switch (i) {
                case 0: // open profile
                    LinkSpan.this.onClick(context);
                    break;
                case 1: // copy handle
                    TouchableSpan.copy(context, content);
                    break;
                case 2: // favorite user
                    new TimeoutThread(() -> {
                        try {
                            User user = new UserJSONImplMastodon(new GetAccountByID(link).execSync());

                            int current = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

                            FavoriteUsersDataSource.getInstance(context).createUser(user, current);

                        } catch (Exception e) {

                        }
                    }).start();
                    break;
                case 3: // mute user
                    new SetAccountMuted(link, true).setCallback(new Callback<>() {
                        @Override
                        public void onSuccess(Relationship result) {
                            UiUtils.performMuteAction((Activity) context, sharedPrefs, link, true);
                        }

                        @Override
                        public void onError(ErrorResponse error) {

                        }
                    }).exec();

                    break;
                case 4: // muffle user
                    Set<String> muffled = UiUtils.getMuffledUsersKyes(sharedPrefs);

                    if (!muffled.contains(link)) {
                        HashMap userList = UiUtils.getMuffledUsers(sharedPrefs);
                        userList.put(link, content);
                        sharedPrefs.edit().putString(AppSettings.MUFFLED_USERS_ID, JsonHelper.toJSONString(userList)).commit();
                        sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
                        sharedPrefs.edit().putBoolean("just_muted", true).commit();

                        if (context instanceof DrawerActivity) {
                            ((Activity) context).recreate();
                        }
                    }
                    break;
                case 5: // share profile
                    TouchableSpan.share(context, originalHref);
                    break;
            }
        });

        builder.create().show();
    }

    public String getLink() {
        return link;
    }

    public Type getType() {
        return type;
    }

    public void setListener(OnLinkClickListener listener) {
        this.listener = listener;
    }

    public interface OnLinkClickListener {
        void onLinkClick(LinkSpan span);
    }

    public enum Type {
        URL,
        MENTION,
        HASHTAG,
        CUSTOM
    }
}
