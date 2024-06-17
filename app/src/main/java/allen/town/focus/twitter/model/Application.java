package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.RequiredField;
import org.parceler.Parcel;

import java.io.Serializable;

@Parcel
public class Application extends BaseModel implements Serializable {
	@RequiredField
	public String name;
	public String website;
	public String vapidKey;
	public String clientId;
	public String clientSecret;

	@Override
	public String toString(){
		return "Application{"+
				"name='"+name+'\''+
				", website='"+website+'\''+
				", vapidKey='"+vapidKey+'\''+
				", clientId='"+clientId+'\''+
				", clientSecret='"+clientSecret+'\''+
				'}';
	}
}
