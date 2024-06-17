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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.model.Card;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.views.BlurhashCrossfadeDrawable;
import allen.town.focus.twitter.views.OutlineProviders;
import twitter4j.User;


public class TrendsLinksAdapter extends ArrayAdapter<User> {

    protected Context context;

    protected List<Card> hashTagList;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        private TextView name, title, subtitle;
        private ImageView photo;
        private BlurhashCrossfadeDrawable crossfadeDrawable = new BlurhashCrossfadeDrawable();
        private boolean didClear;
    }

    public TrendsLinksAdapter(Context context, List<Card> hashtags) {
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

        v = inflater.inflate(R.layout.item_trending_link, viewGroup, false);

        holder = new ViewHolder();

        holder.name = v.findViewById(R.id.name);
        holder.title = v.findViewById(R.id.title);
        holder.subtitle = v.findViewById(R.id.subtitle);
        holder.photo = v.findViewById(R.id.photo);
        holder.photo.setOutlineProvider(OutlineProviders.roundedRect(12));
        holder.photo.setClipToOutline(true);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final Card item) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.name.setText(item.providerName);
        holder.title.setText(item.title);
        int num = item.history.get(0).uses;
        if (item.history.size() > 1)
            num += item.history.get(1).uses;
        holder.subtitle.setText(mContext.getResources().getQuantityString(R.plurals.discussed_x_times, num, num));
        holder.crossfadeDrawable.setSize(item.width, item.height);
        holder.crossfadeDrawable.setBlurhashDrawable(item.blurhashPlaceholder);
        holder.crossfadeDrawable.setCrossfadeAlpha(0f);
        holder.photo.setImageDrawable(null);
        //原版的有blur效果，没实现
        holder.photo.setImageDrawable(holder.crossfadeDrawable);
        Glide.with(mContext).load(item.image).into(holder.photo);

        view.setOnClickListener(view1 -> {
            UiUtils.openURL(mContext, false, item.url);
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
