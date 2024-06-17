package allen.town.focus.twitter.api.requests.statuses;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.StatusContext;

public class GetStatusContext extends MastodonAPIRequest<StatusContext>{
	public GetStatusContext(String id){
		super(MastodonAPIRequest.HttpMethod.GET, "/statuses/"+id+"/context", StatusContext.class);
	}
}
