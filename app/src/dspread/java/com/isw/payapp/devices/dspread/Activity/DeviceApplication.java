package com.isw.payapp.devices.dspread.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.QPOSService;
import com.isw.payapp.devices.dspread.DSpreadPinPadService;
import com.isw.payapp.devices.dspread.POSManager;
import com.isw.payapp.helpers.ActivityLifecycleHandler;

public class DeviceApplication extends Application {

    private static DeviceApplication instance;
    private static Context applicationContext;
    public static QPOSService pos;
    public static Handler handler;
    public static boolean isKeyInjectionSuccessful = false;
    public static String mposDeviceId;
    public static View loginView;
    public static Activity activity;

    // Track initialization state
    private static boolean isPOSManagerInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // FIX 1: Initialize instance and context FIRST
        instance = this;
        applicationContext = this;

        // Initialize POSManager ONLY HERE - Single source of truth
        initializePOSManager();

        // FIX 2: Initialize POSManager BEFORE using it
        // Safe POSManager initialization
//        if (!POSManager.isInitialized()) {
//            POSManager.init(this);
//            Log.d("DeviceApplication", "POSManager initialized in Application");
//        } else {
//            Log.d("DeviceApplication", "POSManager already initialized");
//        }



        registerActivityLifecycleCallbacks(new ActivityLifecycleHandler());

        Log.w("D60DeviceApplication", "DeviceApplication: Step1");

        // FIX 3: Now it's safe to use POSManager
        open(QPOSService.CommunicationMode.UART);

        if (pos != null) {
            pos.setDeviceAddress("/dev/ttyS1");
            pos.setD20Trade(true);
            Log.w("", "DeviceApplication: DeviceInstanceActivated");
        } else {
            Log.w("", "DeviceApplication: DeviceInstanceFailed");
        }
    }

    /**
     * Centralized POSManager initialization
     */
    private void initializePOSManager() {
        if (!isPOSManagerInitialized) {
            POSManager.init(applicationContext);
            isPOSManagerInitialized = true;
            Log.d("DeviceApplication", "POSManager initialized successfully");
        } else {
            Log.d("DeviceApplication", "POSManager already initialized");
        }
    }

    /**
     * Method for other classes to check if POSManager is ready
     */
    public static boolean isPOSManagerReady() {
        return isPOSManagerInitialized && POSManager.isInitialized();
    }

    public void open(QPOSService.CommunicationMode mode) {
        TRACE.w("Base Application open");
        pos = QPOSService.getInstance(getApplicationContext(), mode);

        // FIX 4: Proper null checking
        if (pos == null) {
            Log.e("DeviceApplication", "QPOSService instance is null");
            return;
        }

        // FIX 5: Only set device address for UART mode
        if (mode == QPOSService.CommunicationMode.UART) {
            pos.setDeviceAddress("/dev/ttyS1");
        }

        if (mode == QPOSService.CommunicationMode.USB_OTG_CDC_ACM) {
            pos.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM);
        }

        pos.resetQPOS();
        TRACE.w("Base Application open status: " + pos);
        pos.setContext(this);

        DSpreadPinPadService.QPOSCallback listener = new DSpreadPinPadService.QPOSCallback();

        // FIX 6: POSManager is now properly initialized
        POSManager.getInstance().setQPOSService(pos);
        pos.initListener(listener);
    }

    // FIX 7: Proper getter methods
    public static DeviceApplication getInstance() {
        return instance;
    }

    public static Context getApplicationContextStatic() {
        return applicationContext;
    }

    public static boolean isApplicationReady() {
        return instance != null && applicationContext != null;
    }

    public static QPOSService getPos() {
        return pos;
    }
}