package com.isw.payapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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

import com.bumptech.glide.Glide;

import com.isw.payapp.Adapters.ImageAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentKeyDownloadBinding;

import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.helpers.LogoHelper;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.ItemData;

// Import the refactored Processor and its callback
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.utils.CommonUtil;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.RSAUtil;
import com.isw.payapp.utils.ThreeDES;
import com.isw.payapp.utils.UnsafeOkHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class KeyDownload extends Fragment {

    //http://software.codemagus.com/WebTools/cgi-bin/cmldkgipek
    private FragmentKeyDownloadBinding binding;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ItemData> itemDataList;

    private IPinPadProcessor posPinPad;

    private SessionManager sessionManager;
    int ret;

    // Executor for background tasks (replacement for AsyncTask)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Live data (should ideally come from a secure configuration)
    private final String ipektwLive = "33707E4927C4A0D51282944D541770D4";
    private final String iksnLive ="FFFF000002DDDDE00000";//"09110000020705E00000";// "FFFF000002DDDDE00000";
    private final String kcvLive = "10B9824432E458DD";


    private final String ipektwTest = "D6D8291E53A7BF2B67973ADF78E9B882"; //D6D8291E53A7BF2B67973ADF78E9B882 D6D8291E53A7BF2B
    private final String iksnTest = "FFFF000006DDDDE00000";
    private final String kcvTest = "10B9824432E458DD";

    private static final String TAG = "KeyDownload";

    // Progress dialog
    private ProgressDialog progressDialog;
    private AlertDialog confirmationDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentKeyDownloadBinding.inflate(inflater, container, false);
//        Glide.with(this)
//                .load(R.drawable.sidian_bank_logo)
//                .into(binding.imageView);
        LogoHelper.setupLogo(binding.getRoot());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        sessionManager = new SessionManager(requireContext());
        itemDataList = generateItemData();
        imageAdapter = new ImageAdapter(requireContext(), itemDataList);
        recyclerView.setAdapter(imageAdapter);

        // Initialize the PIN pad
        posPinPad = DeviceFactory.createPinPad(requireContext());
        posPinPad.initPinPad();

        Set<String> allowedRoles = Set.of("SUPERVISOR", "ADMIN");
        if (!allowedRoles.contains(sessionManager.getKeyRoleType())) {
            showUserNotAllowedDialog();
            return;
        }

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ItemData itemData, int position) {
                String clickedTitle = itemData.getTitle();

                switch (clickedTitle) {
                    case "Keydownload":
                        showConfirmationDialog("Key Download",
                                "Are you sure you want to download new keys? This will overwrite existing keys.",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            handleKeyDownload();
                                        }
                                    }
                                });
                        break;

                    case "LoadKey":
                        showConfirmationDialog("Load Key",
                                "Are you sure you want to load the initial PIN key?",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            handleLoadKey();
                                        }
                                    }
                                });
                        break;

                    case "Delete":
                        showConfirmationDialog("Delete Keys",
                                "WARNING: This will delete all keys from the device. Are you sure you want to proceed?",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            handleDeleteKeys();
                                        }
                                    }
                                });
                        break;

                    case "Format":
                        showConfirmationDialog("Format PIN Pad",
                                "WARNING: This will reset the PIN pad to factory settings. All data will be lost. Are you sure?",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            handleFormatPinPad();
                                        }
                                    }
                                });
                        break;

                    default:
                        Toast.makeText(requireContext(), "Clicked: " + clickedTitle, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProgressDialog(String message) {
        requireActivity().runOnUiThread(() -> {
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

    private void showUserNotAllowedDialog(){
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.userNotAllowed)
                .setMessage(R.string.onlySupervisorAllowed)
                .setPositiveButton(R.string.bn_confirm, (dialog, which) -> navigateBack())
                .setCancelable(false)
                .show();
    }

    private void hideProgressDialog() {
        requireActivity().runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void showConfirmationDialog(String title, String message, DialogInterface.OnClickListener listener) {
        requireActivity().runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Yes", listener)
                    .setNegativeButton("No", null)
                    .setCancelable(true);

            if (confirmationDialog != null && confirmationDialog.isShowing()) {
                confirmationDialog.dismiss();
            }
            confirmationDialog = builder.create();
            confirmationDialog.show();
        });
    }

    private void showResultDialog(String title, String message, boolean isSuccess) {
        requireActivity().runOnUiThread(() -> {
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

        TerminalXmlParser parser = new TerminalXmlParser();
        RSAUtil rsaUtil = new RSAUtil(1024);
        ExecutorService networkExecutor = NetworkExecutor.getExecutor();

        String keyDownloadUrl = "https://" + TerminalConfig.loadTerminalDataFromJson(getContext(), "__transip") + ":"
                + TerminalConfig.loadTerminalDataFromJson(getContext(), "__transport") + "/";

        networkExecutor.execute(() -> {
            try {
                // Get the unsafe OkHttpClient
                OkHttpClient unsafeClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

                NetworkService.initialize(getContext(), keyDownloadUrl);
                NetworkService networkService = NetworkService.getInstance();
                List<Object> pkModExp = new ArrayList<>();
                Map<String, String> components = rsaUtil.getKeyComponents();
                pkModExp.add(components.get("modulus"));
                pkModExp.add(components.get("exponent"));

                String response = networkService.postPayLoadSync(TerminalXmlParser.KeyDownload(getContext(), pkModExp));
                Log.i("RESPONSE::" , response);

                Map<String, String> resultMap = CommonUtil.convertXMLToMap(response);
                String encryptedKey = resultMap.get("pinkey");

                String clearKey = rsaUtil.decryptWithKeyComponents(encryptedKey, components.get("privateModulus"),
                        components.get("privateExponent"), true);



                Log.i("pinKey.substring(0,32):" , clearKey);

                executor.execute(() -> {
                   // posPinPad.initPinPad();
                    final int result = posPinPad.injectDukptKey(clearKey, iksnLive, "");

                    requireActivity().runOnUiThread(() -> {
                        hideProgressDialog();
                        if (result == 0) {
                            Log.i("DUKPT", "LOAD INITIAL PIN KEY SUCCESS: " + result);
                            showResultDialog("Success", "Key download completed successfully!", true);
                            Toast.makeText(requireActivity(), "Load INITIAL PIN success!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i("DUKPT", "LOAD INITIAL PIN KEY FAILED: " + result);
                            showResultDialog("Error", "Key download failed! Error code: " + result, false);
                            Toast.makeText(requireActivity(), "Load INITIAL PIN failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    hideProgressDialog();
                    showResultDialog("Error", "Key download failed: " + e.getMessage(), false);
                    Toast.makeText(requireActivity(), "Key download error!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleLoadKey() {
        showProgressDialog("Loading initial PIN key...");

        executor.execute(() -> {
            final int result = posPinPad.injectDukptKey(ipektwTest, iksnTest, kcvLive);

            requireActivity().runOnUiThread(() -> {
                hideProgressDialog();
                if (result == 0) {
                    Log.i("DUKPT", "LOAD INITIAL PIN KEY SUCCESS: " + result);
                    showResultDialog("Success", "Initial PIN key loaded successfully!", true);
                    Toast.makeText(requireActivity(), "Load INITIAL PIN success!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("DUKPT", "LOAD INITIAL PIN KEY FAILED: " + result);
                    showResultDialog("Error", "Failed to load initial PIN key! Error code: " + result, false);
                    Toast.makeText(requireActivity(), "Load INITIAL PIN failed!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleDeleteKeys() {
        showProgressDialog("Deleting keys...");

        executor.execute(() -> {
            final int result = posPinPad.deleteKeys();

            requireActivity().runOnUiThread(() -> {
                hideProgressDialog();
                if (result == 0) {
                    Log.i("KeyDownload", "DELETE KEYS SUCCESS: " + result);
                    showResultDialog("Success", "All keys deleted successfully!", true);
                    Toast.makeText(requireActivity(), "DELETE KEYS SUCCESS!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("KeyDownload", "DELETE KEYS FAILED: " + result);
                    showResultDialog("Error", "Failed to delete keys! Error code: " + result, false);
                    Toast.makeText(requireActivity(), "DELETE KEYS FAILED!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleFormatPinPad() {
        showProgressDialog("Resetting PIN pad...");

        executor.execute(() -> {
            final int result = posPinPad.resetKey();

            requireActivity().runOnUiThread(() -> {
                hideProgressDialog();
                if (result == 0) {
                    Log.i("KeyDownload", "PINPAD RESET SUCCESS: " + result);
                    showResultDialog("Success", "PIN pad reset successfully!", true);
                    Toast.makeText(requireActivity(), "PINPAD RESET SUCCESS!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("KeyDownload", "PINPAD RESET FAILED: " + result);
                    showResultDialog("Error", "PIN pad reset failed! Error code: " + result, false);
                    Toast.makeText(requireActivity(), "PINPAD RESET FAILED!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up dialogs
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (confirmationDialog != null && confirmationDialog.isShowing()) {
            confirmationDialog.dismiss();
        }

        // Clean up the executor when the view is destroyed
        executor.shutdown();
        // Close the device connection
        if (posPinPad != null) {
            posPinPad.deviceClose();
        }
        binding = null;
    }

    private List<ItemData> generateItemData() {
        List<ItemData> itemDataList = new ArrayList<>();
        int[] imageId = {R.drawable.rkd_key, R.drawable.load_key, R.drawable.key_delete_1, R.drawable.reset_password};
        String[] titleData = {"Keydownload", "LoadKey", "Delete", "Format"};

        // Validate arrays have same length
        if (imageId.length != titleData.length) {
            throw new IllegalStateException("Image and title arrays must have the same length");
        }

        for (int i = 0; i < imageId.length; i++) {
            itemDataList.add(new ItemData(imageId[i], titleData[i]));
        }
        return itemDataList;
    }

    private void navigateBack() {
       // cleanupResources();
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

}