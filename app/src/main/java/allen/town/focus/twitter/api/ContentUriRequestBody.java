package allen.town.focus.twitter.api;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.utils.UiUtils;
import okhttp3.MediaType;
import okio.Okio;
import okio.Source;

public class ContentUriRequestBody extends CountingRequestBody {
    private final Uri uri;
    private InputStream inputStream;

    public ContentUriRequestBody(InputStream inputStream, Uri uri, ProgressListener progressListener) throws IOException {
        super(progressListener);
        this.inputStream = inputStream;
        this.uri = uri;
        if ("file".equals(uri.getScheme())) {
            length = inputStream.available();
        } else {
            try (Cursor cursor = App.getInstance().getApplicationContext().getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
                //编辑过的图片是file (cursor为空)，这里只支持content，进度条不可用
                if (cursor != null) {
                    cursor.moveToFirst();
                    length = cursor.getInt(0);
                }

            }
        }


    }

    @Override
    public MediaType contentType() {
        if ("file".equals(uri.getScheme())) {
            return UiUtils.getFileMediaType(new File(uri.getPath()));
        } else {
            return MediaType.get(App.getInstance().getApplicationContext().getContentResolver().getType(uri));
        }
    }

    @Override
    protected Source openSource() throws IOException {
        return Okio.source(/*App.getInstance().getApplicationContext().getContentResolver().openInputStream(uri)*/inputStream);
    }
}
