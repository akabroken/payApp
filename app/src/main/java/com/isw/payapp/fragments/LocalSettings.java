package com.isw.payapp.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.isw.payapp.Adapters.ImageAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentLocalSettingsBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.ItemData;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.utils.CommonUtil;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.RSAUtil;
import com.isw.payapp.utils.UnsafeOkHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class LocalSettings extends Fragment {
    private static final String TAG = "LocalSettings";

    // Configuration constants
    private static final Set<String> ALLOWED_ROLES = Set.of("SUPERVISOR", "ADMIN");

    // Key configuration
    private static final String IKSK_LIVE = "FFFF000006DDDDE00000";
    private static final String KCV_LIVE = "10B9824432E458DD";
    private static final String IPEKTW_TEST = "D6D8291E53A7BF2B67973ADF78E9B882";
    private static final String IKSK_TEST = "FFFF000006DDDDE00000";

    private FragmentLocalSettingsBinding binding;
    private SessionManager sessionManager;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ItemData> itemDataList;
    private IPinPadProcessor posPinPad;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI components
    private ProgressDialog progressDialog;
    private AlertDialog confirmationDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLocalSettingsBinding.inflate(inflater, container, false);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();

        if (!checkAuthenticationAndPermissions()) {
            return;
        }

        setupRecyclerView();
        initializePinPad();
    }

    private void initializeComponents() {
        sessionManager = new SessionManager(requireContext());
        recyclerView = binding.recyclerView;
        itemDataList = generateItemData();
    }

    private boolean checkAuthenticationAndPermissions() {
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return false;
        }

        if (!ALLOWED_ROLES.contains(sessionManager.getKeyRoleType())) {
            showUserNotAllowedDialog();
            return false;
        }

        return true;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        imageAdapter = new ImageAdapter(requireContext(), itemDataList);
        recyclerView.setAdapter(imageAdapter);

        setupItemClickListeners();
    }

    private void setupItemClickListeners() {
        imageAdapter.setOnItemClickListener((itemData, position) -> {
            String clickedTitle = itemData.getTitle();
            handleMenuItemClick(clickedTitle);
        });
    }

    private void handleMenuItemClick(String menuTitle) {
        switch (menuTitle) {
            case "Keydownload":
                showConfirmationDialog(
                        "Key Download",
                        "Are you sure you want to download new keys? This will overwrite existing keys.",
                        (dialog, which) -> handleKeyDownload()
                );
                break;

            case "Load Test Keys":
                showConfirmationDialog(
                        "Load Test Keys",
                        "Are you sure you want to load the initial PIN key?",
                        (dialog, which) -> handleLoadKey()
                );
                break;

            case "Delete Keys":
                showConfirmationDialog(
                        "Delete Keys",
                        "WARNING: This will delete all keys from the device. Are you sure you want to proceed?",
                        (dialog, which) -> handleDeleteKeys()
                );
                break;

            case "Format PIN Pad":
                showConfirmationDialog(
                        "Format PIN Pad",
                        "WARNING: This will reset the PIN pad to factory settings. All data will be lost. Are you sure?",
                        (dialog, which) -> handleFormatPinPad()
                );
                break;

            case "Terminal Settings":
                navigateToTerminalSettings();
                break;

            default:
                showToast("Clicked: " + menuTitle);
        }
    }

    private void initializePinPad() {
        posPinPad = DeviceFactory.createPinPad(requireContext());
        posPinPad.initPinPad();
//        if (initResult != 0) {
//            Log.w(TAG, "PIN pad initialization returned non-zero result: " + initResult);
//        }
    }

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(requireContext());
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
            }
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void showConfirmationDialog(String title, String message,
                                        DialogInterface.OnClickListener positiveListener) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Yes", positiveListener)
                    .setNegativeButton("No", null)
                    .setCancelable(true);

            dismissExistingConfirmationDialog();
            confirmationDialog = builder.create();
            confirmationDialog.show();
        });
    }

    private void dismissExistingConfirmationDialog() {
        if (confirmationDialog != null && confirmationDialog.isShowing()) {
            confirmationDialog.dismiss();
        }
    }

    private void showResultDialog(String title, String message, boolean isSuccess) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setCancelable(true);

            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void handleKeyDownload() {
        showProgressDialog("Downloading keys... Please wait.");

        ConfigManager.refreshConfig(requireContext());
        TerminalConfigModel config = ConfigManager.getConfig(requireContext());

        String keyDownloadUrl = String.format("https://%s:%s/",
                config.getTransip(), config.getTransport());

        NetworkExecutor.getExecutor().execute(() -> {
            try {
                performKeyDownload(keyDownloadUrl);
            } catch (Exception e) {
                Log.e(TAG, "Key download failed", e);
                handleKeyOperationError("Key download failed: " + e.getMessage());
            }
        });
    }

    private void performKeyDownload(String keyDownloadUrl) throws Exception {
        OkHttpClient unsafeClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        NetworkService.initialize(requireContext(), keyDownloadUrl);
        NetworkService networkService = NetworkService.getInstance();

        RSAUtil rsaUtil = new RSAUtil(1024);
        Map<String, String> components = rsaUtil.getKeyComponents();

        List<Object> pkModExp = new ArrayList<>();
        pkModExp.add(components.get("modulus"));
        pkModExp.add(components.get("exponent"));

        String response = networkService.postPayLoadSync(
                TerminalXmlParser.KeyDownload(requireContext(), pkModExp));
        Log.i(TAG, "Key download response: " + response);

        Map<String, String> resultMap = CommonUtil.convertXMLToMap(response);
        String encryptedKey = resultMap.get("pinkey");

        String clearKey = rsaUtil.decryptWithKeyComponents(
                encryptedKey,
                components.get("privateModulus"),
                components.get("privateExponent"),
                true
        );

        Log.i(TAG, "Decrypted PIN key: " + clearKey);

        executePinPadOperation(() ->
                        posPinPad.injectDukptKey(clearKey, IKSK_LIVE, ""),
                "Key download"
        );
    }

    private void handleLoadKey() {
        showProgressDialog("Loading initial PIN key...");

        executePinPadOperation(() ->
                        posPinPad.injectDukptKey(IPEKTW_TEST, IKSK_TEST, KCV_LIVE),
                "Load initial PIN key"
        );
    }

    private void handleDeleteKeys() {
        showProgressDialog("Deleting keys...");

        executePinPadOperation(() ->
                        posPinPad.deleteKeys(),
                "Delete keys"
        );
    }

    private void handleFormatPinPad() {
        showProgressDialog("Resetting PIN pad...");

        executePinPadOperation(() ->
                        posPinPad.resetKey(),
                "PIN pad reset"
        );
    }

    private void executePinPadOperation(PinPadOperation operation, String operationName) {
        executor.execute(() -> {
            try {
                final int result = operation.execute();

                runOnUiThread(() -> {
                    hideProgressDialog();
                    if (result == 0) {
                        Log.i(TAG, operationName + " SUCCESS: " + result);
                        showResultDialog("Success", operationName + " completed successfully!", true);
                        showToast(operationName + " success!");
                    } else {
                        Log.i(TAG, operationName + " FAILED: " + result);
                        showResultDialog("Error", operationName + " failed! Error code: " + result, false);
                        showToast(operationName + " failed!");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, operationName + " exception", e);
                handleKeyOperationError(operationName + " exception: " + e.getMessage());
            }
        });
    }

    @FunctionalInterface
    private interface PinPadOperation {
        int execute();
    }

    private void handleKeyOperationError(String errorMessage) {
        runOnUiThread(() -> {
            hideProgressDialog();
            showResultDialog("Error", errorMessage, false);
            showToast("Operation error!");
        });
    }

    private void navigateToTerminalSettings() {
        try {
            NavHostFragment.findNavController(this).navigate(R.id.local_settings_to_terminal);
        } catch (Exception e) {
            Log.e(TAG, "Navigation to terminal settings failed", e);
            showToast("Navigation failed");
        }
    }

    private void showUserNotAllowedDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.userNotAllowed)
                .setMessage(R.string.onlySupervisorAllowed)
                .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private void navigateToLogin() {
        try {
            NavHostFragment.findNavController(this).navigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Navigation to login failed", e);
            showToast(getString(R.string.navigation_error));
        }
    }

    private void navigateBack() {
        if (!isAdded() || isStateSaved()) {
            Log.w(TAG, "Fragment detached or state already saved. Skipping navigation.");
            return;
        }

        try {
            NavHostFragment.findNavController(this).navigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Navigation back failed", e);
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().onBackPressed();
            }
        }
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && !sessionManager.isLoggedIn()) {
            navigateToLogin();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupResources();
    }

    private void cleanupResources() {
        // Clean up dialogs
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        dismissExistingConfirmationDialog();

        // Clean up executor
        if (!executor.isShutdown()) {
            executor.shutdown();
        }

        // Close device connection
        if (posPinPad != null) {
            posPinPad.deviceClose();
        }

        binding = null;
    }

    private List<ItemData> generateItemData() {
        List<ItemData> itemDataList = new ArrayList<>();
        int[] imageIds = {
                R.drawable.rkd_key,
                R.drawable.load_key,
                R.drawable.key_delete_1,
                R.drawable.reset_password,
                R.drawable.terminal_setting_1
        };
        String[] titles = {
                "Keydownload",
                "Load Test Keys",
                "Delete Keys",
                "Format PIN Pad",
                "Terminal Settings"
        };

        // Validate arrays have same length
        if (imageIds.length != titles.length) {
            throw new IllegalStateException("Image and title arrays must have the same length");
        }

        for (int i = 0; i < imageIds.length; i++) {
            itemDataList.add(new ItemData(imageIds[i], titles[i]));
        }
        return itemDataList;
    }
}