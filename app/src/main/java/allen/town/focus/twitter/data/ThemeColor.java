package allen.town.focus.twitter.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.klinker.android.launcher.api.ResourceHelper;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;

public class ThemeColor {

    public ResourceHelper helper;
    private Context mContext;


    public ThemeColor(String prefix, Context context) {
        this.mContext = context;
        helper = new ResourceHelper(context, Utils.PACKAGE_NAME);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


    }
}
