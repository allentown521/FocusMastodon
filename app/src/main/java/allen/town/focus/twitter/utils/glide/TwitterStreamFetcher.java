package allen.town.focus.twitter.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Map;

import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.utils.ImageUtils;
import allen.town.focus_common.util.Timber;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches an {@link InputStream} using the okhttp library.
 */
public class TwitterStreamFetcher implements DataFetcher<InputStream> {
    private Context context = App.getInstance().getApplicationContext();
    private static final String TAG = "TwitterStreamFetcher";
    private final Call.Factory client;
    private final GlideUrl url;
    private InputStream stream;
    private ResponseBody responseBody;
    private DataCallback<? super InputStream> callback;
    // call may be accessed on the main thread while the object is in use on other threads. All other
    // accesses to variables may occur on different threads, but only one at a time.
    private volatile Call call;

    // Public API.
    @SuppressWarnings("WeakerAccess")
    public TwitterStreamFetcher(Call.Factory client, GlideUrl url) {
        this.client = client;
        this.url = url;
    }

    @Override
    public void loadData(
            @NonNull Priority priority, @NonNull final DataCallback<? super InputStream> callback) {
        String urlString;

        if (!url.toStringUrl().contains("bytebucket.org/jklinker")) {
            urlString = URLDecoder.decode(url.toStringUrl());
        } else {
            urlString = url.toStringUrl();
        }

//        Timber.d("load data " + urlString);
        if (urlString.contains(" ")) {
            String[] pics = urlString.split(" ");
            Bitmap[] bitmaps = new Bitmap[pics.length];

            // need to download all of them, then combine them
            for (int i = 0; i < pics.length; i++) {
                String url = pics[i];
                try {
                    //升级到glide4以后会引入莫名其妙有时候不显示图片的问题,发现和这个分支有关系
                    bitmaps[i] = getBitmapFromUrl(url);
//                    bitmaps[i] = Glide.with(context).asBitmap().load(url).into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                } catch (Exception e) {
                    Timber.e(e,"loadData bitmap");
                }
            }

            // now that we have all of them, we need to put them together
            Bitmap combined = ImageUtils.combineBitmaps(context, bitmaps);
            if (combined != null) {
                callback.onDataReady(convertToInputStream(combined));
            } else {
                callback.onLoadFailed(new NullPointerException("combined Bitmap is null"));
            }

        } else {
            Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl().replace("http://", "https://"));

            for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                requestBuilder.addHeader(key, headerEntry.getValue());
            }
            Request request = requestBuilder.build();

            Response response;
            call = client.newCall(request);
            try {
                response = call.execute();
                responseBody = response.body();
                if (!response.isSuccessful()) {
                    callback.onLoadFailed(new IOException("Request failed with code: " + response.code()));
                }

                long contentLength = responseBody.contentLength();
                stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
                callback.onDataReady(stream);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }


        }
    }

    private Bitmap getBitmapFromUrl(String urlpath) {
        Bitmap map = null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        //暂时先解决不显示的问题后续再优化
        opts.inSampleSize = 2;
        try {
            URL url = new URL(urlpath);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream in;
            in = conn.getInputStream();
            map = BitmapFactory.decodeStream(in,null,opts);
        } catch (Exception e) {
            Timber.e(e,"getBitmapFromUrl");
        }
        return map;
    }

    @Override
    public void cleanup() {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            // Ignored
        }
        if (responseBody != null) {
            responseBody.close();
        }
        callback = null;
    }

    private InputStream convertToInputStream(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        return new ByteArrayInputStream(bos.toByteArray());
    }

    @Override
    public void cancel() {
        Call local = call;
        if (local != null) {
            local.cancel();
        }
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
