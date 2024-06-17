package allen.town.focus.twitter.utils.redirects;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.settings.AppSettings;

public class RedirectToMyAccount extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);
        ProfilePager.start(this, AppSettings.getInstance(this).myId);

        overridePendingTransition(0, 0);
        finish();
    }
}