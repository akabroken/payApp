package com.isw.payapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class BaseApplication extends Application {

    private static BaseApplication instance;
    private static Context applicationContext;
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize common instances
        instance = this;
        applicationContext = this;

        // Common initialization for all variants
        initializeCommonServices();

        // Variant-specific initialization
        initializeVariantSpecificServices();

        Log.d("BasePayAppApplication", "Base application initialized");
    }

    /**
     * Common services initialization for all variants
     */
    protected void initializeCommonServices() {
        // Common initialization that applies to all POS devices
        // For example: Analytics, Crash reporting, Database, etc.
    }

    /**
     * Variant-specific initialization - to be overridden by subclasses
     */
    protected void initializeVariantSpecificServices() {
        // Base implementation does nothing
        // Variant-specific subclasses will override this
    }

    /**
     * Safe method to check if variant-specific services are available
     */
    protected boolean isVariantServiceAvailable(String serviceClassName) {
        try {
            Class.forName(serviceClassName);
            return true;
        } catch (ClassNotFoundException e) {
            Log.d("BasePayAppApplication", "Variant service not available: " + serviceClassName);
            return false;
        }
    }

    // Common getter methods
    public static BaseApplication getInstance() {
        return instance;
    }

    public static Context getApplicationContextStatic() {
        return applicationContext;
    }

    public static boolean isApplicationReady() {
        return instance != null && applicationContext != null;
    }

    // Add this method to your BaseApplication class:
    /**
     * Get the variant-specific application instance
     */
    public static BaseApplication getVariantApplication() {
        return instance;
    }

    /**
     * Check if running on specific POS hardware
     */
    public boolean isRunningOnPOSDevice() {
        return !(this instanceof DefaultApplication) ||
                ((DefaultApplication) this).getDetectedBrand().equals("GENERIC");
    }
}
