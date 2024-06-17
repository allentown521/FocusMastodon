package allen.town.focus.twitter.api.requests.list;

import com.google.gson.reflect.TypeToken;

import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.MastoList;

public class GetLists extends MastodonAPIRequest<List<MastoList>> {
    public GetLists() {
        super(HttpMethod.GET, "/lists/", new TypeToken<>() {
        });
    }
}
