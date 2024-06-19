package allen.town.focus.twitter.api.requests.oauth;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.session.AccountSessionManager;
import allen.town.focus.twitter.model.Application;

public class CreateOAuthApp extends MastodonAPIRequest<Application>{
	public CreateOAuthApp(){
		super(HttpMethod.POST, "/apps", Application.class);
		setRequestBody(new Request());
	}

	private static class Request{
		public String clientName="Focus for Mastodon";
		public String redirectUris=AccountSessionManager.REDIRECT_URI;
		public String scopes=AccountSessionManager.SCOPE;
		public String website="http://focus.hk.cn";
	}
}
