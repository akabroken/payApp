package com.isw.payapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPinSelectBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.model.UserModel;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.UnsafeOkHttpClient;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import okhttp3.OkHttpClient;

public class PinSelect extends Fragment implements EmvServiceCallback {

    private FragmentPinSelectBinding binding;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private Future<String> futureTask;
    private TerminalConfig terminalConfig;

    private MyProgressDialog progressDialog;
    private IEmvProcessor emvProcessor;
    private TransactionData payData;
    private boolean isProgressShowing = false;
    private boolean isTransactionInProgress = false;
    private boolean isTellerAuthenticated = false;
    private static final String TAG = "PinSelect";

    private static final String LOGIN_URL = "https://smarttrans.interswitch-ke.com:81/";
    private AlertDialog loginDialog;

    private String tellerNames;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPinSelectBinding.inflate(inflater, container, false);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponents();
        setupClickListeners();

        // Show login dialog first before starting payment process
        showTellerLoginDialog();
    }


    private void showTellerLoginDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Teller Authentication");
            builder.setMessage("Please login to authorize transaction");

            // Inflate custom layout for login form
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_teller_login, null);
            builder.setView(dialogView);

            EditText etUsername = dialogView.findViewById(R.id.et_username);
            EditText etPassword = dialogView.findViewById(R.id.et_password);
            TextView tvLoginMessage = dialogView.findViewById(R.id.tv_login_message);

            builder.setPositiveButton("Login", null);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                navigateBack();
            });

            loginDialog = builder.create();
            loginDialog.show();

            // Override positive button to prevent automatic dismissal
            Button loginButton = loginDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            loginButton.setOnClickListener(v -> {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    tvLoginMessage.setText("Please enter both username and password");
                    tvLoginMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    return;
                }

                // Show loading state
                loginButton.setEnabled(false);
                loginButton.setText("Authenticating...");
                tvLoginMessage.setText("");

                authenticateTellerWithBackend(username, password, new AuthenticationCallback() {
                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            isTellerAuthenticated = true;
                            if (loginDialog != null && loginDialog.isShowing()) {
                                loginDialog.dismiss();
                            }
                            // Start payment process after successful login
                            startPaymentProcess();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        requireActivity().runOnUiThread(() -> {
                            loginButton.setEnabled(true);
                            loginButton.setText("Login");
                            tvLoginMessage.setText(errorMessage);
                            tvLoginMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        });
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing login dialog", e);
            showToast("Error initializing login");
        }
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
        // Check multiple conditions to ensure fragment is in valid state
        if (!isAdded() || getContext() == null || isDetached() || getActivity() == null || getActivity().isFinishing()) {
            Log.w("PinSelect", "Fragment not in valid state for device connected message");
            return;
        }

        // Run on UI thread to ensure thread safety
        requireActivity().runOnUiThread(() -> {
            // Double-check after switching to UI thread
            if (!isAdded() || getContext() == null) {
                return;
            }

            try {
                showProgress(getString(R.string.device_connected));
            } catch (IllegalStateException e) {
                Log.e("PinSelect", "Error showing device connected progress", e);
            }
        });
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

    private interface AuthenticationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private void handleLoginResponse(String response, String username, AuthenticationCallback callback) {
        try {
            if (TextUtils.isEmpty(response)) {
                callback.onError("Empty response from server");
                return;
            }

            Document doc = parseXmlResponse(response);
            String responseCode = getValue(doc, "responseCode");
            String responseMessage = getValue(doc, "responseMessage");

            Log.d(TAG, "Login response - Code: " + responseCode + ", Message: " + responseMessage);

            if ("00".equals(responseCode)) {
                tellerNames = getValue(doc, "names");
                Log.i(TAG,"getValue(doc, \"names\")"+tellerNames);

                payData.setTellerdetail(getValue(doc, "names"));

                // Store teller session information

                showToast("Login successful!");
                callback.onSuccess();
            } else {
                String errorMsg = TextUtils.isEmpty(responseMessage)
                        ? "Invalid username or password"
                        : responseMessage;
                callback.onError(errorMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling login response", e);
            callback.onError("Login processing failed");
        }
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
//            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
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

    public static String generateTerminalUsersRequest(UserModel userModel) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element terminalUsersRequest = doc.createElement("terminalUsersRequest");
        doc.appendChild(terminalUsersRequest);

        createElement(doc, terminalUsersRequest, "username", userModel.getUsername());
        createElement(doc, terminalUsersRequest, "password", userModel.getPassword());
        createElement(doc, terminalUsersRequest, "firstName", "");
        createElement(doc, terminalUsersRequest, "lastName", "");
        createElement(doc, terminalUsersRequest, "terminalId", userModel.getTid());
        createElement(doc, terminalUsersRequest, "merchantId", userModel.getMid());
        createElement(doc, terminalUsersRequest, "role", userModel.getRole());
        createElement(doc, terminalUsersRequest, "reqType", userModel.getRequestType());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        return writer.toString();
    }

    private static void createElement(Document doc, Element parent, String tagName, String value) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }

    private static String getValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String content = element.getTextContent();
                return content != null ? content.trim() : "";
            }
        }
        return "";
    }

    private Document parseXmlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void authenticateTellerWithBackend(String username, String password, AuthenticationCallback callback) {
        Log.d(TAG, "Authenticating teller: " + username);

        try {
            UserModel userModel = createUserModel(username, password);
            String requestXml = generateTerminalUsersRequest(userModel);
            Log.i(TAG, "Login request XML generated");

            ExecutorService networkExecutor = NetworkExecutor.getExecutor();
            networkExecutor.execute(() -> {
                try {
                    // Initialize network service
                    NetworkService.initialize(requireContext(), LOGIN_URL);
                    NetworkService networkService = NetworkService.getInstance();

                    Log.d(TAG, "Sending login request...");
                    String response = networkService.postPayLoadSyncLogin(requestXml);
                    Log.i(TAG, "Login response received");

                    handleLoginResponse(response, username, callback);

                } catch (Exception e) {
                    Log.e(TAG, "Network error during authentication", e);
                    callback.onError("Network error. Please check your connection.");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error during authentication setup", e);
            callback.onError("Authentication failed. Please try again.");
        }
    }

    private UserModel createUserModel(String username, String password) {
        UserModel userModel = new UserModel();
        ConfigManager.refreshConfig(getActivity());
        TerminalConfigModel config = ConfigManager.getConfig(getActivity());
        userModel.setUsername(username);
        userModel.setPassword(password);
        userModel.setRole("TELLER");

        try {
            if (terminalConfig == null) {
                terminalConfig = new TerminalConfig();
            }
            userModel.setTid(config.getTid());
            userModel.setMid(config.getMid());
        } catch (Exception e) {
            Log.e(TAG, "Error loading terminal data", e);
            throw new RuntimeException("Failed to load terminal configuration");
        }

        userModel.setRequestType("LOGIN");
        return userModel;
    }

    private void startPaymentProcess() {
        try {
            // Check if user is allowed and authenticated
            if (!isTellerAuthenticated) {
                showAuthenticationRequiredDialog();
                return;
            }


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
            Log.e(TAG, "Error starting payment process", e);
            if (emvProcessor != null) {
                emvProcessor.cancelTransaction();
            }
            handleError(e);
        }
    }

    private void showProgress(String message) {
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

    private void showAuthenticationRequiredDialog() {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireActivity())
                .setTitle("Authentication Required")
                .setMessage("Please login as teller to authorize transactions")
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    showTellerLoginDialog();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> navigateBack())
                .show();
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
        if (!isAdded()) return;

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
        try {
            terminalConfig = new TerminalConfig();
            sessionManager = new SessionManager(requireContext());
            executorService = Executors.newSingleThreadExecutor();
            progressDialog = createProgressDialog();
            payData = createPayData();
            emvProcessor = DeviceFactory.createEmvFunc(requireActivity(), payData, this);

            setupEmvProcessorViews();
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

            if (binding.pinpadEditText == null || binding.scvText == null ||
                    binding.tvReceipt == null || binding.btnSendReceipt == null) {
                Log.e(TAG, "One or more views are null, cannot set views");
                return;
            }

            setViewsMethod.invoke(
                    emvProcessor,
                    binding.pinpadEditText,
                    binding.scvText,
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

    // ... (keep the rest of your existing methods like showTimeoutDialog, createProgressDialog, etc.)
    // The following methods remain mostly the same as your original code:
    // showUserNotAllowedDialog, createProgressDialog, showProgress, updateProgressSafe,
    // handleError, showSuccess, hideProgress, navigateBack, createPayData,
    // and all the EmvServiceCallback methods

    private void showUserNotAllowedDialog(){
        if (!isAdded()) return;

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.userNotAllowed)
                .setMessage(R.string.onlyTellerAllowed)
                .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private MyProgressDialog createProgressDialog() {
        try {
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

            return dialog;
        } catch (Exception e) {
            Log.e(TAG, "Error creating progress dialog", e);
            return null;
        }
    }

    private void showTimeoutDialog() {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.overTime)
                .setMessage(R.string.transaction_timeout)
                .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private void handleError(Exception e) {
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

    @Override
    public void onDestroyView() {
        cleanupResources();
        super.onDestroyView();
    }

    private void cleanupResources() {
        isTransactionInProgress = false;
        isTellerAuthenticated = false;
        hideProgress();

        // Clean up login dialog
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }

        if (futureTask != null && !futureTask.isDone()) {
            futureTask.cancel(true);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        if (emvProcessor != null) {
            releaseDSpreadResources();
            emvProcessor.cancelTransaction();
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        binding = null;
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
            Log.e(TAG, "DSpread resource release failed: " + e.getMessage());
        }
    }

    private TransactionData createPayData() {
        ConfigManager.refreshConfig(getActivity());
        TerminalConfigModel config = ConfigManager.getConfig(getActivity());
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        String manufacturer = android.os.Build.MANUFACTURER;

        TransactionData payData = new TransactionData();
        payData.setAmount("1.00");
        payData.setPaymentApp(ConstValues.PAY_APP_PINCHANGE);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PINCHANGE);
        payData.setMid(config.getMid());
        payData.setTtype("POS");
        payData.setTmanu(manufacturer.toUpperCase());
        payData.setTid(config.getTid());
        payData.setUid("2331903647");
        payData.setMloc(config.getMerchantloc());
        payData.setBatt("100");
        payData.setTim(timeStamp.replace("T", " "));
        payData.setCsid("SS:100");
        payData.setPstat("1");
        payData.setLang("EN");
        payData.setPoscondcode(config.getPosCode());
        payData.setPosgeocode( "00254000000000404");
        payData.setCurrencycode(config.getCurrencycode());
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
        payData.setPosdatacode("510101511344101");
        payData.setPosEntryMode("051");
        payData.setCurrency("KES");
        payData.setTransactionType("Pin Select");
        Log.i(TAG, "tellerNames :"+ tellerNames);
        //payData.setTellerdetail(sessionManager.getKeyFullname());


        payData.setTranType("PIN_CHANGE");

        return payData;
    }

    // ... (keep all your existing EmvServiceCallback implementation methods)
    // They can remain exactly as you have them
}