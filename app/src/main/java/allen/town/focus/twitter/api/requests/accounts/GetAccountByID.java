package allen.town.focus.twitter.api.requests.accounts;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Account;

public class GetAccountByID extends MastodonAPIRequest<Account>{
	public GetAccountByID(String id){
		super(HttpMethod.GET, "/accounts/"+id, Account.class);
	}
}
