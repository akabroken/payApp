package com.isw.payapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPinSelectBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
//import com.isw.payapp.devices.dspread.DSpreadEmvService;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.TransactionData;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PinSelect extends Fragment implements EmvServiceCallback {

    private FragmentPinSelectBinding binding;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private Future<String> futureTask;

    private MyProgressDialog progressDialog;
    private IEmvProcessor emvProcessor;
    private TransactionData payData;
    private boolean isProgressShowing = false;
    private boolean isTransactionInProgress = false;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPinSelectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponents();
        setupClickListeners();
        startPaymentProcess();
    }

    private void startPaymentProcess() {
        try {
            if (emvProcessor == null) {
                handleError(new RuntimeException("EMV processor not initialized"));
                return;
            }

            isTransactionInProgress = true;
            showProgress(getString(R.string.initializing_device));

            // Initialize and start EMV process
            emvProcessor.initializeDevice();
            emvProcessor.initializeEmvService();
            emvProcessor.startEmvService();

        } catch (Exception e) {
            if (emvProcessor != null) {
                emvProcessor.cancelTransaction();
            }
            handleError(e);
        }
    }

    private void setupClickListeners() {
        binding.imageViewBack.setOnClickListener(v -> navigateBack());
        binding.imageViewCancel.setOnClickListener(v -> {
            if (isTransactionInProgress) {
                showCancelConfirmationDialog();
            } else {
                navigateBack();
            }
        });
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.confirm_cancel)
                .setMessage(R.string.cancel_transaction_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    cancelTransactionAndNavigateBack();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void cancelTransactionAndNavigateBack() {
        if (emvProcessor != null) {
            emvProcessor.cancelTransaction();
        }
        navigateBack();
    }

    private void initializeComponents() {
        updateProgressSafe(getString(R.string.initializing_device));
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        progressDialog = createProgressDialog();
        payData = createPayData();
        emvProcessor = DeviceFactory.createEmvFunc(requireActivity(), payData, this);

        try {
            // Use common interface or base class check first for better design
            if (emvProcessor == null) {
                Log.w("PinSelect", "EMV processor is null, cannot set views");
                return;
            }

            Class<?> processorClass = emvProcessor.getClass();

            // Check if the class has the setViews method using a safer approach
            java.lang.reflect.Method setViewsMethod = null;
            try {
                setViewsMethod = processorClass.getMethod(
                        "setViews",
                        EditText.class,
                        View.class,
                        View.class,
                        View.class
                );
            } catch (NoSuchMethodException e) {
                Log.d("PinSelect", "setViews method not available for " + processorClass.getSimpleName());
                return;
            }

            // Validate that all views are not null before invoking
            if (binding.pinpadEditText == null || binding.scvText == null ||
                    binding.tvReceipt == null || binding.btnSendReceipt == null) {
                Log.e("PinSelect", "One or more views are null, cannot set views");
                return;
            }

            // Invoke the method with null safety
            setViewsMethod.invoke(
                    emvProcessor,
                    binding.pinpadEditText,
                    binding.scvText,
                    binding.tvReceipt,
                    binding.btnSendReceipt
            );

            Log.d("PinSelect", "Successfully set views for " + processorClass.getSimpleName());

        } catch (java.lang.reflect.InvocationTargetException e) {
            // This catches exceptions thrown by the method itself
            Throwable targetException = e.getTargetException();
            Log.e("PinSelect", "Error in setViews method execution: " + targetException.getMessage(), targetException);
        } catch (IllegalAccessException e) {
            Log.e("PinSelect", "Illegal access to setViews method - method may be private", e);
        } catch (IllegalArgumentException e) {
            Log.e("PinSelect", "Invalid arguments passed to setViews method", e);
        } catch (Exception e) {
            Log.e("PinSelect", "Unexpected error setting views", e);
        }
    }

    private void setupDSpreadViewsIfAvailable() {
        try {
            // Check if DSpreadEmvService class exists in classpath
            Class<?> dspreadClass = Class.forName("com.isw.payapp.devices.dspread.DSpreadEmvService");

            // Check if current emvProcessor is an instance of DSpreadEmvService
            if (dspreadClass.isInstance(emvProcessor)) {
                // Get the setViews method
                Method setViewsMethod = dspreadClass.getMethod("setViews",
                        EditText.class, TextView.class, TextView.class, Button.class);

                // Invoke the method on the emvProcessor instance
                setViewsMethod.invoke(emvProcessor,
                        binding.pinpadEditText,
                        binding.scvText,
                        binding.tvReceipt,
                        binding.btnSendReceipt);

                Log.d("PinSelect", "DSpread views setup successfully");
            }
        } catch (ClassNotFoundException e) {
            // DSpread classes not available - this is expected for Telpo variants
            Log.d("PinSelect", "DSpread classes not available (Telpo variant)");
        } catch (Exception e) {
            Log.e("PinSelect", "Error setting up DSpread views: " + e.getMessage());
        }
    }

    private void showTimeoutDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.overTime)
                .setMessage(R.string.transaction_timeout)
                .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private MyProgressDialog createProgressDialog() {
//        MyProgressDialog.OnTimeOutListener timeOutListener = this::showTimeoutDialog;
//        MyProgressDialog dialog = new MyProgressDialog(requireActivity(), 80000, timeOutListener);
//
//        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
//        layoutParams.width = 500;
//        layoutParams.height = 500;
//        dialog.getWindow().setAttributes(layoutParams);
//        dialog.setCancelable(true);
//        dialog.setCanceledOnTouchOutside(true);
//
//        return dialog;
        try {
            MyProgressDialog.OnTimeOutListener timeOutListener = this::showTimeoutDialog;
            MyProgressDialog dialog = new MyProgressDialog(requireActivity(), 120000, timeOutListener); // Increased timeout to 120 seconds

            if (dialog.getWindow() != null) {
                WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.dimAmount = 0.7f; // Add dim background
                dialog.getWindow().setAttributes(layoutParams);
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false); // Prevent accidental dismissal

            return dialog;
        } catch (Exception e) {
            Log.e("PinSelect", "Error creating progress dialog", e);
            return null;
        }
    }

    private void showProgress(String message) {
//        requireActivity().runOnUiThread(() -> {
//            try {
//                if (progressDialog == null) {
//                    progressDialog = createProgressDialog();
//                }
//
//                if (!isProgressShowing) {
//                    progressDialog.setMessage(message);
//                    progressDialog.show();
//                    isProgressShowing = true;
//                } else {
//                    progressDialog.setMessage(message);
//                }
//            } catch (Exception e) {
//                Log.e("PinSelect", "Error showing progress: " + e.getMessage());
//            }
//        });
//        if (!isAdded()) {
//            Log.w("TAG", "Fragment not attached. Skipping showProgress.");
//            return;
//        }
//
//        Activity activity = getActivity();
//        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
//            Log.w("TAG", "Activity not available. Skipping progress dialog.");
//            return;
//        }
//
//        activity.runOnUiThread(() -> {
//            // Double-check: Fragment might detach between thread switch
//            if (!isAdded() || getActivity() == null) {
//                Log.w("TAG", "Fragment detached during UI thread execution. Skipping dialog.");
//                return;
//            }
//
//            try {
//                if (progressDialog == null) {
//                    progressDialog = createProgressDialog();
//                }
//
//                if (!isProgressShowing) {
//                    progressDialog.setMessage(message);
//                    progressDialog.show();
//                    isProgressShowing = true;
//                } else {
//                    progressDialog.setMessage(message);
//                }
//            } catch (Exception e) {
//                Log.e("TAG", "Error showing progress: " + e.getMessage(), e);
//            }
//        });
        Log.d("ProgressDebug", "showProgress called: " + message);
        if (!isAdded()) {
            Log.w("ProgressDebug", "Fragment not attached. Skipping showProgress.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w("ProgressDebug", "Activity not available. Skipping progress dialog.");
            return;
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) {
                Log.w("ProgressDebug", "Fragment detached during UI thread execution.");
                return;
            }

            try {
                if (progressDialog == null) {
                    progressDialog = createProgressDialog();
                    Log.d("ProgressDebug", "Created new progress dialog");
                }

                if (!isProgressShowing) {
                    progressDialog.setMessage(message);
                    progressDialog.show();
                    isProgressShowing = true;
                    Log.d("ProgressDebug", "Showing progress dialog: " + message);
                } else {
                    progressDialog.setMessage(message);
                    Log.d("ProgressDebug", "Updated progress dialog: " + message);
                }
            } catch (Exception e) {
                Log.e("ProgressDebug", "Error showing progress: " + e.getMessage(), e);
            }
        });
    }

    private void updateProgressSafe(String message) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setMessage(message);
            }
        });
    }

    private void handleError(Exception e) {
//        requireActivity().runOnUiThread(() -> {
//            isTransactionInProgress = false;
//            hideProgress();
//            new AlertDialog.Builder(requireActivity())
//                    .setTitle(R.string.error_title)
//                    .setMessage(e.getMessage() != null ? e.getMessage() : getString(R.string.unknown_error))
//                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
//                    .setCancelable(false)
//                    .show();
//        });
        if (!isAdded()) {
            Log.w("TAG", "Fragment not attached. Skipping handleError.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w("TAG", "Activity not available. Skipping UI error handling.");
            return;
        }

        activity.runOnUiThread(() -> {
            // Double-check inside UI thread — Fragment might detach during thread switch
            if (!isAdded() || getActivity() == null) {
                Log.w("TAG", "Fragment detached during UI thread execution. Skipping dialog.");
                return;
            }

            isTransactionInProgress = false;
            hideProgress();

            String message = e.getMessage() != null ? e.getMessage() : getString(R.string.unknown_error);

            new AlertDialog.Builder(activity)
                    .setTitle(R.string.error_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
                    .setCancelable(false)
                    .show();
        });
    }

    private void showSuccess(String message) {
        requireActivity().runOnUiThread(() -> {
            isTransactionInProgress = false;
            hideProgress();
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.success)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
                    .setCancelable(false)
                    .show();
        });
    }

    private void hideProgress() {
        if (!isAdded()) {
            Log.w("TAG", "Fragment not attached. Skipping onTransactionCancelled UI.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            Log.w("TAG", "Activity is null. Skipping UI update.");
            return;
        }
        requireActivity().runOnUiThread(() -> {
            try {
                if (progressDialog != null && isProgressShowing) {
                    progressDialog.dismiss();
                    isProgressShowing = false;
                }
                //progressDialog.dismiss();
            } catch (Exception e) {
                Log.e("PinSelect", "Error hiding progress: " + e.getMessage());
            }
        });
    }

    private void navigateBack() {
        cleanupResources();
        //NavHostFragment.findNavController(this).navigateUp();
        if (!isAdded() || isStateSaved()) {
            Log.w("TAG", "Fragment detached or state already saved. Skipping navigation.");
            return;
        }

        try {
            NavHostFragment.findNavController(this).navigateUp();
        } catch (IllegalStateException e) {
            Log.e("TAG", "NavController not available.", e);
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.onBackPressed();
            }
        }
    }

    private TransactionData createPayData() {
        TerminalConfig terminalConfig = new TerminalConfig();
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        String manufacturer = android.os.Build.MANUFACTURER;

        TransactionData payData = new TransactionData();
        payData.setAmount("100");
        payData.setPaymentApp(ConstValues.PAY_APP_PINCHANGE);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PINCHANGE);
        payData.setMid(terminalConfig.loadTerminalDataFromJson(requireContext(), "__mid"));
        payData.setTtype("POS");
        payData.setTmanu(manufacturer.toUpperCase());
        payData.setTid(terminalConfig.loadTerminalDataFromJson(requireContext(), "__tid"));
        payData.setUid("2331903647");
        payData.setMloc(terminalConfig.loadTerminalDataFromJson(requireContext(), "__merchantloc"));
        payData.setBatt("100");
        payData.setTim(timeStamp.replace("T", " "));
        payData.setCsid("SS:100");
        payData.setPstat("1");
        payData.setLang("EN");
        payData.setPoscondcode(terminalConfig.loadTerminalDataFromJson(requireContext(), "__posCode"));
        payData.setPosgeocode(terminalConfig.loadTerminalDataFromJson(requireContext(), "__posgeocode"));
        payData.setCurrencycode(terminalConfig.loadTerminalDataFromJson(requireContext(), "__currencycode"));
        payData.setTmodel(android.os.Build.MODEL);
        payData.setComms("WiFi");
        payData.setCstat("806868");
        payData.setSversion("PayApp-V1-0.00");
        payData.setHasbattery("0");
        payData.setLasttranstime(timeStamp.replace("T", " "));
        payData.setTtid("000003");
        payData.setType("trans");
        payData.setHook("C:selHook.kxml");
        payData.setSelacctype("default");
        payData.setChvm("OnlinePin");
        payData.setPosdatacode(terminalConfig.loadTerminalDataFromJson(requireContext(), "__posDataCodeEmv"));
        payData.setPosEntryMode("051");
        payData.setTellerdetail(sessionManager.getKeyFullname());
        payData.setTranType("PIN_CHANGE");

        return payData;
    }

    @Override
    public void onWaitingStatusChanged(boolean waiting) {
        if (!isAdded() || isStateSaved()) {
            Log.w("TAG", "Fragment not attached or state saved. Skipping UI update.");
            return;
        }

        if (waiting) {
            Context context = getContext();
            if (context != null) {
                showProgress(context.getString(R.string.waiting_for_card));
            } else {
                Log.w("TAG", "Context is null. Cannot show progress message.");
            }
        } else {
            hideProgress();
        }
    }

    @Override
    public void onShowPinPad(boolean show) {
        if (show) {
            hideProgress();
          //  showProgress(getString(R.string.please_input_pin));
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
    }

    @Override
    public void onTitleTextChanged(String title) {
        showProgress(title);
    }

    @Override
    public void onSendDingTalkMessage(boolean success, String data) {
        // Handle DingTalk message result if needed
    }

    @Override
    public void onShowPinPadWithKeyboard(List<String> dataList, boolean isOnlinePin, boolean isChangePin) {
//        requireActivity().runOnUiThread(() -> {
//            showProgress(isChangePin ?
//                    getString(R.string.input_pin_for_change) :
//                    getString(R.string.please_input_pin));
//        });
    }

    @Override
    public void onPinInputReceived(String value) {
        // Handle PIN input if needed
    }

    @Override
    public void onError(String value) {
        handleError(new RuntimeException(value));
    }

    @Override
    public void onTransactionCancelled() {
        if (!isAdded()) {
            Log.w("TAG", "Fragment not attached. Skipping onTransactionCancelled UI.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            Log.w("TAG", "Activity is null. Skipping UI update.");
            return;
        }
        requireActivity().runOnUiThread(() -> {
            isTransactionInProgress = false;
            hideProgress();
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.transaction_cancelled)
                    .setMessage(R.string.transaction_cancelled_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> navigateBack())
                    .show();
        });
    }

    @Override
    public void onDeviceConnected(String res) {
       // showProgress(getString(R.string.device_connected));
        Context context = getContext();
        if (context != null) {
            showProgress(context.getString(R.string.device_connected));
        } else {
            // Fallback to application context if needed
            showProgress(getResources().getString(R.string.device_connected));
        }
    }

    @Override
    public void onDeviceDisconnected(String res) {
        //handleError(new RuntimeException(getString(R.string.device_disconnected)));
        if (!isAdded()) {
            Log.w("TAG", "Fragment not attached. Skipping onDeviceDisconnected.");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.w("TAG", "Context is null. Skipping error handling.");
            return;
        }

        String message = context.getString(R.string.device_disconnected);
        handleError(new RuntimeException(message));
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
    public void onDestroyView() {
        cleanupResources();
        super.onDestroyView();
    }

    private void cleanupResources() {
        isTransactionInProgress = false;
        hideProgress();

        if (futureTask != null && !futureTask.isDone()) {
            futureTask.cancel(true);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        if (emvProcessor != null) {
//            if (emvProcessor instanceof DSpreadEmvService) {
//                ((DSpreadEmvService) emvProcessor).releaseResources();
//            }
            releaseDSpreadResources();
            emvProcessor.cancelTransaction();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        binding = null;
    }
    private void releaseDSpreadResources() {
        try {
            Class<?> dspreadClass = Class.forName("com.isw.payapp.devices.dspread.DSpreadEmvService");
            if (dspreadClass.isInstance(emvProcessor)) {
                Method releaseMethod = dspreadClass.getMethod("releaseResources");
                releaseMethod.invoke(emvProcessor);
            }
        } catch (ClassNotFoundException e) {
            // Silent catch - expected for Telpo
        } catch (Exception e) {
            Log.e("PinSelect", "DSpread resource release failed: " + e.getMessage());
        }
    }
}