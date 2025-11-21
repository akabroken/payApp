package com.isw.payapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.isw.payapp.R;
import com.isw.payapp.Adapters.TerminalConfigAdapter;
import com.isw.payapp.databinding.FragmentTerminalBinding;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;

public class Terminal extends Fragment {
    private static final String TAG = "TerminalFragment";
    private static final String CONFIG_FILENAME = "config.json";
    private static final String CONFIG_ORIGINAL_FILENAME = "config_original.json";

    // Role permissions
    private static final Set<String> ALLOWED_ROLES = Set.of("SUPERVISOR", "ADMIN");

    private FragmentTerminalBinding binding;
    private TerminalConfigAdapter adapter;
    private TerminalConfigModel terminalConfig;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeSessionManager();

        if (!checkAuthenticationAndPermissions()) {
            return;
        }

        initializeViews();
        setupRecyclerView();
        loadConfig();
    }

    private void initializeSessionManager() {
        sessionManager = new SessionManager(requireContext());
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

    private void initializeViews() {
        // Navigation buttons
        binding.imageViewBack.setOnClickListener(v -> navigateBack());
        binding.imageViewCancel.setOnClickListener(v -> navigateBack());

        // Save button
        setupSaveButton();
    }

    private void setupSaveButton() {
        Button buttonSave = binding.getRoot().findViewById(R.id.buttonSave);
        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> saveConfigWithFeedback());
        }
    }

    private void setupRecyclerView() {
        adapter = new TerminalConfigAdapter();
        adapter.setOnItemClickListener(this::onConfigItemClicked);

        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadConfig() {
        TerminalConfigModel savedConfig = loadConfigFromInternalStorage();
        if (savedConfig != null) {
            terminalConfig = savedConfig;
            adapter.setConfig(terminalConfig);
            Log.d(TAG, "Config loaded from internal storage");
        } else {
            loadConfigFromAssets();
        }
    }

    private void loadConfigFromAssets() {
        try (InputStream inputStream = requireContext().getAssets().open(CONFIG_FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String json = readStreamContents(reader);
            json = json.replace("__", ""); // Remove underscores for Gson parsing

            Gson gson = new Gson();
            terminalConfig = gson.fromJson(json, TerminalConfigModel.class);
            adapter.setConfig(terminalConfig);

            Log.d(TAG, "Config loaded from assets");

        } catch (Exception e) {
            Log.e(TAG, "Error loading config from assets", e);
            terminalConfig = createDefaultConfig();
            adapter.setConfig(terminalConfig);
        }
    }

    private String readStreamContents(BufferedReader reader) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private TerminalConfigModel createDefaultConfig() {
        TerminalConfigModel config = new TerminalConfigModel();
        Context context = getContext();

        if (context != null) {
            loadConfigFromTerminalConfig(context, config);
        } else {
            setDefaultHardcodedValues(config);
        }

        Log.d(TAG, "Default config created");
        return config;
    }

    private void loadConfigFromTerminalConfig(Context context, TerminalConfigModel config) {
        config.setBank(getTerminalConfigValue(context, "bank"));
        config.setMid(getTerminalConfigValue(context, "mid"));
        config.setTid(getTerminalConfigValue(context, "tid"));
        config.setMerchantloc(getTerminalConfigValue(context, "merchantloc"));
        config.setAddress1(getTerminalConfigValue(context, "address1"));
        config.setAddress2(getTerminalConfigValue(context, "address2"));
        config.setCity(getTerminalConfigValue(context, "city"));
        config.setState(getTerminalConfigValue(context, "state"));
        config.setZip(getTerminalConfigValue(context, "zip"));
        config.setCurrencycode(getTerminalConfigValue(context, "currencycode"));
        config.setPosCode(getTerminalConfigValue(context, "posCode"));
        config.setMtype(getTerminalConfigValue(context, "mtype"));
        config.setTransip(getTerminalConfigValue(context, "transip"));
        config.setTransport(getTerminalConfigValue(context, "transport"));
        config.setKeysetid(getTerminalConfigValue(context, "keysetid"));
        config.setLoginurl(getTerminalConfigValue(context, "loginurl"));
        config.setLoginport(getTerminalConfigValue(context, "loginport"));
    }

    private String getTerminalConfigValue(Context context, String key) {
        try {
            return TerminalConfig.loadTerminalDataFromJson(context, key);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load value for key: " + key, e);
            return "";
        }
    }

    private void setDefaultHardcodedValues(TerminalConfigModel config) {
        config.setBank("Sidian Bank");
        config.setMid("CBLKE0000000001");
        config.setTid("CBLKE001");
        config.setMerchantloc("Busia Branch");
        config.setAddress1("Busia Branch");
        config.setAddress2("Busia Branch");
        config.setCity("Nairobi");
        config.setState("Nairobi");
        config.setZip("");
        config.setCurrencycode("404");
        config.setPosCode("00");
        config.setMtype("7011");
        config.setTransip("apps.qa.interswitch-ke.com");
        config.setTransport("7075");
        config.setKeysetid("000006");
        config.setLoginurl("smarttrans.interswitch-ke.com");
        config.setLoginport("81");
    }

    private void onConfigItemClicked(TerminalConfigAdapter.ConfigItem item) {
        showEditDialog(item);
    }

    private void showEditDialog(TerminalConfigAdapter.ConfigItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.edit_item_title, item.getDisplayName()));

        final EditText input = createEditText(item.getValue());
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) ->
                updateConfigItem(item, input.getText().toString().trim()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    private EditText createEditText(String currentValue) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentValue);
        input.selectAll(); // Select all text for easy editing

        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding, padding, padding);

        return input;
    }

    private void updateConfigItem(TerminalConfigAdapter.ConfigItem item, String newValue) {
        if (!newValue.isEmpty()) {
            adapter.updateItem(item.getKey(), newValue);
            showToast(getString(R.string.item_updated, item.getDisplayName()));
        } else {
            showToast(getString(R.string.empty_value_warning));
        }
    }

    private void saveConfigWithFeedback() {
        if (saveConfig()) {
            showToast(getString(R.string.config_saved_success));
        } else {
            showToast(getString(R.string.config_saved_error));
        }
    }

    private boolean saveConfig() {
        try {
            TerminalConfigModel updatedConfig = adapter.getUpdatedConfig();
            saveUpdatedConfig(updatedConfig);
            showSuccessDialogWithOptions();
            //NavHostFragment.findNavController(this).navigate(R.id.terminal_to_login);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving configuration", e);
            return false;
        }
    }

    private void showSuccessDialogWithOptions() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Configuration Saved")
                .setMessage("Your configuration has been saved successfully. What would you like to do?")
                .setPositiveButton("Go to Login", (dialog, which) -> navigateToLogin())
                .show();

//        .setNegativeButton("Stay Here", (dialog, which) -> {
//            // User chooses to stay on current screen
//            showToast("Configuration saved");
//        })
//                .setNeutralButton("View Settings", (dialog, which) -> {
//                    // Optional: Navigate to settings or refresh current view
//                    refreshCurrentView();
//                })
    }

    private void showErrorDialogWithRetry() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Save Failed")
                .setMessage("Unable to save configuration. Would you like to try again?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Retry saving
                    saveConfig();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshCurrentView() {
        // Refresh the current view if needed
        loadConfig();
        showToast("View refreshed");
    }

    private void navigateToLogin() {
        try {
            // Clear any session data if needed
            if (sessionManager != null) {
                sessionManager.logout();
            }

            NavHostFragment.findNavController(this).navigate(R.id.terminal_to_login);
        } catch (Exception e) {
            Log.e(TAG, "Navigation to login failed", e);
            showToast("Unable to navigate to login");
        }
    }

    private void saveUpdatedConfig(TerminalConfigModel config) {
        try {
            // Save pretty-printed JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(config);
            saveConfigToInternalStorage(json, CONFIG_FILENAME);

            // Save original format
            saveOriginalFormatConfig(config);

            Log.i(TAG, "Configuration saved successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error saving updated config", e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }

    private void saveOriginalFormatConfig(TerminalConfigModel config) {
        try {
            String originalFormatJson = createOriginalFormatJson(config);
            saveConfigToInternalStorage(originalFormatJson, CONFIG_ORIGINAL_FILENAME);
        } catch (Exception e) {
            Log.e(TAG, "Error saving original format config", e);
        }
    }

    private String createOriginalFormatJson(TerminalConfigModel config) {
        // Fixed JSON format - added missing commas
        return String.format(
                "{\n" +
                        "    \"bank\": \"%s\",\n" +
                        "    \"mid\": \"%s\",\n" +
                        "    \"tid\": \"%s\",\n" +
                        "    \"merchantloc\": \"%s\",\n" +
                        "    \"address1\": \"%s\",\n" +
                        "    \"address2\": \"%s\",\n" +
                        "    \"city\": \"%s\",\n" +
                        "    \"state\": \"%s\",\n" +
                        "    \"zip\": \"%s\",\n" +
                        "    \"currencycode\": \"%s\",\n" +
                        "    \"posCode\": \"%s\",\n" +
                        "    \"mtype\": \"%s\",\n" +
                        "    \"transip\": \"%s\",\n" +
                        "    \"transport\": \"%s\",\n" +
                        "    \"keysetid\": \"%s\",\n" +
                        "    \"loginurl\": \"%s\",\n" +
                        "    \"loginport\": \"%s\"\n" +
                        "}",
                config.getBank(),
                config.getMid(),
                config.getTid(),
                config.getMerchantloc(),
                config.getAddress1(),
                config.getAddress2(),
                config.getCity(),
                config.getState(),
                config.getZip(),
                config.getCurrencycode(),
                config.getPosCode(),
                config.getMtype(),
                config.getTransip(),
                config.getTransport(),
                config.getKeysetid(),
                config.getLoginurl(),
                config.getLoginport()
        );
    }

    private void saveConfigToInternalStorage(String json, String filename) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                requireContext().openFileOutput(filename, Context.MODE_PRIVATE))) {
            outputStreamWriter.write(json);
            Log.d(TAG, "Config saved to: " + filename);
        } catch (Exception e) {
            Log.e(TAG, "Error saving config to internal storage: " + filename, e);
            throw new RuntimeException("Failed to save config file: " + filename, e);
        }
    }

    private TerminalConfigModel loadConfigFromInternalStorage() {
        try (InputStream inputStream = requireContext().openFileInput(CONFIG_FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String json = readStreamContents(reader);
            Gson gson = new Gson();
            return gson.fromJson(json, TerminalConfigModel.class);

        } catch (Exception e) {
            Log.d(TAG, "No saved config found in internal storage");
            return null;
        }
    }

    private void navigateBack() {
        saveConfig(); // Save automatically before navigating back
        NavHostFragment.findNavController(this).navigate(R.id.terminal_to_local_settings);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLocalSetting() {
        try {
            NavHostFragment.findNavController(this).navigateUp();
        } catch (Exception e) {
            Log.e("Navigation", "Invalid navigation action", e);
            showToast(getString(R.string.navigation_error));
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

    @Override
    public void onResume() {
        super.onResume();
        // Check session expiration when fragment resumes
        if (sessionManager != null && !sessionManager.isLoggedIn()) {
            navigateToLocalSetting();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}