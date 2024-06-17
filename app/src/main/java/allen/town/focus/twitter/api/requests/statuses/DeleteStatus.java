package allen.town.focus.twitter.api.requests.statuses;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;

public class DeleteStatus extends MastodonAPIRequest<Status>{
	public DeleteStatus(String id){
		super(MastodonAPIRequest.HttpMethod.DELETE, "/statuses/"+id, Status.class);
	}
}
