package allen.town.focus.twitter.services.background_refresh;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.ActivityUtils;
import allen.town.focus.twitter.utils.Utils;

public class SecondActivityRefreshService extends Worker {

    private final Context context;
    public SecondActivityRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(SecondActivityRefreshService.class).build());
    }

    @NonNull
    @Override
    public Result doWork() {
        AppSettings settings = AppSettings.getInstance(context);
        ActivityUtils utils = new ActivityUtils(context, true);

        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return Result.success();
        }

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification(ActivityUtils.SECOND_NOTIFICATION_ID);
        }

        return Result.success();
    }
}
