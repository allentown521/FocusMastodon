package allen.town.focus.twitter.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

import allen.town.focus.twitter.utils.ImageUtils;

public class CircleBitmapTransform extends BitmapTransformation {
    public CircleBitmapTransform(Context context) {
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return ImageUtils.getCircleBitmap(toTransform);
    }

    public String getId() {
        return "allen.town.focus.twitter.CIRCLE_TRANSFORM";
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(getId().getBytes());
    }
}
