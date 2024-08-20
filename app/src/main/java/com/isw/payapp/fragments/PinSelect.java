package com.isw.payapp.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPinSelectBinding;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.model.PayData;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class PinSelect extends Fragment {

    private FragmentPinSelectBinding binding;
    private ImageView imageViewBack,imageViewExit;
    ProgressDialog progressDoalog;

    private ExecutorService executorService;
    private Future<String> futureTask;

    Bitmap bitmap;
    boolean waitsign;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       binding = FragmentPinSelectBinding.inflate(inflater,container,false);
       return  binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle saveInstantState){
        super.onViewCreated(view,saveInstantState);

        imageViewBack = binding.imageViewBack.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(PinSelect.this)
                        .navigate(R.id.pinselect_index);
            }
        });
        imageViewExit = binding.imageViewCancel.findViewById(R.id.imageViewCancel);
        imageViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(PinSelect.this)
                        .navigate(R.id.pinselect_index);
            }
        });

        PayData payData = new PayData();
        payData.setAmount("1");


        executorService = Executors.newSingleThreadExecutor();

        try {
            startBackgroundTask();
            executorService.shutdownNow();
          //  closeWin();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void startBackgroundTask() throws ExecutionException, InterruptedException {
        //
        MyProgressDialog.OnTimeOutListener dialogTimeOutListener = new MyProgressDialog.OnTimeOutListener() {
            @Override
            public void onTimeOut() {

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.overTime)
                        .setPositiveButton(R.string.bn_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //closeWin();
                                NavHostFragment.findNavController(PinSelect.this)
                                        .navigate(R.id.pinselect_index);
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        };
        final MyProgressDialog progressDialog = new MyProgressDialog(getActivity(), 80000, dialogTimeOutListener);
        WindowManager.LayoutParams layoutParams = progressDialog.getWindow().getAttributes();
        layoutParams.width = 500;
        layoutParams.height = 250;
        progressDialog.getWindow().setAttributes(layoutParams);
        ProgressListener progressLister = new ProgressListener() {
            @Override
            public void onProgressUpdate(int progress) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //  textViewProgress.setText("Progress: " + progress);
                        progressDialog.setTitle("Closing process");
                        progressDialog.setMessage("Test");
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setCancelable(true);
                        progressDialog.setMessage("Progress: " + progress);
                        progressDialog.show();
                    }
                });
            }

            @Override
            public void onProgressEnd() throws InterruptedException {
                Thread.sleep(1000);
                progressDialog.dismiss();
            }
        };
        PayData payData = new PayData();
        payData.setAmount("1");
        payData.setPaymentApp(ConstValues.PAY_APP_PINCHANGE);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PINCHANGE);

//        PaymentProcessor processor = new PaymentProcessor(getActivity(),progressDialog,10,1000, progressLister,payData);
        RequestProcessor processor = new RequestProcessor(getActivity(),progressDialog,progressLister,payData);

        futureTask = executorService.submit(processor);

    }


    @Override
    public void onDestroyView() {

        binding = null;

        super.onDestroyView();
    }
}