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

package allen.town.focus.twitter.utils.text;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import allen.town.focus.twitter.utils.EmojiUtils;
import allen.town.focus.twitter.utils.HtmlParser;

import java.util.regex.Matcher;

public class TextUtils {

    /**
     * 因为textview继承LinkedTextView的原因，导致其他方式实现的点击色都没有了
     * @param context
     * @param textView
     * @param holder
     * @param clickable
     * @param allLinks
     * @param extBrowser
     */
    public static void linkifyText(Context context, TextView textView, View holder, boolean clickable, String allLinks, boolean extBrowser) {
        Linkify.TransformFilter filter = new Linkify.TransformFilter() {
            public final String transformUrl(final Matcher match, String url) {
                return match.group();
            }
        };

        textView.setLinksClickable(clickable);

        //Linkify.addLinks(context, textView, Patterns.PHONE, null, filter, textView, surfaceView);
        Linkify.addLinks(context, textView, Patterns.EMAIL_ADDRESS, null, filter, textView, allLinks, extBrowser);
        Linkify.addLinks(context, textView, Regex.VALID_URL, null, filter, textView, allLinks, extBrowser);
        Linkify.addLinks(context, textView, Regex.HASHTAG_PATTERN, null, filter, textView, allLinks, extBrowser);
        Linkify.addLinks(context, textView, Regex.CASHTAG_PATTERN, null, filter, textView, allLinks, extBrowser);
        Linkify.addLinks(context, textView, Regex.MENTION_PATTERN, null, filter, textView, allLinks, extBrowser);
        //部分需要用下面这个，写推特这些不需要有跳转事件的不需要
    }

    public static Spannable colorText(Context context, String tweet, int color) {
        return colorText(context, tweet, color, false);
    }

    public static Spannable colorText(Context context, String tweet, int color, boolean emojis) {
        Spannable finish = new SpannableString(tweet);

        Matcher m = Regex.MENTION_PATTERN.matcher(tweet);
        while (m.find()) {
            finish = changeText(finish, m.group(0), color);
        }
        m = Regex.HASHTAG_PATTERN.matcher(tweet);
        while (m.find()) {
            finish = changeText(finish, m.group(0), color);
        }
        m = Regex.CASHTAG_PATTERN.matcher(tweet);
        while (m.find()) {
            finish = changeText(finish, m.group(0), color);
        }
        m = Regex.VALID_URL.matcher(tweet);
        while (m.find()) {
            finish = changeText(finish, m.group(0), color);
        }

        if (emojis) {
            EmojiUtils.addSmiles(context, finish);
        }

        return finish;
    }

    public static Spannable changeText(Spannable original, String target, int colour) {
        target = target.replaceAll("\"", "");
        String vString = original.toString();
        int startSpan = 0, endSpan = 0;
        Spannable spanRange = original;

        while (true) {
            startSpan = vString.indexOf(target, endSpan);
            ForegroundColorSpan foreColour = new ForegroundColorSpan(colour);
            //BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(colour);

            // Need a NEW span object every loop, else it just moves the span
            if (startSpan < 0)
                break;
            endSpan = startSpan + target.length();
            try {
                spanRange.setSpan(foreColour, startSpan, endSpan,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (IndexOutOfBoundsException e) {

            }
        }

        return spanRange;
    }
}
