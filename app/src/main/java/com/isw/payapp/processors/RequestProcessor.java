package com.isw.payapp.processors;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.isw.payapp.R;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.interfaces.CardReaderListener;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.payments.CardReaderJob;
import com.isw.payapp.model.TransactionData;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RequestProcessor implements Callable<String> {
    private static final String TAG = "RequestProcessor";

    private final Context context;
    private final TransactionData payData;
    private final ProgressListener progressListener;
    private final MyProgressDialog progressDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CardReaderJob cardReaderJob;
    private Future<String> cardReaderFuture;
    private ExecutorService executorService;
    private volatile boolean isCancelled = false;

    public RequestProcessor(Context context,
                            MyProgressDialog progressDialog,
                            ProgressListener progressListener,
                            TransactionData payData) {
        this.context = context.getApplicationContext();
        this.progressDialog = progressDialog;
        this.progressListener = progressListener;
        this.payData = payData;
    }

    @Override
    public String call() throws Exception {
        Log.d(TAG, "Starting request processor");

        // Create executor service for card reader job
        executorService = Executors.newSingleThreadExecutor();

        try {
            // Create card reader job with listener
            cardReaderJob = new CardReaderJob(context, payData, createCardReaderListener());

            // Submit the job to executor
            cardReaderFuture = executorService.submit(cardReaderJob);

            // Wait for completion with timeout
            String result = cardReaderFuture.get(2, TimeUnit.MINUTES); // 2 minutes timeout

            Log.i(TAG, "Card reader job completed: " + result);

            // Notify progress listener on main thread
            mainHandler.post(() -> {
                if (progressListener != null) {
                    try {
                        progressListener.onProgressEnd();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            return "Transaction Completed: " + result;

        } catch (Exception e) {
            Log.e(TAG, "Error in request processor: " + e.getMessage(), e);

            // Handle cancellation specifically
            if (isCancelled) {
                mainHandler.post(() -> {
                    if (progressListener != null) {
                        try {
                            progressListener.onProgressEnd();
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
                return "Transaction Cancelled";
            }

            // Notify error on main thread
            mainHandler.post(() -> {
                if (progressListener != null) {
                    try {
                        progressListener.onProgressEnd();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    // You might want to add an error callback here
                }
            });

            throw e;

        } finally {
            closeResources();
        }
    }

    private CardReaderListener createCardReaderListener() {
        return new CardReaderListener() {
            @Override
            public void onProgressUpdate(int progress) {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.setTitle(context.getString(R.string.starting_activity));
                        progressDialog.setMessage(context.getString(R.string.please_wait, progress));
                        showProgressDialog();
                    }
                });
            }

            @Override
            public void onProgressEnd() {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.dismiss();
                    }
                    if (progressListener != null && !isCancelled) {
                        try {
                            progressListener.onProgressEnd();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public void onProgressStart(String message) {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.setTitle(context.getString(R.string.initializing));
                        progressDialog.setMessage(message);
                        showProgressDialog();
                    }
                });
            }

            @Override
            public void onRequestSent(String message) {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.setTitle(context.getString(R.string.sending_request));
                        progressDialog.setMessage(message);
                        showProgressDialog();

                        if ("Complete".equals(message)) {
                            dismissAfterDelay(1000);
                        }
                    }
                });
            }

            @Override
            public void onCardDataRead(String cardData) {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.setTitle(context.getString(R.string.card_read));
                        progressDialog.setMessage(cardData);
                        showProgressDialog();

                        if ("CardDetect".equals(cardData)) {
                            dismissAfterDelay(1000);
                        }
                    }
                });
            }

            @Override
            public void onRequestComplete(String response) {
                updateUI(() -> {
                    if (progressDialog != null && !isCancelled) {
                        progressDialog.setTitle(context.getString(R.string.process_complete));
                        progressDialog.setMessage(response);
                        showProgressDialog();
                        dismissAfterDelay(1000);
                    }

                    // Also notify progress listener
                    if (progressListener != null && !isCancelled) {
                        try {
                            progressListener.onProgressEnd();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }

    private void updateUI(Runnable action) {
        if (!isCancelled) {
            mainHandler.post(action);
        }
    }

    private void showProgressDialog() {
        if (progressDialog != null && !isCancelled) {
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            if (!progressDialog.isShowing()) {
                try {
                    progressDialog.show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing progress dialog", e);
                }
            }
        }
    }

    private void dismissAfterDelay(long delayMillis) {
        if (!isCancelled) {
            mainHandler.postDelayed(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing progress dialog", e);
                    }
                }
            }, delayMillis);
        }
    }

    private void closeResources() {
        Log.d(TAG, "Closing resources");

        try {
            // Cancel card reader job if it's still running
            if (cardReaderFuture != null && !cardReaderFuture.isDone()) {
                cardReaderFuture.cancel(true);
            }

            // Cancel the card reader job directly
            if (cardReaderJob != null) {
                cardReaderJob.cancelTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling card reader job", e);
        }

        try {
            // Shutdown executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor service did not terminate in time");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error shutting down executor service", e);
        }

        try {
            // Dismiss progress dialog
            if (progressDialog != null && progressDialog.isShowing()) {
                mainHandler.post(() -> {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing progress dialog in cleanup", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in progress dialog cleanup", e);
        }

        isCancelled = true;
    }

    // Public method to cancel the processor from outside
    public void cancel() {
        Log.i(TAG, "Cancelling request processor");
        isCancelled = true;
        closeResources();

        mainHandler.post(() -> {
            if (progressListener != null) {
                try {
                    progressListener.onProgressEnd();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    // Helper method to check if processor is cancelled
    public boolean isCancelled() {
        return isCancelled;
    }

    // Add missing import for Log

}