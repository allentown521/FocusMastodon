package allen.town.focus.twitter.views.popups;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.views.widgets.PopupLayout;


public class ConversationPopupLayout extends PopupLayout {

    ListView list;
    LinearLayout spinner;

    public ConversationPopupLayout(Context context, View main) {
        super(context);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        if (AppSettings.getInstance(context).revampedTweets()) {
            list.setDivider(null);
        }

        //setTitle(getContext().getString(R.string.conversation));
        showTitle(false);
        setFullScreen();

        content.addView(main);
    }

    @Override
    public View setMainLayout() {
        return null;
    }
}
