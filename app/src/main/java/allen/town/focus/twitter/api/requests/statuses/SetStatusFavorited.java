package allen.town.focus.twitter.api.requests.statuses;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;

public class SetStatusFavorited extends MastodonAPIRequest<Status>{
	public SetStatusFavorited(String id, boolean favorited){
		super(MastodonAPIRequest.HttpMethod.POST, "/statuses/"+id+"/"+(favorited ? "favourite" : "unfavourite"), Status.class);
		setRequestBody(new Object());
	}
}
