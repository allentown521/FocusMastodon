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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.drawer_activities.lists.ChoosenListActivity;
import allen.town.focus.twitter.activities.drawer_activities.lists.ViewUsers;
import allen.town.focus.twitter.model.MastoList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;

public class ListsArrayAdapter extends ArrayAdapter<MastoList> {

    private Context context;

    private List<MastoList> lists;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        public TextView text;
    }

    public ListsArrayAdapter(Context context, List<MastoList> lists) {
        super(context, R.layout.tweet);

        this.context = context;
        this.lists = lists;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public MastoList getItem(int i) {
        return lists.get(i);
    }

    @Override
    public int getCount() {
        return lists.size();
    }


    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.text, viewGroup, false);

        holder = new ViewHolder();

        holder.text = (TextView) v.findViewById(R.id.text);

        // sets up the font sizes
        holder.text.setTextSize(24);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final MastoList list) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = list.getTitle();
        final String id = list.getId() + "";

        holder.text.setText(name);

        holder.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent list = new Intent(context, ChoosenListActivity.class);
                list.putExtra("list_id", id);
                list.putExtra("list_name", name);
                context.startActivity(list);
            }
        });

        holder.text.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                AlertDialog.Builder builder = new AccentMaterialDialog(
                        context,
                        R.style.MaterialAlertDialogTheme
                );
                builder.setItems(R.array.lists_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final int DELETE_LIST = 0;
                        final int VIEW_USERS = 1;
                        switch (i) {
                            case DELETE_LIST:
                                new DeleteList().execute(id + "");
                                break;

                            case VIEW_USERS:
                                Intent viewUsers = new Intent(context, ViewUsers.class);
                                viewUsers.putExtra("list_id", Long.parseLong(id));
                                viewUsers.putExtra("list_name", name);
                                context.startActivity(viewUsers);
                                break;
                        }

                    }
                });

                builder.create();
                builder.show();

                return false;
            }
        });

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {
            v = newView(parent);
        } else {
            v = convertView;
        }

        bindView(v, context, lists.get(position));

        return v;
    }

    class DeleteList extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {

            boolean destroyedList;
            try {
                new allen.town.focus.twitter.api.requests.list.DeleteList(urls[0]).execSync();
                destroyedList = true;
            } catch (Exception e) {
                destroyedList = false;
            }


            return destroyedList;
        }

        protected void onPostExecute(Boolean deleted) {

            if (deleted) {
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.deleted_list), Toast.LENGTH_SHORT);
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.back_to_refresh), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }

        }
    }
}