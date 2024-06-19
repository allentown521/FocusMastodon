package allen.town.focus.twitter.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;

@Parcel
public class Conversation extends BaseModel implements DisplayItemsParent, Serializable {
    @RequiredField
    public String id;
    //	@RequiredField
    public boolean unread;
    public List<Account> accounts;

    @SerializedName("last_status")
    @Nullable
    public Status status;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        if (status != null)
            status.postprocess();
    }

    @Override
    public String getID() {
        return id;
    }

}
