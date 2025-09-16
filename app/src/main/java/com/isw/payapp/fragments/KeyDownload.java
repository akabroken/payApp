package com.isw.payapp.fragments;

import android.os.AsyncTask;
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
import com.isw.payapp.databinding.FragmentKeyDownloadBinding;

import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.model.ItemData;
import com.isw.payapp.terminal.processors.KeydownloadProcessor;
import com.isw.payapp.terminal.services.KeyDownloadSrv;

import java.util.ArrayList;
import java.util.List;

//import android.util.Base64;


public class KeyDownload extends Fragment {

    private FragmentKeyDownloadBinding binding;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;

    //UsbThermalPrinter usbThermalPrinter;
    private KeyDownloadSrv keyDownloadSrv;

    private IPinPadProcessor posPinPad;
    String sMasterKey, sMasterKey1;
    String sPinKey, sPinKey1;
    String sDesKey;
    int ret;
    String sBDK, sIPEK, encBDK, encBDK2, iKSN, sPinKeyDec, iPEK;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
       // usbThermalPrinter = new UsbThermalPrinter(getContext());
        binding = FragmentKeyDownloadBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //int ret = -1;
        //master key
        sMasterKey1 = "407AC1437929F85702342015A4E07F8C";

        //pin key encrypted with master key
        sPinKey1 = "C2C89BE64C5846EA266B16451A4AE08A";

        sPinKeyDec = "6B11A506FB85FC07BF4F87428E55386C";

        //des encryption key encrypted with master key

        sBDK = "463E3870D608E6D5E032DFEF0192FBCB";

        encBDK = "E408C836D59898CA42EBBF749726CD91";
        encBDK2 = "B9A28CCCE481A04FDAE4F9D1C03A6ECC";

        iKSN = "FFFF000006DDDDE00000";
        //iPEK = "6B11A506FB85FC07BF4F87428E55386C";
        iPEK = "6B11A506FB85FC07BF4F87428E55386C";
        //301C04106276A16D9B8C9BDA382A9BADA4AD2F9B04086BBB78309CC1C81E

        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<ItemData> imageUrls = generateImageUrls();
        imageAdapter = new ImageAdapter(getContext(), imageUrls);
        recyclerView.setAdapter(imageAdapter);

        posPinPad = DeviceFactory.createPinPad(getContext());
        posPinPad.initPinPad();

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ItemData clickedItem = imageUrls.get(position);
                String clickedTitle = clickedItem.getTitle();

                switch (clickedTitle) {
                    case "Keydownload":
                        KeydownloadProcessor processor = new KeydownloadProcessor();
                        try {
                            processor.process();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        NavHostFragment.findNavController(KeyDownload.this)
                                .navigate(R.id.fragment_keydownload_to_homefragment);
                        break;

                    case "LoadKey":
                        //String ipektw = "D6D8291E53A7BF2B0000000000000000";
                      //  String ipektw = "D6D8291E53A7BF2B"; //test
                       String ipektw = "33707E4927C4A0D50000000000000000"; //live - "33707E4927C4A0D5"
                        String ipek1 = "6276A16D9B8C9BDA382A9BADA4AD2F9B";
                        //String ipek1 = "6276A16D9B8C9BDA382A9BAD00000F9B";
                        String bdk_ipek = "463E3870D608E6D5E032DFEF0192FBCB";
                        String ipek2 = "6276A16D9B8C9BDA";
                        String ipek3 = "382A9BADA4AD2F9B";
                        String iksn_live = "FFFF000002DDDDE00000";
                        String iksn_test = "FFFF000006DDDDE00000";
                        String kcv_test = "CC5A6985E41061B4";
                        String kcv_live = "10B9824432E458DD";

                        new AsyncTask<Void, Void, Integer>() {

                            @Override
                            protected Integer doInBackground(Void... voids) {
                                ret = posPinPad.injectDukptKey(ipektw, iksn_live, kcv_live);
                                posPinPad.deviceClose();
                                return ret;
                            }

                            @Override
                            protected void onPostExecute(Integer ret) {
                                if (0 == ret) {
                                    Log.i("DUKPTIPEKK", "LOAD INITIAL PIN KEY RESPONSE : " + ret);
                                    Toast.makeText(getActivity(), "Load INITIAL PIN success!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.i("DUKPTIPEKK", "LOAD INITIAL PIN KEY RESPONSE : " + ret);
                                    Toast.makeText(getActivity(), "Load INITIAL failed!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.execute();
                        break;
                    case "Delete":
                        new AsyncTask<Void, Void, Integer>() {

                            @Override
                            protected Integer doInBackground(Void... voids) {
                                ret = posPinPad.deleteKeys();
                                return ret;

                            }

                            @Override
                            protected void onPostExecute(Integer ret) {
                                if (0 == ret) {
                                    Log.i("DUKPTIPEKK", "DELETE KEYS SUCCESS : " + ret);
                                    Toast.makeText(getActivity(), "DELETE KEYS SUCCESS !", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.i("DUKPTIPEKK", "DELETE KEYS failed  : " + ret);
                                    Toast.makeText(getActivity(), "DELETE KEYS FAILED !", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.execute();
                        break;

                    case "Format":
                        new AsyncTask<Void, Void, Integer>() {

                            @Override
                            protected Integer doInBackground(Void... voids) {
                              //  return PinpadService.TP_PinpadFormat(getActivity());
                                return posPinPad.resetKey();
                            }

                            @Override
                            protected void onPostExecute(Integer ret) {
                                if (0 == ret) {
                                    Log.i("DUKPTIPEKK", "PINPAD FORMAT SUCCESS : " + ret);
                                    Toast.makeText(getActivity(), "PINPAD FORMAT SUCCESS !", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.i("DUKPTIPEKK", "PINPAD FORMAT failed  : " + ret);
                                    Toast.makeText(getActivity(), "PINPAD FORMAT FAILED !", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.execute();
                        break;
                    default:
                        Toast.makeText(getContext(), "Clicked: " + clickedTitle, Toast.LENGTH_SHORT).show();
                }
//                if(clickedTitle.equals("Purchase")){
//                    NavHostFragment.findNavController(HomeFragment.this)
//                            .navigate(R.id.action_HomeFragment_to_PaymentFragment);
//                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
       posPinPad.deviceClose();
        binding = null;
    }

    private List<ItemData> generateImageUrls() {
        List<ItemData> imageUrls = new ArrayList<>();

        int[] imageId = {R.drawable.new_keydownload, R.drawable.new_load_keys, R.drawable.new_delete_keys, R.drawable.new_format_keys};
        String[] titleData = {"Keydownload", "LoadKey", "Delete", "Format"};

        // Use a single loop to iterate through the arrays
        for (int i = 0; i < imageId.length; i++) {
            int imageResource = imageId[i];
            String title = titleData[i];

            ItemData itemData = new ItemData(imageResource, title);
            imageUrls.add(itemData);
        }

        return imageUrls;
    }

}

