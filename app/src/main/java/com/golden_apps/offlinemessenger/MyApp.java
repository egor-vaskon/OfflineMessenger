package com.golden_apps.offlinemessenger;

import android.app.Application;

import com.golden_apps.offlinemessenger.io.WifiDirectManager;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        WifiDirectManager.initInstance(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        WifiDirectManager.getInstance().disconnect(null);
    }
}
