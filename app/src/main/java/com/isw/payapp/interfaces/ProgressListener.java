package com.isw.payapp.interfaces;

public interface ProgressListener {
    void onProgressUpdate(int progress);
    void onProgressEnd() throws InterruptedException;
//    void onProgressUpdate(String message);
//    void onRequestSent(String message);
//    void onCardDataRead(String cardData);
//    void onRequestComplete(String response);
//    void onError(String errorMessage);
//    void showProgressDialog(String message);
//    void dismissProgressDialog();

}
