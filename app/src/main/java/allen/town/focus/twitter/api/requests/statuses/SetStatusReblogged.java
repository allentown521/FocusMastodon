package allen.town.focus.twitter.api.requests.statuses;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;

public class SetStatusReblogged extends MastodonAPIRequest<Status> {
	public SetStatusReblogged(String id, boolean reblogged){
		super(HttpMethod.POST, "/statuses/"+id+"/"+(reblogged ? "reblog" : "unreblog"), Status.class);
		setRequestBody(new Object());
	}
}
