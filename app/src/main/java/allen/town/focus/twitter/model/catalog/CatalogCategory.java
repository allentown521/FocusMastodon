package allen.town.focus.twitter.model.catalog;

import java.io.Serializable;

import allen.town.focus.twitter.api.AllFieldsAreRequired;
import allen.town.focus.twitter.model.BaseModel;

@AllFieldsAreRequired
public class CatalogCategory extends BaseModel implements Serializable {
	public String category;
	public int serversCount;

	@Override
	public String toString(){
		return "CatalogCategory{"+
				"category='"+category+'\''+
				", serversCount="+serversCount+
				'}';
	}
}
