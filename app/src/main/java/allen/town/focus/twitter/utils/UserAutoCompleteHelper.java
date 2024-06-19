package allen.town.focus.twitter.utils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.appcompat.widget.ListPopupWindow;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.adapters.AutoCompleteHashtagAdapter;
import allen.town.focus.twitter.adapters.AutoCompletePeopleAdapter;
import allen.town.focus.twitter.adapters.AutoCompleteUserArrayAdapter;
import allen.town.focus.twitter.api.requests.search.GetSearchResults;
import allen.town.focus.twitter.data.sq_lite.FollowersDataSource;
import allen.town.focus.twitter.data.sq_lite.HashtagDataSource;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.Timber;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class UserAutoCompleteHelper {

    private static final int POPUP_WINDOW_HEIGHT = 300;

    public interface Callback {
        void onUserSelected(User selectedUser);
    }

    private Activity context;
    private Handler handler;
    private Handler visibilityHandler;
    private ListPopupWindow userAutoComplete;
    private ListPopupWindow hashtagAutoComplete;
    private AutoCompleteHelper autoCompleter;
    private EditText textView;
    private Callback callback;

    private AutoCompletePeopleAdapter adapter;

    private HeaderPaginationList<User> users = new HeaderPaginationList<>();

    public static UserAutoCompleteHelper applyTo(Activity activity, EditText tv) {
        UserAutoCompleteHelper helper = new UserAutoCompleteHelper(activity);
        helper.on(tv);

        return helper;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private UserAutoCompleteHelper(Activity activity) {
        this.handler = new Handler();
        this.visibilityHandler = new Handler();
        this.context = activity;
        this.autoCompleter = new AutoCompleteHelper();

        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        hashtagAutoComplete = new ListPopupWindow(context);
        hashtagAutoComplete.setHeight(Utils.toDP(POPUP_WINDOW_HEIGHT, context));
        hashtagAutoComplete.setWidth((int) (width * .75));
        hashtagAutoComplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);

        userAutoComplete = new ListPopupWindow(context);
        userAutoComplete.setHeight(Utils.toDP(POPUP_WINDOW_HEIGHT, context));
        userAutoComplete.setWidth((int) (width * .95));
        userAutoComplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);
    }

    private ListPopupWindow on(final EditText textView) {
        this.textView = textView;
        userAutoComplete.setAnchorView(textView);
        hashtagAutoComplete.setAnchorView(textView);

        hashtagAutoComplete.setAdapter(new AutoCompleteHashtagAdapter(hashtagAutoComplete, context,
                HashtagDataSource.getInstance(context).getCursor(""), textView));

        userAutoComplete.setAdapter(new AutoCompletePeopleAdapter(userAutoComplete, context,
                FollowersDataSource.getInstance(context).getCursor(AppSettings.getInstance(context).currentAccount,
                        textView.getText().toString()), textView));

        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                visibilityHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final String searchText = textView.getText().toString();
                        final int position = textView.getSelectionStart() - 1;

                        handleText(searchText, position);
                    }
                }, 100);
            }
        });

        if (!AppSettings.getInstance(context).followersOnlyAutoComplete) {
            userAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    userAutoComplete.dismiss();
                    autoCompleter.completeTweet(textView, users.get(i).getScreenName(), '@');

                    if (callback != null) {
                        callback.onUserSelected(users.get(i));
                    }
                }
            });
        }

        hashtagAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                hashtagAutoComplete.dismiss();
            }
        });

        return userAutoComplete;
    }

    private void handleText(String searchText, int position) {
        if (position < 0 || position > searchText.length() - 1) {
            return;
        }

        try {
            if (searchText.charAt(position) == '#' && AppSettings.getInstance(context).autoCompleteHashtags) {
                hashtagAutoComplete.show();
                userAutoComplete.dismiss();
            } else if (searchText.charAt(position) == ' ') {
                hashtagAutoComplete.dismiss();
                userAutoComplete.dismiss();
            } else if (hashtagAutoComplete.isShowing()) {
                String adapterText = "";

                int localPosition = position;

                do {
                    adapterText = searchText.charAt(localPosition--) + adapterText;
                } while (localPosition >= 0 && searchText.charAt(localPosition) != '#');

                adapterText = adapterText.replace("#", "");
                hashtagAutoComplete.setAdapter(new AutoCompleteHashtagAdapter(hashtagAutoComplete, context,
                        HashtagDataSource.getInstance(context).getCursor(adapterText), textView));
            }

            if (searchText.charAt(position) == '@') {
                userAutoComplete.show();
                hashtagAutoComplete.dismiss();
            } else if (searchText.charAt(position) == ' ') {
                userAutoComplete.dismiss();
                hashtagAutoComplete.dismiss();
            } else if (userAutoComplete.isShowing()) {
                String adapterText = "";

                int localPosition = position;

                do {
                    adapterText = searchText.charAt(localPosition--) + adapterText;
                } while (localPosition >= 0 && searchText.charAt(localPosition) != '@');

                adapterText = adapterText.replace("@", "");
                search(adapterText);
            }
        } catch (Exception e) {
            //throw new RuntimeException("text: " + searchText + ", position index: " + position, e);
            e.printStackTrace();
        }
    }

    public ListPopupWindow getHashtagAutoComplete() {
        return hashtagAutoComplete;
    }

    public ListPopupWindow getUserAutoComplete() {
        return userAutoComplete;
    }

    private void search(final String screenName) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AppSettings settings = AppSettings.getInstance(context);
                        if (settings.followersOnlyAutoComplete) {
                            if (adapter != null) {
                                try {
                                    adapter.getCursor().close();
                                } catch (Exception e) {

                                }
                            }

                            final Cursor cursor = FollowersDataSource.getInstance(context).getCursor(settings.currentAccount, screenName);
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter = new AutoCompletePeopleAdapter(userAutoComplete, context, cursor, textView);
                                    userAutoComplete.setAdapter(adapter);
                                }
                            });
                        } else {

                            try {
                                users = UserJSONImplMastodon.createPagableUserList(new GetSearchResults(screenName, GetSearchResults.Type.ACCOUNTS, false, null, 20).execSync().accounts);
                            } catch (Exception e) {
                                Timber.e("Error getting users: " + e.getMessage());
                            }

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    userAutoComplete.setAdapter(new AutoCompleteUserArrayAdapter(context, users));
                                }
                            });
                        }
                    }
                }).start();
            }
        }, 150);

    }
}
