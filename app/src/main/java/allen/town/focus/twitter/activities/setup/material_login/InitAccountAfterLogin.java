package allen.town.focus.twitter.activities.setup.material_login;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.SearchRecentSuggestions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.api.requests.accounts.GetAccountByAcct;
import allen.town.focus.twitter.api.requests.accounts.GetAccountFollowers;
import allen.town.focus.twitter.api.requests.accounts.SetAccountFollowed;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.api.requests.timelines.GetHomeTimeline;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.data.sq_lite.FollowersDataSource;
import allen.town.focus.twitter.data.sq_lite.HomeDataSource;
import allen.town.focus.twitter.data.sq_lite.MentionsDataSource;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.MySuggestionsProvider;
import allen.town.focus.twitter.utils.ServiceUtils;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus_common.util.Timber;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;

public class InitAccountAfterLogin {

    public static void init(Context context, Callback callback) {
        Observable.just(0).subscribeOn(Schedulers.io()).subscribe(new Observer<>() {
            @Override
            public void onCompleted() {
                ServiceUtils.rescheduleAllServices(context);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                try {
                    SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(App.getInstance());
                    AppSettings appSettings = AppSettings.getInstance(App.getInstance());

                    //在不用的服务器上获取到的accountId不一样
                    new GetAccountByAcct("allentown@mastodon.social").setCallback(new Callback<>() {
                        @Override
                        public void onSuccess(Account result) {
                            //关注allentown@mostodon.social
                            new SetAccountFollowed(result.id, true, true).exec();
                        }

                        @Override
                        public void onError(ErrorResponse error) {

                        }
                    }).exec();


                    //获取home timeline
                    HeaderPaginationList<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(new GetHomeTimeline(null, null, 40, null).execSync());

                    if (statuses.size() > 0) {
                        sharedPrefs.edit().putLong("last_tweet_id_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), statuses.get(0).getId()).commit();
                    }

                    HomeDataSource dataSource = HomeDataSource.getInstance(context);

                    List<StatusJSONImplMastodon> filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.HOME)).collect(Collectors.toList());

                    for (StatusJSONImplMastodon status : filteredList) {
                        try {
                            dataSource.createTweet(status, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));
                        } catch (Exception e) {
                            dataSource = HomeDataSource.getInstance(context);
                            dataSource.createTweet(status, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));
                        }
                    }


                    //sync mentions
                    List<Notification> list;
                    list = new GetNotifications("", "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();

                    List<StatusJSONImplMastodon> mentionStatuses = new ArrayList<>();
                    if (list != null && list.size() > 0) {
                        for (Notification noti :
                                list) {
                            if (noti.status != null) {
                                mentionStatuses.add(new StatusJSONImplMastodon(noti.status));
                            }
                        }
                    }

                    List<StatusJSONImplMastodon> filteredMentionList = mentionStatuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                    MentionsDataSource mentionsSource = MentionsDataSource.getInstance(context);
                    for (twitter4j.Status status : filteredMentionList) {
                        try {
                            mentionsSource.createTweet(status, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));
                        } catch (Exception e) {
                            mentionsSource = MentionsDataSource.getInstance(context);
                            mentionsSource.createTweet(status, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));
                        }
                    }


                    //sync followings
                    try {
                        int pageCount = 0;
                        String nextMaxId = null;
                        HeaderPaginationList<Account> paging;
                        HeaderPaginationList<Account> friendsPaging = new HeaderPaginationList<>();
                        do {
                            pageCount++;
                            paging = new GetAccountFollowers(appSettings.myId, nextMaxId, 80).execSync();
                            if (paging.hasNext()) {
                                nextMaxId = paging.getNextCursor();
                            } else {
                                nextMaxId = null;
                            }
                            friendsPaging.addAll(paging);
                        } while (nextMaxId != null && pageCount < 2);
                        HeaderPaginationList<User> userPagableResponseList = UserJSONImplMastodon.createPagableUserList(friendsPaging);
                        FollowersDataSource followers = FollowersDataSource.getInstance(App.getInstance());

                        final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(App.getInstance(),
                                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

                        for (User friend : userPagableResponseList) {
                            followers.createUser(friend, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));

                            // insert them into the suggestion search provider
                            suggestions.saveRecentQuery(
                                    "@" + friend.getScreenName(),
                                    null);
                        }
                    } catch (Exception e) {
                        Timber.e("failed to get followers", e);
                    }

                } catch (Exception e) {
                    // Error in updating status
                    Timber.e("Twitter Update Error", e);

                }
            }
        });

    }


}
