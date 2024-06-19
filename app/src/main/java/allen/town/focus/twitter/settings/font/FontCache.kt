package allen.town.focus.twitter.settings.font

import allen.town.focus_common.util.Timber
import android.content.Context
import android.graphics.Typeface
import rx.Observable
import rx.schedulers.Schedulers

object FontCache {
    @JvmStatic
    var fontCache: HashMap<String, Typeface> = HashMap()

    @JvmStatic
    fun cache(context: Context) {
        Observable.just(0)
            .observeOn(Schedulers.io()).subscribe({
                if (fontCache.size > 0) {
                    Timber.d("font cache has get")
                    return@subscribe
                }
                val fontList = Font.fontList
                for (font in fontList) {
                    try {
                        if (font.path != null) {
                            if (font.isExternal) {
                                fontCache.put(
                                    font.cssName,
                                    Typeface.createFromFile(font.path)
                                )
                            } else {
                                fontCache.put(
                                    font.cssName,
                                    Typeface.createFromAsset(context.getAssets(), font.path)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("ignore cache exception $e")
                    }
                }
                Timber.d("cache finish get ${fontCache.size}")
            }, {
                Timber.e(it, "cache error")
            })
    }
}