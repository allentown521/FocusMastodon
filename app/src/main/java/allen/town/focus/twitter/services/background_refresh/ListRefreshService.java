package allen.town.focus.twitter.services.background_refresh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.adapters.TimelinePagerAdapter;
import allen.town.focus.twitter.api.requests.timelines.GetListTimeline;
import allen.town.focus.twitter.data.sq_lite.ListDataSource;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus_common.util.Timber;
import twitter4j.StatusJSONImplMastodon;

public class ListRefreshService extends Worker {

    private final Context context;

    public ListRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "list-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(ListRefreshService.class).build());
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.syncInterval / 1000; // convert to seconds

        if (settings.syncInterval != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(ListRefreshService.class, refreshInterval, TimeUnit.SECONDS)
                            .setConstraints(new Constraints.Builder()
                                    .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                    .build())
                            .build();
            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
        }
    }

    private int COUNT_PER_PAGE = 40;

    @NonNull
    @Override
    public Result doWork() {
        if (!MainActivity.canSwitch || WidgetRefreshService.isRunning || ListRefreshService.isRunning) {
            return Result.success();
        }

        sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);

        List<Long> listIds = new ArrayList<>();

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_LIST_TIMELINE) {
                listIds.add(sharedPrefs.getLong(listIdentifier, 0L));
            }
        }

        for (Long listId : listIds) {
            if (MainActivity.canSwitch) {
                Timber.v("refreshing list: " + listId);

                ListRefreshService.isRunning = true;
                long sinceId = 0;
                Context context = getApplicationContext();
                AppSettings settings = AppSettings.getInstance(context);


                long[] lastId = ListDataSource.getInstance(context).getLastIds(listId);

                final List<twitter4j.Status> statuses = new ArrayList<>();

                boolean foundStatus = false;


                if (lastId[0] > 0) {
                    sinceId = lastId[0];
                }

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {

                    try {
                        if (!foundStatus) {

                            HeaderPaginationList<StatusJSONImplMastodon> list = StatusJSONImplMastodon.createStatusList(
                                    new GetListTimeline(listId + "", null, null, COUNT_PER_PAGE, sinceId + "")
                                            .execSync());
                            sinceId = list.get(0).getId();

                            statuses.addAll(list);

                            if (list.size() < COUNT_PER_PAGE) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // the page doesn't exist
                        foundStatus = true;
                    } catch (OutOfMemoryError o) {
                        // don't know why...
                    }
                }

                ListDataSource dataSource = ListDataSource.getInstance(context);
                dataSource.insertTweets(statuses, listId);

                sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME_LIST_STARTER + listId, true).commit();
                context.sendBroadcast(new Intent(IntentConstant.LIST_REFRESHED_ACTION + listId));
            }

            ListRefreshService.isRunning = false;
        }

        return Result.success();
    }
}