package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.RequiredField;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.io.Serializable;

/**
 * Represents a custom emoji.
 */
@Parcel
public class Emoji extends BaseModel implements Serializable {
	/**
	 * The name of the custom emoji.
	 */
	@RequiredField
	public String shortcode;
	/**
	 * A link to the custom emoji.
	 */
	@RequiredField
	public String url;
	/**
	 * A link to a static copy of the custom emoji.
	 */
	@RequiredField
	public String staticUrl;
	/**
	 * Whether this Emoji should be visible in the picker or unlisted.
	 */
	@RequiredField
	public boolean visibleInPicker;
	/**
	 * Used for sorting custom emoji in the picker.
	 */
	public String category;

	public Emoji(){

	}
	@ParcelConstructor
	public Emoji(String shortcode, String url, String staticUrl, boolean visibleInPicker, String category) {
		this.shortcode = shortcode;
		this.url = url;
		this.staticUrl = staticUrl;
		this.visibleInPicker = visibleInPicker;
		this.category = category;
	}

	@Override
	public String toString(){
		return "Emoji{"+
				"shortcode='"+shortcode+'\''+
				", url='"+url+'\''+
				", staticUrl='"+staticUrl+'\''+
				", visibleInPicker="+visibleInPicker+
				", category='"+category+'\''+
				'}';
	}
}
