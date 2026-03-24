package com.isw.payapp.devices.castles.CastlesApplication;

import com.isw.payapp.BaseApplication;

import android.util.Log;

import CTOS.CtSettings;
import CTOS.CtSystem;
import CTOS.CtSystemException;

public class CastlesApplication extends BaseApplication {

    private static final String TAG = "CastlesApplication";

    private CtSettings ctSettings;
    private CtSystem system;

    @Override
    protected void initializeVariantSpecificServices() {
        super.initializeVariantSpecificServices();

        Log.d(TAG, "Initializing Castles-specific services");

        // Initialize Castles POS specific services here
        initializeCastlesPOSHardware();
        initializeCastlesPaymentServices();
        initializeCastlesPrinterService();
    }

    /**
     * Initialize Castles POS hardware specific configurations
     */
    private void initializeCastlesPOSHardware() {
        Log.d(TAG, "Initializing Castles POS hardware");

        try {
            // Check if we're actually running on Castles hardware
            if (isRunningOnCastlesHardware()) {
                // Initialize Castles specific hardware drivers
                // For example:
                // - Castles PIN pad initialization
                // - Castles MSR (Magnetic Stripe Reader) initialization
                // - Castles contactless reader initialization
                system = new CtSystem();
                int SULD = 0;
                int Kernel = 3;
                int Rootfs = 30;
                int BIOS = 31;
                byte buf[] = new byte[16];
                byte bmode = 0; // 0, 1
                // getModuleVersion
                try {
                    Log.d(TAG, "SULD Version = " + system.getModuleVersion(SULD));
                    Log.d(TAG, "KERNEL Version = " + system.getModuleVersion(Kernel));
                    Log.d(TAG, "ROOTFS Version = " + system.getModuleVersion(Rootfs));
                    Log.d(TAG, "BIOS Version = " + system.getModuleVersion(BIOS));
                } catch (CtSystemException e) {
                    e.showStatus();
                }

                // getSerialNumber
                Log.d(TAG, " return = " + Integer.toString(system.getSerialNumber(buf)));
                Log.d(TAG, String.format("%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x"
                        , buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6]
                        , buf[7], buf[8], buf[9], buf[10], buf[11], buf[12]
                        , buf[13], buf[14], buf[15]));
// getKeyHash
                try {
                    Log.d(TAG, "getKeyHash = " + system.getKeyHash(0));
                    Log.d(TAG, "getKeyHash = " + system.getKeyHash(1));
                } catch (CtSystemException e) {
                    e.showStatus();
                }
// getDeviceModel
                try {
                    Log.d(TAG, String.format("Model = %d", system.getDeviceModel()));
                } catch (CtSystemException e) {
                    e.showStatus();
                }

                // getModelName
                Log.d(TAG, String.format("Model = %s", system.getModelName()));

               // if(system.getModelName().equals("S1P2")){
                    wifiFunc();
                //}
// shutdown
              //  Log.d(TAG, String.format("return = %X", system.shutdown(bmode)));

                Log.d(TAG, "Castles POS hardware initialized successfully");
            } else {
                Log.w(TAG, "Not running on Castles hardware - skipping hardware init");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Castles POS hardware", e);
        }
    }

    /**
     * Initialize Castles payment services
     */
    private void initializeCastlesPaymentServices() {
        Log.d(TAG, "Initializing Castles payment services");

        try {
            // Initialize Castles-specific payment modules
            // For example:
            // - EMV configuration
            // - Payment kernel initialization
            // - Security module setup

            // Check if variant services are available before initializing
            if (isVariantServiceAvailable("com.castles.payment.PaymentService")) {
                // Initialize Castles payment service
                Log.d(TAG, "Castles payment services initialized");
            } else {
                Log.w(TAG, "Castles payment service classes not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Castles payment services", e);
        }
    }

    /**
     * Initialize Castles printer service
     */
    private void initializeCastlesPrinterService() {
        Log.d(TAG, "Initializing Castles printer service");

        try {
            // Initialize Castles printer specific configurations
            // For example:
            // - Printer driver initialization
            // - Paper size configuration
            // - Print format settings

            if (isVariantServiceAvailable("com.castles.print.PrinterService")) {
                // Initialize printer
                Log.d(TAG, "Castles printer service initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Castles printer service", e);
        }
    }

    /**
     * Check if running on actual Castles hardware
     */
    private boolean isRunningOnCastlesHardware() {
        try {
            // Method 1: Check system properties
            String hardware = android.os.Build.HARDWARE;
            String manufacturer = android.os.Build.MANUFACTURER;
            String model = android.os.Build.MODEL;

            Log.d(TAG, String.format("Device info - Hardware: %s, Manufacturer: %s, Model: %s",
                    hardware, manufacturer, model));

            // Check if this is Castles hardware
            // You'll need to know the actual Castles device identifiers
            if (manufacturer != null && manufacturer.toLowerCase().contains("castles")) {
                return true;
            }

            // Method 2: Check for Castles specific classes
            try {
                Class.forName("com.castles.pos.CastlesPOS");
                return true;
            } catch (ClassNotFoundException e) {
                // Class not found, continue with other checks
            }

            // Method 3: Check build properties that might indicate Castles hardware
            if (model != null) {
                String modelLower = model.toLowerCase();
                if (modelLower.contains("castle") ||
                        modelLower.contains("s300") ||  // Example model
                        modelLower.contains("vega")) {   // Example model
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Castles hardware", e);
            return false;
        }
    }

    /**
     * Override to provide Castles-specific application behavior
     */
    @Override
    public boolean isRunningOnPOSDevice() {
        // Always return true for Castles application as it's specifically for Castles POS
        return true;
    }

    /**
     * Castles-specific method to get device serial number
     */
    public String getCastlesDeviceSerial() {
        try {
            // Implement Castles-specific serial number retrieval
            return android.os.Build.SERIAL;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device serial", e);
            return "UNKNOWN";
        }
    }

    /**
     * Castles-specific method to check if printer is available
     */
    public boolean isPrinterAvailable() {
        // Implement Castles-specific printer availability check
        return isVariantServiceAvailable("com.castles.print.PrinterService");
    }

    public void wifiFunc(){
        //init
        String errorMsg = "";
        String ssid = "Kiongozi";
        String password = "Cuchaza*@3637#";
        ssid = "Interswitch_EAK";
        password ="m#m@zX-W+h%eFycr";
        int type = 3; //1->NOPASS,2->WEP,3->WPA)

        try {
            ctSettings = new CtSettings();
            ctSettings.openWifi();
            errorMsg = ctSettings.setDhcpWifi( ssid, password, type);
            Log.i("errorMsg",errorMsg);

        }catch (Exception e){
            e.printStackTrace();
        }


    }
}