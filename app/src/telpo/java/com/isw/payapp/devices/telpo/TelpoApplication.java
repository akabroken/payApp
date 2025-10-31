package com.isw.payapp.devices.telpo;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.BaseApplication;

public class TelpoApplication extends BaseApplication {
    @Override
    protected void initializeVariantSpecificServices() {
        // Telpo specific initialization
        Log.w("TelpoApplication", "Initializing Telpo specific services");

        // Initialize Telpo specific services here
        // For example: TelpoPrinter, TelpoCardReader, etc.

        // Example:
        try {
            Class<?> telpoService = Class.forName("com.isw.payapp.devices.telpo.TelpoInitializer");
            java.lang.reflect.Method initMethod = telpoService.getMethod("initialize", Context.class);
            initMethod.invoke(null, getApplicationContext());
            Log.d("TelpoApplication", "Telpo services initialized");
        } catch (Exception e) {
            Log.e("TelpoApplication", "Failed to initialize Telpo services", e);
        }
    }
}
