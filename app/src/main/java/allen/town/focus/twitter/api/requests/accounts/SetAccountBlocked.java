package allen.town.focus.twitter.api.requests.accounts;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Relationship;

public class SetAccountBlocked extends MastodonAPIRequest<Relationship>{
	public SetAccountBlocked(String id, boolean blocked){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(blocked ? "block" : "unblock"), Relationship.class);
		setRequestBody(new Object());
	}
}
