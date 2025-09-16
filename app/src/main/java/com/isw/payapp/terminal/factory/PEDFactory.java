package com.isw.payapp.terminal.factory;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.utils.StringUtils;
//import com.telpo.pinpad.PinParam;
//import com.telpo.pinpad.PinpadService;

public class PEDFactory {

    private Context myContecx;
//    private EmvService emvService;
   // private PinpadService pinpadService;
    StringBuffer logBuf = new StringBuffer("");
    int ret;



    public PEDFactory(Context context){
        this.myContecx = context;
    }

//    public KeyFactory(Context context, PinpadService pinpadService) {
//        this.myContecx = context;
////        this.emvService = emvService;
//        this.pinpadService = pinpadService;
//    }

//    public void openApp() {
//        emvService.Open(myContecx);
//        ret = emvService.Device_Open(myContecx);
//        if (0 == ret) {
//            Log.i("EMVService", "device open success");
//        } else {
//            Log.e("EMVService", "device open failed:" + ret);
//        }
//    }

//    public int initPinpad() {
//        ret = pinpadService.Open(myContecx);
//        if (0 == ret) {
//            Log.i("PINPADService", "PINPAD open success");
//        } else {
//            Log.e("PINPADService", "PINPAD open Failed");
//        }
//        return ret;
//    }
//
//    public int dukptStart(int idx) {
//        Log.i("DUKPTSTART", "DUKPT Start");
//        ret = pinpadService.TP_PinpadDukptSessionStart(idx);
//        if (pinpadService.PIN_OK == ret) {
//            Log.i("DUKPTSTART", "DUKPT Start Success : " + ret);
//        } else {
//            Log.e("DUKPTSTART", "DUKPT Start Failed : " + ret);
//        }
//        return ret;
//    }
//
//    public int dukptEnd() {
//        Log.i("DUKPTEND", "DUKPT End");
//        ret = pinpadService.TP_PinpadDukptSessionEnd();
//        if (pinpadService.PIN_OK == ret) {
//            Log.i("DUKPTEND", "DUKPT End Success : " + ret);
//        } else {
//            Log.e("DUKPTEND", "DUKPT End Success : " + ret);
//        }
//        return ret;
//    }
//
////    public void closeApp() {
////        emvService.deviceClose();
////    }
//
//    /*pinblockformat => ISO 9564 -0,ISO 9564 - 1,ISO 9564 - 2,ISO 9564 - 3
//     * show_pan => 0,1
//     * */
//    public String getDukptPin(int pin_format, int show_pan, String pan, String amt, int min_pin_len, int max_pin_len, int wait_time) {
//        String pinblock = "";
//        PinParam pinParam = new PinParam(myContecx);
//        pinParam.PinBlockFormat = pin_format;
//        pinParam.IsShowCardNo = show_pan;
//        pinParam.CardNo = pan;
//        pinParam.Amount = amt;
//        pinParam.MinPinLen = min_pin_len;
//        pinParam.MaxPinLen = max_pin_len;
//        pinParam.WaitSec = wait_time;
//
//        Log.w("DUKPTPIN", "start to TP_PinpadDukptGetPin mode"+ pinParam.PinBlockFormat);
//        ret = pinpadService.TP_PinpadDukptGetPin(pinParam);
//        if(pinpadService.PIN_OK != ret){
//            Log.e("DUKPTPIN","Error getting PINBLOCK :"+ret);
//            return null;
//        }
//        Log.w("pin", "PinBlock ："+ StringUtils.bytesToHexString(pinParam.Pin_Block));
//        Log.w("pin", "current KSN："+ StringUtils.bytesToHexString(pinParam.Curr_KSN));
//        pinblock = StringUtils.bytesToHexString(pinParam.Pin_Block)+"|"+
//                StringUtils.bytesToHexString(pinParam.Curr_KSN);
//        return pinblock;
//    }
//
//    public int dukptWriteBdk(int index, int mkindex, int mode, byte[] bdk, byte[] ksn) {
//        Log.i("DUKPTWRTBDK", "Write dukpt BDK Start");
//        ret = pinpadService.TP_PinpadWriteDukptKey(bdk, ksn, index, mode, mkindex);
//        if (0 == ret) {
//            Log.i("DUKPTWRTBDK", "Write dukpt BDK Success " + ret);
//        } else {
//            Log.e("DUKPTWRTBDK", "Write dukpt BDK Failed " + ret);
//        }
//        return ret;
//    }
//
//    public int dukptWritePek(int index, int mkindex, int mode, byte[] ipek, byte[] ksn) {
//        Log.i("DUKPTWRTPEK", "Write dukpt PEK Start");
//        ret = pinpadService.TP_PinpadWriteDukptIPEK(ipek, ksn, index, mode, mkindex);
//        if (0 == ret) {
//            Log.i("DUKPTWRTPEK", "Write dukpt PEK Success");
//        } else {
//            Log.e("DUKPTWRTPEK", "Write dukpt PEK Success");
//        }
//        return ret;
//    }
//
//    public int dukptSetKsn(int index, byte[] ksn) {
//        Log.i("DUKPTSETKSN", "DUKPT SET KSN Start");
//        ret = pinpadService.TP_PinpadDukptSetKSN(index, ksn);
//        if (0 == ret) {
//            Log.i("DUKPTSETKSN", "DUKPT SET KSN Success");
//        } else {
//            Log.e("DUKPTSETKSN", "DUKPT SET KSN Failed");
//        }
//        return ret;
//    }
//
//    public int dukptCheckKey(int idx) {
//        Log.i("DUKPTCHECKKEY", "DUKPT check Key Start");
//        ret = pinpadService.TP_PinpadCheckKey(pinpadService.KEY_TYPE_DUKPT, idx);
//        if (0 == ret) {
//            Log.i("DUKPTCHECKKEY", "DUKPT check Key Success");
//        } else {
//            Log.e("DUKPTCHECKKEY", "DUKPT check Key Failed");
//        }
//        return ret;
//    }
//
//    public int dukptDelKey(int idx) {
//        Log.i("DUKPTDELKEY", "DUKPT Del Key Start");
//        ret = pinpadService.TP_PinpadDeleteKey(pinpadService.KEY_TYPE_DUKPT, idx);
//        if (0 == ret) {
//            Log.i("DUKPTDELKEY", "DUKPT Delete Key Success");
//        } else {
//            Log.e("DUKPTDELKEY", "DUKPT Delete Key Success");
//        }
//        return ret;
//    }
//
//    public int dukptMAC(byte[] data, byte[] out, byte[] outKSN) {
//        String ret_data = "";
//        Log.i("DUKPTMAC", "DUKPT Do MAC Start");
//        out = new byte[8];
//        outKSN = new byte[10];
//        ret = pinpadService.TP_PinpadDukptGetMac(data, out, outKSN);
//        if (pinpadService.PIN_OK == ret) {
//            ret_data = "dukpt mac：" + StringUtils.bytesToHexString(out) + "\n current KSN："
//                    + StringUtils.bytesToHexString(outKSN);
//            Log.i("DUKPTMAC", "DUKPT Do MAC : " + ret);
//            Log.i("DUKPTMACVAL", ret_data);
//            //ret_data =
//        } else {
//            Log.e("DUKPTMAC", "DUKPT Do MAC : " + ret);
//        }
//        return ret;
//    }
//
//    public int dukptDES(byte[] data, int mode, byte[] out, byte[] outKSN, int idx, String dukptMACdata) {
//        Log.i("DUKPTDES", "DUKPT DES Start");
//        String val = "";
//        out = new byte[data.length];
//        outKSN = new byte[10];
//        if (data.length % 8 != 0) {
//            Log.e("DUKPTDES", "len of Data must be divisible by 8 ");
//            ret = 999999;
//            return ret;
//        }
//        ret = pinpadService.TP_PinpadDukptDes(data, out, outKSN, mode);
//        if (ret != pinpadService.PIN_OK) {
//            Log.e("DUKPTDES", "dukpt mac error：" + ret);
//            ret = 999998;
//            return ret;
//        } else {
//            val = "dukpt DES：" + StringUtils.bytesToHexString(out)
//                    + "\n current KSN：" + StringUtils.bytesToHexString(outKSN);
//            Log.w("DUKPTDES", "dukpt DES：" + val);
//        }
//        byte[] temp = new byte[out.length];
//        mode = (mode + 1) % 2;
//        ret = pinpadService.TP_PinpadDukptDes(out, temp, outKSN, mode);
//        if (StringUtils.bytesToHexString(temp).equals(dukptMACdata)) {
//            Log.i("DUKPTDES", "data match ");
//        } else {
//            Log.e("DUKPTDES", "data not match：" + StringUtils.bytesToHexString(temp));
//            Log.w("dukpt", "not match：" + StringUtils.bytesToHexString(temp));
//        }
//        return ret;
//    }

}
