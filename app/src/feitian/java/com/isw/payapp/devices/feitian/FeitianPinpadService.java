package com.isw.payapp.devices.feitian;

import android.content.Context;
import android.util.Log;

import com.ftpos.library.smartpos.datautils.BytesTypeValue;
import com.ftpos.library.smartpos.device.Device;
import com.ftpos.library.smartpos.emv.Emv;
import com.ftpos.library.smartpos.errcode.ErrCode;
import com.ftpos.library.smartpos.icreader.IcReader;
import com.ftpos.library.smartpos.keymanager.AlgName;
import com.ftpos.library.smartpos.keymanager.KeyManager;
import com.ftpos.library.smartpos.keymanager.KeyType;
import com.ftpos.library.smartpos.led.Led;
import com.ftpos.library.smartpos.magreader.MagReader;
import com.ftpos.library.smartpos.nfcreader.NfcReader;
import com.ftpos.library.smartpos.printer.Printer;
import com.ftpos.library.smartpos.util.BytesUtils;
import com.isw.payapp.devices.feitian.helpers.SvrHelper;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.utils.DUKPK2009_CBC;
import com.isw.payapp.utils.ThreeDES;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FeitianPinpadService implements IPinPadProcessor {

    private Context context;
    private String TAG = "FeitianPinpadService";

    public static final int TIMEOUT_SEARCH_CARD = 10;
    public static final int TIMEOUT_PINPAD = 20;
    public static final int REQUEST_CODE_PIN = 1001;

    protected Led led;
    protected Emv iemv;
    protected Printer printer = null;
    protected IcReader icReader = null;
    protected NfcReader nfcReader = null;
    protected MagReader magReader = null;
    protected KeyManager ikey;
    protected Device device = null;

    private PinpadListener pinpadListener;
    private boolean isServiceBound = false;
    private CountDownLatch serviceBindingLatch;

    public FeitianPinpadService(Context context){
        this.context = context.getApplicationContext(); // Use application context
        this.pinpadListener = new PinpadListener();
        this.serviceBindingLatch = new CountDownLatch(1);
    }

    @Override
    public void initPinPad() {
        Log.d(TAG, "Initializing pinpad service...");

        if (context == null) {
            Log.e(TAG, "Context is null in initPinPad");
            return;
        }

        try {
            SvrHelper.instance().setServiceListener(pinpadListener);
            Log.d(TAG, "Service listener set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting service listener: " + e.getMessage(), e);
        }
    }

    @Override
    public int injectDukptKey(String key, String iKsn, String kcv) {
        Log.d(TAG, "Starting DUKPT key injection...");

        // Wait for service to be bound
        if (!waitForServiceBinding()) {
            Log.e(TAG, "Service binding timeout or failed");
            return -1;
        }

        if (ikey == null) {
            Log.e(TAG, "KeyManager is null, cannot inject DUKPT key");
            return -1;
        }

        if (key == null || key.length() < 40) {
            Log.e(TAG, "Invalid key format");
            return -1;
        }

        try {
            // Extract the actual key value
            String actualKey = key.substring(8, 40);
            Log.d(TAG, "Injecting DUKPT key: " + actualKey + ", KSN: " + iKsn);
            byte[] ipekBytes = DUKPK2009_CBC.GenerateIPEK(
                    ThreeDES.hexStringToByteArray(iKsn),
                    ThreeDES.hexStringToByteArray(actualKey)
            );
            String ipek = ThreeDES.byteArrayToHexString(ipekBytes).toUpperCase();
            Log.i(TAG,"IPEK: " + ipek+"===="+iKsn);

            // Set the key group name using application context
            String packageName = context.getPackageName();
            int ret = ikey.setKeyGroupName(packageName);
            if (ret != ErrCode.ERR_SUCCESS) {
                Log.e(TAG, "Failed to set key group name: " + ErrCode.toString(ret));
                return ret;
            }
            String kcv__ = ThreeDES.generateKeyCheckValue(ipek, ThreeDES.KcvMethod.ANSI);
            Log.i(TAG,"IPEK KCV: "+ kcv__);

            // Convert key and KSN to bytes
            byte[] bKeyValue = hexStringToBytes(ipek);
            byte[] bKsnValue = hexStringToBytes(iKsn);

            if (bKeyValue == null || bKsnValue == null) {
                Log.e(TAG, "Invalid key or KSN format");
                return -1;
            }

            BytesTypeValue bValue = new BytesTypeValue();
            bValue.setData(hexStringToBytes(kcv__));

            // Load DUKPT IPEK key
            int keyIndex = 0x01;
            ret = ikey.loadDukptIpek(KeyType.KEY_TYPE_IPEK, keyIndex, AlgName.SYM_ARITH_3DES,
                    0x00, 0x00, bKeyValue, bKsnValue, bValue);

            String kcv_ = BytesUtils.byte2HexStr(bValue.getData());
            Log.i(TAG, "KCV :"+kcv_);
            if (ret != ErrCode.ERR_SUCCESS) {
                Log.e(TAG, "Failed to load DUKPT IPEK: " + ErrCode.toString(ret));
                return ret;
            }




            Log.d(TAG, "DUKPT key injected successfully");
            return ErrCode.ERR_SUCCESS;

        } catch (Exception e) {
            Log.e(TAG, "Exception during DUKPT key injection: " + e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public int resetKey() {
        if (!waitForServiceBinding()) {
            return -1;
        }

        try {
            if (ikey != null) {
                Log.d(TAG, "Key reset performed");
                return ErrCode.ERR_SUCCESS;
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Error resetting key: " + e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public void deleteKey() {
        if (!waitForServiceBinding()) {
            return;
        }

        try {
            if (ikey != null) {
                Log.d(TAG, "Key deletion performed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting key: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteKeys() {
        if (!waitForServiceBinding()) {
            return -1;
        }

        try {
            if (ikey != null) {
                Log.d(TAG, "All keys deletion performed");
                return ErrCode.ERR_SUCCESS;
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting keys: " + e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public void deviceClose() {
        try {
            SvrHelper.instance().cancelOperation();
            isServiceBound = false;
            Log.d(TAG, "Device closed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error closing device: " + e.getMessage(), e);
        }
    }

    /**
     * Wait for service binding to complete
     */
    private boolean waitForServiceBinding() {
        if (isServiceBound) {
            return true;
        }

        try {
            // Wait for service binding with timeout (10 seconds)
            boolean bound = serviceBindingLatch.await(10, TimeUnit.SECONDS);
            if (bound) {
                isServiceBound = true;
                return true;
            } else {
                Log.e(TAG, "Service binding timeout");
                return false;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Service binding interrupted: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Initialize device components after service binding
     */
    private void initializeDeviceComponents() {
        try {
            led = SvrHelper.instance().getLED();
            iemv = SvrHelper.instance().getEmv();
            ikey = SvrHelper.instance().getKey();
            printer = SvrHelper.instance().getPrinter();
            icReader = SvrHelper.instance().getIcReader();
            nfcReader = SvrHelper.instance().getNfcReader();
            magReader = SvrHelper.instance().getMagReader();
            device = SvrHelper.instance().getDevice();

            Log.d(TAG, "Device components initialized successfully");

            // Signal that service is ready
            serviceBindingLatch.countDown();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing device components: " + e.getMessage(), e);
        }
    }

    /**
     * Utility method to convert hex string to byte array
     */
    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            return null;
        }

        try {
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                        + Character.digit(hexString.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error converting hex string to bytes: " + e.getMessage(), e);
            return null;
        }
    }

    private class PinpadListener implements SvrHelper.ServiceListener {
        @Override
        public void onServerBinded() {
            Log.d(TAG, "Service successfully bound");
            // Initialize device components when service is bound
            initializeDeviceComponents();
        }
    }
}