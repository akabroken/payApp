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
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPinSelectBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.dspread.DSpreadEmvService;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.TransactionData;

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
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        progressDialog = createProgressDialog();
        payData = createPayData();
        emvProcessor = DeviceFactory.createEmvFunc(requireActivity(), payData, this);

        if (emvProcessor instanceof DSpreadEmvService) {
            ((DSpreadEmvService) emvProcessor).setViews(
                    binding.pinpadEditText,
                    binding.scvText,
                    binding.tvReceipt,
                    binding.btnSendReceipt
            );
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
        MyProgressDialog.OnTimeOutListener timeOutListener = this::showTimeoutDialog;
        MyProgressDialog dialog = new MyProgressDialog(requireActivity(), 80000, timeOutListener);

        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.width = 500;
        layoutParams.height = 500;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
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
        if (!isAdded()) {
            Log.w("TAG", "Fragment not attached. Skipping showProgress.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w("TAG", "Activity not available. Skipping progress dialog.");
            return;
        }

        activity.runOnUiThread(() -> {
            // Double-check: Fragment might detach between thread switch
            if (!isAdded() || getActivity() == null) {
                Log.w("TAG", "Fragment detached during UI thread execution. Skipping dialog.");
                return;
            }

            try {
                if (progressDialog == null) {
                    progressDialog = createProgressDialog();
                }

                if (!isProgressShowing) {
                    progressDialog.setMessage(message);
                    progressDialog.show();
                    isProgressShowing = true;
                } else {
                    progressDialog.setMessage(message);
                }
            } catch (Exception e) {
                Log.e("TAG", "Error showing progress: " + e.getMessage(), e);
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
            if (emvProcessor instanceof DSpreadEmvService) {
                ((DSpreadEmvService) emvProcessor).releaseResources();
            }
            emvProcessor.cancelTransaction();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        binding = null;
    }
}