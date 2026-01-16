package com.isw.payapp.devices.newtelpo;

import android.content.Context;
import android.util.Log;

import com.common.apiutil.util.SDKUtil;
import com.isw.payapp.BaseApplication;

public class NewTelpoApplication extends BaseApplication {

    @Override
    protected void initializeVariantSpecificServices() {
        // Telpo specific initialization
        Log.w("NewTelpoApplication", "Initializing Telpo specific services");

        // Initialize Telpo specific services here
        // For example: newTelpoPrinter, newTelpoCardReader, etc.

        // Example:
        try {
            //SDKUtil.getInstance(this).initSDK();
//            Class<?> newTelpoService = Class.forName("com.isw.payapp.devices.newtelpo.TelpoInitializer");
//            java.lang.reflect.Method initMethod = newTelpoService.getMethod("initialize", Context.class);
//            initMethod.invoke(null, getApplicationContext());
            Log.d("NewTelpoApplication", "Telpo services initialized");
        } catch (Exception e) {
            Log.e("NewTelpoApplication", "Failed to initialize Telpo services", e);
        }
    }
}
