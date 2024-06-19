package allen.town.focus.twitter.views.popups;

import android.content.Context;

import allen.town.focus.twitter.R;

public class FavoritersPopupLayout extends RetweetersPopupLayout {
    public FavoritersPopupLayout(Context context) {
        super(context);
    }

    @Override
    public void setUserWindowTitle() {
        setTitle(getContext().getString(R.string.favorites));
        noContentText.setText(getResources().getString(R.string.no_favorites));
    }

}
