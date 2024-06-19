package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.AllFieldsAreRequired;
import org.parceler.Parcel;

import java.io.Serializable;

@AllFieldsAreRequired
@Parcel
public class Mention extends BaseModel implements Serializable {
	public String id;
	public String username;
	public String acct;
	public String url;

	@Override
	public String toString(){
		return "Mention{"+
				"id='"+id+'\''+
				", username='"+username+'\''+
				", acct='"+acct+'\''+
				", url='"+url+'\''+
				'}';
	}
}
