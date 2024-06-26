package allen.town.focus.twitter.services.abstract_services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public abstract class KillerIntentService extends IntentService {

    private static final long TIMEOUT = 5 * 60 * 1000; // 5 mins

    protected String name;

    public KillerIntentService(String name) {
        super(name);
        this.name = name;
    }

    protected abstract void handleIntent(Intent intent);

    @Override
    public void onHandleIntent(Intent intent) {

        // activity sometimes get stuck and burns though data... I have not been able to find out why.
        // So, lets kill the process if it takes longer than 5 mins
        Thread killer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIMEOUT);

                    Log.v("Focus_for_Mastodon_killer", "activity refresh killed. What is the issue here...?");


                    android.os.Process.killProcess(android.os.Process.myPid());
                } catch (InterruptedException e) {

                }
            }
        });

        killer.start();

        handleIntent(intent);

        // stop the killer from destroying the app
        killer.interrupt();
    }
}
