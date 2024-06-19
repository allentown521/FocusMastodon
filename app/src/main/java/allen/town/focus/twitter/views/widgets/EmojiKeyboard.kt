/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package allen.town.focus.twitter.views.widgets

import allen.town.focus.twitter.R
import allen.town.focus.twitter.adapters.emoji.EmojiAdapter
import allen.town.focus.twitter.adapters.emoji.OnEmojiSelectedListener
import allen.town.focus.twitter.data.sq_lite.EmojiDataSource
import allen.town.focus.twitter.data.sq_lite.Recent
import allen.town.focus.twitter.model.Emoji
import allen.town.focus.twitter.utils.EmojiUtils
import allen.town.focus.twitter.views.widgets.text.FontPrefEditText
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.astuetz.PagerSlidingTabStrip

class EmojiKeyboard(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private lateinit var input: FontPrefEditText
    private var recyclerView: RecyclerView? = null
    private var tabs: PagerSlidingTabStrip? = null
    private var backspace: ImageButton? = null
    private var keyboardHeight = 0
    private var emojiAdapter: EmojiAdapter? = null
    override fun onFinishInflate() {
        super.onFinishInflate()
        try {
            recyclerView = findViewById<View>(R.id.emojiKeyboardRecyclerView) as RecyclerView
            backspace = findViewById<View>(R.id.delete) as ImageButton
            val d =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            keyboardHeight = (d.height / 3.0).toInt()
            dataSource = EmojiDataSource(context)
            dataSource!!.open()
            recents = dataSource!!.allRecents as ArrayList<Recent>
            recyclerView!!.layoutManager =
                GridLayoutManager(context, 7, GridLayoutManager.VERTICAL, false)
            recyclerView!!.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, keyboardHeight)
            tabs = findViewById<View>(R.id.emojiTabs) as PagerSlidingTabStrip
            backspace!!.setOnClickListener { removeText() }
        } catch (e: Exception) {
        }
    }

    fun setAttached(et: FontPrefEditText) {
        input = et
    }

    fun setEmojiList(list: List<Emoji>) {
        emojiAdapter = EmojiAdapter(
            list,
            object : OnEmojiSelectedListener {
                override fun onEmojiSelected(shortcode: String) {
                    insertEmoji(":$shortcode: ")
                }
            },
            false
        )
        if (recyclerView != null) {
            recyclerView!!.adapter = emojiAdapter
        }
    }

    val isShowing: Boolean
        get() = visibility == VISIBLE

    /**
     * 立即设置为不可见，不然发推时有隐藏动画认为可见会不关闭弹窗
     * @param visible
     */
    fun setVisibilityImmediately(visible: Boolean) {
        visibility = if (visible) VISIBLE else GONE
    }

    fun setVisibility(visible: Boolean) {
        visibility = VISIBLE
        val animation = AnimationUtils.loadAnimation(
            context,
            if (visible) R.anim.emoji_slide_out else R.anim.emoji_slide_in
        )
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                visibility = if (visible) VISIBLE else GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        startAnimation(animation)
    }

    fun toggleVisibility() {
        setVisibility(visibility != VISIBLE)
    }

    fun insertEmoji(text: CharSequence) {
/*      原始的写法

        input!!.isEnabled = false
        val beforeSelectionStart = input!!.selectionStart
        val beforeLength = input!!.text.toString().length
        val before = input!!.text!!.subSequence(0, beforeSelectionStart)
        val after = input!!.text!!
            .subSequence(input!!.selectionEnd, beforeLength)
        input!!.setText(TextUtils.concat(before, emoji, after))
        input!!.isEnabled = true
        input!!.setSelection(beforeSelectionStart + (input!!.text.toString().length - beforeLength))*/


        //tusky的写法
        // If you select "backward" in an editable, you get SelectionStart > SelectionEnd
        val start = input.selectionStart.coerceAtMost(input.selectionEnd)
        val end = input.selectionStart.coerceAtLeast(input.selectionEnd)
        val textToInsert = if (start > 0 && !input.text?.get(start - 1)?.isWhitespace()!!) {
            " $text"
        } else {
            text
        }
        input.text?.replace(start, end, textToInsert)

        // Set the cursor after the inserted text
        input.setSelection(start + text.length)



        /*        for (Recent recent1 : recents) {
            if (recent1.text.equals(emoji)) {
                dataSource.updateRecent(icon + "");
                recent1.count++;
                return;
            }
        }
        Recent recent = dataSource.createRecent(emoji, icon + "");
        if (recent != null) recents.add(recent);*/
    }

    private fun removeText() {
        val currentText = input!!.text.toString()
        if (currentText.length > 0 && input!!.selectionStart > 0) {
            input!!.isEnabled = false
            val selection = input!!.selectionStart
            input!!.setText(
                EmojiUtils.getSmiledText(
                    context,
                    StringBuilder(input!!.text.toString()).deleteCharAt(selection - 1).toString()
                )
            )
            input!!.isEnabled = true
            input!!.setSelection(selection - 1)
        }
    }

    fun removeRecent(position: Int) {
        try {
            dataSource!!.deleteRecent(recents!![position].id)
            recents!!.removeAt(position)
        } catch (e: Exception) {
        }
    }

    companion object {
        private var dataSource: EmojiDataSource? = null
        private var recents: ArrayList<Recent>? = null
    }
}