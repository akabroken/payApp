package com.isw.payapp.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPaymentBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.dialog.DialogListener;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.dialog.WritePadDialog;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Purchase extends Fragment implements EmvServiceCallback {

    private static final String TAG = "Purchase";
    private MyProgressDialog progressDialog = null;
    private TerminalConfig terminalConfig;
    private TransactionData transactionData;
    private ExecutorService executorService;
    private Future<String> futureTask;
    private IEmvProcessor emvProcessor;
    private SessionManager sessionManager;

    private TextView title_tv;
    private EditText inputAmt;
    private MaterialButton sale; // Changed to MaterialButton
    private String Amount;
    private ImageView imageViewBack, imageViewExit;
    private ProgressBar progressBar; // Added progress bar

    private MediaPlayer OKplayer;
    private MediaPlayer FAILplayer;
    private MediaPlayer notionPlayer;
    private MediaPlayer stopPlayer;
    private MediaPlayer rejectPlayer;

    private Bitmap bitmap;
    private boolean isProgressShowing = false;
    private boolean isTransactionInProgress = false;
    private boolean isTellerAuthenticated = false;
    private boolean userCancel = false;
    private boolean waitsign;

    private Handler handler;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager km;
    private WritePadDialog writePadDialog;

    private FragmentPaymentBinding binding;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize media players
        if (getActivity() != null) {
            OKplayer = MediaPlayer.create(getActivity(), R.raw.success1);
            FAILplayer = MediaPlayer.create(getActivity(), R.raw.fail1);
            stopPlayer = MediaPlayer.create(getActivity(), R.raw.trans_stop1);
            rejectPlayer = MediaPlayer.create(getActivity(), R.raw.trans_reject1);
        }




        // Initialize write pad dialog
        if (getActivity() != null) {
            writePadDialog = new WritePadDialog(getActivity(), new DialogListener() {
                @Override
                public void refreshActivity(Object object) {
                    if (object instanceof Bitmap) {
                        bitmap = (Bitmap) object;
                        bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 256);
                        waitsign = false;
                    }
                }
            });
        }

        // Initialize views - FIXED: Using correct view.findViewById
        inputAmt = view.findViewById(R.id.inputAmt);
        if (inputAmt != null) {
            inputAmt.setSelection(inputAmt.getText().length()); // Move cursor to end
            Amount = inputAmt.getText().toString();

            inputAmt.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (getActivity() != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity()
                                .getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(v, 0);
                        }
                        v.requestFocus();
                    }
                    return true;
                }
            });

            inputAmt.addTextChangedListener(new TextWatcher() {
                private boolean isChanged = false;

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isChanged) {
                        return;
                    }

                    String str = s.toString();
                    isChanged = true;
                    String cuttedStr = str;

                    // Remove dot from string
                    for (int i = str.length() - 1; i >= 0; i--) {
                        char c = str.charAt(i);
                        if ('.' == c) {
                            cuttedStr = str.substring(0, i) + str.substring(i + 1);
                            break;
                        }
                    }

                    // Delete leading zeros
                    int NUM = cuttedStr.length();
                    int zeroIndex = -1;
                    for (int i = 0; i < NUM - 2; i++) {
                        char c = cuttedStr.charAt(i);
                        if (c != '0') {
                            zeroIndex = i;
                            break;
                        } else if (i == NUM - 3) {
                            zeroIndex = i;
                            break;
                        }
                    }

                    if (zeroIndex != -1) {
                        cuttedStr = cuttedStr.substring(zeroIndex);
                    }

                    // Add leading zero if less than 3 digits
                    if (cuttedStr.length() < 3) {
                        cuttedStr = "0" + cuttedStr;
                    }

                    // Add decimal point for two decimal places
                    if (cuttedStr.length() >= 2) {
                        cuttedStr = cuttedStr.substring(0, cuttedStr.length() - 2)
                                + "." + cuttedStr.substring(cuttedStr.length() - 2);
                    }

                    inputAmt.setText(cuttedStr);
                    inputAmt.setSelection(inputAmt.getText().length());
                    isChanged = false;

                    if (sale != null) {
                        boolean isValidAmount = !cuttedStr.equals("0.00") && !cuttedStr.isEmpty();
                        sale.setEnabled(isValidAmount);

                        // Update button text based on amount
                        if (isValidAmount) {
                            sale.setText(getString(R.string.tap_card_to_pay));
                        } else {
                            sale.setText(getString(R.string.enter_amount_first));
                        }
                    }
                }
            });
        }

        sale = view.findViewById(R.id.btn_saleStart);
        if (sale != null) {
            sale.setEnabled(false);
            sale.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (inputAmt != null) {
                        Amount = inputAmt.getText().toString();

                        // Validate amount
                        if (Amount.equals("0.00") || Amount.isEmpty()) {
                            showErrorDialog(getString(R.string.invalid_amount));
                            return;
                        }

                        transactionData = new TransactionData();
                        transactionData.setAmount(Amount);
                        transactionData.setPaymentApp(ConstValues.PAY_APP_PURCHASE);
                        transactionData.setPaymentReqTag(ConstValues.POST_PAY_PURCHASE);
                        transactionData.setTransactionType("Purchase");


                        sale.setEnabled(false);
                        showProgressBar(true);
                        executorService = Executors.newSingleThreadExecutor();
                        initializeComponents();
                        startTransaction();
                    }
                }
            });
        }

        // Initialize progress bar
        progressBar = view.findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Delayed soft keyboard show
        if (inputAmt != null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            InputMethodManager inputManager = (InputMethodManager)
                                    getActivity().getSystemService(INPUT_METHOD_SERVICE);
                            if (inputManager != null && inputAmt != null) {
                                inputManager.showSoftInput(inputAmt, 0);
                            }
                        });
                    }
                }
            }, 300);
        }

        // Back button
        imageViewBack = view.findViewById(R.id.imageViewBack);
        if (imageViewBack != null) {
            imageViewBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateBack();
                }
            });
        }

        // Exit button
        imageViewExit = view.findViewById(R.id.imageViewCancel);
        if (imageViewExit != null) {
            imageViewExit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Show confirmation before exiting
                    showExitConfirmationDialog();
                }
            });
        }
    }

    private void showExitConfirmationDialog() {
        if (!isAdded() || getActivity() == null) return;

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.exit_transaction)
                .setMessage(R.string.exit_transaction_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> navigateBack())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
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

    private void showProgressBar(boolean show) {
        if (progressBar != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        }
    }

    private void initializeComponents() {
        try {
            if (!isAdded()) {
                Log.e(TAG, "Fragment not attached during initialization");
                return;
            }

            terminalConfig = new TerminalConfig();
            sessionManager = new SessionManager(requireContext());
            executorService = Executors.newSingleThreadExecutor();
            progressDialog = createProgressDialog();

            if (transactionData != null && getActivity() != null) {
                emvProcessor = DeviceFactory.createEmvFunc(getActivity(), transactionData, this);
                setupEmvProcessorViews();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            handleError(e);
        }
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

            if (binding.purchaseEditText == null || binding.scvPayText == null ||
                    binding.tvReceipt == null || binding.btnSendReceipt == null) {
                Log.e(TAG, "One or more views are null, cannot set views");
                return;
            }

            setViewsMethod.invoke(
                    emvProcessor,
                    binding.purchaseEditText,
                    binding.scvPayText,
                    binding.tvReceipt,
                    binding.btnSendReceipt
            );

            Log.d(TAG, "Successfully set views for EMV processor");

        } catch (NoSuchMethodException e) {
            Log.d(TAG, "setViews method not available for EMV processor");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up EMV processor views", e);
        }
    }

    private void setupEmvProcessorViews(String t) {
        try {
            if (emvProcessor == null) {
                Log.w(TAG, "EMV processor is null, cannot set views");
                return;
            }

            View view = getView();
            if (view == null) {
                Log.e(TAG, "View is null, cannot set views");
                return;
            }

            // FIXED: Using correct view IDs from the XML layout
            EditText pinpadEditText = view.findViewById(R.id.purchaseEditText); // Changed from pinpadEditText to purchaseEditText
            View scvText = view.findViewById(R.id.scvPayText); // Changed from scvText to scvPayText
            TextView tvReceipt = view.findViewById(R.id.tvReceipt);
            Button btnSendReceipt = view.findViewById(R.id.btnSendReceipt);

            if (pinpadEditText == null || scvText == null ||
                    tvReceipt == null || btnSendReceipt == null) {
                Log.w(TAG, "One or more EMV views are null, but continuing anyway");
                // Don't return, some views might be optional
            }

            Class<?> processorClass = emvProcessor.getClass();
            try {
                Method setViewsMethod = processorClass.getMethod(
                        "setViews",
                        EditText.class,
                        TextView.class,
                        TextView.class,
                        Button.class
                );

                setViewsMethod.invoke(
                        emvProcessor,
                        pinpadEditText,
                        scvText,
                        tvReceipt,
                        btnSendReceipt
                );

                Log.d(TAG, "Successfully set views for EMV processor");
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "setViews method not available for EMV processor, trying alternative methods");

                // Try alternative method signatures
                try {
                    Method setViewsMethod = processorClass.getMethod("setViews", EditText.class, TextView.class);
                    setViewsMethod.invoke(emvProcessor, pinpadEditText, scvText);
                    Log.d(TAG, "Successfully set views with alternative method");
                } catch (NoSuchMethodException e2) {
                    Log.d(TAG, "Alternative setViews method also not available");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up EMV processor views", e);
        }
    }

    private void startTransaction() {
        if (emvProcessor != null && transactionData != null) {
            isTransactionInProgress = true;
            showProgress(getString(R.string.processing_transaction));
            showProgressBar(true);

            // Start the transaction process here
            try {

                    emvProcessor.initializeDevice();
                    emvProcessor.initializeEmvService();
                    emvProcessor.startEmvService();

                // Assuming emvProcessor has a startTransaction method
//                Method startTransactionMethod = emvProcessor.getClass().getMethod("startTransaction", TransactionData.class);
//                startTransactionMethod.invoke(emvProcessor, transactionData);
            } catch (Exception e) {
                Log.e(TAG, "Error starting transaction", e);
                handleError(e);
            }
        } else {
            showErrorDialog(getString(R.string.transaction_initialization_failed));
            showProgressBar(false);
            if (sale != null) {
                sale.setEnabled(true);
            }
        }
    }

    private void showTimeoutDialog() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.overTime)
                    .setMessage(R.string.transaction_timeout)
                    .setPositiveButton(R.string.bn_confirm, (dialog, which) -> {
                        navigateBack();
                    })
                    .setCancelable(false)
                    .show();
        });
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

    private void showCancelConfirmationDialog() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.cancel_transaction)
                    .setMessage(R.string.cancel_transaction_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (emvProcessor != null) {
                            try {
                                emvProcessor.cancelTransaction();
                            } catch (Exception e) {
                                Log.e(TAG, "Error cancelling transaction", e);
                            }
                        }
                        navigateBack();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        // Continue transaction
                        if (progressDialog != null && !progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                    })
                    .show();
        });
    }

    private void showSuccess(String message) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            isTransactionInProgress = false;
            hideProgress();
            showProgressBar(false);
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

    private void navigateBack() {
        cleanupResources();
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
        releaseMediaPlayer(notionPlayer);

        OKplayer = null;
        FAILplayer = null;
        stopPlayer = null;
        rejectPlayer = null;
        notionPlayer = null;

        // Re-enable sale button if needed
        if (sale != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                sale.setEnabled(true);
            });
        }
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
                        if (sale != null) {
                            sale.setEnabled(true);
                        }
                        showProgressBar(false);
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    @Override
    public void onDestroy() {
        if (emvProcessor != null) {
            try {
                emvProcessor.cancelTransaction();
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling transaction on destroy", e);
            }
        }
        cleanupResources();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        cleanupResources();
        binding = null;
        super.onDestroyView();
    }
}