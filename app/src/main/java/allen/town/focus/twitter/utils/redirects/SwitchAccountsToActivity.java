package allen.town.focus.twitter.utils.redirects;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.MainActivity;

public class SwitchAccountsToActivity extends WhiteToolbarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int page = -1;
        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        if (currentAccount == 1) {
            sharedPrefs.edit().putInt(AppSettings.CURRENT_ACCOUNT, 2).commit();
            currentAccount = 2;
        } else {
            sharedPrefs.edit().putInt(AppSettings.CURRENT_ACCOUNT, 1).commit();
            currentAccount = 1;
        }

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_ACTIVITY) {
                page = i;
            }
        }

        if (page == -1) {
            page = 0;
        }

        sharedPrefs.edit().putBoolean(AppSettings.OPEN_A_PAGE, true).commit();
        sharedPrefs.edit().putInt(AppSettings.OPEN_WHAT_PAGE, page).commit();


        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        AppSettings.invalidate();
        main.putExtra("switch_account", true);
        overridePendingTransition(0, 0);
        finish();
        startActivity(main);
    }
}
