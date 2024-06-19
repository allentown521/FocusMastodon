package allen.town.focus.twitter.api.requests.accounts;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Relationship;

public class SetAccountFollowed extends MastodonAPIRequest<Relationship>{
	public SetAccountFollowed(String id, boolean followed, boolean showReblogs){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(followed ? "follow" : "unfollow"), Relationship.class);
		if(followed)
			setRequestBody(new Request(showReblogs, null));
		else
			setRequestBody(new Object());
	}

	private static class Request{
		public Boolean reblogs, notify;

		public Request(Boolean reblogs, Boolean notify){
			this.reblogs=reblogs;
			this.notify=notify;
		}
	}
}
