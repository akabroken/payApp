package com.isw.payapp.tasks;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.isw.payapp.commonActions.PrinterAction;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.terminal.config.DefaultAppCapk;
import com.isw.payapp.terminal.model.CardModel;
import com.isw.payapp.terminal.model.EmvModel;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;

public class TaskDoReadCard extends AsyncTask<Integer, String, Integer> {

    EmvService emvService;
    CardModel cardModel;
    EmvModel emvModel;
    Context context;
    Handler handler;
    int event;
    //MyHandler handler;

    MyProgressDialog progressDialog;
    String Amount;
    String[] data = new String[3];
    boolean userCancel = false;
    boolean isSupportIC = true;
    boolean isSupportMag = true;
    boolean isSupportNfc = true;
    long startMs;
    int ret;
    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;

    public TaskDoReadCard(Context context, EmvService emvService, Handler handler,
                          MyProgressDialog progressDialog, String Amount) {
        this.emvService = emvService;
        this.context = context;
        this.cardModel = cardModel;

        this.Amount = Amount;
        this.progressDialog = progressDialog;
        // this.event = event;//int event,
        this.handler = handler;

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface progressDialog) {
                userCancel = true;
                cancel(true);
            }
        });
        //dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //progressDialog.setIndeterminate(false);
        progressDialog.setTitle("Read card");
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("abc");

    }

    @Override
    protected void onPreExecute() {
        //super.onPreExecute();
        progressDialog.show();

    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        if (integer == 100) {
            deviceClose();
            //  context.finish();
        }

            /*if (event == Mag) {
                new AlertDialog.Builder(context)
                        .setMessage("track1:\n" + data[0] + "\ntrack2:\n" + data[1] + "\ntrack3:\n" + data[2])
                        .setPositiveButton("OK", null)
                        .show();
            }
            if (event == IC) {
                new AlertDialog.Builder(context)
                        .setMessage("IC process finish:" + ret )
                        .setPositiveButton("OK", null)
                        .show();
            }

            if (event == Nfc) {
                new AlertDialog.Builder(context)
                        .setMessage("NFC process finish:" + ret +"(0x"+ Integer.toHexString(ret))
                        .setPositiveButton("OK", null)
                        .show();
            }*/
    }

    @Override
    protected void onProgressUpdate(String... values) {
        progressDialog.setMessage(values[0]);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected Integer doInBackground(Integer... params) {
        PrinterAction printerAction = new PrinterAction();

        try {
            publishProgress("open device..");
            openDevice();
            Thread.sleep(500);

            publishProgress("detect card ..");
            event = detectCard();

            Log.e("WEEEEE", "event  " + event);

            if (event == IC) {
                // PinParam param = new PinParam(context);
                publishProgress("Read IC card ..");
                ret = EmvService.IccCard_Poweron();
                Log.w("readcard", "IccCard_Poweron: " + ret);
                ret = emvService.Emv_TransInit();
                Log.w("readcard", "Emv_TransInit: " + ret);

                {
                    EmvParam mEMVParam;
                    mEMVParam = new EmvParam();
                    mEMVParam.MerchName = "TEST ANDROID ISW".getBytes();
                    //   mEMVParam.MerchCateCode=
                    mEMVParam.MerchId = "CBLKE0000000001".getBytes();
                    mEMVParam.TermId = "CBLKE001".getBytes();
                    mEMVParam.TerminalType = 0x22;
                    mEMVParam.Capability = new byte[]{(byte) 0xE0, (byte) 0xF9, (byte) 0xC8};
                    mEMVParam.ExCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01};
                    mEMVParam.CountryCode = new byte[]{(byte) 0x04, (byte) 0x04};
                    mEMVParam.TransType = 0x00; //0x31
                    emvService.Emv_SetParam(mEMVParam);
                }
                startMs = System.currentTimeMillis();

                //   emvService.Emv_SetOfflinePiDisplayPan(0);
                //emvService.

                Log.e("emvlog_start", "emvlog_start");

              //  emvService.Emv_SetOfflinePinCBenable(1);  //offline card will call back oninputpin

                ret = emvService.Emv_StartApp(0);
                Log.e("yw", "Emv_StartApp: " + ret);

                if (ret == EmvService.EMV_TRUE) {
                    handler.sendMessage(handler.obtainMessage(4));

                } else {

                    handler.sendMessage(handler.obtainMessage(7));
                }

            } else {
                progressDialog.dismiss();
                //   stopPlayer.start();
                Log.w("Read NFC card", "NFC: " + "detect error:" + ret);
                handler.sendMessage(handler.obtainMessage(4));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
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
                Log.e("yw", "userCancel");
                return IC;
            }

            if (isSupportMag) {
                ret = EmvService.MagStripeCheckCard(1000);
                DefaultAppCapk.Log("MagStripeCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("yw", "Mag");
                    return Mag;
                }
            }

            if (isSupportIC) {
                ret = EmvService.IccCheckCard(300);
                DefaultAppCapk.Log("IccCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("yw", "IC");
                    return IC;
                }
            }

            if (isSupportNfc) {
                ret = EmvService.NfcCheckCard(1000);
                DefaultAppCapk.Log("NfcCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("yw", "Nfc");
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


