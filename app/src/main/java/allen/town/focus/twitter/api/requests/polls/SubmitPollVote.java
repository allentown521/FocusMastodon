package allen.town.focus.twitter.api.requests.polls;


import java.util.List;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Poll;

public class SubmitPollVote extends MastodonAPIRequest<Poll> {
	public SubmitPollVote(String pollID, List<Integer> choices){
		super(HttpMethod.POST, "/polls/"+pollID+"/votes", Poll.class);
		setRequestBody(new Body(choices));
	}

	private static class Body{
		public List<Integer> choices;

		public Body(List<Integer> choices){
			this.choices=choices;
		}
	}
}
