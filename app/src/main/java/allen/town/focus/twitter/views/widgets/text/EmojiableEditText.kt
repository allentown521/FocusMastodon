package allen.town.focus.twitter.views.widgets.text

import allen.town.focus_common.views.CursorAccentEditText
import android.content.Context
import android.text.method.KeyListener
import androidx.emoji.widget.EmojiEditTextHelper
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import allen.town.focus.twitter.data.EmojiStyle
import allen.town.focus.twitter.settings.AppSettings

open class EmojiableEditText : CursorAccentEditText {

    private val useEmojiCompat: Boolean
        get() = isInEditMode || AppSettings.getInstance(context).emojiStyle != EmojiStyle.DEFAULT

    private var mEmojiEditTextHelper: EmojiEditTextHelper? = null
    private val emojiEditTextHelper: EmojiEditTextHelper
        get() {
            if (mEmojiEditTextHelper == null) {
                mEmojiEditTextHelper = EmojiEditTextHelper(this)
            }
            return mEmojiEditTextHelper as EmojiEditTextHelper
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }


    private fun init() {
        if (useEmojiCompat) {
            super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
        }
    }

    override fun setKeyListener(keyListener: KeyListener?) {
        if (useEmojiCompat) {
            super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener!!))
        } else {
            super.setKeyListener(keyListener)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        return if (useEmojiCompat) {
            val inputConnection = super.onCreateInputConnection(outAttrs)
            emojiEditTextHelper.onCreateInputConnection(inputConnection, outAttrs)!!
        } else {
            super.onCreateInputConnection(outAttrs)
        }
    }
}