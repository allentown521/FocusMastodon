package allen.town.focus.twitter.api.session;

public class AccountActivationInfo{
	public String email;
	public long lastEmailConfirmationResend;

	public AccountActivationInfo(String email, long lastEmailConfirmationResend){
		this.email=email;
		this.lastEmailConfirmationResend=lastEmailConfirmationResend;
	}
}
