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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.widget.Toolbar;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.model.MastoList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import twitter4j.ResponseList;


public class ListChooser extends WhiteToolbarActivity {

    private Context context;
    private AppSettings settings;

    private ListView listView;
    private ListChooserArrayAdapter arrayAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        context = this;

        settings = AppSettings.getInstance(this);

        Utils.setUpMainTheme(context, settings);
        setContentView(R.layout.list_chooser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.lists));
        
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    i = i - 1;
                }

                MastoList list = arrayAdapter.getItem(i);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("listId", Long.parseLong(list.getId()));
                returnIntent.putExtra("name", list.getTitle());
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {

            View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
            listView.addHeaderView(viewHeader, null, false);
            listView.setHeaderDividersEnabled(false);
        }

        new GetLists().execute();
    }

    class GetLists extends AsyncTask<String, Void, List<MastoList>> {

        protected List<MastoList> doInBackground(String... urls) {
            try {

                List<MastoList> lists = new allen.town.focus.twitter.api.requests.list.GetLists().execSync();

                Collections.sort(lists, Comparator.comparing(MastoList::getTitle));

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(List<MastoList> lists) {

            if (lists != null) {
                arrayAdapter = new ListChooserArrayAdapter(context, lists);
                listView.setAdapter(arrayAdapter);
                listView.setVisibility(View.VISIBLE);
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}
