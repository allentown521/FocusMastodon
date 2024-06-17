package allen.town.focus.twitter.activities.main_fragments.home_fragments.extentions;

import android.database.Cursor;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.activities.main_fragments.home_fragments.HomeExtensionFragment;


public class FavUsersTweetsFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getFavUsersCursor(currentAccount);
    }
}