package allen.town.focus.twitter.utils.video;

import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class MediaHelper {

    public static int GetDuration( Uri uri ) {
        return GetMediaMetadataRetrieverPropertyInteger( uri, MediaMetadataRetriever.METADATA_KEY_DURATION, 0 );
    }

    public static int GetMediaMetadataRetrieverPropertyInteger( Uri uri, int key, int defaultValue ) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource( uri.toString() );
        String value = retriever.extractMetadata( key );

        if ( value == null ) {
            return defaultValue;
        }
        return Integer.parseInt( value );

    }

}