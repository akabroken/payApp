package com.isw.payapp.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentReversalBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.dialog.DialogListener;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.dialog.WritePadDialog;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Reversal extends Fragment implements EmvServiceCallback {

    private static final String TAG = "Reversal";
    private MyProgressDialog progressDialog = null;
    private ExecutorService executorService;
    private Future<String> futureTask;
    private SessionManager sessionManager;

    private EditText inputAmt, inputStan, inputAuth;
    private MaterialButton sale;
    private String Amount, stan, auth;
    private ImageView imageViewBack, imageViewExit;
    private ProgressBar progressBar;
    private TerminalConfig terminalConfig;
    private TransactionData transactionData;
    private IEmvProcessor emvProcessor;
    private MediaPlayer OKplayer;
    private MediaPlayer FAILplayer;
    private MediaPlayer stopPlayer;
    private MediaPlayer rejectPlayer;

    private Bitmap bitmap;
    private boolean isProgressShowing = false;
    private boolean isTransactionInProgress = false;
    private boolean waitsign = false;

    private WritePadDialog writePadDialog;

    private FragmentReversalBinding binding;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReversalBinding.inflate(inflater, container, false);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        setupViews();
        setupListeners();
        initializeReversalComponents();

        checkForIncomingData();
        showSoftKeyboardWithDelay();

    }

    private void checkForIncomingData() {
        Bundle args = getArguments();
        if (args != null) {
            Log.d(TAG, "Received arguments: " + args.keySet());

            String amount = args.getString("amount");
            String stan = args.getString("stan");
            String authId = args.getString("authId");
            String referenceNumber = args.getString("referenceNumber");

            Log.d(TAG, "Amount from args: " + amount);
            Log.d(TAG, "STAN from args: " + stan);
            Log.d(TAG, "Auth ID from args: " + authId);
            Log.d(TAG, "Reference Number from args: " + referenceNumber);

            // Set values if they exist
            if (amount != null && inputAmt != null) {
                Log.d(TAG, "Setting amount to: " + amount);
                inputAmt.setText(amount);
                // Trigger text change listener to format the amount
                if (amount.length() > 0) {
                    inputAmt.setSelection(amount.length());
                }
            } else {
                Log.d(TAG, "Amount is null or inputAmt is null");
            }

            if (stan != null && inputStan != null) {
                Log.d(TAG, "Setting STAN to: " + stan);
                inputStan.setText(stan);
            } else {
                Log.d(TAG, "STAN is null or inputStan is null");
            }

            if (authId != null && inputAuth != null) {
                Log.d(TAG, "Setting Auth ID to: " + authId);
                inputAuth.setText(authId);
            } else {
                Log.d(TAG, "Auth ID is null or inputAuth is null");
            }

            // Update button state
            updateButtonState();

            // Also update the class variables
            if (amount != null) Amount = amount;
            if (stan != null) this.stan = stan;
            if (authId != null) auth = authId;

        } else {
            Log.d(TAG, "No arguments received");
        }
    }

    private void initializeComponents(View view) {
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

        // Initialize views - USING BINDING
        if (binding != null) {
            inputAmt = binding.inputAmt;
            inputStan = binding.ostan;
            inputAuth = binding.oauth;
//            imageViewBack = binding.imageViewBack;
//            imageViewExit = binding.imageViewCancel;
            sale = binding.btnSaleStart;
            progressBar = binding.progressBar;
        }
        // Fallback to findViewById if binding fails
        else {
            inputAmt = view.findViewById(R.id.inputAmt);
            inputStan = view.findViewById(R.id.ostan);
            inputAuth = view.findViewById(R.id.oauth);
            imageViewBack = view.findViewById(R.id.imageViewBack);
            imageViewExit = view.findViewById(R.id.imageViewCancel);
            sale = view.findViewById(R.id.btn_saleStart);
            progressBar = view.findViewById(R.id.progressBar);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Debug log to check view initialization
        Log.d(TAG, "Views initialized - inputAmt: " + inputAmt +
                ", inputStan: " + inputStan +
                ", inputAuth: " + inputAuth);
    }
    private void initializeComponents(View view, Object o) {
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

        // Initialize views
        inputAmt = view.findViewById(R.id.inputAmt);
        inputStan = view.findViewById(R.id.ostan);
        inputAuth = view.findViewById(R.id.oauth);
        imageViewBack = view.findViewById(R.id.imageViewBack);
        imageViewExit = view.findViewById(R.id.imageViewCancel);
        sale = view.findViewById(R.id.btn_saleStart);
        progressBar = view.findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupViews() {
        if (inputAmt != null) {
            inputAmt.setSelection(inputAmt.getText().length());
            Amount = inputAmt.getText().toString();
        }

        if (sale != null) {
            sale.setEnabled(false);
        }
    }

    private void setupListeners() {
        setupAmountInputListener();
        setupStanInputListener();
        setupAuthInputListener();
        setupButtonListeners();
    }

    private void setupAmountInputListener() {
        if (inputAmt == null) return;

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
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isChanged) return;

                String str = s.toString();
                isChanged = true;

                // Format the amount input
                String formattedAmount = formatAmount(str);
                inputAmt.setText(formattedAmount);
                inputAmt.setSelection(inputAmt.getText().length());
                isChanged = false;

                updateButtonState();
            }
        });
    }

    private String formatAmount(String amountStr) {
        // Remove dots
        String cuttedStr = amountStr.replace(".", "");

        // Remove leading zeros
        int num = cuttedStr.length();
        int zeroIndex = -1;
        for (int i = 0; i < num - 2; i++) {
            char c = cuttedStr.charAt(i);
            if (c != '0') {
                zeroIndex = i;
                break;
            } else if (i == num - 3) {
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

        return cuttedStr;
    }

    private void setupStanInputListener() {
        if (inputStan != null) {
            inputStan.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateButtonState();
                }
            });
        }
    }

    private void setupAuthInputListener() {
        if (inputAuth != null) {
            inputAuth.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateButtonState();
                }
            });
        }
    }

    private void setupButtonListeners() {
        // Sale button
        if (sale != null) {
            sale.setOnClickListener(v -> {
                try {
                    processReversal();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Back button
        if (imageViewBack != null) {
            imageViewBack.setOnClickListener(view -> navigateBack());
        }

        // Exit button
        if (imageViewExit != null) {
            imageViewExit.setOnClickListener(view -> showExitConfirmationDialog());
        }
    }

    private void initializeReversalComponents() {
        try {
            if (!isAdded()) {
                Log.e(TAG, "Fragment not attached during initialization");
                return;
            }

            terminalConfig = new TerminalConfig();
            sessionManager = new SessionManager(requireContext());
            executorService = Executors.newSingleThreadExecutor();
            progressDialog = createProgressDialog();

            // Initialize transactionData here
            transactionData = new TransactionData(); // INITIALIZE HERE

            // Now create emvProcessor with initialized transactionData
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

    private void showSoftKeyboardWithDelay() {
        if (inputStan != null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            InputMethodManager inputManager = (InputMethodManager)
                                    getActivity().getSystemService(INPUT_METHOD_SERVICE);
                            if (inputManager != null && inputStan != null) {
                                inputManager.showSoftInput(inputStan, 0);
                            }
                        });
                    }
                }
            }, 300);
        }
    }

    private void updateButtonState() {
        if (sale != null && inputAmt != null && inputStan != null && inputAuth != null) {
            String amount = inputAmt.getText().toString().trim();
            String stan = inputStan.getText().toString().trim();
            String auth = inputAuth.getText().toString().trim();

            boolean isValidAmount = !amount.equals("0.00") && !amount.isEmpty();
            boolean hasStan = !stan.isEmpty();
            boolean hasAuth = !auth.isEmpty();



            sale.setEnabled(isValidAmount && hasStan && hasAuth);

            // Update button text based on state
            if (sale.isEnabled()) {
                sale.setText(getString(R.string.process_reversal));
            } else {
                sale.setText(getString(R.string.fill_all_fields));
            }
        }
    }

    private void processReversal() throws Exception {
        if (!validateInputs()) return;

        // ADD NULL CHECK for emvProcessor
        if (emvProcessor == null) {
            Log.e(TAG, "EMV processor is null, reinitializing...");
            initializeReversalComponents();
            if (emvProcessor == null) {
                showErrorDialog(getString(R.string.payment_processor_not_initialized));
                showProgressBar(false);
                if (sale != null) {
                    sale.setEnabled(true);
                }
                return;
            }
        }

        Log.i(TAG, "Processing reversal - STAN: " + stan + ", Auth: " + auth + ", Amount: " + Amount);

        // Disable button and show progress
        if (sale != null) {
            sale.setEnabled(false);
        }
        showProgressBar(true);
        showProgress(getString(R.string.processing_reversal));

        // Start reversal process
        startReversalTransaction();
    }

    private void startReversalTransaction() throws Exception {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        if (emvProcessor == null) {
            showErrorDialog("Payment processor not available");
            return;
        }

        isTransactionInProgress = true;

        // Set transaction data BEFORE starting the service
        if (transactionData == null) {
            transactionData = new TransactionData();
        }

        transactionData.setAmount(Amount);
        transactionData.setAuthCode(auth);
        transactionData.setTransCnt(stan);
        transactionData.setPaymentApp(ConstValues.PAY_APP_REVERSAL);
        transactionData.setPaymentReqTag(ConstValues.POST_PAY_REVERSAL);
        transactionData.setTransactionType("Reversal");

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
    }

    private boolean validateInputs() {
        if (inputAmt == null || inputStan == null || inputAuth == null) {
            showErrorDialog(getString(R.string.invalid_input_fields));
            return false;
        }

        Amount = inputAmt.getText().toString().trim();
        stan = inputStan.getText().toString().trim();
        auth = inputAuth.getText().toString().trim();

        // Validate inputs
        if (Amount.equals("0.00") || Amount.isEmpty()) {
            showErrorDialog(getString(R.string.invalid_amount));
            return false;
        }

        if (stan.isEmpty()) {
            showErrorDialog(getString(R.string.enter_stan));
            return false;
        }

        if (auth.isEmpty()) {
            showErrorDialog(getString(R.string.enter_auth_id));
            return false;
        }

        return true;
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

            // Re-enable button
            if (sale != null) {
                sale.setEnabled(true);
            }
        });
    }

    private void showExitConfirmationDialog() {
        if (!isAdded() || getActivity() == null) return;

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.exit_reversal)
                .setMessage(R.string.exit_reversal_message)
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
        if (sale != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> sale.setEnabled(true));
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
        cleanupResources();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        cleanupResources();
        binding = null;
        super.onDestroyView();
    }

    // Helper abstract class for simplified TextWatcher
    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
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