package allen.town.focus.twitter.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.MastoList;

/**
 * 获取包含此帐户的列表
 */
public class GetAccountInLists extends HeaderPaginationRequest<MastoList> {
    public GetAccountInLists(String id) {
        super(HttpMethod.GET, "/accounts/" + id + "/lists", new TypeToken<>() {
        });
    }
}
