//package com.isw.payapp.processors;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.os.Handler;
//
//import com.isw.payapp.dialog.MyProgressDialog;
//import com.isw.payapp.interfaces.CardReaderListener;
//import com.isw.payapp.tasks.CardReaderTask;
//import com.isw.payapp.terminal.model.PayData;
//import com.telpo.emv.EmvService;
//import com.telpo.pinpad.PinpadService;
//
//import java.util.concurrent.Callable;
//
//public class PaymentProcessorBk implements Callable<String> {
//    private Context context;
//    private PayData payData;
//    private Handler handler;
//    Bitmap bitmap;
//    private final Handler uiHandler;
//    private final MyProgressDialog progressDialog;
//
//    boolean waitsign = true;
//
////    MyProgressDialog progressDialog = null;
////    WritePadDialog writePadDialog = null;
//
//    public PaymentProcessorBk(Context context, Handler uiHandler, MyProgressDialog progressDialog, PayData payData){
//        this.context = context;
//        this.payData = payData;
//        this.uiHandler =uiHandler;
//        this.progressDialog = progressDialog;
//
//    }
//
//
//    @Override
//    public String call() throws Exception {
////        int ret = initCardReader();
////        if(ret != EmvService.EMV_TRUE)
////            return "FALSE";
//        //
//        CardReaderTask task = new CardReaderTask(context,uiHandler,progressDialog,payData);
//        CardReaderListener listener = new CardReaderListener() {
//            private  MyProgressDialog progressDialog = null;
//
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
//
//    private void close(){
//        PinpadService.Close();
//        EmvService.deviceClose();
//    }
//}
