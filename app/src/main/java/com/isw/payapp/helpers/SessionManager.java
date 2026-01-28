package com.isw.payapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullNames";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_LAST_ACTIVITY_TIME = "lastActivityTime";
    private static final String KEY_ROLE_TYPE = "roleType";
    private static final String KEY_LOGIN_TIME = "loginTime";

    // Session timeout constants (in milliseconds)
    private static final long SESSION_TIMEOUT = 8 * 60 * 60 * 1000L; // 8 hours
    private static final long INACTIVITY_TIMEOUT = 10 * 60 * 1000L; // 10 minutes

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save user session details
    public boolean createSession(String username, String fullNames, String roleType) {
        try {
            long currentTime = System.currentTimeMillis();

            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_FULLNAME, fullNames);
            editor.putString(KEY_ROLE_TYPE, roleType);
            editor.putLong(KEY_LOGIN_TIME, currentTime);
            editor.putLong(KEY_LAST_ACTIVITY_TIME, currentTime);

            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Session created successfully for user: " + username);
                Log.d(TAG, "Login time: " + currentTime);
            } else {
                Log.e(TAG, "Failed to commit session data");
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error creating session", e);
            return false;
        }
    }

    // Update last activity time
    public void updateLastActivityTime() {
        try {
            long currentTime = System.currentTimeMillis();
            editor.putLong(KEY_LAST_ACTIVITY_TIME, currentTime);
            editor.apply();
            Log.d(TAG, "Last activity time updated: " + currentTime);
        } catch (Exception e) {
            Log.e(TAG, "Error updating last activity time", e);
        }
    }

    // Get last activity time
    public long getLastActivityTime() {
        return sharedPreferences.getLong(KEY_LAST_ACTIVITY_TIME, 0);
    }

    // Check if session has expired due to inactivity
    public boolean isSessionExpired() {
        try {
            if (!isSessionActive()) {
                return true;
            }

            long lastActivityTime = getLastActivityTime();
            long currentTime = System.currentTimeMillis();
            long idleTime = currentTime - lastActivityTime;

            boolean expired = idleTime > INACTIVITY_TIMEOUT;

            if (expired) {
                Log.d(TAG, "Session expired due to inactivity. Idle time: " + (idleTime / 60000) + " minutes");
            }

            return expired;
        } catch (Exception e) {
            Log.e(TAG, "Error checking session expiry", e);
            return true; // Assume expired on error
        }
    }

    // Check if session is still active (not timed out from login)
    private boolean isSessionActive() {
        long loginTime = getLoginTime();
        if (loginTime == 0) {
            return false; // Never logged in
        }

        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - loginTime;

        boolean active = sessionDuration <= SESSION_TIMEOUT;

        if (!active) {
            Log.d(TAG, "Session timed out after: " + (sessionDuration / 3600000) + " hours");
            logout(); // Auto-logout on timeout
        }

        return active;
    }

    // Get login time
    private long getLoginTime() {
        return sharedPreferences.getLong(KEY_LOGIN_TIME, 0);
    }

    // Get the logged-in user's username
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    // Get full name
    public String getKeyFullname() {
        return sharedPreferences.getString(KEY_FULLNAME, null);
    }

    // Get role type
    public String getKeyRoleType() {
        return sharedPreferences.getString(KEY_ROLE_TYPE, null);
    }

    // Check if the user is logged in
    public boolean isLoggedIn() {
        try {
            // First check if the flag says we're logged in
            boolean isLoggedInFlag = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

            if (!isLoggedInFlag) {
                Log.d(TAG, "Not logged in: KEY_IS_LOGGED_IN is false");
                return false;
            }

            // Check if session is still active (not timed out)
            if (!isSessionActive()) {
                Log.d(TAG, "Not logged in: Session not active");
                return false;
            }

            // Check for required user data
            String username = getUsername();
            if (username == null || username.trim().isEmpty()) {
                Log.d(TAG, "Not logged in: Username is missing");
                return false;
            }

            Log.d(TAG, "User is logged in: " + username);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error checking login status", e);
            return false;
        }
    }

    // Combined check for logout due to inactivity
    public boolean shouldLogoutDueToInactivity() {
        return isLoggedIn() && isSessionExpired();
    }

    // Get session information for debugging
    public String getSessionInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Logged in: ").append(sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false));
        info.append("\nUsername: ").append(getUsername());
        info.append("\nFull Name: ").append(getKeyFullname());
        info.append("\nRole: ").append(getKeyRoleType());
        info.append("\nLogin Time: ").append(getLoginTime());
        info.append("\nLast Activity: ").append(getLastActivityTime());
        info.append("\nCurrent Time: ").append(System.currentTimeMillis());

        long idleTime = System.currentTimeMillis() - getLastActivityTime();
        info.append("\nIdle Time (min): ").append(idleTime / 60000);

        long sessionDuration = System.currentTimeMillis() - getLoginTime();
        info.append("\nSession Duration (hrs): ").append(sessionDuration / 3600000);

        return info.toString();
    }

    // Clear session details (logout)
    public void logout() {
        try {
            String username = getUsername(); // Log before clearing

            editor.clear();
            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "User logged out successfully: " + username);
            } else {
                Log.e(TAG, "Failed to clear session data");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
        }
    }

    // Check if session data exists (for debugging)
    public boolean hasSessionData() {
        return sharedPreferences.contains(KEY_IS_LOGGED_IN);
    }

    // Force clear all session data (for testing)
    public void forceClearSession() {
        SharedPreferences.Editor clearEditor = sharedPreferences.edit();
        clearEditor.clear();
        clearEditor.commit();
        Log.d(TAG, "Session forcefully cleared");
    }
}