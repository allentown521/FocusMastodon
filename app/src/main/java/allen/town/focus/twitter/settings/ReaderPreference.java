package allen.town.focus.twitter.settings;

import android.content.SharedPreferences;

import allen.town.focus.twitter.settings.font.Font;

import java.util.HashSet;
import java.util.Set;

import allen.town.focus_common.util.Timber;

public class ReaderPreference<T> {
    protected final T defaultValue;
    protected final String key;
    private final Set<OnChangeListener> onChangeListeners = new HashSet();
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            Timber.d("Preference %s changed", key);
            if (str.equals(key)) {
                Timber.d("Preference %s changed notified", key);
                for (OnChangeListener onChangeListener : onChangeListeners) {
                    onChangeListener.onChanged();
                }
            }
        }
    };
    protected final SharedPreferences preferences;

    public interface OnChangeListener {
        void onChanged();
    }


    public ReaderPreference(SharedPreferences sharedPreferences, String str, T t) {
        this.preferences = sharedPreferences;
        this.key = str;
        this.defaultValue = t;
    }

    public void addListener(OnChangeListener onChangeListener) {
        if (this.onChangeListeners.isEmpty()) {
            this.preferences.registerOnSharedPreferenceChangeListener(this.onSharedPreferenceChangeListener);
        }
        this.onChangeListeners.add(onChangeListener);
    }

    public void removeListener(OnChangeListener onChangeListener) {
        this.onChangeListeners.remove(onChangeListener);
        if (this.onChangeListeners.isEmpty()) {
            this.preferences.unregisterOnSharedPreferenceChangeListener(this.onSharedPreferenceChangeListener);
        }
    }

    public T get() {
        T t = this.defaultValue;
        if (t instanceof String) {
            return (T) this.preferences.getString(this.key, (String) t);
        }
        if (t instanceof Integer) {
            return (T) Integer.valueOf(this.preferences.getInt(this.key, ((Integer) t).intValue()));
        }
        if (t instanceof Float) {
            return (T) Float.valueOf(this.preferences.getFloat(this.key, ((Float) t).floatValue()));
        }
        if (t instanceof Font) {
            int ordinal = this.preferences.getInt(this.key, ((Font) this.defaultValue).getIndex());
            if (ordinal >= Font.fontList.size()) {
                ordinal = Font.fontList.size() - 1;
            }
            return (T) Font.fontList.get(ordinal);
        }
        if (t instanceof Boolean) {
            return (T) Boolean.valueOf(this.preferences.getBoolean(this.key, ((Boolean) t).booleanValue()));
        }
        throw new IllegalArgumentException("Preference type not implemented " + this.defaultValue.getClass());
    }

    public void set(T t) {
        SharedPreferences.Editor edit = this.preferences.edit();
        if (t instanceof String) {
            edit.putString(this.key, (String) t);
        } else if (t instanceof Integer) {
            edit.putInt(this.key, ((Integer) t).intValue());
        } else if (t instanceof Float) {
            edit.putFloat(this.key, ((Float) t).floatValue());
        } else if (t instanceof Font) {
            edit.putInt(this.key, ((Font) t).getIndex());
        } else if (t instanceof Boolean) {
            edit.putBoolean(this.key, ((Boolean)t).booleanValue());
        } else {
            throw new IllegalArgumentException("Preference type not implemented " + t.getClass());
        }
        edit.commit();
    }

    public boolean isSet() {
        return this.preferences.contains(this.key);
    }

    public void delete() {
        this.preferences.edit().remove(this.key).commit();
    }
}
