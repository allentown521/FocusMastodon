package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.AllFieldsAreRequired;

import java.io.Serializable;
import java.time.Instant;

@AllFieldsAreRequired
public class Marker extends BaseModel implements Serializable {
	public String lastReadId;
	public long version;
	public Instant updatedAt;

	@Override
	public String toString(){
		return "Marker{"+
				"lastReadId='"+lastReadId+'\''+
				", version="+version+
				", updatedAt="+updatedAt+
				'}';
	}
}
