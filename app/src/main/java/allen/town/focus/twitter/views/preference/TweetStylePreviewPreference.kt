package allen.town.focus.twitter.views.preference

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import code.name.monkey.appthemehelper.ThemeStore
import com.bumptech.glide.Glide
import allen.town.focus.twitter.R
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.Utils
import allen.town.focus.twitter.utils.text.TextUtils
import java.text.SimpleDateFormat
import java.util.*

class TweetStylePreviewPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    companion object {
        private val NAME = "allentown"
        private val SCREEN_NAME = "@allentown521"
        private val RETWEETER = "FocusApps"
        private val TWEET = "@FocusForMastodon The app is great!"

    }

    private var root: View? = null
    private lateinit var tweetHolder: LinearLayout

    init {
        layoutResource = R.layout.preference_tweet_preview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        root = holder.itemView
            showTweet()
    }

    fun refreshUI(){
        showTweet()
    }

    fun showTweet() {
        tweetHolder = root?.findViewById(R.id.root_view)!!
        val settings = AppSettings.getInstance(context)

        tweetHolder.removeAllViews()
        val tweet = when {
            settings.condensedTweets() -> LayoutInflater.from(context).inflate(R.layout.tweet_condensed, tweetHolder, true)
            settings.revampedTweets() -> LayoutInflater.from(context).inflate(R.layout.tweet_revamp, tweetHolder, true)
            else -> LayoutInflater.from(context).inflate(R.layout.tweet, tweetHolder, true)
        }


        setTweetContent(tweet)
    }
    
    @SuppressLint("SetTextI18n")
    private fun setTweetContent(tweet: View) {
        val settings = AppSettings.getInstance(context)

        val profilePic = tweet.findViewById<ImageView>(R.id.profile_pic)
        val name = tweet.findViewById<TextView>(R.id.name)
        val screenName = tweet.findViewById<TextView>(R.id.screenname)
        val retweeter = tweet.findViewById<TextView>(R.id.retweeter)
        val time = tweet.findViewById<TextView>(R.id.time)
        val tweetText = tweet.findViewById<TextView>(R.id.tweet)
        val background = tweet.findViewById<View>(R.id.background)
        val image = tweet.findViewById<ImageView>(R.id.image)
        val imageHolder = tweet.findViewById<View>(R.id.picture_holder)
        val revampedRetweetIcon = tweet.findViewById<View>(R.id.retweet_icon)

            image.clipToOutline = true

        tweetText.textSize = settings.textSize.toFloat()
        screenName.textSize = (settings.textSize - if (settings.condensedTweets() || settings.revampedTweets()) 1 else 2).toFloat()
        name.textSize = (settings.textSize + if (settings.condensedTweets()) 1 else 4).toFloat()
        time.textSize = (settings.textSize - if (settings.revampedTweets()) 2 else 3).toFloat()
        retweeter.textSize = (settings.textSize - 2).toFloat()

        name.text = NAME
        screenName.text = SCREEN_NAME
        tweetText.text = TextUtils.colorText(context, TWEET, ThemeStore.accentColor(context))
        retweeter.text = TextUtils.colorText(context, context.resources.getString(R.string.retweeter) + RETWEETER, ThemeStore.accentColor(context))
        time.text = Utils.getTimeAgo(System.currentTimeMillis() - 1000 * 60 * 5, context, settings.revampedTweets())

        profilePic.setImageResource(R.mipmap.ic_splash)
        image.setImageResource(R.drawable.twitter_preview)

        when {
            settings.picturesType == AppSettings.PICTURES_NORMAL || settings.picturesType == AppSettings.CONDENSED_TWEETS || settings.revampedTweets() -> {
                imageHolder.visibility = View.VISIBLE
            }
            settings.picturesType == AppSettings.PICTURES_SMALL -> {
                imageHolder.layoutParams.height = Utils.toDP(120, context)
                imageHolder.requestLayout()
                imageHolder.visibility = View.VISIBLE
            }
            else -> imageHolder.visibility = View.GONE
        }

        retweeter.visibility = View.VISIBLE

        if (!settings.revampedTweets()) {
            val a = context.theme.obtainStyledAttributes(intArrayOf(R.attr.windowBackground))
            val resource = a.getResourceId(0, 0)
            a.recycle()
            background.setBackgroundResource(resource)
        } else {
            retweeter.text = TextUtils.colorText(context, "@" + RETWEETER, ThemeStore.accentColor(context))
            revampedRetweetIcon.visibility = View.VISIBLE
            tweetHolder?.getChildAt(0)?.setPadding(0,0,0,Utils.toDP(12, context))
        }

        if (settings.absoluteDate) {
            var dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
            var timeFormatter = android.text.format.DateFormat.getTimeFormat(context)

            if (AppSettings.getInstance(context).militaryTime) {
                dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                timeFormatter = SimpleDateFormat("kk:mm")
            }

            val date = Date(System.currentTimeMillis() - 1000 * 60 * 5)
            time.text = timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date)
        }
    }
}