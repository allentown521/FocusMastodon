package allen.town.focus.twitter.api.requests.accounts;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Account;

public class GetAccountByAcct extends MastodonAPIRequest<Account> {
    public GetAccountByAcct(String acct) {
        super(HttpMethod.GET, "/accounts/lookup", new TypeToken<>() {
        });
        if (!TextUtils.isEmpty(acct))
            addQueryParameter("acct", acct);
    }
}
