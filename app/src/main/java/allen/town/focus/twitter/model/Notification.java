package allen.town.focus.twitter.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.io.Serializable;
import java.time.Instant;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;

@Parcel
public class Notification extends BaseModel implements DisplayItemsParent, Serializable {
	@RequiredField
	public String id;
//	@RequiredField
	public Type type;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Account account;


	@Nullable
	public Status status;

	@Override
	public void postprocess() throws ObjectValidationException {
		super.postprocess();
		account.postprocess();
		if(status!=null)
			status.postprocess();
	}

	@Override
	public String getID(){
		return id;
	}

	public enum Type{
		@SerializedName("follow")
		FOLLOW,
		@SerializedName("follow_request")
		FOLLOW_REQUEST,
		@SerializedName("mention")
		MENTION,
		@SerializedName("reblog")
		REBLOG,
		@SerializedName("favourite")
		FAVORITE,
		@SerializedName("poll")
		POLL,
		@SerializedName("status")
		STATUS
	}
}
