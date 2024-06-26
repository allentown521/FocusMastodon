package allen.town.focus.twitter.activities.drawer_activities.discover.trends;
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

import android.os.Bundle;

import allen.town.focus.twitter.settings.AppSettings;

public class LauncherSearchedTrends extends SearchedTrendsActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        int acc = getIntent().getIntExtra(AppSettings.CURRENT_ACCOUNT, 0);

        if (acc != 0) {
            AppSettings.getSharedPreferences(this)
                    .edit()
                    .putInt(AppSettings.CURRENT_ACCOUNT, acc)
                    .commit();

            AppSettings.invalidate();
        }

        super.onCreate(savedInstanceState);
    }

}
