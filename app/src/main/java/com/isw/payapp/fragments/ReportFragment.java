package com.isw.payapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.database.TransactionDatabaseHelper;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReportFragment extends Fragment implements EmvServiceCallback {

    private static final String TAG = "ReportFragment";

    private LinearLayout transactionListLayout;
    private IEmvProcessor emvProcessor;
    private ProgressBar progressBar;
    private Future<String> futureTask;
    private TransactionData transactionData;
    private SessionManager sessionManager;
    private TerminalConfig terminalConfig;
    private TextView noTransactionsText;
    private EditText purchaseEditText;
    private View scvPayText;
    private TextView tvReceipt;
    private Button btnSendReceipt;
    private MediaPlayer OKplayer;
    private MediaPlayer FAILplayer;
    private MediaPlayer stopPlayer;
    private MediaPlayer rejectPlayer;
    private TextView summaryText;
    private Button btnClearAll;

    private TransactionDatabaseHelper dbHelper;
    private Context context;
    private MyProgressDialog progressDialog = null;
    private boolean isTransactionInProgress = false;
    private boolean isProgressShowing = false;
    private ExecutorService executorService;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Debug arguments
        debugArguments();
        // Initialize views
        transactionListLayout = view.findViewById(R.id.transaction_list_layout);
        noTransactionsText = view.findViewById(R.id.no_transactions_text);
        summaryText = view.findViewById(R.id.summary_text);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        purchaseEditText = view.findViewById(R.id.purchaseEditText);
        scvPayText = view.findViewById(R.id.scvPayText);
        tvReceipt = view.findViewById(R.id.tvReceipt);
        btnSendReceipt = view.findViewById(R.id.btnSendReceipt);



        // Initialize database helper
        dbHelper = new TransactionDatabaseHelper(context);
        sessionManager = new SessionManager(requireContext());
        terminalConfig = new TerminalConfig();
        progressDialog = createProgressDialog();
        executorService = Executors.newSingleThreadExecutor();

        // Set up button listener
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());

        // Load transactions
        loadTransactions();

        return view;
    }

    private MyProgressDialog createProgressDialog() {
        try {
            if (!isAdded() || getActivity() == null) {
                return null;
            }

            MyProgressDialog.OnTimeOutListener timeOutListener = this::showTimeoutDialog;
            MyProgressDialog dialog = new MyProgressDialog(requireActivity(), 120000, timeOutListener);

            if (dialog.getWindow() != null) {
                WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.dimAmount = 0.7f;
                dialog.getWindow().setAttributes(layoutParams);
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnCancelListener(dialogInterface -> {
                if (isTransactionInProgress) {
                    showCancelConfirmationDialog();
                }
            });

            return dialog;
        } catch (Exception e) {
            Log.e(TAG, "Error creating progress dialog", e);
            return null;
        }
    }
    private void showTimeoutDialog() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.overTime)
                    .setMessage(R.string.transaction_timeout)
                    .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                    .setCancelable(false)
                    .show();
        });
    }

    private void showCancelConfirmationDialog() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.cancel_transaction)
                    .setMessage(R.string.cancel_reversal_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> navigateBack())
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        // Continue transaction
                        if (progressDialog != null && !progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                    })
                    .show();
        });
    }
    private void showErrorDialog(String message) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.error_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
    }

    private void showSuccess(String message) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.success)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
                    .setCancelable(false)
                    .show();

            // Play success sound
            if (OKplayer != null) {
                try {
                    OKplayer.start();
                } catch (Exception e) {
                    Log.e(TAG, "Error playing success sound", e);
                }
            }
        });
    }

    private void showProgressBar(boolean show) {
        if (progressBar != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        }
    }


    private void showProgress(String message) {
        Log.d(TAG, "showProgress called: " + message);
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached. Skipping showProgress.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity not available. Skipping progress dialog.");
            return;
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) {
                Log.w(TAG, "Fragment detached during UI thread execution.");
                return;
            }

            try {
                if (progressDialog == null) {
                    progressDialog = createProgressDialog();
                    Log.d(TAG, "Created new progress dialog");
                }

                if (progressDialog != null) {
                    if (!isProgressShowing) {
                        progressDialog.setMessage(message);
                        progressDialog.show();
                        isProgressShowing = true;
                        Log.d(TAG, "Showing progress dialog: " + message);
                    } else {
                        progressDialog.setMessage(message);
                        Log.d(TAG, "Updated progress dialog: " + message);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing progress: " + e.getMessage(), e);
            }
        });
    }

    private void hideProgress() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached. Skipping hideProgress.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "Activity is null. Skipping UI update.");
            return;
        }

        activity.runOnUiThread(() -> {
            try {
                if (progressDialog != null && isProgressShowing) {
                    progressDialog.dismiss();
                    isProgressShowing = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding progress: " + e.getMessage());
            }
        });
    }


    private void handleError(Exception e) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached. Skipping handleError.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity not available. Skipping UI error handling.");
            return;
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) {
                Log.w(TAG, "Fragment detached during UI thread execution. Skipping dialog.");
                return;
            }

            String message = e.getMessage() != null ? e.getMessage() : getString(R.string.unknown_error);

            // Play error sound
            if (FAILplayer != null) {
                try {
                    FAILplayer.start();
                } catch (Exception ex) {
                    Log.e(TAG, "Error playing fail sound", ex);
                }
            }

            new AlertDialog.Builder(activity)
                    .setTitle(R.string.error_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        // Re-enable sale button on error
                        showProgressBar(false);
                    })
                    .setCancelable(false)
                    .show();
        });
    }


    private void cleanupResources() {
        isTransactionInProgress = false;
        hideProgress();
        showProgressBar(false);

        if (futureTask != null && !futureTask.isDone()) {
            futureTask.cancel(true);
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = null;

        // Release media players
        releaseMediaPlayer(OKplayer);
        releaseMediaPlayer(FAILplayer);
        releaseMediaPlayer(stopPlayer);
        releaseMediaPlayer(rejectPlayer);

        OKplayer = null;
        FAILplayer = null;
        stopPlayer = null;
        rejectPlayer = null;

        // Re-enable sale button

    }

    private void releaseMediaPlayer(MediaPlayer player) {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media player", e);
            }
        }
    }

    private void navigateBack() {
      //  cleanupResources();
        if (!isAdded() || isStateSaved()) {
            Log.w(TAG, "Fragment detached or state already saved. Skipping navigation.");
            return;
        }

        try {
            NavHostFragment.findNavController(this).navigateUp();
        } catch (IllegalStateException e) {
            Log.e(TAG, "NavController not available.", e);
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.onBackPressed();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh transactions when fragment resumes
        loadTransactions();
    }

    private void loadTransactions() {
        // Clear existing views
        transactionListLayout.removeAllViews();

        // Get all transactions from database
        List<Receipt> transactions = dbHelper.getAllTransactions();

        if (transactions.isEmpty()) {
            noTransactionsText.setVisibility(View.VISIBLE);
            summaryText.setText("No transactions found");
            return;
        }

        noTransactionsText.setVisibility(View.GONE);

        // Update summary
        String summary = String.format(Locale.getDefault(),
                "Total Transactions: %d", transactions.size());
        summaryText.setText(summary);

        // Add each transaction to the list
        for (Receipt receipt : transactions) {
            addTransactionItem(receipt);
        }
    }

    private void addTransactionItem(Receipt receipt) {
        // Create transaction item layout
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_transactions, transactionListLayout, false);

        // Get views from item layout
        TextView tvCardNumber = itemView.findViewById(R.id.tv_card_number);
        TextView tvAmount = itemView.findViewById(R.id.tv_amount);
        TextView tvDateTime = itemView.findViewById(R.id.tv_date_time);
        TextView tvCardholder = itemView.findViewById(R.id.tv_cardholder_name);
        Button btnDetails = itemView.findViewById(R.id.btn_details);

        // Set data
        tvCardNumber.setText(receipt.getCardNumber());

        String amountText = receipt.getAmount();
        if (amountText == null || amountText.isEmpty()) {
            amountText = "0.00";
        }
        tvAmount.setText(String.format("%s %s", amountText, receipt.getCurrency()));

        // Format date-time (show only time if it's today)
        String formattedDateTime = formatDateTime(receipt.getDateTime());
        tvDateTime.setText(formattedDateTime);

        // Cardholder name (you might want to get this from elsewhere)
        tvCardholder.setText("Card Holder");

        // Set up details button click listener
        btnDetails.setOnClickListener(v -> showTransactionDetails(receipt));

        // Add item to list
        transactionListLayout.addView(itemView);
    }

    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat;

            Date date = inputFormat.parse(dateTime);
            if (date == null) return dateTime;

            // Check if date is today
            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateOnly.format(new Date());

            if (dateTime.startsWith(today)) {
                // Show only time for today's transactions
                outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            } else {
                // Show date and time for older transactions
                outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            }

            return outputFormat.format(date);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
            return dateTime;
        }
    }

    private void showTransactionDetails(Receipt receipt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Transaction Details");

        // Create detailed view
        View detailsView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_transaction_details, null);

        // Set all details
        TextView tvCardNumber = detailsView.findViewById(R.id.detail_card_number);
        TextView tvAmount = detailsView.findViewById(R.id.detail_amount);
        TextView tvDateTime = detailsView.findViewById(R.id.detail_date_time);
        TextView tvMerchant = detailsView.findViewById(R.id.detail_merchant);
        TextView tvTerminalId = detailsView.findViewById(R.id.detail_terminal_id);
        TextView tvTransactionType = detailsView.findViewById(R.id.detail_transaction_type);
        TextView tvEntryMode = detailsView.findViewById(R.id.detail_entry_mode);
        TextView tvAid = detailsView.findViewById(R.id.detail_aid);
        TextView tvAtc = detailsView.findViewById(R.id.detail_atc);
        TextView tvTvr = detailsView.findViewById(R.id.detail_tvr);
        TextView tvResponse = detailsView.findViewById(R.id.detail_response);
        TextView tvStan = detailsView.findViewById(R.id.detail_stan);
        TextView tvAuthId = detailsView.findViewById(R.id.detail_auth_id);
        TextView tvReferenceNumber = detailsView.findViewById(R.id.detail_reference_number);

        // Set values
        tvCardNumber.setText(receipt.getCardNumber());
        tvAmount.setText(String.format("%s %s", receipt.getAmount(), receipt.getCurrency()));
        tvDateTime.setText(receipt.getDateTime());
        tvMerchant.setText(receipt.getMerchant());
        tvTerminalId.setText(receipt.getTerminalId());
        tvTransactionType.setText(receipt.getTransactionType());
        tvEntryMode.setText(receipt.getEntryMode());
        tvAid.setText(receipt.getAid());
        tvAtc.setText(receipt.getAtc());
        tvTvr.setText(receipt.getTvr());
        tvResponse.setText(receipt.getResponse());
        tvStan.setText(receipt.getStan());
        tvAuthId.setText(receipt.getAuthId());
        tvReferenceNumber.setText(receipt.getReferenceNumber());

        builder.setView(detailsView);

        // Add action buttons
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Check if transaction is from today for reversal option
        boolean isToday = isTransactionToday(receipt.getDateTime());


        if (isToday && !receipt.getTransactionType().equals("Pin Select")) {
            builder.setNegativeButton("Reversal", (dialog, which) -> {
                // Pass data to Reversal fragment
                navigateToReversalWithData(receipt);
            });
        }

        builder.setNeutralButton("Reprint", (dialog, which) -> {
            // Reprint receipt
            try {
                reprintReceipt(receipt);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLUE);
    }

    private boolean isTransactionToday(String dateTime) {
        try {
            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateOnly.format(new Date());

            return dateTime.startsWith(today);

        } catch (Exception e) {
            Log.e(TAG, "Error checking if transaction is from today", e);
            return false;
        }
    }

    private void navigateToReversalWithData(Receipt receipt) {
        try {
            // Create a bundle with the required data
            Bundle bundle = new Bundle();
            bundle.putString("amount", receipt.getAmount());
            bundle.putString("stan", receipt.getStan());
            bundle.putString("authId", receipt.getAuthId());
            bundle.putString("referenceNumber", receipt.getReferenceNumber());

            // Debug log
            Log.d("ReportFragment", "Passing to Reversal - Amount: " + receipt.getAmount() +
                    ", STAN: " + receipt.getStan() +
                    ", AuthId: " + receipt.getAuthId());
            if (transactionData == null) {
                transactionData = new TransactionData();
            }

            transactionData.setAmount(receipt.getAmount());
            transactionData.setAuthCode(receipt.getAuthId());
            transactionData.setTransCnt( receipt.getStan());
            transactionData.setPaymentApp(ConstValues.PAY_APP_REVERSAL);
            transactionData.setPaymentReqTag(ConstValues.POST_PAY_REVERSAL);
            transactionData.setTransactionType("Reversal");

            if (getActivity() != null) {
                emvProcessor = DeviceFactory.createEmvFunc(getActivity(), transactionData, this);
                if (emvProcessor != null) {
                    setupEmvProcessorViews();
                    Log.d(TAG, "EMV processor initialized successfully");
                } else {
                    Log.e(TAG, "Failed to create EMV processor");
                    showErrorDialog("Failed to initialize payment processor");
                }
            }

            if (emvProcessor == null) {
                showErrorDialog("Payment processor not available");
                return;
            }




            // Update emvProcessor with new transaction data if needed
            try {
                Class<?> processorClass = emvProcessor.getClass();
                Method setTransactionDataMethod = processorClass.getMethod("setTransactionData", TransactionData.class);
                setTransactionDataMethod.invoke(emvProcessor, transactionData);
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "setTransactionData method not available, continuing...");
            } catch (Exception e) {
                Log.e(TAG, "Error setting transaction data", e);
            }

            executorService.execute(() -> {
                try {
                    // Initialize device and services
                    emvProcessor.initializeDevice();
                    emvProcessor.initializeEmvService();
                    emvProcessor.startEmvService();

                    // Process the reversal
                    // You might need to call a specific method for reversal
                    // Check if there's a method like processReversal() in IEmvProcessor
                    Method processMethod = null;
                    try {
                        processMethod = emvProcessor.getClass().getMethod("processReversal");
                    } catch (NoSuchMethodException e) {
                        Log.d(TAG, "No processReversal method found, using startEmvService");
                        // If no specific reversal method, the service should handle it based on transaction data
                    }

                    if (processMethod != null) {
                        String result = (String) processMethod.invoke(emvProcessor);
                        handleReversalResult(result);
                    } else {
                        // The reversal will be processed through callbacks
                        Log.d(TAG, "Reversal transaction started via EMV service");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error in reversal transaction", e);
                    handleError(e);
                } finally {
                    if (executorService != null && !executorService.isShutdown()) {
                        executorService.shutdown();
                    }
                }
            });

        } catch (Exception e) {
            Log.e("ReportFragment", "Error navigating to reversal", e);
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleReversalResult(String result) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            isTransactionInProgress = false;
            hideProgress();
            showProgressBar(false);

            if (result != null && (result.contains("Success") || result.contains("Approved"))) {
                showSuccess(getString(R.string.reversal_successful));
            } else {
                String errorMessage = result != null ? result : getString(R.string.reversal_failed);
                showErrorDialog(errorMessage);
            }

        });
    }

    private void setupEmvProcessorViews() {
        try {
            if (emvProcessor == null) {
                Log.w(TAG, "EMV processor is null, cannot set views");
                return;
            }

            Class<?> processorClass = emvProcessor.getClass();
            Method setViewsMethod = processorClass.getMethod(
                    "setViews",
                    EditText.class,
                    View.class,
                    View.class,
                    View.class
            );

            if (purchaseEditText == null || scvPayText == null ||
                    tvReceipt == null || btnSendReceipt == null) {
                Log.e(TAG, "One or more views are null, cannot set views");
                return;
            }

            setViewsMethod.invoke(
                    emvProcessor,
                    purchaseEditText,
                    scvPayText,
                    tvReceipt,
                    btnSendReceipt
            );

            Log.d(TAG, "Successfully set views for EMV processor");

        } catch (NoSuchMethodException e) {
            Log.d(TAG, "setViews method not available for EMV processor");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up EMV processor views", e);
        }
    }

    private void debugArguments() {
        if (getArguments() != null) {
            Bundle args = getArguments();
            for (String key : args.keySet()) {
                Object value = args.get(key);
                Log.d(TAG, "Argument - " + key + ": " + value + " (type: " +
                        (value != null ? value.getClass().getSimpleName() : "null") + ")");
            }
        } else {
            Log.d(TAG, "No arguments bundle found");
        }
    }

    private void reprintReceipt(Receipt receipt) throws Exception{
        // TODO: Implement receipt reprinting
        // You'll need to call your printer service here
        Toast.makeText(context, "Reprinting receipt...", Toast.LENGTH_SHORT).show();

        IPrinterProcessor iPrinterProcessor = DeviceFactory.createPrinter(context);
        iPrinterProcessor.initializePrinter();
        iPrinterProcessor.printReceipt(receipt);


        // Example: You might want to call a service or method from your FeitianEmvService
        // printReceipt(receipt);
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(context)
                .setTitle("Clear All Transactions")
               // .setMessage("Are you sure you want to delete all transaction records? This action cannot be undone.")
                .setMessage("Disabled for now")
                .setPositiveButton("Yes", (dialog, which) -> {
                   // dbHelper.clearAllTransactions();
                    loadTransactions(); // Refresh the list
                    Toast.makeText(context, "All transactions cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onShowPinPad(boolean show) {
        if (show) {
            hideProgress();
            showProgress(getString(R.string.please_input_pin));
        } else {
            hideProgress();
        }
    }

    @Override
    public void onLoading(String message) {
        showProgress(message);
    }

    @Override
    public void onStopLoading() {
        hideProgress();
        showProgressBar(false);
    }

    @Override
    public void onTransactionSuccess(String content) {
        showSuccess(getString(R.string.transaction_successful));
    }

    @Override
    public void onTransactionFailed(String errorMessage) {
        handleError(new RuntimeException(errorMessage));
    }

    @Override
    public void onTitleTextChanged(String title) {
        showProgress(title);
    }

    @Override
    public void onSendDingTalkMessage(boolean success, String data) {
        // Implement if needed
        Log.d(TAG, "DingTalk message sent: " + success + ", data: " + data);
    }

    @Override
    public void onShowPinPadWithKeyboard(List<String> dataList, boolean isOnlinePin, boolean isChangePin) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            String message = isChangePin ?
                    getString(R.string.input_pin_for_change) :
                    getString(R.string.please_input_pin);
            showProgress(message);
        });
    }

    @Override
    public void onPinInputReceived(String value) {
        // Handle PIN input if needed
        Log.d(TAG, "PIN input received: " + value);
    }

    @Override
    public void onError(String value) {
        handleError(new RuntimeException(value));
    }

    @Override
    public void onTransactionCancelled() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached. Skipping onTransactionCancelled UI.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "Activity is null. Skipping UI update.");
            return;
        }

        activity.runOnUiThread(() -> {
            isTransactionInProgress = false;
            hideProgress();
            showProgressBar(false);
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.transaction_cancelled)
                    .setMessage(R.string.transaction_cancelled_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
                    .show();
        });
    }

    @Override
    public void onDeviceConnected(String res) {
        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) {
            Log.w(TAG, "Fragment not in valid state for device connected message");
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getContext() == null) {
                return;
            }

            try {
                showProgress(getString(R.string.device_connected));
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error showing device connected progress", e);
            }
        });
    }

    @Override
    public void onDeviceDisconnected(String res) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached. Skipping onDeviceDisconnected.");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context is null. Skipping error handling.");
            return;
        }

        String message = context.getString(R.string.device_disconnected);
        handleError(new RuntimeException(message));
    }
    @Override
    public void onWaitingStatusChanged(boolean waiting) {
        if (!isAdded() || isStateSaved()) {
            Log.w(TAG, "Fragment not attached or state saved. Skipping UI update.");
            return;
        }

        if (waiting) {
            Context context = getContext();
            if (context != null) {
                showProgress(context.getString(R.string.waiting_for_card));
            } else {
                Log.w(TAG, "Context is null. Cannot show progress message.");
            }
        } else {
            hideProgress();
        }
    }
}