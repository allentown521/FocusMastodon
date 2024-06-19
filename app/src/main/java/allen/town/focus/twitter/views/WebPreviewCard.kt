package allen.town.focus.twitter.views

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.BrowserActivity
import allen.town.focus.twitter.activities.media_viewer.VideoViewerActivity
import allen.town.focus.twitter.data.WebPreview
import allen.town.focus.twitter.model.Card
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.WebIntentBuilder
import allen.town.focus.twitter.utils.text.TouchableSpan
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class WebPreviewCard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    interface OnLoad {
        fun onLinkLoaded(link: String, preview: WebPreview)
    }

    private var loadedPreview: WebPreview? = null

    private val progress: ProgressBar by lazy { findViewById<View>(R.id.progress_bar) as ProgressBar }
    private val blankImage: ImageView by lazy { findViewById<View>(R.id.blank_image) as ImageView }
    private val image: ImageView by lazy { findViewById<View>(R.id.web_image) as ImageView }
    private val title: TextView by lazy { findViewById<View>(R.id.web_title) as TextView }
    private val summary: TextView by lazy { findViewById<View>(R.id.web_summary) as TextView }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_web_preview, this, true)

        title.textSize = AppSettings.getInstance(context).textSize + 1.toFloat()
        summary.textSize = AppSettings.getInstance(context).textSize.toFloat()

        setOnClickListener { }
        setOnLongClickListener { true }
    }

    fun displayPreview(card: Card?) {
        if (card == null) {
            return
        }

        if (TextUtils.isEmpty(card.image)) {
            blankImage.visibility = View.VISIBLE
            image.visibility = View.GONE
        } else {
            blankImage.visibility = View.GONE
            image.visibility = View.VISIBLE

            try {
                Glide.with(context).load(card.image).into(image)
            } catch (e: IllegalArgumentException) {
                // destroyed activity
            }
        }

        progress.visibility = View.GONE

        if (!TextUtils.isEmpty(card.title)) {
            if (title.visibility != View.VISIBLE) title.visibility = View.VISIBLE
            title.text = card.title
        } else {
            title.visibility = View.GONE
        }

        if (!TextUtils.isEmpty(card.description)) {
            summary.text = card.description
        } else {
            if (!TextUtils.isEmpty(card.authorName)) summary.text = card.authorName
            else summary.visibility = View.GONE
        }

        val link = card.url
        setOnClickListener {
            if (link.contains("/i/web/status/") || link.contains("twitter.com") && link.contains("/moments/")) {
                val browser = Intent(context, BrowserActivity::class.java)
                browser.putExtra("url", link)
                context.startActivity(browser)
            } else if (link.contains("vine.co/v/")) {
                VideoViewerActivity.startActivity(context, 0L, link, "")
            } else {
                WebIntentBuilder(context)
                    .setUrl(link)
                    .build().start()
            }
        }

        setOnLongClickListener {
            TouchableSpan.longClickWeb(context, link)
            true
        }
    }

    fun clear() {
        Glide.with(context).clear(image)
        blankImage.visibility = View.GONE
        progress.visibility = View.GONE

        title.text = ""
        summary.text = ""

        loadedPreview = null

        setOnClickListener { }
        setOnLongClickListener { true }

        tag = ""
    }

    companion object {
        private val ignoredLinks = listOf(
            "pic.twitter.com",
            "twitter.com/i/moments",
            "tl.gd",
            "vine.co",
            "twitch.tv",
            "youtube",
            "youtu.be",
            "bit.ly"
        )

        fun ignoreLink(link: String): Boolean {
            return ignoredLinks.any { link.contains(it) }
        }
    }
}