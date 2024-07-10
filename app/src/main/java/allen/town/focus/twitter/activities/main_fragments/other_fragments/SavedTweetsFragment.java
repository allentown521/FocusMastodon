package allen.town.focus.twitter.activities.main_fragments.other_fragments;
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

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.HomeExtensionFragment;
import allen.town.focus.twitter.data.sq_lite.SavedTweetsDataSource;

public class SavedTweetsFragment extends HomeExtensionFragment {

    public static final String REFRESH_ACTION = "allen.town.focus.twitter.SAVED_TWEETS_REFRESHED";

    public BroadcastReceiver resetLists = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(true);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(REFRESH_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(resetLists, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(resetLists, filter);
        }
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_saved_tweets_summary);
    }

    @Override
    public Cursor getCursor() {
        return SavedTweetsDataSource.getInstance(context).getCursor(settings.currentAccount);
    }
}