package com.isw.payapp.devices.dspread.callbacks;

import android.bluetooth.BluetoothDevice;

public interface IConnectionServiceCallback {

    // ==================== Device Connection Status Callbacks ====================

    /**
     * Request QPOS Connection
     */
    default void onRequestQposConnected() {}

    /**
     * Request QPOS Disconnection
     */
    default void onRequestQposDisconnected() {}

    /**
     * Request No QPOS Detected
     */
    default void onRequestNoQposDetected() {}
}
