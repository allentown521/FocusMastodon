package allen.town.focus.twitter.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceGroup;

import allen.town.focus.twitter.R;

public class PrefFragmentAdvanced extends PrefFragment {

    @Override
    public void setPreferences(int position) {
        switch (position) {
            case 0: // advanced app style
                addPreferencesFromResource(R.xml.settings_advanced_app_style);
                setupAppStyle();
                break;
            case 1: // advanced widget customization
                break;
            case 2: // advanced swipable page and app drawer
                break;
            case 3: // advanced background refreshes
                addPreferencesFromResource(R.xml.settings_advanced_background_refreshes);
                setUpBackgroundRefreshes();
                break;
            case 4: // advanced notifications
                break;
            case 5: // data saving
                break;
            case 6: // location
                break;
            case 7: // mute management
                break;
            case 8: // app memory
                break;
            case 9: // other options
                break;
        }
    }

    @Override
    public void setupAppStyle() {

    }

    @Override
    public void setUpBackgroundRefreshes() {
        final Context context = getActivity();

        final AppSettings settings = AppSettings.getInstance(context);
        final SharedPreferences sharedPrefs = settings.sharedPrefs;

        int count = 0;
        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }
        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        final boolean mentionsChanges = count == 2;

        if(count != 2) {
            ((PreferenceGroup) findPreference("other_options")).removePreference(findPreference("sync_second_mentions"));
        }
    }

}
