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

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.widget.SwitchCompat;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.settings.AppSettings;

import java.util.ArrayList;
import java.util.List;


public class ChooserFragment extends Fragment {

    private static final String DEFAULT_CLICKED = "allen.town.focus.twitter.CLICKED_CHECK";

    protected Context context;
    protected SharedPreferences sharedPrefs;
    protected ActionBar actionBar;
    public SwitchCompat check;

    public boolean finishedCreate = false;

    private boolean thisFragmentClicked = false;
    BroadcastReceiver defaultClicked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!thisFragmentClicked) {
                check.setChecked(false);
            } else {
                thisFragmentClicked = false;
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = activity.getActionBar();

        sharedPrefs = AppSettings.getSharedPreferences(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        List<String> list = new ArrayList<String>();
        list.add(getString(R.string.do_not_use));
        list.add(getString(R.string.timeline));
        list.add(getString(R.string.mentions));
        list.add(getString(R.string.direct_messages));
        list.add(getString(R.string.list_page));
        list.add(getString(R.string.favorite_users_tweets));
        list.add(getString(R.string.link_page));
        list.add(getString(R.string.picture_page));
        list.add(getString(R.string.second_acc_mentions));
        list.add(getString(R.string.world_timeline));
        list.add(getString(R.string.local_timeline));
        list.add(getString(R.string.hashtags));
        list.add(getString(R.string.activity));
        list.add(getString(R.string.favorite_tweets));
        list.add(context.getString(R.string.user_tweets));
        list.add(context.getString(R.string.saved_tweets));
        list.add(getString(R.string.bookmarked_tweets));
        list.add(getString(R.string.lists));
        list.add(getString(R.string.retweets));
        list.add(getString(R.string.favorite_users));
        list.add(getString(R.string.discover));


        View layout = inflater.inflate(R.layout.configuration_page, null);

        Spinner spinner = (Spinner) layout.findViewById(R.id.selection_spinner);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        check = (SwitchCompat) layout.findViewById(R.id.default_page);
        final LinearLayout checkLayout = (LinearLayout) layout.findViewById(R.id.default_page_layout);
        checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!check.isChecked()) {
                    check.setChecked(true);
                    thisFragmentClicked = true;
                    context.sendBroadcast(new Intent(DEFAULT_CLICKED));
                }
            }
        });

        int position = getArguments().getInt("position", 0);
        int curr = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
        if (position == sharedPrefs.getInt(AppSettings.DEFAULT_TIMELINE_PAGE + curr, 0)) {
            check.setChecked(true);
        }

        int type = sharedPrefs.getInt("account_" + curr + "_page_" + (position + 1), AppSettings.PAGE_TYPE_NONE);

        if (type != AppSettings.PAGE_TYPE_NONE) {
            String listIdentifier = "account_" + curr + "_list_" + (position + 1) + "_long";
            String userIdentifier = "account_" + curr + "_user_tweets_" + (position + 1) + "_long";
            String nameIdentifier = "account_" + curr + "_name_" + (position + 1);
            String searchIdentifier = "account_" + curr + "_search_" + (position + 1);

            setType(type);
            setName(sharedPrefs.getString(nameIdentifier, ""));
            setSearchQuery(sharedPrefs.getString(searchIdentifier, ""));
            setListId(sharedPrefs.getLong(listIdentifier, 0l));
            setUserId(sharedPrefs.getLong(userIdentifier, 0l));
        }

        switch (type) {
            case AppSettings.PAGE_TYPE_HOME:
                spinner.setSelection(1);
                break;
            case AppSettings.PAGE_TYPE_MENTIONS:
                spinner.setSelection(2);
                break;
            case AppSettings.PAGE_TYPE_DMS:
                spinner.setSelection(3);
                break;
            case AppSettings.PAGE_TYPE_LIST_TIMELINE:
                spinner.setSelection(4);
                break;
            case AppSettings.PAGE_TYPE_FAV_USERS_TWEETS:
                spinner.setSelection(5);
                break;
            case AppSettings.PAGE_TYPE_LINKS:
                spinner.setSelection(6);
                break;
            case AppSettings.PAGE_TYPE_PICS:
                spinner.setSelection(7);
                break;
            case AppSettings.PAGE_TYPE_SECOND_MENTIONS:
                spinner.setSelection(8);
                break;
            case AppSettings.PAGE_TYPE_WORLD_TIMELINE:
                spinner.setSelection(9);
                break;
            case AppSettings.PAGE_TYPE_LOCAL_TIMELINE:
                spinner.setSelection(10);
                break;
            case AppSettings.PAGE_TYPE_HASHTAGS:
                spinner.setSelection(11);
                break;
            case AppSettings.PAGE_TYPE_ACTIVITY:
                spinner.setSelection(12);
                break;
            case AppSettings.PAGE_TYPE_FAVORITE_STATUS:
                spinner.setSelection(13);
                break;
            case AppSettings.PAGE_TYPE_USER_TWEETS:
                spinner.setSelection(14);
                break;
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                spinner.setSelection(15);
                break;
            case AppSettings.PAGE_TYPE_BOOKMARKED_TWEETS:
                spinner.setSelection(16);
                break;
            case AppSettings.PAGE_TYPE_LIST:
                spinner.setSelection(17);
                break;
            case AppSettings.PAGE_TYPE_RETWEET:
                spinner.setSelection(18);
            case AppSettings.PAGE_TYPE_FAV_USERS:
                spinner.setSelection(19);
                break;
            case AppSettings.PAGE_TYPE_DISCOVER:
                spinner.setSelection(20);
                break;
            default:
                spinner.setSelection(0);
                break;
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!finishedCreate) {
                    return;
                }

                switch (position) {
                    case 0:
                        setType(AppSettings.PAGE_TYPE_NONE);
                        break;
                    case 1:
                        setType(AppSettings.PAGE_TYPE_HOME);
                        break;
                    case 2:
                        setType(AppSettings.PAGE_TYPE_MENTIONS);
                        break;
                    case 3:
                        setType(AppSettings.PAGE_TYPE_DMS);
                        break;
                    case 4:
                        Intent chooser = new Intent(context, ListChooser.class);
                        startActivityForResult(chooser, REQUEST_LIST);
                        break;
                    case 5:
                        setType(AppSettings.PAGE_TYPE_FAV_USERS_TWEETS);
                        break;
                    case 6:
                        setType(AppSettings.PAGE_TYPE_LINKS);
                        break;
                    case 7:
                        setType(AppSettings.PAGE_TYPE_PICS);
                        break;
                    case 8:
                        setType(AppSettings.PAGE_TYPE_SECOND_MENTIONS);
                        break;
                    case 9:
                        setType(AppSettings.PAGE_TYPE_WORLD_TIMELINE);
                        break;
                    case 10:
                        setType(AppSettings.PAGE_TYPE_LOCAL_TIMELINE);
                        break;
                    case 11:
                        setType(AppSettings.PAGE_TYPE_HASHTAGS);
                        break;
                    case 12:
                        setType(AppSettings.PAGE_TYPE_ACTIVITY);
                        break;
                    case 13:
                        setType(AppSettings.PAGE_TYPE_FAVORITE_STATUS);
                        break;
                    case 14:
                        chooser = new Intent(context, UserChooser.class);
                        startActivityForResult(chooser, REQUEST_USER);
                        break;
                    case 15:
                        setType(AppSettings.PAGE_TYPE_SAVED_TWEETS);
                        break;
                    case 16:
                        setType(AppSettings.PAGE_TYPE_BOOKMARKED_TWEETS);
                        break;
                    case 17:
                        setType(AppSettings.PAGE_TYPE_LIST);
                        break;
                    case 18:
                        setType(AppSettings.PAGE_TYPE_RETWEET);
                        break;
                    case 19:
                        setType(AppSettings.PAGE_TYPE_FAV_USERS);
                        break;
                    case 20:
                        setType(AppSettings.PAGE_TYPE_DISCOVER);
                        break;
                    default:
                        setType(position);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finishedCreate = true;
            }
        }, 500);

        return layout;
    }

    public static final int REQUEST_LIST = 1;
    public static final int REQUEST_SAVED_SEARCH = 2;
    public static final int REQUEST_USER = 3;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_LIST) {
            if(resultCode == Activity.RESULT_OK) {
                setListId(data.getLongExtra("listId", 0));
                setUserId(0);
                String listName = data.getStringExtra("name");
                setName(listName);
                setSearchQuery("");
                setType(AppSettings.PAGE_TYPE_LIST_TIMELINE);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                setType(AppSettings.PAGE_TYPE_NONE);
                setName("");
                setListId(0);
                setUserId(0);
                setSearchQuery("");
            }
        } else if (requestCode == REQUEST_SAVED_SEARCH) {
            if (resultCode == Activity.RESULT_OK) {
                setSearchQuery(data.getStringExtra("search_query"));
                setType(AppSettings.PAGE_TYPE_HASHTAGS);
                setName("");
                setUserId(0);
                setListId(0);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                setType(AppSettings.PAGE_TYPE_NONE);
                setSearchQuery("");
                setName("");
                setUserId(0);
                setListId(0);
            }
        } else if (requestCode == REQUEST_USER) {
            if (resultCode == Activity.RESULT_OK) {
                setUserId(data.getLongExtra("userId", 0));
                String listName = data.getStringExtra("name");
                setListId(0);
                setName(listName);
                setSearchQuery("");
                setType(AppSettings.PAGE_TYPE_USER_TWEETS);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                setType(AppSettings.PAGE_TYPE_NONE);
                setName("");
                setListId(0);
                setUserId(0);
                setSearchQuery("");
            }
        }
    }

    public int type = AppSettings.PAGE_TYPE_NONE;
    public long listId = 0;
    public long userId = 0;
    public String name = "";
    public String searchQuery = "";

    protected void setType(int type) {
        this.type = type;
    }
    protected void setListId(long id) {
        this.listId = id;
    }
    protected void setUserId(long id) {
        this.userId = id;
    }
    protected void setName(String listName) {
        this.name = listName;
    }
    protected void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(DEFAULT_CLICKED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(defaultClicked, filter , RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(defaultClicked, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        context.unregisterReceiver(defaultClicked);
    }
}
