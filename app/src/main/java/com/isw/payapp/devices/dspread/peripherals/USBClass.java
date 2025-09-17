package com.isw.payapp.devices.dspread.peripherals;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class USBClass {

    private static UsbManager mManager;
    private static HashMap<String, UsbDevice> mDevices;
    private static PendingIntent mPermissionIntent;
    private static USBClass instance;

    private UsbPermissionListener usbPermissionListener;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.i("USBCLASS","USB permission granted for device " + device);
                        Toast.makeText(context, "USB permission granted", Toast.LENGTH_SHORT).show();
                        if (usbPermissionListener != null) {
                            usbPermissionListener.onPermissionGranted(device);
                        }
                    } else {
                        Log.i("USBCLASS","USB permission denied for device " + device);
                        Toast.makeText(context, "USB permission denied", Toast.LENGTH_SHORT).show();
                        if (usbPermissionListener != null) {
                            usbPermissionListener.onPermissionDenied(device);
                        }
                    }
                }
            }
        }
    };

    private static final String ACTION_USB_PERMISSION = "com.dspread.pos.USB_PERMISSION";

    // Singleton pattern to manage lifecycle
    public static synchronized USBClass getInstance() {
        if (instance == null) {
            instance = new USBClass();
        }
        return instance;
    }

    private USBClass() {
        mDevices = new HashMap<>();
    }

    /**
     * Must be called to register the USB permission receiver.
     * Call in Activity.onCreate() or Service.onCreate()
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @SuppressLint("NewApi")
    public void init(Context context) {
        mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (mManager == null) return;

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent usbIntent = new Intent(ACTION_USB_PERMISSION);
            usbIntent.setPackage(context.getPackageName());
            mPermissionIntent = PendingIntent.getBroadcast(context, 0, usbIntent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            mPermissionIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        try {
            context.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } catch (IllegalArgumentException e) {
            Log.e("USBCLASS","Receiver already registered: " + e.getMessage());
        }
    }

    /**
     * Must be called to unregister the receiver.
     * Call in Activity.onDestroy() or Service.onDestroy()
     */
    public void release(Context context) {
        try {
            context.unregisterReceiver(mUsbReceiver);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.w("USBCLASS","Receiver not registered or already unregistered: " + e.getMessage());
        }
    }

    /**
     * Scan connected USB devices and request permission for valid ones.
     * Does NOT return device list immediately — use listener for results.
     */
    public ArrayList<String> getUsbDevices(Context context) {
        if (mManager == null) {
            mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (mManager == null) {
                Log.e("USBCLASS","UsbManager is not available.");
                return new ArrayList<>();
            }
        }

        ArrayList<String> deviceList = new ArrayList<>();

        for (UsbDevice device : mManager.getDeviceList().values()) {
            // Filter by vendor IDs (adjust as needed)
            int vid = device.getVendorId();
            if (vid == 2965 || vid == 0x03EB || vid == 1027 || vid == 6790) {

                if (mManager.hasPermission(device)) {
                    // Already have permission — query device name
                    String deviceName = getDeviceNameFromDescriptor(device);
                    if (deviceName != null) {
                        deviceList.add(deviceName);
                        mDevices.put(deviceName, device);
                    }
                } else {
                    // Request permission
                    Log.i("USBCLASS","Requesting permission for USB device: " + device);
                    mManager.requestPermission(device, mPermissionIntent);
                    // Will not add now — wait for broadcast response
                }
            }
        }

        return deviceList;
    }

    private String getDeviceNameFromDescriptor(UsbDevice device) {
        UsbDeviceConnection connection = mManager.openDevice(device);
        if (connection == null) return null;

        try {
            byte[] rawBuf = new byte[255];
            int len = connection.controlTransfer(0x80, 0x06, 0x0302, 0x0409, rawBuf, 255, 1000);
            if (len > 2) {
                byte[] nameBytes = new byte[len - 2];
                System.arraycopy(rawBuf, 2, nameBytes, 0, len - 2);
                return new String(nameBytes).trim();
            }
        } catch (Exception e) {
            Log.e("USBCLASS","Failed to read device name: " + e.getMessage());
        } finally {
            connection.close();
        }
        return "Unknown Device";
    }

    public HashMap<String, UsbDevice> getDevices() {
        return mDevices;
    }

    public void setUsbPermissionListener(UsbPermissionListener listener) {
        this.usbPermissionListener = listener;
    }

    public interface UsbPermissionListener {
        void onPermissionGranted(UsbDevice device);
        void onPermissionDenied(UsbDevice device);
    }
}
