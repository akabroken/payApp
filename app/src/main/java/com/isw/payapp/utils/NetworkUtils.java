package com.isw.payapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtils {

    /**
     * Checks if the device has an active internet connection.
     *
     * @param context The application context.
     * @return True if the network is available, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0 (Marshmallow) and above
            return checkNetworkForModernApi(connectivityManager);
        } else {
            // For older versions of Android (API < 23)
            return checkNetworkForLegacyApi(connectivityManager);
        }
    }

    /**
     * Checks network connectivity for Android 6.0 and above.
     *
     * @param connectivityManager The ConnectivityManager instance.
     * @return True if the network is available, false otherwise.
     */
    private static boolean checkNetworkForModernApi(ConnectivityManager connectivityManager) {
        android.net.Network network = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            network = connectivityManager.getActiveNetwork();
        }
        if (network == null) {
            return false;
        }

        android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Checks network connectivity for Android versions below 6.0.
     *
     * @param connectivityManager The ConnectivityManager instance.
     * @return True if the network is available, false otherwise.
     */
    private static boolean checkNetworkForLegacyApi(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
