package com.isw.payapp.interfaces;

public interface CardReaderListener {
    void onProgressUpdate(int progress) throws InterruptedException;
    void onProgressEnd();
    void onProgressStart(String message);
    void onRequestSent(String message);
    void onCardDataRead(String cardData);
    void onRequestComplete(String response);
//    void onError(String errorMessage);
//    void showProgressDialog(String message);
//    void dismissProgressDialog();

}
