package allen.town.focus.twitter.views.popups;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import allen.town.focus.twitter.views.widgets.PopupLayout;

public class WebPopupLayout extends PopupLayout {

    private View webView;

    public WebPopupLayout(Context context, View webView) {
        super(context);

        this.webView = webView;

        showTitle(false);
        setFullScreen();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        webView.setLayoutParams(params);

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
