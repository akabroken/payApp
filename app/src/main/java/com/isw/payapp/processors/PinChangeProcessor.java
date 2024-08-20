package com.isw.payapp.processors;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.interfaces.CardReaderListener;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.tasks.CardReaderJob;
import com.isw.payapp.terminal.model.PayData;

import java.util.concurrent.Callable;

import lombok.SneakyThrows;

public class PinChangeProcessor implements Callable<String> {

    private Context context;
    private PayData payData;
    private Handler handler;
    Bitmap bitmap;
    private  Handler uiHandler;
    private final ProgressListener cListener;
    private CardReaderJob cardReaderJob;
    private final MyProgressDialog progressDialog;
    private CardReaderListener cardListener;

    public PinChangeProcessor(Context context,MyProgressDialog progressDialog,ProgressListener cListener,PayData payData){
        this.cListener = cListener;
        this.context = context;
        this.progressDialog = progressDialog;
        this.payData = payData;
    }


    @Override
    public String call() throws Exception {
        String  wait = null;
        //
        cardReaderJob = new CardReaderJob(context,payData);
        cardListener = new CardReaderListener() {
            @Override
            public void onProgressUpdate(int progress) throws InterruptedException {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //   progressDialog = new MyProgressDialog(context);
                        progressDialog.setTitle("Starting activity");
                        progressDialog.setCancelable(true);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage("Please wait,, "+ progress);
                        progressDialog.show();
                    }
                });
                Thread.sleep(100);
            }

            @Override
            public void onProgressEnd() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //
                        if(progressDialog ==null)
                            //   progressDialog = new MyProgressDialog(context);

                            progressDialog.dismiss();
                        return;
                    }
                });
            }

            @Override
            public void onProgressStart(String message) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //   progressDialog = new MyProgressDialog(context);
                        progressDialog.setTitle("Initializing..");
                        progressDialog.setCancelable(true);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage(message);
                        progressDialog.show();
                    }
                });
            }

            @Override
            public void onRequestSent(String message) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        //   progressDialog = new MyProgressDialog(context);
                        progressDialog.setTitle("Sending request");
                        progressDialog.setCancelable(true);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage(message);
                        progressDialog.show();
                        if(message.equals("Complete")){
                            Thread.sleep(1000);
                            progressDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void onCardDataRead(String cardData) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //   progressDialog = new MyProgressDialog(context);
                        progressDialog.setTitle("Card read..");
                        progressDialog.setCancelable(true);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage(cardData);
                        progressDialog.show();
                        if(cardData.equals("CardDetect"))
                            try {
                                Thread.sleep(1000);
                                progressDialog.dismiss();
                            }catch (Exception e){ e.printStackTrace();}

                    }

                });
            }

            @Override
            public void onRequestComplete(String response) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //   progressDialog = new MyProgressDialog(context);
                        progressDialog.setTitle("Process complete");
                        progressDialog.setCancelable(true);
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setMessage(response);
                        progressDialog.show();
                        try{
                            Thread.sleep(1000);
                            progressDialog.dismiss();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        cardReaderJob.setListner(cardListener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    String result = cardReaderJob.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(100);
        cListener.onProgressEnd();
        return "Transaction Completed";
    }
}
