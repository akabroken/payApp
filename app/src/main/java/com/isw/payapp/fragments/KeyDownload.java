package com.isw.payapp.fragments;

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
import com.isw.payapp.model.ItemData;

// Import the refactored Processor and its callback
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.terminal.processors.KeydownloadProcessor;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.utils.RSAUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyDownload extends Fragment {

    private FragmentKeyDownloadBinding binding;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ItemData> itemDataList;

    private IPinPadProcessor posPinPad;
    int ret;

    // Executor for background tasks (replacement for AsyncTask)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Live data (should ideally come from a secure configuration)
    private final String ipektwLive = "33707E4927C4A0D50000000000000000";
    private final String iksnLive = "FFFF000002DDDDE00000";
    private final String kcvLive = "10B9824432E458DD";

    private final String ipektwTest = "D6D8291E53A7BF2B";
    private final String iksnTest = "FFFF000006DDDDE00000";
    private final String kcvTest = "10B9824432E458DD";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentKeyDownloadBinding.inflate(inflater, container, false);
        Glide.with(this)
                .load(R.drawable.sidian_bank_logo)
                .into(binding.imageView);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        itemDataList = generateItemData();
        imageAdapter = new ImageAdapter(requireContext(), itemDataList);
        recyclerView.setAdapter(imageAdapter);

        // Initialize the PIN pad
        posPinPad = DeviceFactory.createPinPad(requireContext());
        posPinPad.initPinPad();

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ItemData itemData, int position) {
                String clickedTitle = itemData.getTitle();

                switch (clickedTitle) {
                    case "Keydownload":
                        handleKeyDownload();
                        break;

                    case "LoadKey":
                        handleLoadKey();
                        break;

                    case "Delete":
                        handleDeleteKeys();
                        break;

                    case "Format":
                        handleFormatPinPad();
                        break;

                    default:
                        Toast.makeText(requireContext(), "Clicked: " + clickedTitle, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleKeyDownload() {
        // Show a progress indicator here if you have one
       // binding.progressBar.setVisibility(View.VISIBLE);

        TerminalXmlParser parser = new TerminalXmlParser();
        RSAUtil rsaUtil = new RSAUtil();

        // 2. Get the URL from a config source (e.g., BuildConfig, shared preferences)
        String keyDownloadUrl = "https://" + TerminalConfig.loadTerminalDataFromJson(getContext(), "__transip") + ":"
                + TerminalConfig.loadTerminalDataFromJson(getContext(), "__transport") + "/";;

        // 3. Create the Processor
        KeydownloadProcessor processor = new KeydownloadProcessor(
                requireContext(),
                parser,
                rsaUtil,
                keyDownloadUrl
        );

        // 4. Execute the key download process asynchronously
        processor.process(new KeydownloadProcessor.KeyDownloadCallback() {
            @Override
            public void onSuccess(String decryptedPinKey) {
                requireActivity().runOnUiThread(() -> {
                  //  binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Key Download & Decryption Successful!", Toast.LENGTH_SHORT).show();
                    Log.i("KeyDownload", "Decrypted Key: " + decryptedPinKey);

                    // Inject the decrypted key into the PIN pad
                    executor.execute(() -> {
                        final int injectionResult = posPinPad.injectDukptKey(decryptedPinKey, iksnLive, kcvLive);

                        requireActivity().runOnUiThread(() -> {
                            if (injectionResult == 0) {
                                Toast.makeText(requireContext(), "Key Injection Successful!", Toast.LENGTH_SHORT).show();
                                // Navigate only on success
                                NavHostFragment.findNavController(KeyDownload.this)
                                        .navigate(R.id.fragment_keydownload_to_homefragment);
                            } else {
                                Toast.makeText(requireContext(), "Key Injection Failed: " + injectionResult, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                  //  binding.progressBar.setVisibility(View.GONE);
                    Log.e("KeyDownload", "Key Download Failed", e);
                    Toast.makeText(requireContext(), "Key Download Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleLoadKey() {
        executor.execute(() -> {
            final int result = posPinPad.injectDukptKey(ipektwLive, iksnLive, kcvLive);

            requireActivity().runOnUiThread(() -> {
                if (result == 0) {
                    Log.i("DUKPT", "LOAD INITIAL PIN KEY SUCCESS: " + result);
                    Toast.makeText(requireActivity(), "Load INITIAL PIN success!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("DUKPT", "LOAD INITIAL PIN KEY FAILED: " + result);
                    Toast.makeText(requireActivity(), "Load INITIAL PIN failed!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleDeleteKeys() {
        executor.execute(() -> {
            final int result = posPinPad.deleteKeys();

            requireActivity().runOnUiThread(() -> {
                if (result == 0) {
                    Log.i("KeyDownload", "DELETE KEYS SUCCESS: " + result);
                    Toast.makeText(requireActivity(), "DELETE KEYS SUCCESS!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("KeyDownload", "DELETE KEYS FAILED: " + result);
                    Toast.makeText(requireActivity(), "DELETE KEYS FAILED!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleFormatPinPad() {
        executor.execute(() -> {
            final int result = posPinPad.resetKey();

            requireActivity().runOnUiThread(() -> {
                if (result == 0) {
                    Log.i("KeyDownload", "PINPAD RESET SUCCESS: " + result);
                    Toast.makeText(requireActivity(), "PINPAD RESET SUCCESS!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("KeyDownload", "PINPAD RESET FAILED: " + result);
                    Toast.makeText(requireActivity(), "PINPAD RESET FAILED!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    
}