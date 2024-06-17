package allen.town.focus.twitter.api;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.CallSuper;
import androidx.annotation.StringRes;

import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import allen.town.focus.twitter.BuildConfig;
import allen.town.focus.twitter.api.session.AccountSession;
import allen.town.focus.twitter.api.session.AccountSessionManager;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.BaseModel;
import allen.town.focus.twitter.model.Token;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.views.AccentProgressDialog;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import okhttp3.Call;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class MastodonAPIRequest<T> extends APIRequest<T> {
    private static final String TAG = "MastodonAPIRequest";

    private String domain;
    private AccountSession account;
    private String path;
    private String method;
    private Object requestBody;
    private List<Pair<String, String>> queryParams;
    Class<T> respClass;
    TypeToken<T> respTypeToken;
    Call okhttpCall;
    Token token;
    boolean canceled;
    Map<String, String> headers;
    private ProgressDialog progressDialog;
    protected boolean removeUnsupportedItems;
    private SharedPreferences prefs;

    public MastodonAPIRequest(HttpMethod method, String path, Class<T> respClass) {
        this.path = path;
        this.method = method.toString();
        this.respClass = respClass;
        prefs = AppSettings.getSharedPreferences(App.getInstance());
    }

    public MastodonAPIRequest(HttpMethod method, String path, TypeToken<T> respTypeToken) {
        this.path = path;
        this.method = method.toString();
        this.respTypeToken = respTypeToken;
        prefs = AppSettings.getSharedPreferences(App.getInstance());
    }

    @Override
    public synchronized void cancel() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "canceling request " + this);
        canceled = true;
        if (okhttpCall != null) {
            okhttpCall.cancel();
        }
    }


    public MastodonAPIRequest<T> exec(String accountID) {
        try {
            account = AccountSessionManager.getInstance().getAccount(accountID);
            domain = account.domain;
            account.getApiController().submitRequest(this);
        } catch (Exception x) {
            Log.e(TAG, "exec: this shouldn't happen, but it still did", x);
            invokeErrorCallback(new MastodonErrorResponse(x.getLocalizedMessage(), -1, x));
        }
        return this;
    }

    /**
     * 为当前账号执行请求
     *
     * @return
     */
    @Override
    public MastodonAPIRequest<T> exec() {
        try {
            account = AccountSessionManager.getInstance().getAccount(AppSettings.getInstance(App.getInstance()).mySessionId + "");
            domain = account.domain;
            account.getApiController().submitRequest(this);
        } catch (Exception x) {
            Log.e(TAG, "exec: this shouldn't happen, but it still did", x);
            invokeErrorCallback(new MastodonErrorResponse(x.getLocalizedMessage(), -1, x));
        }
        return this;
    }

    /**
     * 同步执行，为当前账号执行请求
     *
     * @return
     */
    public T execSync() throws Exception {
        T result;
        account = AccountSessionManager.getInstance().getAccount(AppSettings.getInstance(App.getInstance()).mySessionId + "");
        domain = account.domain;
        result = account.getApiController().submitRequestSync(this);
        return result;
    }

    /**
     * 为另一个账号执行请求
     *
     * @return
     */
    public MastodonAPIRequest<T> execSecondAccount() {
        try {
            account = AccountSessionManager.getInstance().getAccount(AppSettings.getInstance(App.getInstance()).secondSessionId + "");
            domain = account.domain;
            account.getApiController().submitRequest(this);
        } catch (Exception x) {
            Log.e(TAG, "exec: this shouldn't happen, but it still did", x);
            invokeErrorCallback(new MastodonErrorResponse(x.getLocalizedMessage(), -1, x));
        }
        return this;
    }

    public T execSecondAccountSync() throws Exception {
        T result;
        account = AccountSessionManager.getInstance().getAccount(AppSettings.getInstance(App.getInstance()).secondSessionId + "");
        domain = account.domain;
        result = account.getApiController().submitRequestSync(this);
        return result;
    }

    public MastodonAPIRequest<T> execNoAuth(String domain) {
        this.domain = domain;
        AccountSessionManager.getInstance().getUnauthenticatedApiController().submitRequest(this);
        return this;
    }

    public MastodonAPIRequest<T> exec(String domain, Token token) {
        this.domain = domain;
        this.token = token;
        AccountSessionManager.getInstance().getUnauthenticatedApiController().submitRequest(this);
        return this;
    }

    public MastodonAPIRequest<T> wrapProgress(Activity activity, @StringRes int message, boolean cancelable) {
        progressDialog = AccentProgressDialog.show(activity, activity.getString(message));
        progressDialog.setCancelable(cancelable);
        if (cancelable) {
            progressDialog.setOnCancelListener(dialog -> cancel());
        }
        progressDialog.show();
        return this;
    }

    protected void setRequestBody(Object body) {
        requestBody = body;
    }

    protected void addQueryParameter(String key, String value) {
        if (queryParams == null)
            queryParams = new ArrayList<>();
        queryParams.add(new Pair<>(key, value));
    }

    protected void addHeader(String key, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(key, value);
    }

    protected String getPathPrefix() {
        return "/api/v1";
    }

    public Uri getURL() {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(domain)
                .path(getPathPrefix() + path);
        if (queryParams != null) {
            for (Pair<String, String> param : queryParams) {
                builder.appendQueryParameter(param.first, param.second);
            }
        }
        return builder.build();
    }

    public String getMethod() {
        return method;
    }

    public RequestBody getRequestBody() throws IOException {
        return requestBody == null ? null : new JsonObjectRequestBody(requestBody);
    }

    @Override
    public MastodonAPIRequest<T> setCallback(Callback<T> callback) {
        super.setCallback(callback);
        return this;
    }

    @CallSuper
    public void validateAndPostprocessResponse(T respObj, Response httpResponse) throws IOException {
        if (respObj instanceof BaseModel) {
            ((BaseModel) respObj).postprocess();
        } else if (respObj instanceof List) {
            if (removeUnsupportedItems) {
                Iterator<?> itr = ((List<?>) respObj).iterator();
                while (itr.hasNext()) {
                    Object item = itr.next();
                    if (item instanceof BaseModel) {
                        try {
                            ((BaseModel) item).postprocess();
                        } catch (ObjectValidationException x) {
                            Log.w(TAG, "Removing invalid object from list", x);
                            itr.remove();
                        }
                    }
                }
                for (Object item : ((List<?>) respObj)) {
                    if (item instanceof BaseModel) {
                        ((BaseModel) item).postprocess();
                    }
                }
            } else {
                for (Object item : ((List<?>) respObj)) {
                    if (item instanceof BaseModel)
                        ((BaseModel) item).postprocess();
                }
            }
        }
    }

    void onError(ErrorResponse err) {
        if (!canceled)
            invokeErrorCallback(err);
    }

    void onError(String msg, int httpStatus, Throwable exception) {
        if (!canceled)
            invokeErrorCallback(new MastodonErrorResponse(msg, httpStatus, exception));
    }

    void onSuccess(T resp) {
        if (!canceled)
            invokeSuccessCallback(resp);
    }

    @Override
    protected void onRequestDone() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH
    }
}
