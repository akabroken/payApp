package com.isw.payapp.devices.feitian;

import android.util.Log;

import com.isw.payapp.BaseApplication;
import com.isw.payapp.devices.feitian.helpers.SvrHelper;

public class FeitianApplication extends BaseApplication {
    private static final String TAG = "FeitianApplication";

    @Override
    protected void initializeVariantSpecificServices() {
        Log.i(TAG, "Initializing Feitian-specific services");

        try {
            // Initialize Feitian SDK and services
            // FeitianDeviceManager.initialize(this);
            SvrHelper.instance().init(this);
            SvrHelper.instance().bindService();
            Log.d(TAG, "Feitian services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Feitian services", e);
        }
    }
}
