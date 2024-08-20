//package com.isw.payapp.tasks;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.media.ThumbnailUtils;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.isw.payapp.dialog.DialogListener;
//import com.isw.payapp.dialog.MyProgressDialog;
//import com.isw.payapp.dialog.WritePadDialog;
//import com.isw.payapp.interfaces.CardReaderListener;
//import com.isw.payapp.terminal.config.DefaultAppCapk;
//import com.isw.payapp.terminal.model.PayData;
//import com.telpo.emv.EmvService;
//import com.telpo.pinpad.PinpadService;
//
//import java.util.concurrent.Callable;
//
//public class PaymentTaskBk implements Callable<String> {
//    private Context context;
//    private PayData payData;
//    private Handler handler;
//    Bitmap bitmap;
//    private  Handler uiHandler;
//
//    boolean waitsign = true;
//
//    MyProgressDialog progressDialog = null;
//    WritePadDialog writePadDialog = null;
//
//    public PaymentTaskBk(Context context, PayData payData){
//        this.context = context;
//        this.payData = payData;
//
//    }
//
//
//    @Override
//    public String call() throws Exception {
//        int ret = initCardReader();
//        if(ret != EmvService.EMV_TRUE)
//            return "FALSE";
//        //
//        CardReaderTask task = new CardReaderTask(context, uiHandler,progressDialog,payData);
//        CardReaderListener listener = new CardReaderListener() {
//            private  MyProgressDialog progressDialog1 = null;
//
//            @Override
//            public void onProgressUpdate(String message) {
//
//                System.out.println("Progress: " + message);
//            }
//
//            @Override
//            public void onRequestSent(String message) {
//
//            }
//
//            @Override
//            public void onCardDataRead(String cardData) {
//                System.out.println("Card Data Read State: " + cardData);
//            }
//
//            @Override
//            public void onRequestComplete(String response) {
//                System.out.println("Request Complete: " + response);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                System.err.println("Error: " + errorMessage);
//            }
//        };
//        task.setListener(listener);
//        try {
//            task.waitForCardInsertion();
//            task.detectCard();
//            task.cardDitected();
//            task.postRequestToServer("EMV Data from card");
//        } catch (InterruptedException e) {
//            listener.onError(e.getMessage());
//        }
//        close();
//        return "TRUE";
//    }
//
//    private int initCardReader(){
//        int ret = EmvService.Open(context);
//        if (ret != EmvService.EMV_TRUE) {
//            Log.e("PAYMENTTASK", "EmvService.Open fail");
//            Toast.makeText(context, "EmvService.Open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//
//        ret = EmvService.deviceOpen();
//        if (ret != 0) {
//            Log.e("PAYMENTTASK", "EmvService.Open fail");
//            Toast.makeText(context, "EmvService.Open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//
//        ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure
//
//
//        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
//            PinpadService.TP_PinpadFormat(context);
//            ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure
//        }
//        Log.d("PAYMENTTASK", "PinpadService deviceOpen open:" + ret);
//        if (ret != 0) {
//            Toast.makeText(context, "PinpadService open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//        EmvService.Emv_SetDebugOn(1);//Turn on debugging information
//
//        EmvService.Emv_RemoveAllApp();
//        DefaultAppCapk.Add_All_APP();
//
//        EmvService.Emv_RemoveAllCapk();
//        DefaultAppCapk.Add_All_CAPK();
//        return  EmvService.EMV_TRUE;
//    }
//
//    private void close(){
//        PinpadService.Close();
//        EmvService.deviceClose();
//    }
//
//    private void dismissDialog(){
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                // Initialize progressDialog if not already initialized
//                if (progressDialog == null) {
//                    progressDialog = new MyProgressDialog(context);
//                }
//                // Dismiss progressDialog if still showing
//                if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                }
//            }
//        });
//    }
//
//    private void getHandler(final int state) {
//        new Handler(Looper.getMainLooper()).post(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog = new MyProgressDialog(context);
//                        writePadDialog = new WritePadDialog(context, new DialogListener() {
//                            @Override
//                            public void refreshActivity(Object object) {
//                                bitmap = (Bitmap) object;
//                                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 256);
//                                waitsign = false;
//                            }
//                        });
//                        Handler handler = new MyHandler(context, progressDialog, writePadDialog);
//                        handler.sendMessage(handler.obtainMessage(state));
//                    }
//                }
//        );
//
//    }
//    private void getHandlerMsg(final Message message) {
//        new Handler(Looper.getMainLooper()).post(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog = new MyProgressDialog(context);
//                        writePadDialog = new WritePadDialog(context, new DialogListener() {
//                            @Override
//                            public void refreshActivity(Object object) {
//                                bitmap = (Bitmap) object;
//                                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 256);
//                                waitsign = false;
//                            }
//                        });
//                        Handler handler = new MyHandler(context, progressDialog, writePadDialog);
//                        handler.sendMessage(message);
//                    }
//                }
//        );
//
//    }
//
//    private void initHandlers() {
//
//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        new Handler(Looper.getMainLooper()).post(
//                                new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        progressDialog = new MyProgressDialog(context);
//                                        writePadDialog = sgPad();
//                                        handler = new MyHandler(context, progressDialog, writePadDialog);
//                                    }
//                                }
//                        );
//                    }
//                }
//        ).start();
//
//    }
//
//    private WritePadDialog sgPad() {
//        WritePadDialog writePad = new WritePadDialog(context, new DialogListener() {
//            @Override
//            public void refreshActivity(Object object) {
//                bitmap = (Bitmap) object;
//                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 256);
//                waitsign = false;
//            }
//        });
//        return writePad;
//    }
//
//    public void dialogMaster(String... msg) {
//        final String[] theMsg = msg;
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                // Initialize progressDialog if not already initialized
//                if (progressDialog == null) {
//                    progressDialog = new MyProgressDialog(context);
//                    progressDialog.setTitle(theMsg[0]);
//                    progressDialog.setCancelable(true);
//                    progressDialog.setCanceledOnTouchOutside(false);
//                    progressDialog.setMessage(theMsg[1]);
//
//                }
//
//                // Show progressDialog
//                progressDialog.show();
//
//
//                // Notify listener
//
//
//                // Dismiss progressDialog if still showing
//                if (progressDialog != null && progressDialog.isShowing()) {
//                    progressDialog.dismiss();
//                }
//            }
//        });
//    }
//
//
//
//
//}
