package twitter4j;

import java.util.List;

import allen.town.focus.twitter.model.Attachment;
import allen.town.focus.twitter.model.Hashtag;
import allen.town.focus.twitter.model.Mention;

/*package*/ public class EntitiesParseUtil {

    /*package*/
    public static UserMentionEntity[] getUserMentionsMastodon(List<Mention> entities) throws JSONException {
        if (entities != null) {
            int len = entities.size();
            UserMentionEntity[] userMentionEntities = new UserMentionEntity[len];
            for (int i = 0; i < len; i++) {
                userMentionEntities[i] = new UserMentionEntityJSONImplMastodon(entities.get(i));
            }
            return userMentionEntities;
        } else {
            return null;
        }
    }


    /*package*/
    /*package*/

    public static HashtagEntity[] getHashtagsMastodon(List<Hashtag> entities) throws JSONException, Exception {
        if(entities == null){
            return null;
        }
        int len = entities.size();
        HashtagEntity[] hashtagEntities = new HashtagEntity[len];
        for (int i = 0; i < len; i++) {
            hashtagEntities[i] = new HashtagEntityJSONImplMastodon(entities.get(i));
        }
        return hashtagEntities;
    }

    /*package*/

    /*package*/
    static MediaEntity[] getMedia(List<Attachment> entities) throws JSONException, Exception {
        int len = entities.size();
        MediaEntity[] mediaEntities = new MediaEntity[len];
        for (int i = 0; i < len; i++) {
            mediaEntities[i] = new MediaEntityJSONImplMastodon(entities.get(i));
        }
        return mediaEntities;
    }
}
