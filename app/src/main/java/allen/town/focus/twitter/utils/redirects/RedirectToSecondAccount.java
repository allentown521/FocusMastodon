package allen.town.focus.twitter.utils.redirects;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.settings.AppSettings;

public class RedirectToSecondAccount extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);
        //这种写法不对，自己当前域看到的accountId和其他域看到的不一样
        ProfilePager.start(this, AppSettings.getInstance(this).secondId);

        overridePendingTransition(0, 0);
        finish();
    }
}