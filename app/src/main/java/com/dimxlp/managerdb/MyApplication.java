package com.dimxlp.managerdb;

import android.app.Application;
import com.onesignal.OneSignal;

public class MyApplication extends Application {
    private static final String ONESIGNAL_APP_ID = "5c64ce39-c602-4bde-beb7-deaa9a6fa8cf";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose OneSignal logging for debugging
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // Initialize OneSignal
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
    }
}
