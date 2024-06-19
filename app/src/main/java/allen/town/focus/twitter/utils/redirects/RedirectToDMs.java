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

import androidx.appcompat.app.AppCompatActivity;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.MainActivity;

public class RedirectToDMs extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        int page = 0;
        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_DMS) {
                page = i;
            }
        }

        Intent dm = new Intent(this, MainActivity.class);

        sharedPrefs.edit().putBoolean(AppSettings.OPEN_A_PAGE, true).commit();
        sharedPrefs.edit().putInt(AppSettings.OPEN_WHAT_PAGE, page).commit();

        finish();

        overridePendingTransition(0,0);

        startActivity(dm);
    }
}