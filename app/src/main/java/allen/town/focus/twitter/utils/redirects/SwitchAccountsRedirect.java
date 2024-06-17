package allen.town.focus.twitter.utils.redirects;
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.MainActivity;

public class SwitchAccountsRedirect extends WhiteToolbarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int page = -1;
        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        // first lets see if they have the second mentions page
        // if they do, then we will direct them to that instead of switching accounts
        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_SECOND_MENTIONS) {
                page = i;
            }
        }

        if (page == -1) {
            // they do not use the second mentions page, so we will switch accounts
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

                if (type == AppSettings.PAGE_TYPE_MENTIONS) {
                    page = i;
                }
            }

            if (page == -1) {
                page = 0;
            }
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
