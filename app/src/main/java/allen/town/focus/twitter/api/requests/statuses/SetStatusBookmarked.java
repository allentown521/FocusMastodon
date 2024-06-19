package allen.town.focus.twitter.api.requests.statuses;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;

public class SetStatusBookmarked extends MastodonAPIRequest<Status> {
	public SetStatusBookmarked(String id, boolean bookmarked){
		super(HttpMethod.POST, "/statuses/"+id+"/"+(bookmarked ? "bookmark" : "unbookmark"), Status.class);
		setRequestBody(new Object());
	}
}
