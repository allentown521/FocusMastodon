package allen.town.focus.twitter.api.requests.list;

import com.google.gson.reflect.TypeToken;

import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;


public class AddAccountToList extends MastodonAPIRequest<Void> {
    public AddAccountToList(String id, List<String> accountIds) {
        super(HttpMethod.POST, "/lists/" + id + "/accounts", new TypeToken<>() {
        });
        setRequestBody(new Body(accountIds.toArray(new String[accountIds.size()])));
    }

    private static class Body {
        public String[] account_ids;

        public Body(String[] account_ids) {
            this.account_ids = account_ids;
        }
    }
}
