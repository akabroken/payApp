package com.isw.payapp.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

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
import android.widget.Toast;

import com.isw.payapp.Adapters.TransactionAdapter2;
import com.isw.payapp.R;
import com.isw.payapp.constant.ConstValues;
import com.isw.payapp.databinding.FragmentFuelMgntBinding;
import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.interfaces.ProgressListener;
import com.isw.payapp.processors.RequestProcessor;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.TransactionData;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class FuelManagementFragment extends Fragment {

    private FragmentFuelMgntBinding binding;

    private RecyclerView recyclerView;
    private TransactionAdapter2 transactionAdapter;
    private List<Transaction> transactionList;

    private SessionManager sessionManager;
    private ProgressDialog progressDoalog;
    private EditText enterLitersText;
    private Button processTransaction, viewHistory, cancelTransaction;
    private ExecutorService executorService;
    private Future<String> futureTask;
    private TransactionData payData ;
    private  View v;

    public FuelManagementFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        v = inflater.inflate(R.layout.item_transaction, container, false);
//        viewHistory.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//
//                recyclerView = v.findViewById(R.id.recyclerView);
//                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//                recyclerView.setAdapter(transactionAdapter);
//            }
//        });
        binding = FragmentFuelMgntBinding.inflate(inflater, container, false);
        return  binding.getRoot();
    }
    public void onViewCreated(@NonNull View view, Bundle saveInstantState) {
        super.onViewCreated(view, saveInstantState);

        sessionManager = new SessionManager(getContext());

        processTransaction = binding.processPaymentButton.findViewById(R.id.processPaymentButton);
        viewHistory = binding.viewHistoryButton.findViewById(R.id.viewHistoryButton);
        cancelTransaction = binding.cancelPaymentButton.findViewById(R.id.cancelPaymentButton);






        enterLitersText = binding.amountInput.findViewById(R.id.amountInput);
        enterLitersText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed here
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Prevent infinite loops by checking the flag
                if (isUpdating) {
                    return;
                }

                isUpdating = true;

                try {
                    // Step 1: Get the raw input and remove any non-digit characters
                    String rawInput = s.toString().replaceAll("[^\\d]", "");

                    // Step 2: Handle empty input
                    if (rawInput.isEmpty()) {
                        enterLitersText.setText("0.00");
                        enterLitersText.setSelection(enterLitersText.getText().length());
                        isUpdating = false;
                        return;
                    }

                    // Step 3: Trim leading zeros
                    rawInput = rawInput.replaceFirst("^0+(?!$)", ""); // Remove leading zeros but leave at least one digit

                    // Step 4: Format the input to have exactly two decimal places
                    StringBuilder formattedAmount = new StringBuilder();
                    if (rawInput.length() <= 2) {
                        // Handle cases where there are fewer than 2 digits
                        formattedAmount.append("0.");
                        formattedAmount.append(String.format("%02d", Integer.parseInt(rawInput)));
                    } else {
                        // Insert a decimal point two characters from the end
                        formattedAmount.append(rawInput.substring(0, rawInput.length() - 2));
                        formattedAmount.append(".");
                        formattedAmount.append(rawInput.substring(rawInput.length() - 2));
                    }

                    // Step 5: Update the EditText with the formatted value
                    enterLitersText.setText(formattedAmount.toString());
                    enterLitersText.setSelection(enterLitersText.getText().length());

                    // Step 6: Enable/disable the "sale" button based on the value
                    if (formattedAmount.toString().equals("0.00")) {
                        processTransaction.setEnabled(false);
                    } else {
                        processTransaction.setEnabled(true);
                    }
                } catch (Exception e) {
                    // Log and handle any unexpected errors
                    e.printStackTrace();
                } finally {
                    // Reset the flag to allow future updates
                    isUpdating = false;
                }
            }
        });
        enterLitersText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, 0);
                v.requestFocus();
                return true;
            }
        });

        new Timer().schedule(new TimerTask() {
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) enterLitersText.getContext().getSystemService(INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(enterLitersText, 0);
            }
        }, 300);


        processTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                processFuelRequest();
                enterLitersText.setText("0.00");
                executorService = Executors.newSingleThreadExecutor();

                try {

                    startBackgroundTask(enterLitersText.getText().toString());
                    executorService.shutdownNow();
                    //  closeWin();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });







    }

    public void processFuelRequest(){
        Toast.makeText(requireActivity(), "Tuko kwa process transaction!!", Toast.LENGTH_SHORT).show();
        TransactionData payData = new TransactionData();
        TerminalConfig terminalConfig = new TerminalConfig();
        payData.setPosEntryMode(terminalConfig.loadTerminalDataFromJson(getContext(),"__pos"));
    }

    private void hideKeyboard(View view) {
        // Get the InputMethodManager using the public API
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(getContext().INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void startBackgroundTask(String amt) throws ExecutionException, InterruptedException {
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
                                NavHostFragment.findNavController(FuelManagementFragment.this)
                                        .navigate(R.id.action_pinselect_index_to_indexpage);
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

        TerminalConfig terminalConfig = new TerminalConfig();
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date());
        payData.setAmount(amt);
        payData.setPaymentApp(ConstValues.PAY_APP_PURCHASE);
        payData.setPaymentReqTag(ConstValues.POST_PAY_PURCHASE);
        payData.setMid(terminalConfig.loadTerminalDataFromJson(getContext(), "__mid"));
        payData.setTtype("POS");
        payData.setTmanu("TELPO");
        payData.setTid(terminalConfig.loadTerminalDataFromJson(getContext(), "__tid"));
        payData.setUid("2331903647");
        payData.setMloc(terminalConfig.loadTerminalDataFromJson(getContext(), "__merchantloc"));
        payData.setBatt("100");
        payData.setTim(timeStamp.replace("T"," "));
        payData.setCsid("SS:100");
        payData.setPstat("1");
        payData.setLang("EN");
        payData.setPoscondcode(terminalConfig.loadTerminalDataFromJson(getContext(), "__posCode"));
        payData.setPosgeocode(terminalConfig.loadTerminalDataFromJson(getContext(), "__posgeocode"));
        payData.setCurrencycode(terminalConfig.loadTerminalDataFromJson(getContext(), "__currencycode"));
        payData.setTmodel("TPS900");
        payData.setComms("WiFi");
        payData.setCstat("806868");
        payData.setSversion("PayApp-V1-0.00");
        payData.setHasbattery("0");
        payData.setLasttranstime(timeStamp.replace("T"," "));
        payData.setTtid("000003");
        payData.setType("trans");
        payData.setHook("C:selHook.kxml");
        payData.setSelacctype("default");
        payData.setChvm("OnlinePin");
        payData.setPosdatacode(terminalConfig.loadTerminalDataFromJson(getContext(), "__posDataCodeEmv"));
        payData.setPosEntryMode("051");
        payData.setTellerdetail(sessionManager.getKeyFullname());

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