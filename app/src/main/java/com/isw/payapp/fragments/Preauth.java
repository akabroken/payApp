package com.isw.payapp.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentPreauthBinding;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.dialog.WritePadDialog;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.tasks.MyHandler;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.model.PayData;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;
import com.telpo.tps550.api.printer.UsbThermalPrinter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Preauth#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Preauth extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    //Fragment
    private FragmentPreauthBinding fragmentPreauthBinding;
    //
    private  MyProgressDialog progressDialog ;

    //
    private ExecutorService executorService;
    private Future<String> futureTask;
    private TextView textViewProgress;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String Amount;
    private Context context;
    EmvService emvService;
    WritePadDialog writePadDialog;
    boolean waitsign = true;
    Bitmap bitmap;
    TextView title_tv;
    EditText inputAmt;
    Button sale;
    String cardNum;

    PayData payData;


    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;

    int event;
    int ret;

    MediaPlayer OKplayer;
    MediaPlayer FAILplayer;
    MediaPlayer notionPlayer;
    MediaPlayer stopPlayer;
    MediaPlayer rejectPlayer;

    boolean userCancel = false;

    private  Handler handler;

    PowerManager pm;
    PowerManager.WakeLock wakeLock;
    KeyguardManager km;

    UsbThermalPrinter usbThermalPrinter;

    public Preauth() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Preauth.
     */
    // TODO: Rename and change types and number of parameters
    public static Preauth newInstance(String param1, String param2) {
        Preauth fragment = new Preauth();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    MyProgressDialog.OnTimeOutListener dialogTimeOutListener = new MyProgressDialog.OnTimeOutListener() {
        @Override
        public void onTimeOut() {

            new AlertDialog.Builder(context)
                    .setTitle(R.string.overTime)
                    .setPositiveButton(R.string.bn_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          //  getActivity().finish();
                            closeWin();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    };

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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentPreauthBinding = FragmentPreauthBinding.inflate(inflater, container, false);
        return fragmentPreauthBinding.getRoot();
    }

    @SuppressLint("InvalidWakeLockTag")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usbThermalPrinter = new UsbThermalPrinter(getActivity());
        context = getActivity();
//        emvService = EmvService.getInstance();
//        emvService.setListener(listener);
        pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
        wakeLock.acquire();


        OKplayer = MediaPlayer.create(getActivity(), R.raw.success1);
        FAILplayer = MediaPlayer.create(getActivity(), R.raw.fail1);
        stopPlayer = MediaPlayer.create(getActivity(), R.raw.trans_stop1);
        rejectPlayer = MediaPlayer.create(getActivity(), R.raw.trans_reject1);



        handler = new MyHandler(context, progressDialog, writePadDialog);

        progressDialog = new MyProgressDialog(context, 80000, dialogTimeOutListener);



        inputAmt = fragmentPreauthBinding.inputAmt.findViewById(R.id.inputAmt);
        inputAmt.setSelection(inputAmt.getText().length());//Move the cursor to the far right

        Amount = inputAmt.getText().toString();
        sale = fragmentPreauthBinding.btnSaleStart.findViewById(R.id.btn_saleStart);
        sale.setEnabled(false);

        sale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Amount = inputAmt.getText().toString();
                Log.i("PREAUTH", "AMT - "+ Amount);
                inputAmt.setEnabled(false);
                PayData payData = new PayData();
                payData.setAmount(Amount);

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
               // progressDialog.dismiss();
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
    }

    private void startBackgroundTask() throws ExecutionException, InterruptedException {
        //
        final MyProgressDialog progressDialog = new MyProgressDialog(getActivity(), 160000, dialogTimeOutListener);
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
        payData.setAmount(Amount);
        payData.setPaymentApp(ConstValues.PAY_APP_PREAUTH);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PREAUTH);

//        PaymentProcessor processor = new PaymentProcessor(getActivity(),progressDialog,10,1000, progressLister,payData);
        RequestProcessor processor = new RequestProcessor(getActivity(),progressDialog,progressLister,payData);

        futureTask = executorService.submit(processor);

    }
//
//
//    @Override
//    public void onDestroy() {
////        PinpadService.Close();
////        EmvService.deviceClose();
//        super.onDestroy();
// //       executorService.shutdownNow();
//    }

    @Override
    public void onDestroyView() {

        fragmentPreauthBinding = null;
        PinpadService.Close();
        EmvService.deviceClose();

        super.onDestroyView();
    }

    private void closeWin() {
        NavHostFragment.findNavController(Preauth.this).navigate(R.id.fragment_preauth_to_transaction);
    }
}