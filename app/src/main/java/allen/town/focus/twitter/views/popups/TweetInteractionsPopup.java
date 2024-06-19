package allen.town.focus.twitter.views.popups;

import android.content.Context;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.adapters.TweetInteractionsPagerAdapter;
import allen.town.focus.twitter.views.widgets.PopupLayout;
import allen.town.focus.twitter.settings.AppSettings;

public class TweetInteractionsPopup extends PopupLayout {

    AppSettings settings;

    TabLayout tabs;
    ViewPager viewPager;

    public TweetInteractionsPopup(Context context) {
        super(context);

        showTitle(false);
        setFullScreen();
    }

    @Override
    public View setMainLayout() {
        settings = AppSettings.getInstance(getContext());

        View root = LayoutInflater.from(getContext()).inflate(R.layout.tweet_interactions_popup, (ViewGroup) getRootView(), false);

        tabs = (TabLayout) root.findViewById(R.id.pager_tab_strip);
        viewPager = (ViewPager) root.findViewById(R.id.pager);



        return root;
    }

    public void setInfo(String screenname, long tweetId) {
        TweetInteractionsPagerAdapter adapter = new TweetInteractionsPagerAdapter(((AppCompatActivity) getContext()), screenname, tweetId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);
        tabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0);
    }
}
