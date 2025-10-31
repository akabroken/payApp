package com.isw.payapp;

import android.util.Log;
import java.util.Locale;

public class DefaultApplication extends BaseApplication {

    private static final String TAG = "DefaultApplication";
    private String detectedBrand = "GENERIC";

    @Override
    protected void initializeVariantSpecificServices() {
        detectDeviceBrand();
        initializeBasedOnDetection();
    }

    private void detectDeviceBrand() {
        try {
            // Try to detect device brand dynamically
            String manufacturer = android.os.Build.MANUFACTURER.toUpperCase(Locale.US);
            String model = android.os.Build.MODEL.toUpperCase(Locale.US);

            Log.d(TAG, "Device detection - Manufacturer: " + manufacturer + ", Model: " + model);

            if (manufacturer.contains("TELPO") || model.contains("TELPO")) {
                detectedBrand = "TELPO";
            } else if (manufacturer.contains("PAX") || model.contains("PAX")) {
                detectedBrand = "PAX";
            } else if (manufacturer.contains("FEITIAN") || model.contains("FEITIAN")) {
                detectedBrand = "FEITIAN";
            } else if (manufacturer.contains("DSPREAD") || model.contains("DSPREAD")) {
                detectedBrand = "DSPREAD";
            } else if (manufacturer.contains("CASTLES") || model.contains("CASTLES")) {
                detectedBrand = "CASTLES";
            }

            Log.i(TAG, "Detected device brand: " + detectedBrand);

        } catch (Exception e) {
            Log.e(TAG, "Error detecting device brand", e);
        }
    }

    private void initializeBasedOnDetection() {
        switch (detectedBrand) {
            case "TELPO":
                initializeTelpoFallback();
                break;
            case "PAX":
                initializePaxFallback();
                break;
            case "FEITIAN":
                initializeFeitianFallback();
                break;
            case "DSPREAD":
                initializeDspreadFallback();
                break;
            case "CASTLES":
                initializeCastlesFallback();
                break;
            default:
                initializeGenericFallback();
                break;
        }
    }

    private void initializeTelpoFallback() {
        Log.i(TAG, "Initializing Telpo fallback services");
        // Try to initialize Telpo services dynamically if classes exist
        if (isVariantServiceAvailable("com.telpo.api.TelpoDevice")) {
            // Use reflection to initialize Telpo services
        }
    }

    private void initializePaxFallback() {
        Log.i(TAG, "Initializing PAX fallback services");
        // PAX-specific fallback initialization
    }

    private void initializeFeitianFallback() {
        Log.i(TAG, "Initializing Feitian fallback services");
        // Feitian-specific fallback initialization
    }

    private void initializeDspreadFallback() {
        Log.i(TAG, "Initializing Dspread fallback services");
        // Dspread-specific fallback initialization
    }

    private void initializeCastlesFallback() {
        Log.i(TAG, "Initializing Castles fallback services");
        // Castles-specific fallback initialization
    }

    private void initializeGenericFallback() {
        Log.i(TAG, "Initializing generic fallback services");
        // Generic initialization for unknown devices
    }

    public String getDetectedBrand() {
        return detectedBrand;
    }

    @Override
    protected boolean isVariantServiceAvailable(String serviceClassName) {
        // Enhanced checking with better logging
        try {
            Class<?> serviceClass = Class.forName(serviceClassName);
            Log.d(TAG, "Variant service available: " + serviceClassName);
            return true;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Variant service not available: " + serviceClassName);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking variant service: " + serviceClassName, e);
            return false;
        }
    }
}