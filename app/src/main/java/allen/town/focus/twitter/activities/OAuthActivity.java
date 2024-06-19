package allen.town.focus.twitter.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.setup.material_login.InitAccountAfterLogin;
import allen.town.focus.twitter.api.requests.accounts.GetOwnAccount;
import allen.town.focus.twitter.api.requests.oauth.GetOauthToken;
import allen.town.focus.twitter.api.session.AccountSessionManager;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.Application;
import allen.town.focus.twitter.model.Instance;
import allen.town.focus.twitter.model.Token;
import allen.town.focus_common.views.AccentProgressDialog;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class OAuthActivity extends Activity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Uri uri=getIntent().getData();
		if(uri==null || isTaskRoot()){
			finish();
			return;
		}
		if(uri.getQueryParameter("error")!=null){
			String error=uri.getQueryParameter("error_description");
			if(TextUtils.isEmpty(error))
				error=uri.getQueryParameter("error");
			Toast.makeText(this, error, Toast.LENGTH_LONG).show();
			finish();
			restartMainActivity();
			return;
		}
		String code=uri.getQueryParameter("code");
		if(TextUtils.isEmpty(code)){
			finish();
			return;
		}
		Instance instance= AccountSessionManager.getInstance().getAuthenticatingInstance();
		Application app=AccountSessionManager.getInstance().getAuthenticatingApp();
		if(instance==null || app==null){
			finish();
			return;
		}
		ProgressDialog progress=AccentProgressDialog.show(this,getString(R.string.verifying_login));
		progress.setCancelable(false);
		new GetOauthToken(app.clientId, app.clientSecret, code, GetOauthToken.GrantType.AUTHORIZATION_CODE)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Token token){
						new GetOwnAccount()
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(Account account){
										AccountSessionManager.getInstance().addAccount(instance, token, account, app, null);
										InitAccountAfterLogin.init(OAuthActivity.this, new Callback() {
											@Override
											public void onSuccess(Object result) {
												progress.dismiss();
												finish();
												// not calling restartMainActivity() here on purpose to have it recreated (notice different flags)
												Intent intent=new Intent(OAuthActivity.this, MainActivity.class);
												intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
												startActivity(intent);
											}

											@Override
											public void onError(ErrorResponse error) {

											}
										});

									}

									@Override
									public void onError(ErrorResponse error){
										handleError(error);
										progress.dismiss();
									}
								})
								.exec(instance.uri, token);
					}

					@Override
					public void onError(ErrorResponse error){
						handleError(error);
						progress.dismiss();
					}
				})
				.execNoAuth(instance.uri);
	}

	private void handleError(ErrorResponse error){
		error.showToast(OAuthActivity.this);
		finish();
		restartMainActivity();
	}

	private void restartMainActivity(){
		Intent intent=new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}
}
