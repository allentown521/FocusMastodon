package allen.town.focus.twitter.services;

public class ReplySecondAccountFromWearService extends ReplyFromWearService {

    @Override
    protected int getAccountNumber() {
        if (super.getAccountNumber() == 1) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public boolean isSecondAccount() {
        return true;
    }
}
