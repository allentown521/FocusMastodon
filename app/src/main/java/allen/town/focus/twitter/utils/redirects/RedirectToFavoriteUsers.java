package allen.town.focus.twitter.utils.redirects;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.MainActivity;

public class RedirectToFavoriteUsers extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        SharedPreferences sharedPrefs = AppSettings.getInstance(this).sharedPrefs;

        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        int page = -1;
        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_FAV_USERS_TWEETS) {
                page = i;
            }
        }

        Intent favs = new Intent(this, MainActivity.class);

        if (page != -1) {
            sharedPrefs.edit().putBoolean(AppSettings.OPEN_A_PAGE, true).commit();
            sharedPrefs.edit().putInt(AppSettings.OPEN_WHAT_PAGE, page).commit();
        }

        finish();

        overridePendingTransition(0,0);

        startActivity(favs);
    }
}