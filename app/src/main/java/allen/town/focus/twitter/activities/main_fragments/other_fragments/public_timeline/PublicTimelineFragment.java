package allen.town.focus.twitter.activities.main_fragments.other_fragments.public_timeline;
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

import static allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity.getMaxIdFromList;

import allen.town.focus.twitter.activities.drawer_activities.discover.TrendTweets;
import allen.town.focus.twitter.api.requests.timelines.GetPublicTimeline;
import allen.town.focus.twitter.model.HeaderPaginationList;
import twitter4j.StatusJSONImplMastodon;

public class PublicTimelineFragment extends TrendTweets {

    @Override
    public HeaderPaginationList<StatusJSONImplMastodon> getStatusList() throws Exception {
        return StatusJSONImplMastodon.createStatusList(
                new GetPublicTimeline(false, false, statuses.size() > 0 ? getMaxIdFromList(statuses) + "" : null, COUNT_PER, null).execSync());
    }

}