package allen.town.focus.twitter.activities.setup.material_login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.viewpager.widget.ViewPager;

import allen.town.focus.twitter.activities.main_fragments.onboarding.InstanceChooserLoginFragment;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.AnalyticsHelper;
import allen.town.focus.twitter.utils.Utils;
import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;


public class MaterialLogin extends FragmentStackActivity {

    // CHANGE THIS TO UPDATE THE KEY VERSION
    public static final int KEY_VERSION = 4;

    public interface Callback {
        void onDone();
    }



    private ViewPager pager;
    private ImageView nextButton;
    private ImageView skipButton;

    private String authUrl;

    @Override
    public void onCreate(Bundle bundle) {
        Utils.setUpTheme(this, null);
        super.onCreate(bundle);
        AnalyticsHelper.startLogin(this);

        SharedPreferences sharedPrefs = AppSettings.getInstance(this).sharedPrefs;

        final int currAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
        sharedPrefs.edit().putInt("key_version_" + currAccount, KEY_VERSION).commit();


        if (bundle == null) {
            Nav.go(this, InstanceChooserLoginFragment.class, null);
//            InstanceChooserLoginFragment instanceChooserLoginFragment = new InstanceChooserLoginFragment();
//            getFragmentManager().beginTransaction().add(instanceChooserLoginFragment, "instanceChooserLoginFragment").commit();
        }


    }

    private ImageFragment welcomeFragment;
    private InstanceChooserLoginFragment loginFragment;
    private FinishedFragment finishedFragment;


    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);

        sharedPrefs.edit().putBoolean("version_3_2", false).commit();

        super.finish();
    }


}
