package allen.town.focus.twitter.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;
import org.parceler.ParcelProperty;

import java.io.Serializable;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;
import allen.town.focus.twitter.utils.BlurHashDecoder;
import allen.town.focus.twitter.utils.BlurHashDrawable;

@Parcel
public class Attachment extends BaseModel implements Serializable {
	@RequiredField
	public String id;
	@RequiredField
	public Type type;
	@RequiredField
	public String url;
	public String previewUrl;
	public String remoteUrl;
	public String description;
	@ParcelProperty("blurhash")
	public String blurhash;
	public Metadata meta;

	public transient Drawable blurhashPlaceholder;

	public Attachment(){}

	@ParcelConstructor
	public Attachment(@ParcelProperty("blurhash") String blurhash){
		this.blurhash=blurhash;
		if(blurhash!=null){
			Bitmap placeholder=BlurHashDecoder.decode(blurhash, 16, 16);
			if(placeholder!=null)
				blurhashPlaceholder=new BlurHashDrawable(placeholder, getWidth(), getHeight());
		}
	}

	public int getWidth(){
		if(meta==null)
			return 1920;
		if(meta.width>0)
			return meta.width;
		if(meta.original!=null && meta.original.width>0)
			return meta.original.width;
		if(meta.small!=null && meta.small.width>0)
			return meta.small.width;
		return 1920;
	}

	public int getHeight(){
		if(meta==null)
			return 1080;
		if(meta.height>0)
			return meta.height;
		if(meta.original!=null && meta.original.height>0)
			return meta.original.height;
		if(meta.small!=null && meta.small.height>0)
			return meta.small.height;
		return 1080;
	}

	public double getDuration(){
		if(meta==null)
			return 0;
		if(meta.duration>0)
			return meta.duration;
		if(meta.original!=null && meta.original.duration>0)
			return meta.original.duration;
		return 0;
	}

	@Override
	public void postprocess() throws ObjectValidationException {
		super.postprocess();
		if(blurhash!=null){
			Bitmap placeholder= BlurHashDecoder.decode(blurhash, 16, 16);
			if(placeholder!=null)
				blurhashPlaceholder=new BlurHashDrawable(placeholder, getWidth(), getHeight());
		}
	}

	@Override
	public String toString(){
		return "Attachment{"+
				"id='"+id+'\''+
				", type="+type+
				", url='"+url+'\''+
				", previewUrl='"+previewUrl+'\''+
				", remoteUrl='"+remoteUrl+'\''+
				", description='"+description+'\''+
				", blurhash='"+blurhash+'\''+
				", meta="+meta+
				'}';
	}

	public enum Type{
		@SerializedName("image")
		IMAGE,
		@SerializedName("gifv")
		GIFV,
		@SerializedName("video")
		VIDEO,
		@SerializedName("audio")
		AUDIO,
		@SerializedName("unknown")
		UNKNOWN;

		public boolean isImage(){
			return this==IMAGE || this==GIFV || this==VIDEO;
		}
	}

	@Parcel
	public static class Metadata implements Serializable{
		public double duration;
		public int width;
		public int height;
		public double aspect;
		//不能序列化，注释掉
//		public PointF focus;
		public SizeMetadata original;
		public SizeMetadata small;

		@Override
		public String toString(){
			return "Metadata{"+
					"duration="+duration+
					", width="+width+
					", height="+height+
					", aspect="+aspect+
					", original="+original+
					", small="+small+
					'}';
		}
	}

	@Parcel
	public static class SizeMetadata implements Serializable{
		public int width;
		public int height;
		public double aspect;
		public double duration;
		public int bitrate;

		@Override
		public String toString(){
			return "SizeMetadata{"+
					"width="+width+
					", height="+height+
					", aspect="+aspect+
					", duration="+duration+
					", bitrate="+bitrate+
					'}';
		}
	}
}
