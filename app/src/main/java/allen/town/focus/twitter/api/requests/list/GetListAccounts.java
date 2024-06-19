package allen.town.focus.twitter.api.requests.list;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.requests.HeaderPaginationRequest;
import allen.town.focus.twitter.model.Account;


public class GetListAccounts extends HeaderPaginationRequest<Account> {
    public GetListAccounts(String id,String maxID, String minID, String sinceID,int limit) {
        super(HttpMethod.DELETE, "/lists/" + id + "/accounts", new TypeToken<>() {

        });
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
