package allen.town.focus.twitter.api.session;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.MastodonAPIController;
import allen.town.focus.twitter.api.requests.accounts.GetOwnAccount;
import allen.town.focus.twitter.api.requests.accounts.GetWordFilters;
import allen.town.focus.twitter.api.requests.oauth.CreateOAuthApp;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Account;
import allen.town.focus.twitter.model.Application;
import allen.town.focus.twitter.model.Emoji;
import allen.town.focus.twitter.model.EmojiCategory;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Instance;
import allen.town.focus.twitter.model.Token;
import allen.town.focus.twitter.settings.AppSettings;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountSessionManager {
    private static final String TAG = "AccountSessionManager";
    public static final String SCOPE = "read write follow push";
    public static final String REDIRECT_URI = "focus-mastodon-android-auth://callback";

    private static final AccountSessionManager instance = new AccountSessionManager();

    private HashMap<String, AccountSession> sessions = new HashMap<>();
    private HashMap<String, List<EmojiCategory>> customEmojis = new HashMap<>();
    private HashMap<String, Long> instancesLastUpdated = new HashMap<>();
    private HashMap<String, Instance> instances = new HashMap<>();
    private MastodonAPIController unauthenticatedApiController = new MastodonAPIController(null);
    private Instance authenticatingInstance;
    private Application authenticatingApp;
    private String lastActiveAccountID;
    private SharedPreferences prefs;
    private boolean loadedInstances;

    public static AccountSessionManager getInstance() {
        return instance;
    }

    private AccountSessionManager() {
        prefs = AppSettings.getSharedPreferences(App.getInstance());
        File file = new File(App.getInstance().getFilesDir(), "accounts.json");
        if (!file.exists())
            return;
        HashSet<String> domains = new HashSet<>();
        try (FileInputStream in = new FileInputStream(file)) {
            SessionsStorageWrapper w = MastodonAPIController.gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), SessionsStorageWrapper.class);
            for (AccountSession session : w.accounts) {
                domains.add(session.domain.toLowerCase());
                sessions.put(session.getID(), session);
            }
        } catch (Exception x) {
            Log.e(TAG, "Error loading accounts", x);
        }
        lastActiveAccountID = prefs.getString("lastActiveAccount", null);
        MastodonAPIController.runInBackground(() -> readInstanceInfo(domains));
//		maybeUpdateShortcuts();
    }

    private File getInstanceInfoFile(String domain) {
        return new File(App.getInstance().getFilesDir(), "instance_" + domain.replace('.', '_') + ".json");
    }

    private void readInstanceInfo(Set<String> domains) {
        for (String domain : domains) {
            try (FileInputStream in = new FileInputStream(getInstanceInfoFile(domain))) {
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                InstanceInfoStorageWrapper emojis = MastodonAPIController.gson.fromJson(reader, InstanceInfoStorageWrapper.class);
//				customEmojis.put(domain, groupCustomEmojis(emojis));
                instances.put(domain, emojis.instance);
                instancesLastUpdated.put(domain, emojis.lastUpdated);
            } catch (Exception x) {
                Log.w(TAG, "Error reading instance info file for " + domain, x);
            }
        }
        if (!loadedInstances) {
            loadedInstances = true;
//			maybeUpdateCustomEmojis(domains);
        }
    }

    public Instance getAuthenticatingInstance() {
        return authenticatingInstance;
    }

    public Application getAuthenticatingApp() {
        return authenticatingApp;
    }

    public void authenticate(Activity activity, Instance instance) {
        authenticatingInstance = instance;
        new CreateOAuthApp()
                .setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(Application result) {
                        authenticatingApp = result;
                        Uri uri = new Uri.Builder()
                                .scheme("https")
                                .authority(instance.uri)
                                .path("/oauth/authorize")
                                .appendQueryParameter("response_type", "code")
                                .appendQueryParameter("client_id", result.clientId)
                                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                                .appendQueryParameter("scope", SCOPE)
                                .build();

                        new CustomTabsIntent.Builder()
                                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                                .setShowTitle(true)
                                .build()
                                .launchUrl(activity, uri);
                    }

                    @Override
                    public void onError(ErrorResponse error) {
                        error.showToast(activity);
                    }
                })
                .wrapProgress(activity, R.string.verifying_login, false)
                .execNoAuth(instance.uri);
    }

    public void removeAccount(String id) {
        sessions.remove(id);
        if (lastActiveAccountID.equals(id)) {
            if (sessions.isEmpty())
                lastActiveAccountID = null;
            else
                lastActiveAccountID = getLoggedInAccounts().get(0).getID();
        }
        writeAccountsFile();
//		String domain=session.domain.toLowerCase();
//		if(sessions.isEmpty() || !sessions.values().stream().map(s->s.domain.toLowerCase()).collect(Collectors.toSet()).contains(domain)){
//			getInstanceInfoFile(domain).delete();
//		}
//		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
//			NotificationManager nm=MastodonApp.context.getSystemService(NotificationManager.class);
//			nm.deleteNotificationChannelGroup(id);
//		}
    }

    public void addAccount(Instance instance, Token token, Account self, Application app, AccountActivationInfo activationInfo) {
        instances.put(instance.uri, instance);
        AccountSession session = new AccountSession(token, self, app, instance.uri, activationInfo == null, activationInfo);
        sessions.put(session.getID(), session);
        lastActiveAccountID = session.getID();
        writeAccountsFile();
/*		updateInstanceEmojis(instance, instance.uri);
		if(PushSubscriptionManager.arePushNotificationsAvailable()){
			session.getPushSubscriptionManager().registerAccountForPush(null);
		}
		maybeUpdateShortcuts();*/
        SharedPreferences.Editor e = prefs.edit();

        if (prefs.getInt(AppSettings.CURRENT_ACCOUNT, 1) == 1) {
            e.putBoolean("is_logged_in_1", true);
            e.putString("twitter_users_name_1", self.displayName).commit();
            e.putString("twitter_screen_name_1", self.acct).commit();
            e.putString("twitter_background_url_1", self.headerStatic).commit();
            e.putString("profile_pic_url_1", self.avatarStatic).commit();
            e.putString("twitter_id_1", self.id).commit();
            e.putString("session_id_1", session.getID()).commit();
        } else {
            e.putBoolean("is_logged_in_2", true);
            e.putString("twitter_users_name_2", self.displayName).commit();
            e.putString("twitter_screen_name_2", self.acct).commit();
            e.putString("twitter_background_url_2", self.headerStatic).commit();
            e.putString("profile_pic_url_2", self.avatarStatic).commit();
            e.putString("twitter_id_2", self.id).commit();
            e.putString("session_id_2", session.getID()).commit();
        }

        e.commit(); // save changes

        AppSettings.invalidate();
    }

    public synchronized void writeAccountsFile() {
        File file = new File(App.getInstance().getFilesDir(), "accounts.json");
        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                SessionsStorageWrapper w = new SessionsStorageWrapper();
                w.accounts = new ArrayList<>(sessions.values());
                OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                MastodonAPIController.gson.toJson(w, writer);
                writer.flush();
            }
        } catch (IOException x) {
            Log.e(TAG, "Error writing accounts file", x);
        }
        prefs.edit().putString("lastActiveAccount", lastActiveAccountID).commit();
    }

    @NonNull
    public List<AccountSession> getLoggedInAccounts() {
        return new ArrayList<>(sessions.values());
    }

    @NonNull
    public AccountSession getAccount(String id) {
        AccountSession session = sessions.get(id);
        if (session == null)
            throw new IllegalStateException("Account session " + id + " not found");
        return session;
    }

    @Nullable
    public AccountSession tryGetAccount(String id) {
        return sessions.get(id);
    }

    @Nullable
    public AccountSession getLastActiveAccount() {
        if (sessions.isEmpty() || lastActiveAccountID == null)
            return null;
        if (!sessions.containsKey(lastActiveAccountID)) {
            // TODO figure out why this happens. It should not be possible.
            lastActiveAccountID = getLoggedInAccounts().get(0).getID();
            writeAccountsFile();
        }
        return getAccount(lastActiveAccountID);
    }

    public String getLastActiveAccountID() {
        return lastActiveAccountID;
    }

    public void setLastActiveAccountID(String id) {
        if (!sessions.containsKey(id))
            throw new IllegalStateException("Account session " + id + " not found");
        lastActiveAccountID = id;
        prefs.edit().putString("lastActiveAccount", id).commit();
    }


    @NonNull
    public MastodonAPIController getUnauthenticatedApiController() {
        return unauthenticatedApiController;
    }


    /**
     * 更新filter和个人信息
     */
    public void maybeUpdateLocalInfo() {
        long now = System.currentTimeMillis();
        HashSet<String> domains = new HashSet<>();
        for (AccountSession session : sessions.values()) {
            domains.add(session.domain.toLowerCase());
            if (now - session.infoLastUpdated > 24L * 3600_000L) {
                updateSessionLocalInfo(session);
            }
//            if (now - session.filtersLastUpdated > 3600_000L) {
                updateSessionWordFilters(session);
//            }
        }
        if (loadedInstances) {
//			maybeUpdateCustomEmojis(domains);
        }
    }

    private void updateSessionLocalInfo(AccountSession session) {
        new GetOwnAccount()
                .setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(Account result) {
                        session.self = result;
                        session.infoLastUpdated = System.currentTimeMillis();
                        writeAccountsFile();
                    }

                    @Override
                    public void onError(ErrorResponse error) {

                    }
                })
                .exec(session.getID());
    }

    private void updateSessionWordFilters(AccountSession session) {
        new GetWordFilters()
                .setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(List<Filter> result) {
                        session.wordFilters = result;
                        session.filtersLastUpdated = System.currentTimeMillis();
                        writeAccountsFile();
                    }

                    @Override
                    public void onError(ErrorResponse error) {

                    }
                })
                .exec(session.getID());
    }


    public Instance getInstanceInfo(String domain) {
        return instances.get(domain);
    }

    public void updateAccountInfo(String id, Account account) {
        AccountSession session = getAccount(id);
        session.self = account;
        session.infoLastUpdated = System.currentTimeMillis();
        writeAccountsFile();
    }


    private static class SessionsStorageWrapper {
        public List<AccountSession> accounts;
    }

    private static class InstanceInfoStorageWrapper {
        public Instance instance;
        public List<Emoji> emojis;
        public long lastUpdated;
    }
}
