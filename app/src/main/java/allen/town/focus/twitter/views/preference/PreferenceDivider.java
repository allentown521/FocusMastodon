package allen.town.focus.twitter.views.preference;

import android.content.Context;
import androidx.preference.Preference;
import android.util.AttributeSet;

import allen.town.focus.twitter.R;

public class PreferenceDivider extends Preference {

    public PreferenceDivider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PreferenceDivider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_divider);
    }
}
