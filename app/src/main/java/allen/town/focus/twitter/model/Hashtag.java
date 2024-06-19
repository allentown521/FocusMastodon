package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.RequiredField;
import org.parceler.Parcel;

import java.io.Serializable;
import java.util.List;

@Parcel
public class Hashtag extends BaseModel implements Serializable {
	@RequiredField
	public String name;
	@RequiredField
	public String url;
	public List<History> history;

	@Override
	public String toString(){
		return "Hashtag{"+
				"name='"+name+'\''+
				", url='"+url+'\''+
				", history="+history+
				'}';
	}
}
