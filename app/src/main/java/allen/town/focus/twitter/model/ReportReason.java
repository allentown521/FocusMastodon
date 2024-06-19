package allen.town.focus.twitter.model;

import com.google.gson.annotations.SerializedName;

public enum ReportReason{
	PERSONAL,
	@SerializedName("spam")
	SPAM,
	@SerializedName("violation")
	VIOLATION,
	@SerializedName("other")
	OTHER
}
