package com.magicianguo.dexencrypdemo;

import android.app.Application;
import android.util.Log;

public class MyApp extends Application {
    private static final String TAG = "MyApp";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }
}
