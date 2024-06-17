package allen.town.focus.twitter.api.requests.statuses;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Status;
import allen.town.focus.twitter.model.StatusPrivacy;
import twitter4j.StatusUpdate;

public class CreateStatus extends MastodonAPIRequest<Status> {
    public CreateStatus(Request req) {
        super(HttpMethod.POST, "/statuses", Status.class);
        setRequestBody(req);
        addHeader("Idempotency-Key", UUID.randomUUID().toString());
    }

    public static class Request {
        public String status;
        public List<String> mediaIds;
        public Poll poll;
        public String inReplyToId;
        public boolean sensitive;
        public String spoilerText;
        public StatusPrivacy visibility;
        public Instant scheduledAt;
        public String language;

        public static class Poll {
            public ArrayList<String> options = new ArrayList<>();
            public int expiresIn;
            public boolean multiple;
            public boolean hideTotals;
        }
    }

    public static Request parseStatusUpdate(StatusUpdate statusUpdate) {
        Request request = new Request();
        request.status = statusUpdate.getStatus();
        request.inReplyToId = statusUpdate.getInReplyToStatusId();
        if (statusUpdate.getMediaIds() != null && statusUpdate.getMediaIds().length > 0) {
            request.mediaIds = new ArrayList<>();
            for (Long mediaId :
                    statusUpdate.getMediaIds()) {
                request.mediaIds.add(mediaId + "");
            }
        }
        if (statusUpdate.getPoll() != null) {
            request.poll = statusUpdate.getPoll();
        }
        if (statusUpdate.getSpoilerText() != null) {
            request.spoilerText = statusUpdate.getSpoilerText();
        }
        if (statusUpdate.getVisibility() != null) {
            request.visibility = statusUpdate.getVisibility();
        }

        return request;
    }
}
