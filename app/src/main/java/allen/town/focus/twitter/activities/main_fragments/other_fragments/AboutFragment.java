package allen.town.focus.twitter.activities.main_fragments.other_fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.List;

import allen.town.focus.twitter.BuildConfig;
import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;
import allen.town.focus.twitter.api.requests.accounts.GetAccountByAcct;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Account;
import allen.town.focus_common.ui.customtabs.BrowserLauncher;
import allen.town.focus_common.util.Intents;
import allen.town.focus_common.util.PackageUtils;
import allen.town.focus_common.views.AccentMaterialDialog;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import code.name.monkey.appthemehelper.ThemeStore;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AboutFragment extends AppCompatDialogFragment {
    @BindView(R.id.credits)
    TextView credits;
    @BindView(R.id.icon)
    ImageView icon;
    @BindView(R.id.privacy_policy)
    RelativeLayout policy;
    @BindView(R.id.version_text)
    TextView version;
    @BindView(R.id.opensource)
    View opensourceView;
    @BindViews({R.id.twitter_image, R.id.rate_image, R.id.privacy_policy_image, R.id.more_apps_of_us_image
            , R.id.share_app_image,R.id.opensource_image})
    List<ImageView> styleButtons;

    @OnClick(R.id.share_app)
    public void shareMyApp() {
        Intents.shareText(getContext(), getString(R.string.share_to_friends_tip, PackageUtils.getAppName(getContext())) + " \n" +
                        "https://play.google.com/store/apps/details?id=allen.town.focus.mastodon",
                "");
    }

    @OnClick(R.id.privacy_policy)
    public void showPrivacyPolicy() {
        BrowserLauncher.openUrl(getActivity(), "https://sites.google.com/view/privacypolicyoffocusformastodo/");
    }

    @OnClick(R.id.opensource)
    public void showOpensource() {
        BrowserLauncher.openUrl(getActivity(), "https://github.com/allentown521/FocusMastodon/");
    }

    @OnClick(R.id.twitter_follow_me)
    public void followMe() {
        //在不用的服务器上获取到的accountId不一样
        new GetAccountByAcct("allentown@mastodon.social").setCallback(new Callback<>() {
            @Override
            public void onSuccess(Account result) {
                ProfilePager.start(getContext(), result.id);
            }

            @Override
            public void onError(ErrorResponse error) {

            }
        }).exec();

    }

    @OnClick(R.id.rate_me)
    public void rateMe() {
        Uri parse = Uri.parse("market://details?id=allen.town.focus.mastodon");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);
        Intents.startActivity(getContext(), intent);

    }

    @OnClick(R.id.more_apps_of_us)
    public void moreAppsOfUs() {
        Uri parse = Uri.parse("https://play.google.com/store/apps/dev?id=8458616364286916829");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(parse);
        Intents.startActivity(getContext(), intent);

    }


    @Override
    // android.support.v7.app.AppCompatDialogFragment, android.support.v4.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        View inflate = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_about, null);
        ButterKnife.bind(this, inflate);

        this.version.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
        this.opensourceView.setVisibility(!App.getInstance().isDroid() ? View.GONE : View.VISIBLE);
        butterknife.ViewCollections.run(this.styleButtons, (view, i) -> view
                .setColorFilter(ThemeStore.accentColor(getContext()), PorterDuff.Mode.SRC_IN));
        return new AccentMaterialDialog(getContext(), R.style.MaterialAlertDialogTheme).setView(inflate).create();
    }
}

