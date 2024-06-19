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

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Collections;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.main_fragments.MainFragment;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.ListsArrayAdapter;
import allen.town.focus.twitter.api.requests.list.GetLists;
import allen.town.focus.twitter.databinding.CreateListDialogBinding;
import allen.town.focus.twitter.model.MastoList;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.views.widgets.text.FontPrefEditText;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;

public class ListsFragment extends MainFragment {

    @Override
    protected String getNoContentTitle() {
        return getString(R.string.no_content_lists_page);
    }

    @Override
    protected String getNoContentSummary() {
        return getString(R.string.no_content_lists_page_summary);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (getResources().getBoolean(R.bool.has_drawer) ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        getLists();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.list_activity, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

    }

    private boolean clicked = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add_list:
                CreateListDialogBinding createListDialogBinding = CreateListDialogBinding.inflate(getLayoutInflater());
                AlertDialog dialog = new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme)
                        .setView(createListDialogBinding.getRoot())
                        .setTitle(getResources().getString(R.string.create_new_list))
                        .create();

                final FontPrefEditText name = createListDialogBinding.name;
                final FontPrefEditText description = createListDialogBinding.description;

                createListDialogBinding.cancel.setOnClickListener(v -> {
                    if (!clicked) {
                        dialog.dismiss();
                    }
                    clicked = true;
                });

                createListDialogBinding.privateBtn.setOnClickListener(view -> {
                    if (!clicked) {
                        new CreateList(name.getText().toString(), false, description.getText().toString()).execute();
                        dialog.dismiss();
                    }
                    clicked = true;
                });

                createListDialogBinding.publicBtn.setOnClickListener(view -> {
                    if (!clicked) {
                        new CreateList(name.getText().toString(), true, description.getText().toString()).execute();
                        dialog.dismiss();
                    }
                    clicked = true;
                });

                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getLists() {
        new TimeoutThread(() -> {
            try {

                final List<MastoList> lists;
                try {
                    lists = new GetLists().execSync();
                } catch (OutOfMemoryError e) {
                    return;
                }

                Collections.sort(lists, (result1, result2) -> result1.getTitle().compareTo(result2.toString()));

                context.runOnUiThread(() -> {
                    if (lists.size() > 0) {
                        listView.setAdapter(new ListsArrayAdapter(context, lists));
                        listView.setVisibility(View.VISIBLE);
                    } else {
                        try {
                            noContent.setVisibility(View.VISIBLE);
                        } catch (Exception e) {

                        }
                        listView.setVisibility(View.GONE);
                    }

                    spinner.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                context.runOnUiThread(() -> {
                    noContent.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);

                    spinner.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    class CreateList extends AsyncTask<String, Void, Boolean> {

        String name;
        String description;
        boolean publicList;

        public CreateList(String name, boolean publicList, String description) {
            this.name = name;
            this.publicList = publicList;
            this.description = description;
        }

        protected Boolean doInBackground(String... urls) {
            try {

                new allen.town.focus.twitter.api.requests.list.CreateList(name).execSync();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean created) {
            if (created) {
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }

    private boolean changedConfig = false;
    private boolean activityActive = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activityActive) {
        } else {
            changedConfig = true;
        }
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

    }

    @Override
    public void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changedConfig) {
        }

        activityActive = true;
        changedConfig = false;
    }

}
