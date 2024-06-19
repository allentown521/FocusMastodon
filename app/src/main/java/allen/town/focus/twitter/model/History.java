package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.AllFieldsAreRequired;
import org.parceler.Parcel;

import java.io.Serializable;

@AllFieldsAreRequired
@Parcel
public class History extends BaseModel implements Serializable {
	public long day; // unixtime
	public int uses;
	public int accounts;

	@Override
	public String toString(){
		return "History{"+
				"day="+day+
				", uses="+uses+
				", accounts="+accounts+
				'}';
	}
}
