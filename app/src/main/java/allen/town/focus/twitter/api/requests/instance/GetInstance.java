package allen.town.focus.twitter.api.requests.instance;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Instance;

public class GetInstance extends MastodonAPIRequest<Instance>{
	public GetInstance(){
		super(HttpMethod.GET, "/instance", Instance.class);
	}
}
