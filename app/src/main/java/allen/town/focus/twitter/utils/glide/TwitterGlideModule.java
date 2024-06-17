package allen.town.focus.twitter.utils.glide;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;

import allen.town.focus.twitter.settings.AppSettings;
import okhttp3.Call;
import okhttp3.OkHttpClient;

@GlideModule
public class TwitterGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        AppSettings settings = AppSettings.getInstance(context);
        if (settings.higherQualityImages) {
            builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888));
//            builder.setLogLevel(Log.VERBOSE);
        }
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.replace(GlideUrl.class, InputStream.class, new TwitterUrlLoader.Factory((Call.Factory) new OkHttpClient.Builder().build()));
    }
}