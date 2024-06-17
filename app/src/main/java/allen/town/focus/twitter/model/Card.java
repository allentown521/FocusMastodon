package allen.town.focus.twitter.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.io.Serializable;
import java.util.List;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;
import allen.town.focus.twitter.utils.BlurHashDecoder;
import allen.town.focus.twitter.utils.BlurHashDrawable;

@Parcel
public class Card extends BaseModel implements Serializable {
	@RequiredField
	public String url;
	@RequiredField
	public String title;
	@RequiredField
	public String description;
	@RequiredField
	public Type type;
	public String authorName;
	public String authorUrl;
	public String providerName;
	public String providerUrl;
//	public String html;
	public int width;
	public int height;
	public String image;
	public String embedUrl;
	public String blurhash;
	public List<History> history;

	public transient Drawable blurhashPlaceholder;

	@Override
	public void postprocess() throws ObjectValidationException {
		super.postprocess();
		if(blurhash!=null){
			Bitmap placeholder= BlurHashDecoder.decode(blurhash, 16, 16);
			if(placeholder!=null)
				blurhashPlaceholder=new BlurHashDrawable(placeholder, width, height);
		}
	}

	@Override
	public String toString(){
		return "Card{"+
				"url='"+url+'\''+
				", title='"+title+'\''+
				", description='"+description+'\''+
				", type="+type+
				", authorName='"+authorName+'\''+
				", authorUrl='"+authorUrl+'\''+
				", providerName='"+providerName+'\''+
				", providerUrl='"+providerUrl+'\''+
				", width="+width+
				", height="+height+
				", image='"+image+'\''+
				", embedUrl='"+embedUrl+'\''+
				", blurhash='"+blurhash+'\''+
				", history="+history+
				'}';
	}

	public enum Type{
		@SerializedName("link")
		LINK,
		@SerializedName("photo")
		PHOTO,
		@SerializedName("video")
		VIDEO,
		@SerializedName("rich")
		RICH
	}
}
