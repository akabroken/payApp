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

public class DeviceApplication_bk extends Application {

    private static DeviceApplication_bk instance;
    public static Context getApplicationInstance;
    public static QPOSService pos;
    public static Handler handler;
    public static boolean isKeyInjectionSuccessful = false;
    public static String mposDeviceId;
    public static View loginView;
    public static Activity activity;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set the instance FIRST before any other initialization
        instance = this;
        getApplicationInstance = this;

        POSManager.getInstance().close();
        Log.w("D60DeviceApplication", "DeviceApplication: Step1");

        open(QPOSService.CommunicationMode.UART);
        pos.setDeviceAddress("/dev/ttyS1");
        pos.setD20Trade(true);

        if (pos != null) {
            Log.w("", "DeviceApplication: DeviceInstanceActivated");
        } else {
            Log.w("", "DeviceApplication: DeviceInstanceFailed");
        }
    }

    public void open(QPOSService.CommunicationMode mode) {
        TRACE.w("Base Application open");
        pos = QPOSService.getInstance(getApplicationContext(), mode);

        if (pos == null) {
            pos.forceOpenUart(10);
            if (pos != null) {
                TRACE.i("DeviceApplication: Forcefully Opened");
            } else {
                TRACE.i("DeviceApplication: Failed to Forcefully Be Opened");
            }
        }

        if (mode == QPOSService.CommunicationMode.USB_OTG_CDC_ACM) {
            pos.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM);
        }

        pos.resetQPOS();
        TRACE.w("Base Application open status: " + pos);
        pos.setContext(this);

        DSpreadPinPadService.QPOSCallback listener = new DSpreadPinPadService.QPOSCallback();
        POSManager.getInstance().setQPOSService(pos);
        pos.initListener(listener);
    }

    public static DeviceApplication_bk getInstance() {
        return instance;
    }

    public static QPOSService getPos() {
        return pos;
    }

    public static void setPos(QPOSService pos) {
        DeviceApplication_bk.pos = pos;
    }

    // Add a method to check if application is ready
    public static boolean isApplicationReady() {
        return instance != null && getApplicationInstance != null;
    }
}