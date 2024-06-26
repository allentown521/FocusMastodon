package allen.town.focus.twitter.services;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;

import allen.town.focus.twitter.activities.compose.LauncherCompose;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ComposeTileService extends TileService {

    @Override
    public void onClick() {
        Intent compose = new Intent(this, LauncherCompose.class);
        compose.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(compose);
    }

    @Override
    public void onStartListening() {
        getQsTile().setState(Tile.STATE_ACTIVE);
    }
}
