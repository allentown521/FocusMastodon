package allen.town.focus.twitter.views

import allen.town.focus.twitter.R
import allen.town.focus.twitter.databinding.ViewBackgroundMessageBinding
import allen.town.focus_common.extensions.visible
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * This view is used for screens with content which may be empty or might have failed to download.
 */
class BackgroundMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBackgroundMessageBinding.inflate(LayoutInflater.from(context), this)

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        orientation = VERTICAL

        if (isInEditMode) {
            setup(R.drawable.elephant_offline, R.string.error_network) {}
        }
    }

    /**
     * Setup image, message and button.
     * If [clickListener] is `null` then the button will be hidden.
     */
    fun setup(
        @DrawableRes imageRes: Int,
        @StringRes messageRes: Int,
        clickListener: ((v: View) -> Unit)? = null
    ) {
        binding.messageTextView.setText(messageRes)
        binding.imageView.setImageResource(imageRes)
        binding.button.setOnClickListener(clickListener)
        binding.button.visible(clickListener != null)
    }

}
