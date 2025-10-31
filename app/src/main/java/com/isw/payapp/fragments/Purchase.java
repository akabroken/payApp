package com.isw.payapp.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPaymentBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.dialog.DialogListener;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.dialog.WritePadDialog;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.model.TransactionData;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Purchase extends Fragment {

    Context context;
    MyProgressDialog progressDialog = null;

    private ExecutorService executorService;
    private Future<String> futureTask;

    private IEmvProcessor deviceFactoryEmv;

    TextView title_tv;
    EditText inputAmt;
    Button sale;
    String Amount;
    private ImageView imageViewBack,imageViewExit;

    MediaPlayer OKplayer;
    MediaPlayer FAILplayer;
    MediaPlayer notionPlayer;
    MediaPlayer stopPlayer;
    MediaPlayer rejectPlayer;

    Bitmap bitmap;

    boolean userCancel = false;
    boolean waitsign;

    Handler handler;

    PowerManager pm;
    PowerManager.WakeLock wakeLock;
    KeyguardManager km;

    WritePadDialog writePadDialog;

    private FragmentPaymentBinding binding;




    public void wakeUpAndUnlock(Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
        //Unlock
        kl.disableKeyguard();
        //Get the power manager object
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //Get the PowerManager.WakeLock object. The following parameter | means passing in two values ​​at the same time. The last one is the Tag used in LogCat.
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
        //light up screen
        wl.acquire();
        //freed
        wl.release();
    }


    @SuppressLint("InvalidWakeLockTag")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("InvalidWakeLockTag")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

       // deviceFactoryEmv = DeviceFactory.createEmvFunc(context,null,null,null);
        OKplayer = MediaPlayer.create(getActivity(), R.raw.success1);
        FAILplayer = MediaPlayer.create(getActivity(), R.raw.fail1);
        stopPlayer = MediaPlayer.create(getActivity(), R.raw.trans_stop1);
        rejectPlayer = MediaPlayer.create(getActivity(), R.raw.trans_reject1);

        writePadDialog = new WritePadDialog(getActivity(), new DialogListener() {
            @Override
            public void refreshActivity(Object object) {
                bitmap = (Bitmap) object;
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 256);
                waitsign = false;
            }
        });

        //progressDialog = new MyProgressDialog(context, 80000, dialogTimeOutListener);


        inputAmt = binding.inputAmt.findViewById(R.id.inputAmt);
        inputAmt.setSelection(inputAmt.getText().length());//Move the cursor to the far right
        Amount = inputAmt.getText().toString();
        sale = binding.btnSaleStart.findViewById(R.id.btn_saleStart);
        sale.setEnabled(false);

        sale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Amount = inputAmt.getText().toString();

                TransactionData payData = new TransactionData();
                payData.setAmount(Amount);
                sale.setEnabled(false);

                executorService = Executors.newSingleThreadExecutor();

                try {
                    startBackgroundTask();
                    executorService.shutdownNow();
                    closeWin();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }



            }
        });

        inputAmt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, 0);
                v.requestFocus();
                return true;
            }
        });

        inputAmt.addTextChangedListener(new TextWatcher() {
            private boolean isChanged = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isChanged) {// ----->Returns if the character has not changed
                    return;
                }
                String str = s.toString();
                isChanged = true;
                String cuttedStr = str;
                /* Remove dot from string */
                for (int i = str.length() - 1; i >= 0; i--) {
                    char c = str.charAt(i);
                    if ('.' == c) {
                        cuttedStr = str.substring(0, i) + str.substring(i + 1);
                        break;
                    }
                }
                /* Delete the extra 0 in front */
                int NUM = cuttedStr.length();
                int zeroIndex = -1;
                for (int i = 0; i < NUM - 2; i++) {
                    char c = cuttedStr.charAt(i);
                    if (c != '0') {
                        zeroIndex = i;
                        break;
                    } else if (i == NUM - 3) {
                        zeroIndex = i;
                        break;
                    }
                }
                if (zeroIndex != -1) {
                    cuttedStr = cuttedStr.substring(zeroIndex);
                }
                /* If there are less than 3 digits, add 0 */
                if (cuttedStr.length() < 3) {
                    cuttedStr = "0" + cuttedStr;
                }
                /* Add dot to display two decimal places */
                cuttedStr = cuttedStr.substring(0, cuttedStr.length() - 2)
                        + "." + cuttedStr.substring(cuttedStr.length() - 2);
                inputAmt.setText(cuttedStr);
                inputAmt.setSelection(inputAmt.length());
                isChanged = false;

                if (cuttedStr.equals("0.00")) {
                    sale.setEnabled(false);
                } else {
                    sale.setEnabled(true);
                }
            }

        });

        //Delayed start of soft keyboard for inputting amount
        new Timer().schedule(new TimerTask() {
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) inputAmt.getContext().getSystemService(INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(inputAmt, 0);
            }
        }, 300);

        imageViewBack = binding.imageViewBack.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Purchase.this)
                        .navigate(R.id.payment_to_transaction);
            }
        });
        imageViewExit = binding.imageViewCancel.findViewById(R.id.imageViewCancel);
        imageViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Purchase.this)
                        .navigate(R.id.payment_to_transaction);
            }
        });
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
                                closeWin();
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
        TransactionData payData = new TransactionData();
        payData.setAmount(Amount);
        payData.setPaymentApp(ConstValues.PAY_APP_PURCHASE);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PURCHASE);

//        PaymentProcessor processor = new PaymentProcessor(getActivity(),progressDialog,10,1000, progressLister,payData);
        RequestProcessor processor = new RequestProcessor(getActivity(),progressDialog,progressLister,payData);

        futureTask = executorService.submit(processor);

    }

    private void closeWin() {
        NavHostFragment.findNavController(Purchase.this)
                .navigate(R.id.payment_to_transaction);
    }


    @Override
    public void onDestroy() {
        deviceFactoryEmv.cancelTransaction();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {

        binding = null;

        deviceFactoryEmv.cancelTransaction();
        super.onDestroyView();
    }

}