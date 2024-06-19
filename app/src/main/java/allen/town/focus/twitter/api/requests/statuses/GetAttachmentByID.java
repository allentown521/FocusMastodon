package allen.town.focus.twitter.api.requests.statuses;


import java.io.IOException;

import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.model.Attachment;
import okhttp3.Response;

public class GetAttachmentByID extends MastodonAPIRequest<Attachment>{
	public GetAttachmentByID(String id){
		super(MastodonAPIRequest.HttpMethod.GET, "/media/"+id, Attachment.class);
	}

	@Override
	public void validateAndPostprocessResponse(Attachment respObj, Response httpResponse) throws IOException{
		if(httpResponse.code()==206)
			respObj.url="";
		super.validateAndPostprocessResponse(respObj, httpResponse);
	}
}
