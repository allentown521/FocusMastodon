package allen.town.focus.twitter.widget.timeline

import allen.town.focus.twitter.R
import allen.town.focus.twitter.data.Tweet
import allen.town.focus.twitter.data.sq_lite.HomeDataSource
import allen.town.focus.twitter.data.sq_lite.HomeSQLiteHelper
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource
import allen.town.focus.twitter.model.ReTweeterAccount
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.HtmlParser
import allen.town.focus.twitter.utils.Utils
import allen.town.focus.twitter.utils.text.TextUtils
import allen.town.focus_common.util.JsonHelper.parseObject
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import code.name.monkey.appthemehelper.ThemeStore
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*

class TimelineRemoteViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var tweets = mutableListOf<Tweet>()
    private val settings: AppSettings by lazy { AppSettings.getInstance(context) }

    private var dateFormatter: java.text.DateFormat
    private var timeFormatter: java.text.DateFormat

    init {
        dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context)
        if (settings.militaryTime) {
            dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
            timeFormatter = SimpleDateFormat("kk:mm")
        }

        val locale = context.resources.configuration.locale
        if (locale != null && locale.language != "en") {
            dateFormatter = android.text.format.DateFormat.getDateFormat(context)
        }
    }

    override fun onCreate() {
        reloadTimeline()
    }

    private fun reloadTimeline() {
        tweets.clear()

        val settings = AppSettings.getInstance(context)
        val query = if (!settings.useMentionsOnWidget) {
            val data = HomeDataSource.getInstance(context)
            data.getWidgetCursor(settings.widgetAccountNum)
        } else {
            val data = MentionsDataSource.getInstance(context)
            data.getWidgetCursor(settings.widgetAccountNum)
        }

        try {
            if (query.moveToFirst()) {
                do {
                    tweets.add(
                        Tweet(
                            query.getLong(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME)),
                            query.getLong(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_URL)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_ANIMATED_GIF)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_STATUS_URL)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_EMOJI))
                        )
                    )
                } while (query.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        try {
            query.close()
        } catch (e: Exception) {
        }
    }

    override fun onDataSetChanged() {
        reloadTimeline()
    }

    override fun onDestroy() {
        tweets.clear()
    }

    override fun getCount(): Int {
        return tweets.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        var res = 0
        when (Integer.parseInt(
            AppSettings.getSharedPreferences(context).getString("widget_theme", "4")!!
        )) {
            0 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_light_large_image
            else
                R.layout.widget_conversation_light
            1 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_dark_large_image
            else
                R.layout.widget_conversation_dark
            2 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_light_large_image
            else
                R.layout.widget_conversation_light
            3 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_dark_large_image
            else
                R.layout.widget_conversation_dark
            4 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_light_large_image
            else
                R.layout.widget_conversation_light
            5 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_dark_large_image
            else
                R.layout.widget_conversation_dark
            6 -> res = if (settings.largerWidgetImages)
                R.layout.widget_conversation_dark_large_image
            else
                R.layout.widget_conversation_dark
        }

        val card = RemoteViews(context.packageName, res)

        try {
            card.setTextViewText(
                R.id.contactName,
                if (settings.displayScreenName) "@" + tweets[position].screenName else tweets[position].name
            )
            card.setTextViewText(
                R.id.contactText,
                TextUtils.colorText(
                    context,
                    //不用Html.fromHtml是因为它会将<p>解析为\n，导致内容最后有两个换行
                    HtmlParser.parse(tweets[position].tweet, null, null, null, null, false, false)
                        .toString(),
                    ThemeStore.accentColor(context)
                )
            )

            if (!settings.absoluteDate) {
                card.setTextViewText(
                    R.id.time,
                    Utils.getTimeAgo(tweets[position].time, context, false)
                )
            } else {
                val date = Date(tweets[position].time)
                val text =
                    timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(
                        date
                    )
                card.setTextViewText(R.id.time, text)
            }


            if (context.resources.getBoolean(R.bool.expNotifications)) {
                try {
                    card.setTextViewTextSize(
                        R.id.contactName,
                        TypedValue.COMPLEX_UNIT_DIP,
                        (settings.widgetTextSize + 2).toFloat()
                    )
                    card.setTextViewTextSize(
                        R.id.contactText,
                        TypedValue.COMPLEX_UNIT_DIP,
                        settings.widgetTextSize.toFloat()
                    )
                    card.setTextViewTextSize(
                        R.id.time,
                        TypedValue.COMPLEX_UNIT_DIP,
                        (settings.widgetTextSize - 2).toFloat()
                    )
                    card.setTextViewTextSize(
                        R.id.retweeter,
                        TypedValue.COMPLEX_UNIT_DIP,
                        (settings.widgetTextSize - 2).toFloat()
                    )
                } catch (t: Throwable) {

                }

            }

            card.setImageViewBitmap(
                R.id.contactPicture,
                getCachedPic(tweets[position].picUrl, context)
            )

            val picUrl = tweets[position].website
            val otherUrl = tweets[position].otherWeb
            val link: String

            val displayPic = picUrl != "" && !picUrl.contains("youtube")
            if (displayPic) {
                link = picUrl

                if (settings.widgetImages) {
                    card.setViewVisibility(R.id.picture, View.VISIBLE)
                    card.setImageViewBitmap(R.id.picture, getCachedPic(link, context))
                } else {
                    card.setViewVisibility(R.id.picture, View.GONE)
                }
            } else {
                link = if (otherUrl.isEmpty()) {
                    ""
                } else {
                    otherUrl.split("  ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
                }

                card.setViewVisibility(R.id.picture, View.GONE)
            }

            val retweeterStr = tweets[position].retweeter
            var retweeter: ReTweeterAccount? = null
            try {
                retweeter = parseObject(
                    retweeterStr,
                    ReTweeterAccount::class.java
                )
            } catch (e: java.lang.Exception) {
            }

            if (retweeter != null) {

                val text =
                    context.resources.getString(R.string.retweeter) + "@" + retweeter.displayName
                card.setTextViewText(
                    R.id.retweeter,
                    TextUtils.colorText(
                        context,
                        Html.fromHtml(text).toString(),
                        ThemeStore.accentColor(context)
                    )
                )
                card.setViewVisibility(R.id.retweeter, View.VISIBLE)
            } else {
                card.setViewVisibility(R.id.retweeter, View.GONE)
            }

            val extras = Bundle()
            extras.putString("name", tweets[position].name)
            extras.putString("screenname", tweets[position].screenName)
            extras.putLong("time", tweets[position].time)
            extras.putString("tweet", tweets[position].tweet)
            extras.putString("retweeter", retweeter?.displayName)
            extras.putString("webpage", link)
            extras.putBoolean("picture", displayPic)
            extras.putString("other_links", tweets[position].otherWeb)
            extras.putLong("tweetid", tweets[position].id)
            extras.putString("propic", tweets[position].picUrl)
            extras.putString("users", tweets[position].users)
            extras.putString("hashtags", tweets[position].hashtags)
            extras.putString("animated_gif", tweets[position].animatedGif)
            extras.putString(HomeSQLiteHelper.COLUMN_STATUS_URL, tweets[position].statusUrl)
            extras.putString(HomeSQLiteHelper.COLUMN_EMOJI, tweets[position].emoji)

            // also have to add the strings in the widget provider

            val cardFillInIntent = Intent()
            cardFillInIntent.putExtras(extras)
            card.setOnClickFillInIntent(R.id.widget_card_background, cardFillInIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return card
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return if (tweets.size > 0 && position < tweets.size) {
            tweets[position].id
        } else {
            0
        }
    }

    override fun hasStableIds() = false
    fun getTweets(): List<Tweet> = tweets

    companion object {
        fun getCachedPic(url: String, context: Context): Bitmap? {
            return try {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(200, 200)
                    .get()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}
