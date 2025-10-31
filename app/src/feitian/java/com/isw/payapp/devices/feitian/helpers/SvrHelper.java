package com.isw.payapp.devices.feitian.helpers;


import android.content.Context;

import com.ftpos.library.smartpos.device.Device;
import com.ftpos.library.smartpos.emv.Emv;
import com.ftpos.library.smartpos.icreader.IcReader;
import com.ftpos.library.smartpos.keymanager.KeyManager;
import com.ftpos.library.smartpos.led.Led;
import com.ftpos.library.smartpos.magreader.MagReader;
import com.ftpos.library.smartpos.nfcreader.NfcReader;
import com.ftpos.library.smartpos.pin.PinSeting;
import com.ftpos.library.smartpos.printer.Printer;
import com.ftpos.library.smartpos.servicemanager.OnServiceConnectCallback;
import com.ftpos.library.smartpos.servicemanager.ServiceManager;

public class SvrHelper {
    private Context context;
    private boolean isBinded = false;
    private ServiceListener listener = null;
    private static SvrHelper instance = new SvrHelper();

    private Emv emv = null;
    private KeyManager key = null;
    private com.ftpos.library.smartpos.printer.Printer printer = null;
    private IcReader icReader = null;
    private NfcReader nfcReader = null;
    private MagReader magReader = null;
    private PinSeting pinSeting = null;

    private Device device = null;
    private Led led = null;

    public static SvrHelper instance() {
        return instance;
    }

    public void init(Context context) {
        this.context = context;
    }

    /**
     * Bind Service
     */
    public void bindService() {
        ServiceManager.bindPosServer(context, new OnServiceConnectCallback() {
            @Override
            public void onSuccess() {
                isBinded = true;
                notifyServerBinded();
            }

            @Override
            public void onFail(int code) {
                isBinded = false;
            }
        });
    }

    /**
     * Unbind Service
     */
    public void unbindService() {
        if (!isBinded) return;
        ServiceManager.unbindPosServer();
        isBinded = false;
    }

    /**
     * Set the service listener
     *
     * @param listener service listener
     */
    public void setServiceListener(ServiceListener listener) {
        this.listener = listener;
        if (isBinded) notifyServerBinded();
    }

    public Emv getEmv() {
        if (emv == null) {
            emv = Emv.getInstance(context);
        }
        return emv;
    }


    public KeyManager getKey() {
        if (key == null) {
            key = KeyManager.getInstance(context);
        }
        return key;
    }


    private void notifyServerBinded() {
        if (listener == null) return;
        listener.onServerBinded();
    }

    public Printer getPrinter() {
        if (printer == null) {
            printer = com.ftpos.library.smartpos.printer.Printer.getInstance(context);
        }
        return printer;
    }


    public IcReader getIcReader() {
        if (icReader == null) {
            icReader = IcReader.getInstance(context);
        }
        return icReader;
    }

    public NfcReader getNfcReader() {
        if (nfcReader == null) {
            nfcReader = NfcReader.getInstance(context);
        }
        return nfcReader;
    }

    public MagReader getMagReader() {
        if (magReader == null) {
            magReader = MagReader.getInstance(context);
        }
        return magReader;
    }

    public PinSeting getPinSetting() {
        if (pinSeting == null) {
            if (key == null) {
                key = getKey();
            }
            pinSeting = new PinSeting(context, emv);

        }
        return pinSeting;
    }

    public Device getDevice() {
        if (device == null) {
            device = Device.getInstance(context);
        }
        return device;
    }

    public Led getLED() {
        if (led == null) {
            led = Led.getInstance(context);
        }
        return led;
    }

    public void cancelOperation() {
        if (getDevice() != null) {
            device.cancelOperation();
        }
    }

    /**
     * Set the LED display
     *      false : light off;
     *      true : light on;
     *
     * @param red    Red LED
     * @param yellow Yellow LED
     * @param green  Green LED
     * @param blue   Blue LED
     */
    public void setLed(boolean red, boolean yellow, boolean green, boolean blue) {
        if (getLED() != null) {
            led.ledStatus(red, yellow, green, blue);
        }
    }

    public interface ServiceListener {
        /**
         * Service Bound
         */
        void onServerBinded();
    }
}
