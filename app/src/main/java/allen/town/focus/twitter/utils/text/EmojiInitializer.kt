package allen.town.focus.twitter.utils.text

import android.content.Context
import android.os.Build
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.core.provider.FontRequest
import android.util.Log
import allen.town.focus.twitter.R
import allen.town.focus.twitter.data.EmojiStyle
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.Utils

object EmojiInitializer {

    fun initializeEmojiCompat(context: Context) {
        val fontRequest = when (AppSettings.getInstance(context).emojiStyle) {
            EmojiStyle.ANDROID_O -> createAndroidODownloadRequest()
            else -> null
        }

        if (fontRequest != null) initializeWithRequest(context, fontRequest)
    }

    private fun createAndroidODownloadRequest(): FontRequest {
        return FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)

    }

    private fun initializeWithRequest(context: Context, fontRequest: FontRequest) {
        EmojiCompat.init(FontRequestEmojiCompatConfig(context, fontRequest)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        Log.i("EmojiCompat", "EmojiCompat initialized")
                    }

                    override fun onFailed(throwable: Throwable?) {
                        Log.e("EmojiCompat", "EmojiCompat initialization failed", throwable)
                    }
                }))
    }

    fun isAlreadyUsingGoogleAndroidO(): Boolean {
        return Utils.isAndroidO && Build.MANUFACTURER.toLowerCase() == "google"
    }
}