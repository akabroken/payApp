package com.isw.payapp.helpers;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {

    private SessionManager sessionManager;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        sessionManager = new SessionManager(activity);
        setupTouchListener(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // Check if session expired when activity starts
        if (sessionManager != null && sessionManager.isSessionExpired()) {
            sessionManager.logout();
            // Redirect to login activity
            // Intent intent = new Intent(activity, LoginActivity.class);
            // activity.startActivity(intent);
            // activity.finish();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            sessionManager.updateLastActivityTime();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Not needed for this implementation
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // Not needed for this implementation
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Not needed for this implementation
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // Not needed for this implementation
    }

    private void setupTouchListener(Activity activity) {
        activity.getWindow().getDecorView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (sessionManager != null && sessionManager.isLoggedIn()) {
                    sessionManager.updateLastActivityTime();
                }
                return false;
            }
        });
    }
}