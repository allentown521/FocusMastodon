package allen.town.focus.twitter.api.requests.trends;

import com.google.gson.reflect.TypeToken;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;

public class GetTrendingStatuses extends HeaderPaginationRequest<Status> {
	public GetTrendingStatuses(int offset, int limit){
		super(HttpMethod.GET, "/trends/statuses", new TypeToken<>(){});
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(offset>0)
			addQueryParameter("offset", ""+offset);
	}
}
