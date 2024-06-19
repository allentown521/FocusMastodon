package allen.town.focus.twitter.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Filter;

import java.util.List;

public class GetWordFilters extends MastodonAPIRequest<List<Filter>>{
	public GetWordFilters(){
		super(HttpMethod.GET, "/filters", new TypeToken<>(){});
	}
}
