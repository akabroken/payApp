package com.isw.payapp;

import android.app.Application;

import com.isw.payapp.helpers.ActivityLifecycleHandler;

public class MyCustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleHandler());
    }

}
