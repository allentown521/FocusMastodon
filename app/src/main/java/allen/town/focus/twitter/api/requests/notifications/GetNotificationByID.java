package allen.town.focus.twitter.api.requests.notifications;


import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Notification;

public class GetNotificationByID extends MastodonAPIRequest<Notification> {
	public GetNotificationByID(String id){
		super(HttpMethod.GET, "/notifications/"+id, Notification.class);
	}
}
