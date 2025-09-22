package com.isw.payapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullNames";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_LAST_ACTIVITY_TIME = "lastActivityTime";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save user session details
    public void createSession(String username, String fullNames) {
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULLNAME, fullNames);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        updateLastActivityTime(); // Set initial activity time
        editor.apply();
    }

    // Update last activity time
    public void updateLastActivityTime() {
        editor.putLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis());
        editor.apply();
    }

    // Get last activity time
    public long getLastActivityTime() {
        return sharedPreferences.getLong(KEY_LAST_ACTIVITY_TIME, 0);
    }

    // Check if session has expired (15 minutes = 15 * 60 * 1000 milliseconds)
    public boolean isSessionExpired() {
        long lastActivityTime = getLastActivityTime();
        long currentTime = System.currentTimeMillis();
        long idleTime = currentTime - lastActivityTime;

        return idleTime > (15 * 60 * 1000); // 15 minutes in milliseconds
    }

    // Get the logged-in user's username
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public String getKeyFullname() {
        return sharedPreferences.getString(KEY_FULLNAME, null);
    }

    // Check if the user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && !isSessionExpired();
    }

    // Clear session details (logout)
    public void logout() {
        editor.clear();
        editor.apply();
    }
}