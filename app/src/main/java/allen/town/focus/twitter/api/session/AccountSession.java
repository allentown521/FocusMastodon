package allen.town.focus.twitter.api.session;

import allen.town.focus.twitter.api.MastodonAPIController;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.Application;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.PushSubscription;
import allen.town.focus.twitter.model.Token;

import java.util.ArrayList;
import java.util.List;

public class AccountSession{
	public Token token;
	public Account self;
	public String domain;
	public Application app;
	public long infoLastUpdated;
	public boolean activated=true;
	public String pushPrivateKey;
	public String pushPublicKey;
	public String pushAuthKey;
	public PushSubscription pushSubscription;
	public boolean needUpdatePushSettings;
	public long filtersLastUpdated;
	public List<Filter> wordFilters=new ArrayList<>();
	public String pushAccountID;
	public AccountActivationInfo activationInfo;
	private transient MastodonAPIController apiController;

	AccountSession(Token token, Account self, Application app, String domain, boolean activated, AccountActivationInfo activationInfo){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.activated=activated;
		this.activationInfo=activationInfo;
		infoLastUpdated=System.currentTimeMillis();
	}

	AccountSession(){}

	public String getID(){
		return domain+"_"+self.id;
	}

	public MastodonAPIController getApiController(){
		if(apiController==null)
			apiController=new MastodonAPIController(this);
		return apiController;
	}



}
