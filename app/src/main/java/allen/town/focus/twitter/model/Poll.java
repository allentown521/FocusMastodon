package allen.town.focus.twitter.model;

import allen.town.focus.twitter.api.ObjectValidationException;
import allen.town.focus.twitter.api.RequiredField;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Parcel
public class Poll extends BaseModel implements Serializable {
	@RequiredField
	public String id;
	public Instant expiresAt;
	public boolean expired;
	public boolean multiple;
	public int votersCount;
	public int votesCount;
	public boolean voted;
	@RequiredField
	public List<Integer> ownVotes;
	@RequiredField
	public List<Option> options;
	@RequiredField
	public List<Emoji> emojis;

	public transient ArrayList<Option> selectedOptions;

	public Poll(){

	}
	@ParcelConstructor
	public Poll(String id, Instant expiresAt, boolean expired, boolean multiple, int votersCount, int votesCount, boolean voted, List<Integer> ownVotes, List<Option> options, List<Emoji> emojis) {
		this.id = id;
		this.expiresAt = expiresAt;
		this.expired = expired;
		this.multiple = multiple;
		this.votersCount = votersCount;
		this.votesCount = votesCount;
		this.voted = voted;
		this.ownVotes = ownVotes;
		this.options = options;
		this.emojis = emojis;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(Emoji e:emojis)
			e.postprocess();
	}

	@Override
	public String toString(){
		return "Poll{"+
				"id='"+id+'\''+
				", expiresAt="+expiresAt+
				", expired="+expired+
				", multiple="+multiple+
				", votersCount="+votersCount+
				", votesCount="+votesCount+
				", voted="+voted+
				", ownVotes="+ownVotes+
				", options="+options+
				", emojis="+emojis+
				", selectedOptions="+selectedOptions+
				'}';
	}

	public boolean isExpired(){
		return expired || (expiresAt!=null && expiresAt.isBefore(Instant.now()));
	}

	@Parcel
	public static class Option implements Serializable{
		public String title;
		public Integer votesCount;

		@Override
		public String toString(){
			return "Option{"+
					"title='"+title+'\''+
					", votesCount="+votesCount+
					'}';
		}
	}
}
