package allen.town.focus.twitter.views.popups;

import android.content.Context;
import android.view.View;

import allen.town.focus.twitter.views.widgets.PopupLayout;

/**
 * Created by lucasklinker on 7/26/14.
 */
public class MobilizedWebPopupLayout extends PopupLayout {

    private View webView;

    public MobilizedWebPopupLayout(Context context, View webView) {
        super(context);

        this.webView = webView;

        showTitle(false);
        setFullScreen();

        try {
            content.addView(webView);
        } catch (Exception e) {
            dontShow = true;
        }
    }

    @Override
    public View setMainLayout() {
        return null;
    }
}
