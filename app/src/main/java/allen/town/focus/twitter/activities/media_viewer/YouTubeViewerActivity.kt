package allen.town.focus.twitter.activities.media_viewer

import allen.town.focus.twitter.R
import allen.town.focus.twitter.activities.WhiteToolbarActivity
import allen.town.focus.twitter.settings.AppSettings
import allen.town.focus.twitter.utils.Utils.setUpTheme
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class YouTubeViewerActivity : WhiteToolbarActivity() {
    override fun finish() {
        val sharedPrefs = AppSettings.getSharedPreferences(context)
        sharedPrefs.edit().putBoolean("from_activity", true).commit()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.finish()
    }

    private lateinit var youTubePlayer: YouTubePlayer

    var context: Context? = null
    var url: String? = null
    lateinit var youTubePlayerView: YouTubePlayerView
    lateinit var fullscreenViewContainer: FrameLayout
    private var isFullscreen = false

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isFullscreen) {
                // if the player is in fullscreen, exit fullscreen
                youTubePlayer.toggleFullscreen()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        context = this
        url = intent.getStringExtra("url")
        if (url == null) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK

        val settings = AppSettings(context)
        setUpTheme(this, settings)
        setContentView(R.layout.video_view_activity)
        youTubePlayerView = findViewById(R.id.youtube_player_view)
        fullscreenViewContainer = findViewById(R.id.fragment)

        var video: String?
        try {
            if (url!!.contains("youtube")) { // normal youtube link
                // first get the youtube surfaceView code
                val start = url!!.indexOf("v=") + 2
                val end: Int
                if (url!!.substring(start).contains("&")) {
                    end = url!!.indexOf("&")
                    video = url!!.substring(start, end)
                } else if (url!!.substring(start).contains("?")) {
                    end = url!!.indexOf("?")
                    video = url!!.substring(start, end)
                } else {
                    video = url!!.substring(start)
                }
            } else { // shortened youtube link
                // first get the youtube surfaceView code
                val start = url!!.indexOf(".be/") + 4
                val end: Int
                if (url!!.substring(start).contains("&")) {
                    end = url!!.indexOf("&")
                    video = url!!.substring(start, end)
                } else if (url!!.substring(start).contains("?")) {
                    end = url!!.indexOf("?")
                    video = url!!.substring(start, end)
                } else {
                    video = url!!.substring(start)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting youtube link")
            video = ""
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.enableAutomaticInitialization = false

        youTubePlayerView.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                isFullscreen = true

                // the video will continue playing in fullscreenView
                youTubePlayerView.visibility = View.GONE
                fullscreenViewContainer.visibility = View.VISIBLE
                fullscreenViewContainer.addView(fullscreenView)

                // optionally request landscape orientation
                 requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            override fun onExitFullscreen() {
                isFullscreen = false

                // the video will continue playing in the player
                youTubePlayerView.visibility = View.VISIBLE
                fullscreenViewContainer.visibility = View.GONE
                fullscreenViewContainer.removeAllViews()
            }
        })

        if (TextUtils.isEmpty(video)) {
            showSnack(this, R.string.error_gif, Toast.LENGTH_SHORT)
        } else {
            val finalVideo = video
            val iFramePlayerOptions: IFramePlayerOptions = IFramePlayerOptions
                .Builder()
                .controls(1) // enable full screen button
                .fullscreen(1)
                .build()
            youTubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    this@YouTubeViewerActivity.youTubePlayer = youTubePlayer
                    youTubePlayerView.visibility = View.VISIBLE
                    youTubePlayer.loadVideo(finalVideo!!, 0f)
                }
            }, iFramePlayerOptions)
        }
        val ab = supportActionBar
        if (ab != null) {
            val transparent = ColorDrawable(resources.getColor(android.R.color.transparent))
            ab.setBackgroundDrawable(transparent)
            ab.setDisplayHomeAsUpEnabled(true)
            ab.setDisplayShowHomeEnabled(true)
            ab.setTitle("")
            ab.setIcon(transparent)
            ab.hide()
        }
        findViewById<View>(R.id.toolbar).visibility = View.GONE
        //        findViewById(R.id.fragment).setPadding(0, 0, 0, 0);
    }

}