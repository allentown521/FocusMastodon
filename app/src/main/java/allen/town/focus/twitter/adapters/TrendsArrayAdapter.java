package allen.town.focus.twitter.adapters;
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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.model.Hashtag;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.views.HashtagChartView;
import twitter4j.User;


public class TrendsArrayAdapter extends ArrayAdapter<User> {

    protected Context context;

    protected List<Hashtag> hashTagList;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        public TextView title, subtitle;
        private HashtagChartView chart;
    }

    public TrendsArrayAdapter(Context context, List<Hashtag> hashtags) {
        super(context, R.layout.tweet);

        this.context = context;
        this.hashTagList = hashtags;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        try {
            return hashTagList.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.item_trending_hashtag, viewGroup, false);

        holder = new ViewHolder();

        holder.title = v.findViewById(R.id.title);
        holder.subtitle = v.findViewById(R.id.subtitle);
        holder.chart = v.findViewById(R.id.chart);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final Hashtag item) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.title.setText('#' + item.name);
        int numPeople = item.history.get(0).accounts;
        if (item.history.size() > 1)
            numPeople += item.history.get(1).accounts;
        holder.subtitle.setText(mContext.getResources().getQuantityString(R.plurals.x_people_talking, numPeople, numPeople));
        holder.chart.setData(item.history);

        view.setOnClickListener(view1 -> {
            Intent search = new Intent(context, SearchedTrendsActivity.class);
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, "\"" + item.name + "\"");
            context.startActivity(search);
        });

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();
        }

        bindView(v, context, hashTagList.get(position));

        return v;
    }

}
