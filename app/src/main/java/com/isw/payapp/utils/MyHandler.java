package com.isw.payapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.isw.payapp.R;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.dialog.WritePadDialog;

public class MyHandler extends Handler {


    private Context context;
    Bitmap bitmap;
    private MyProgressDialog progressDialog;
    private WritePadDialog writePadDialog;

    public MyHandler (Context context){
        this.context = context;
    }

    public MyHandler(Context context, MyProgressDialog progressDialog, WritePadDialog writePadDialog ) {
        this.context = context;
        this.progressDialog = progressDialog;
        this.writePadDialog = writePadDialog;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                showProgressDialog();
                break;
            case 2:
                showWritePadDialog();
                break;
            case 3:
                showUnsupportedCardDialog((String) msg.obj);
                break;
            case 4:
                showTransComplete();
            case 5:
                showProgressDialogWithText("Receiving data");
                break;
            case 6:
                showTransactionDeclinedDialog();
                break;
            case 7:
                closeWindows();
                break;
            case 8:
                getReadCard((String) msg.obj);
                break;
            default:
              //  closeWindows();
                break;
        }
    }

    private void showProgressDialog() {
        progressDialog = new MyProgressDialog(context);
        progressDialog.setTitle(context.getString(R.string.server_connecting));
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(context.getString(R.string.please_wait));
        progressDialog.show();
    }

    private void showWritePadDialog() {
        writePadDialog.setCancelable(true);
        writePadDialog.setCanceledOnTouchOutside(false);
        writePadDialog.show();
    }

    private void showUnsupportedCardDialog(String cardInfo) {
        new AlertDialog.Builder(context)
                .setMessage("Unsupported contactless card: " + cardInfo)
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void showTransComplete() {
        new AlertDialog.Builder(context)
                .setMessage("Transaction Complete: ")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void closeWindows() {
        // Implement your logic to close windows/dialogs here
    }

    private void showProgressDialogWithText(String message) {
        progressDialog.setTitle(message);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(context.getString(R.string.please_wait));
        progressDialog.show();
    }

    private void showTransactionDeclinedDialog() {
        progressDialog.setTitle("Response");
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.setMessage("Transaction Declined");
        progressDialog.show();
    }

    private void getReadCard(String msg){
        progressDialog.setTitle("Read card");
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(msg);
    }
}
