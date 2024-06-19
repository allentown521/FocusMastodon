package allen.town.focus.twitter.activities

import allen.town.focus_common.util.Timber
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import allen.town.focus.twitter.R
import allen.town.focus.twitter.data.App

/**
 * Shows the logo while waiting for the main activity to start.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)
        App.getInstance(this).openAdManager.fetchAd(
            findViewById(R.id.container)
        ) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(0, 0)

            Handler().postDelayed({
                try {
                    finish()
                } catch (e: Exception) {
                    //如果play版马上finish了会看到桌面然后才是首页，体验不好
                    Timber.w("splash error $e")
                }
            }, 200)

        }

    }

    /**
     * 开屏页一定要禁止用户对返回按钮的控制，否则将可能导致用户手动退出了App而广告无法正常曝光和计费
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            true
        } else super.onKeyDown(keyCode, event)
    }
}