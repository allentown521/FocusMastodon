package allen.town.focus.twitter.settings.font;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import allen.town.focus.twitter.R;

import allen.town.focus_common.adapter.BindableAdapter;
import allen.town.focus_common.util.Timber;

public class FontsAdapter extends BindableAdapter<Font> {
    public long getItemId(int i) {
        return (long) i;
    }

    public FontsAdapter(Context context) {
        super(context);
    }

    public int getCount() {
        return Font.fontList.size();
    }

    @Override //.ui.adapter.BindableAdapter
    public Font getItem(int i) {
        return Font.fontList.get(i);
    }

    @Override //.ui.adapter.BindableAdapter
    public View newView(LayoutInflater layoutInflater, int i, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.list_item_font_selector, viewGroup, false);
    }

    public void bindView(Font font, int i, View view) {
        TextView textView = (TextView) view;
        textView.setText(font.getLabel());
        if (font.getLabel().equals(Font.DEFAULT_FONT_KEY)) {
            textView.setTypeface(Typeface.DEFAULT);
        } else if (font.getLabel().equals(Font.SANS_SERIF_KEY)) {
            textView.setTypeface(Typeface.SANS_SERIF);
        } else {
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

            textView.setTypeface(typeface);
        }
    }
}