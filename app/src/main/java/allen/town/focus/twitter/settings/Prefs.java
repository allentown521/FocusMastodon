package allen.town.focus.twitter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import allen.town.focus.twitter.settings.font.Font;

import java.util.Set;


public class Prefs {
    public final ReaderPreference<Font> articleListFontType;
    //primary theme
    private final SharedPreferences preferences;

    public Prefs(Context context) {
        this.preferences = context.getSharedPreferences("focus_twitter_sp", 0);
        this.articleListFontType = new ReaderPreference(this.preferences, "article_list_font", Font.DEFAULT_FONT);
    }

    public void saveMainPreferences(Context context,String key, Set<String> set) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(key, set).commit();
    }

    public void saveMainPreferences(Context context,String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
    }

    public String getMainPreferences(Context context,String key,String defalutValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key,defalutValue);
    }

    public static float getFontSizeScale(Context context) {
        return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_font_size_scale_key",
                "1"));
    }

}
