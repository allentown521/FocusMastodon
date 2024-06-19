package allen.town.focus.twitter.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Status;

public class GetHomeTimeline extends HeaderPaginationRequest<Status> {
	/**
	 * https://docs.joinmastodon.org/methods/timelines/#home
	 * @param maxID
	 * @param minID
	 * @param limit 最多 40
	 * @param sinceID
	 */
	public GetHomeTimeline(String maxID, String minID, int limit, String sinceID){
		super(MastodonAPIRequest.HttpMethod.GET, "/timelines/home", new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(sinceID!=null)
			addQueryParameter("since_id", sinceID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
	}
}
