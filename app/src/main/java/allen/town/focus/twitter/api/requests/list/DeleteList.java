package allen.town.focus.twitter.api.requests.list;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;

public class DeleteList extends MastodonAPIRequest<Void>{
	public DeleteList(String id){
		super(HttpMethod.DELETE, "/lists/"+id, Void.class);
	}
}
