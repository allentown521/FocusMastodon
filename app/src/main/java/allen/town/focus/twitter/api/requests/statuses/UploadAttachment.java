package allen.town.focus.twitter.api.requests.statuses;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;


import java.io.IOException;
import java.io.InputStream;

import allen.town.focus.twitter.api.ContentUriRequestBody;
import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.ProgressListener;
import allen.town.focus.twitter.api.ResizedImageRequestBody;
import allen.town.focus.twitter.model.Attachment;
import allen.town.focus.twitter.utils.UiUtils;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadAttachment extends MastodonAPIRequest<Attachment> {
    private Uri uri;
    private ProgressListener progressListener;
    private int maxImageSize;
    private String description;
    private Context context;

//	public UploadAttachment(Context context, Uri uri){
//		super(HttpMethod.POST, "/media", Attachment.class);
//		this.uri=uri;
//		this.context=context;
//	}

//	public UploadAttachment(Uri uri, int maxImageSize, String description){
//		this(uri);
//		this.maxImageSize=maxImageSize;
//		this.description=description;
//	}

    private InputStream inputStream;

    public UploadAttachment(InputStream inputStream, Uri uri) {
        //content开头的有权限问题，直接传进来inputStream就正常
        super(HttpMethod.POST, "/media", Attachment.class);
        this.uri = uri;
        this.inputStream = inputStream;
    }

    public UploadAttachment setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    @Override
    protected String getPathPrefix() {
        return "/api/v2";
    }

    @Override
    public void validateAndPostprocessResponse(Attachment respObj, Response httpResponse) throws IOException {
        if (respObj.url == null)
            respObj.url = "";
        super.validateAndPostprocessResponse(respObj, httpResponse);
    }

    @Override
    public RequestBody getRequestBody() throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", UiUtils.getFileName(uri), maxImageSize > 0 ? new ResizedImageRequestBody(uri, maxImageSize, progressListener) : new ContentUriRequestBody(inputStream, uri, progressListener));
        if (!TextUtils.isEmpty(description))
            builder.addFormDataPart("description", description);
        return builder.build();
    }
}
