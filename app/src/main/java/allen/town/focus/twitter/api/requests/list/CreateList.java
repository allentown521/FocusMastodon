package allen.town.focus.twitter.api.requests.list;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.MastoList;

public class CreateList extends MastodonAPIRequest<MastoList> {
    public CreateList(String title) {
        super(HttpMethod.POST, "/lists/", new TypeToken<>() {
        });
        setRequestBody(new Request(title));
    }

    private static class Request {
        public String title = "";

        Request(String title) {
            this.title = title;
        }
    }
}
