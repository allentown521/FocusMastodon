package allen.town.focus.twitter.api.requests.trends;

import com.google.gson.reflect.TypeToken;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Card;

public class GetTrendingLinks extends MastodonAPIRequest<List<Card>> {
	public GetTrendingLinks(){
		super(HttpMethod.GET, "/trends/links", new TypeToken<>(){});
	}
}
