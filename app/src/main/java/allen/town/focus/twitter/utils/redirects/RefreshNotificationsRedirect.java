package allen.town.focus.twitter.utils.redirects;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import allen.town.focus.twitter.services.background_refresh.MentionsRefreshService;

public class RefreshNotificationsRedirect extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0,0);

        MentionsRefreshService.startNow(this);

        finish();
        overridePendingTransition(0,0);
    }
}
