package allen.town.focus.twitter.api.requests.trends;

import com.google.gson.reflect.TypeToken;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Hashtag;

public class GetTrendingHashtags extends MastodonAPIRequest<List<Hashtag>> {
	public GetTrendingHashtags(int limit){
		super(HttpMethod.GET, "/trends", new TypeToken<>(){});
		addQueryParameter("limit", limit+"");
	}
}
