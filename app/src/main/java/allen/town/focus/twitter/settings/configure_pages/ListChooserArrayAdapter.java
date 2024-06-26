package allen.town.focus.twitter.settings.configure_pages;
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
import android.view.View;

import java.util.List;

import allen.town.focus.twitter.adapters.ListsArrayAdapter;

import allen.town.focus.twitter.model.MastoList;
import twitter4j.ResponseList;


public class ListChooserArrayAdapter extends ListsArrayAdapter {

    private Context context;

    public ListChooserArrayAdapter(Context context, List<MastoList> lists) {
        super(context, lists);
        this.context = context;
    }

    @Override
    public void bindView(final View view, Context mContext, final MastoList list) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = list.getTitle();
        final String id = list.getId() + "";

        holder.text.setText(name);
    }
}
