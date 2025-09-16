package com.isw.payapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullNames";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save user session details
    public void createSession(String username, String fullNames) {
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULLNAME , fullNames );
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply(); // Commit changes asynchronously
    }

    // Get the logged-in user's username
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null); // Default to null if not found
    }

    public String getKeyFullname(){
        return sharedPreferences.getString(KEY_FULLNAME, null);
    }

    // Check if the user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false); // Default to false if not found
    }

    // Clear session details (logout)
    public void logout() {
        editor.clear(); // Clear all session data
        editor.apply();
    }
}
