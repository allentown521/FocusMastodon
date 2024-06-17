package allen.town.focus.twitter.model;


import java.io.Serializable;
import java.util.List;

import allen.town.focus.twitter.api.ObjectValidationException;

public class SearchResults extends BaseModel implements Serializable {
	public HeaderPaginationList<Account> accounts;
	public HeaderPaginationList<Status> statuses;
	public HeaderPaginationList<Hashtag> hashtags;

	@Override
	public void postprocess() throws ObjectValidationException {
		super.postprocess();
		if(accounts!=null){
			for(Account acc:accounts)
				acc.postprocess();
		}
		if(statuses!=null){
			for(Status s:statuses)
				s.postprocess();
		}
		if(hashtags!=null){
			for(Hashtag t:hashtags)
				t.postprocess();
		}
	}
}
