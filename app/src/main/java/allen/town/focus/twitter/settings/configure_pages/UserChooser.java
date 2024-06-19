package allen.town.focus.twitter.settings.configure_pages;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.adapters.AutoCompleteUserArrayAdapter;
import allen.town.focus.twitter.api.requests.search.GetSearchResults;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus_common.util.Timber;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class UserChooser extends WhiteToolbarActivity {

    private AppSettings settings;

    private List<User> users = new ArrayList<>();
    private EditText user;
    private ListPopupWindow userAutoComplete;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        settings = AppSettings.getInstance(this);

        Utils.setUpMainTheme(this, settings);
        setContentView(R.layout.user_chooser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.user_tweets));

        setSupportActionBar(toolbar);

        user = (EditText) findViewById(R.id.user);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        userAutoComplete = new ListPopupWindow(this);
        userAutoComplete.setHeight(Utils.toDP(200, this));
        userAutoComplete.setWidth((int) (width * .75));
        userAutoComplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);

        user.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String tvText = user.getText().toString();

                if (!userAutoComplete.isShowing()) {
                    userAutoComplete.show();
                }

                try {
                    search(tvText.replace("@", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                    userAutoComplete.dismiss();
                }
            }
        });

        userAutoComplete.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("userId", users.get(i).getId());
            returnIntent.putExtra("name", users.get(i).getName());
            setResult(RESULT_OK, returnIntent);
            finish();
        });

        user.post(new Runnable() {
            @Override
            public void run() {
                userAutoComplete.setAnchorView(user);
                userAutoComplete.show();
            }
        });
    }

    private void search(final String screenName) {
        new Thread(() -> {

            try {
                users = UserJSONImplMastodon.createPagableUserList(new GetSearchResults(screenName, GetSearchResults.Type.ACCOUNTS, false, null, 20).execSync().accounts);
            } catch (Exception e) {
                Timber.e(e,"Error getting user list");
            }

            UserChooser.this.runOnUiThread(() -> userAutoComplete.setAdapter(new AutoCompleteUserArrayAdapter(UserChooser.this, users)));
        }).start();
    }
}
