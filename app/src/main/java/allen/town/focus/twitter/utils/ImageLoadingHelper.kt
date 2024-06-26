@file:JvmName("ImageLoadingHelper")

package allen.town.focus.twitter.utils

import allen.town.focus.twitter.R
import android.widget.ImageView
import androidx.annotation.Px
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

private val centerCropTransformation = CenterCrop()

fun loadAvatar(url: String?, imageView: ImageView, @Px radius: Int, animate: Boolean) {
    if (url.isNullOrBlank()) {
        Glide.with(imageView)
            .load(R.drawable.avatar_default)
            .into(imageView)
    } else {
        if (animate) {
            Glide.with(imageView)
                .load(url)
                .transform(
                    centerCropTransformation,
                    RoundedCorners(radius)
                )
                .placeholder(R.drawable.avatar_default)
                .into(imageView)
        } else {
            Glide.with(imageView)
                .asBitmap()
                .load(url)
                .transform(
                    centerCropTransformation,
                    RoundedCorners(radius)
                )
                .placeholder(R.drawable.avatar_default)
                .into(imageView)
        }
    }
}

