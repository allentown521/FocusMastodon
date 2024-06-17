package allen.town.focus.twitter.utils;

import android.content.Context;

import allen.town.focus.twitter.services.background_refresh.ActivityRefreshService;
import allen.town.focus.twitter.services.DataCheckService;
import allen.town.focus.twitter.services.background_refresh.DirectMessageRefreshService;
import allen.town.focus.twitter.services.background_refresh.ListRefreshService;
import allen.town.focus.twitter.services.background_refresh.MentionsRefreshService;
import allen.town.focus.twitter.services.SendQueueService;
import allen.town.focus.twitter.services.SendScheduledTweet;
import allen.town.focus.twitter.services.background_refresh.TimelineRefreshService;
import allen.town.focus.twitter.services.TrimDataService;

public class ServiceUtils {

    public static void rescheduleAllServices(Context context) {
        DataCheckService.scheduleRefresh(context);
        TimelineRefreshService.scheduleRefresh(context);
        TrimDataService.scheduleRefresh(context);
        MentionsRefreshService.scheduleRefresh(context);
        DirectMessageRefreshService.scheduleRefresh(context);
        ListRefreshService.scheduleRefresh(context);
        ActivityRefreshService.scheduleRefresh(context);
        SendScheduledTweet.scheduleNextRun(context);
        SendQueueService.scheduleRefresh(context);
    }
}
