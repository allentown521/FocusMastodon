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

package allen.town.focus.twitter.views.widgets.text;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;

import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.settings.font.Font;
import allen.town.focus.twitter.settings.font.FontCache;

import allen.town.focus_common.util.Timber;

public class FontPrefEditText extends EmojiableEditText {

    private static SharedPreferences sharedPreferences;

    public FontPrefEditText(Context context) {
        super(context);
        setUp(context);
    }

    public FontPrefEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp(context);
    }


    private void setUp(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = AppSettings.getSharedPreferences(context);

        }

        setTypeface(context);
    }

    public static Typeface typeface;
    private static boolean useDeviceFont;

    private void setTypeface(Context context) {
        if(isInEditMode()){
            return;
        }
        Font font = App.getPrefs(getContext()).articleListFontType.get();

        if (font.getLabel().equals(Font.DEFAULT_FONT_KEY)) {
//            textView.setTypeface(Typeface.DEFAULT);
        } else if (font.equals(Font.SANS_SERIF_KEY)) {
            setTypeface(Typeface.SANS_SERIF);
        } else {
            if (font.isCharge() && !App.getInstance().checkSupporter(null, false)) {
                Timber.i("not pro reset edit text font");
                return;
            }
            Typeface typeface;
            //先从缓存取
            typeface = FontCache.getFontCache().get(font.getCssName());
            if (typeface == null) {
                try {
                    //5.0原生无法显示woff2会抛异常(实际发现魅族8.1.0也会所以捕获异常)
                    if (font.isExternal()) {
                        typeface = Typeface.createFromFile(font.getPath());
                    } else {
                        typeface = Typeface.createFromAsset(getContext().getAssets(), font.getPath());
                    }

                } catch (Exception e) {
                    Timber.e("createFromAsset failed " + e.toString());
                    typeface = Typeface.SANS_SERIF;
                }
            }

            setTypeface(typeface);
        }
    }
}
