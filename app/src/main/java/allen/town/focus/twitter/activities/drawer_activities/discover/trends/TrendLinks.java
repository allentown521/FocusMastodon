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

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TrendsLinksAdapter;
import allen.town.focus.twitter.api.requests.trends.GetTrendingLinks;
import allen.town.focus.twitter.model.Card;

public class TrendLinks extends TrendTags {


    @Override
    public void getTrends() {

        new TimeoutThread(() -> {
            try {
                final List<Card> currentTrends = new GetTrendingLinks().execSync();

                ((Activity) context).runOnUiThread(() -> {
                    if (currentTrends != null) {
                        listView.setAdapter(new TrendsLinksAdapter(context, currentTrends));
                    }

                    listView.setVisibility(View.VISIBLE);

                    LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                    spinner.setVisibility(View.GONE);
                });

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }
}
