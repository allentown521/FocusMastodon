package allen.town.focus.twitter.event;


import allen.town.focus.twitter.model.Status;

public class StatusCountersUpdatedEvent {
	public String id;
	public long favorites, reblogs, replies;
	public boolean favorited, reblogged, bookmarked;

	public StatusCountersUpdatedEvent(Status s){
		id=s.id;
		favorites=s.favouritesCount;
		reblogs=s.reblogsCount;
		replies=s.repliesCount;
		favorited=s.favourited;
		reblogged=s.reblogged;
		bookmarked=s.bookmarked;
	}
}
