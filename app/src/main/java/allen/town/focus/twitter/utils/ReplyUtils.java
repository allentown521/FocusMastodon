package allen.town.focus.twitter.utils;

import java.util.List;

import allen.town.focus.twitter.model.ReTweeterAccount;
import twitter4j.UserMentionEntity;

public class ReplyUtils {

    private static int MAX_REPLY_LENGTH = 3;

    public static boolean showMultipleReplyNames(String replies) {
        return replies.split(" ").length < MAX_REPLY_LENGTH;
    }

    public static String getReplyingToHandles(String text) {
        String handles = "";
        String[] split = text.split(" ");
        for (int i = 0; i < split.length; i++) {
            if (split[i].contains("@") && !split[i].contains(".@")) {
                handles += split[i] + " ";
            }
        }

        return handles;
    }

    public static String getReplyingNamesToHandles(List<UserMentionEntity> userMentionEntityList) {
        String handles = "";
        int count = 0;
        if (userMentionEntityList != null && userMentionEntityList.size() > 0) {
            for (int i = 0; i < userMentionEntityList.size(); i++) {
                if (count == MAX_REPLY_LENGTH) {
                    break;
                }

                handles += ReTweeterAccount.getRetweeterFormatUrl(userMentionEntityList.get(i).getName(), userMentionEntityList.get(i).getId() + "") + " ";
                count++;
            }
        }


        return handles;
    }
}
