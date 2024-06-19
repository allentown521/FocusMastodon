package allen.town.focus.twitter.model;

import android.text.TextUtils;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;

/**
 * Represents a user of Mastodon and their associated profile.
 */
@Parcel
public class ReTweeterAccount extends BaseModel implements Serializable {
    // Base attributes

    /**
     * The account id
     */
    @RequiredField
    public String id;
    /**
     * The username of the account, not including domain.
     */
    @RequiredField
    public String username;

    /**
     * The profile's display name.
     */
    @RequiredField
    public String displayName;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        if (TextUtils.isEmpty(displayName))
            displayName = username;
    }


    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }

    @ParcelConstructor
    public ReTweeterAccount(String id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public static String getRetweeterFormatUrl(String screenName, String id) {
        String url = null;
        if (!TextUtils.isEmpty(screenName)) {
            url = "<a id=\"" + id + "\" class=\"mention\">" + screenName + "</span></a>";
        }
        return url;
    }
}
