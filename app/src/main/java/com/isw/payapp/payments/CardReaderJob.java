package com.isw.payapp.payments;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.interfaces.CardReaderListener;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.Transaction;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class CardReaderJob implements Callable<String> {
    private static final String TAG = "CardReaderJob";

    private final Context context;
    private final TransactionData payData;
    private final TerminalConfig terminalConfig;
    private final CardReaderListener listener;

    private IPrinterProcessor printerFactory;
    private IEmvProcessor emvFactory;

    private String transactionResponse = "";
    private String pan = "";
    private volatile boolean isTransactionComplete = false;

    private Activity activity;

    public CardReaderJob(Context context, TransactionData payData, CardReaderListener listener) {
        Log.i(TAG, "Initializing CardReaderJob");
        this.context = context.getApplicationContext();
        this.payData = payData;
        this.listener = listener;
        this.terminalConfig = new TerminalConfig();

        // Create the callback first, then pass it to the factory
        EmvServiceCallback callback = createEmvServiceCallback();
        //emvFactory = DeviceFactory.createEmvFunc(context., payData, callback);
    }

    @Override
    public String call() {
        try {
            Log.i(TAG, "Starting card reading job");

            if (listener != null) {
                listener.onProgressStart("Initializing payment terminal...");
            }

            initializeEmvService();

            // Wait a moment for initialization
            Thread.sleep(2000);

            processIccCard();

            closeResources();

            Log.i(TAG, "Card reading job completed");
            return "Complete";

        } catch (Exception e) {
            Log.e(TAG, "Error in card reading job: " + e.getMessage(), e);
            if (listener != null) {
                listener.onRequestComplete("Error: " + e.getMessage());
            }
            return "Error: " + e.getMessage();
        }
    }

    private EmvServiceCallback createEmvServiceCallback() {
//        return new EmvServiceCallback() {
//            @Override
//            public void onWaitingStatusChanged(boolean waiting) {
//                Log.d(TAG, "Waiting status changed: " + waiting);
//                if (listener != null) {
//                    listener.onProgressStart(waiting ? "Waiting for card..." : "Processing...");
//                }
//            }
//
//            @Override
//            public void onShowPinPad(boolean show) {
//                Log.d(TAG, "Show PIN pad: " + show);
//                if (listener != null) {
//                    listener.onProgressStart(show ? "Please enter PIN" : "PIN entry completed");
//                }
//            }
//
//            @Override
//            public void onLoading(String message) {
//                Log.d(TAG, "Loading: " + message);
//                if (listener != null) {
//                    listener.onProgressStart(message);
//                }
//            }
//
//            @Override
//            public void onStopLoading() {
//                Log.d(TAG, "Loading stopped");
//                if (listener != null) {
//                    listener.onProgressStart("Processing completed");
//                }
//            }
//
//            @Override
//            public void onTransactionSuccess(String content) {
//                Log.i(TAG, "Transaction success: " + content);
//                transactionResponse = content;
//                isTransactionComplete = true;
//
//                if (listener != null) {
//                    listener.onProgressStart("Transaction approved");
//                    // Don't complete here - wait for processing in processIccCard
//                }
//            }
//
//            @Override
//            public void onTransactionFailed(String message) {
//                Log.w(TAG, "Transaction failed: " + message);
//                transactionResponse = "ERROR|" + message;
//                isTransactionComplete = true;
//
//                if (listener != null) {
//                    listener.onRequestComplete("Transaction failed: " + message);
//                }
//            }
//
//            @Override
//            public void onTitleTextChanged(String title) {
//                Log.d(TAG, "Title changed: " + title);
//                if (listener != null) {
//                    listener.onProgressStart(title);
//                }
//            }
//
//            @Override
//            public void onSendDingTalkMessage(boolean success, String data) {
//                Log.i(TAG, "DingTalk message status: " + success + ", Data: " + data);
//                // Handle DingTalk message if needed
//            }
//
//            @Override
//            public void onShowPinPadWithKeyboard(List<String> dataList, boolean isOnlinePin, boolean isChangePin) {
//
//            }
//
//            @Override
//            public void onPinInputReceived(String value) {
//
//            }
//        };
        return null;
    }

    private void initializeEmvService() throws Exception {
        Log.i(TAG, "Initializing EMV service");
        if (listener != null) {
            listener.onProgressStart("Initializing EMV service...");
        }

        if (emvFactory == null) {
            throw new Exception("EMV processor not initialized");
        }

        emvFactory.initializeEmvService();
    }

    private String processIccCard() throws Exception {
        Log.i(TAG, "Processing ICC card");
        if (listener != null) {
            listener.onProgressStart("Please insert/tap/swipe card...");
        }

        // Start the EMV service which will trigger card reading
        emvFactory.startEmvService();

        // Wait for the transaction to complete with timeout
        int timeout = 120000; // 2 minutes timeout for card reading
        int elapsed = 0;
        int checkInterval = 500; // Check every 500ms

        while (!isTransactionComplete && elapsed < timeout) {
            Thread.sleep(checkInterval);
            elapsed += checkInterval;

            // Update progress for long waits
            if (elapsed % 5000 == 0 && listener != null) {
                listener.onProgressStart("Waiting for card... (" + (elapsed/1000) + "s)");
            }
        }

        if (!isTransactionComplete) {
            throw new Exception("Transaction timeout - no response received after " + (timeout/1000) + " seconds");
        }

        if (transactionResponse.isEmpty()) {
            throw new Exception("Transaction completed but no response data");
        }

        return processTransactionResult(transactionResponse);
    }

    private String processTransactionResult(String transactionData) throws Exception {
        Log.i(TAG, "Processing transaction result: " + transactionData);

        if (transactionData == null || transactionData.isEmpty()) {
            throw new Exception("Transaction data is null or empty");
        }

        try {
            if (transactionData.startsWith("APPROVED|")) {
                // Successful transaction format: "APPROVED|transactionId|cardNumber"
                String[] parts = transactionData.split("\\|");
                if (parts.length >= 3) {
                    String transactionId = parts[1];
                    String cardNumber = parts[2];
                    pan = cardNumber; // Store the PAN for receipt printing

                    payData.setResponseCode("00");
                    payData.setResponseMsg("Approved");
                    payData.setAuthCode(transactionId);
                    payData.setRefferanceNo(transactionId);

                    // Generate a STAN (Systems Trace Audit Number)
                    String stan = generateStan();
                    payData.setStan(stan);

                    // Print receipt
                    printReceipt(cardNumber, transactionId, stan);

                    if (listener != null) {
                        listener.onRequestComplete("Transaction approved successfully");
                    }

                    return "APPROVED";
                } else {
                    throw new Exception("Invalid APPROVED response format");
                }

            } else if (transactionData.startsWith("DECLINED|")) {
                String declineReason = extractReason(transactionData, "DECLINED");
                payData.setResponseCode("05");
                payData.setResponseMsg(declineReason);
                throw new Exception("Transaction declined: " + declineReason);

            } else if (transactionData.startsWith("CANCELLED|")) {
                payData.setResponseCode("XX");
                payData.setResponseMsg("Cancelled by user");
                throw new Exception("Transaction cancelled by user");

            } else if (transactionData.startsWith("ERROR|")) {
                String errorReason = extractReason(transactionData, "ERROR");
                payData.setResponseCode("96");
                payData.setResponseMsg(errorReason);
                throw new Exception("Transaction error: " + errorReason);

            } else {
                throw new Exception("Unknown transaction response: " + transactionData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing transaction result", e);
            throw e;
        }
    }

    private String extractReason(String transactionData, String prefix) {
        if (transactionData.startsWith(prefix + "|")) {
            String[] parts = transactionData.split("\\|", 2);
            return parts.length > 1 ? parts[1] : prefix.toLowerCase();
        }
        return "Unknown reason";
    }

    private String generateStan() {
        // Generate a 6-digit STAN
        return String.format("%06d", System.currentTimeMillis() % 1000000);
    }

    private void printReceipt(String cardNumber, String transactionId, String stan) {
        try {
            Log.i(TAG, "Printing receipt for transaction: " + transactionId);

            String maskedPan = maskCardNumber(cardNumber);

            printerFactory = DeviceFactory.createPrinter(context);
            if (printerFactory == null) {
                Log.w(TAG, "Printer factory not available");
                return;
            }

            Receipt receipt = new Receipt();

            // Set receipt data
            receipt.getTransactionData().getCardModel().setPan(maskedPan);
            receipt.getTransactionData().setStan(stan);
            receipt.getTransactionData().setAuthCode(transactionId);
            receipt.getTransactionData().setRefferanceNo(transactionId);
            receipt.getTransactionData().setPaymentApp(payData.getPaymentApp());
            receipt.getTransactionData().setResponseCode(payData.getResponseCode());
            receipt.getTransactionData().setResponseMsg(payData.getResponseMsg());
            receipt.getTransactionData().setAmount(payData.getAmount());
            receipt.getTransactionData().setTranType(payData.getTranType());

            // Print the receipt
            printerFactory.printReceipt(receipt);

            Log.i(TAG, "Receipt printed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error printing receipt", e);
            // Don't throw exception for receipt printing failure
            if (listener != null) {
                listener.onProgressStart("Receipt printing failed: " + e.getMessage());
            }
        }
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber != null ? cardNumber : "UNKNOWN";
        }

        // Mask all but first 6 and last 4 digits
        String firstSix = cardNumber.substring(0, 6);
        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        return firstSix + "******" + lastFour;
    }

    private void closeResources() {
        Log.i(TAG, "Closing resources");
        try {
            if (emvFactory != null) {
                emvFactory.cancelTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing resources", e);
        }

        // Reset state for potential reuse
        transactionResponse = "";
        pan = "";
        isTransactionComplete = false;
    }

    // Helper method to cancel the transaction from outside
    public void cancelTransaction() {
        Log.i(TAG, "Cancelling transaction");
        isTransactionComplete = true;
        transactionResponse = "CANCELLED|Cancelled by user";
        closeResources();
    }
}