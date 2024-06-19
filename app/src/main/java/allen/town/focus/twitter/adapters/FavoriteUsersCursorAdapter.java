package allen.town.focus.twitter.adapters;
/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersDataSource;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersSQLiteHelper;
import allen.town.focus.twitter.activities.main_fragments.other_fragments.FavoriteUsersFragment;

import allen.town.focus_common.util.Timber;
import allen.town.focus_common.views.AccentMaterialDialog;

public class FavoriteUsersCursorAdapter extends PeopleCursorAdapter {
    private FavoriteUsersFragment mFavoriteUsersFragment;
    public FavoriteUsersCursorAdapter(Context context, Cursor cursor,FavoriteUsersFragment favoriteUsersFragment) {
        super(context, cursor);
        mFavoriteUsersFragment = favoriteUsersFragment;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        super.bindView(view, context, cursor);
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = cursor.getLong(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_ID));
        holder.background.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Timber.v( "long clicked");
                new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        )
                        .setTitle(context.getResources().getString(R.string.removing_favorite) + "?")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    FavoriteUsersDataSource dataSource = new FavoriteUsersDataSource(context);
                                    dataSource.open();
                                    dataSource.deleteUser(id);
                                    dataSource.close();
                                    mFavoriteUsersFragment.refreshFavs();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();

                return false;
            }
        });
    }
}
