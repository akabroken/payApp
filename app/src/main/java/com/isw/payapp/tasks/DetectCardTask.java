package com.isw.payapp.tasks;

import android.util.Log;

import com.isw.payapp.terminal.config.DefaultAppCapk;
import com.telpo.emv.EmvService;

public class DetectCardTask {

    EmvService emvService;
    boolean userCancel = false;
    boolean isSupportIC = true;
    boolean isSupportMag = true;
    boolean isSupportNfc = true;
    int event;

    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;

    public DetectCardTask(){

    }

    void openDevice() {
        int ret;
        if (isSupportMag) {
            ret = EmvService.MagStripeOpenReader();
        }

        if (isSupportIC) {
            ret = EmvService.IccOpenReader();
        }

        if (isSupportNfc) {
            ret = EmvService.NfcOpenReader(1000);
        }
    }

    void deviceClose() {
        int ret;
        if (isSupportMag) {
            ret = EmvService.MagStripeCloseReader();
        }

        if (isSupportIC) {
            if (event == IC) {
                ret = EmvService.IccCard_Poweroff();
            }
            ret = EmvService.IccCloseReader();
        }

        if (isSupportNfc) {
            ret = EmvService.NfcCloseReader();
        }
    }

    int detectCard() {
        int ret;
        userCancel = false;
        while (true) {
            if (userCancel) {
                Log.e("DETECT_CARD_TASK", "userCancel");
                return IC;
            }

            if (isSupportMag) {
                ret = EmvService.MagStripeCheckCard(1000);
                DefaultAppCapk.Log("MagStripeCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("DETECT_CARD_TASK", "Mag");
                    return Mag;
                }
            }

            if (isSupportIC) {
                ret = EmvService.IccCheckCard(300);
                DefaultAppCapk.Log("IccCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("DETECT_CARD_TASK", "IC");
                    return IC;
                }
            }

            if (isSupportNfc) {
                ret = EmvService.NfcCheckCard(1000);
                DefaultAppCapk.Log("NfcCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("DETECT_CARD_TASK", "Nfc");
                    return Nfc;
                }
            }
        }
    }

    int detectNFC() {
        userCancel = false;
        long j = System.currentTimeMillis();
        int ret = -1;
        while (System.currentTimeMillis() - j < 20 * 1000) {

            if (userCancel == true) {
                return -4;
            }

            ret = EmvService.NfcCheckCard(100);
            if (ret == 0) {
                return 0;
            }
            j++;
        }
        return ret;
    }

    int detectCardKernel() {
        int ret = -1;
        ret = emvService.NFC_CheckKernelID();
        return ret;
    }

    byte[] getBooleanArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }
}
