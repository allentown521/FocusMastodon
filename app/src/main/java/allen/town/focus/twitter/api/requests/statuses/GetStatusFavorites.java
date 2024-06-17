package allen.town.focus.twitter.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Account;


public class GetStatusFavorites extends HeaderPaginationRequest<Account> {
	public GetStatusFavorites(String id, String maxID, int limit){
		super(HttpMethod.GET, "/statuses/"+id+"/favourited_by", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", limit+"");
	}
}
