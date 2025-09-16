package com.isw.payapp.devices.dspread.Activity.ViewModels;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import com.dspread.print.device.PrintListener;
import com.dspread.print.device.PrinterDevice;
import com.dspread.print.device.PrinterManager;
import com.dspread.print.util.TRACE;
import com.isw.payapp.R;
import com.isw.payapp.devices.dspread.Activity.helpers.PrinterHelper;
import com.isw.payapp.devices.dspread.Activity.http.RetrofitClient;
import com.isw.payapp.devices.dspread.Activity.http.api.DingTalkApiService;
import com.isw.payapp.devices.dspread.POSManager;
import com.isw.payapp.devices.dspread.utils.DialogUtils;
import com.isw.payapp.devices.dspread.utils.TLV;
import com.isw.payapp.devices.dspread.utils.TLVParser;
import com.isw.payapp.devices.dspread.Activity.models.PaymentModel;
import com.isw.payapp.devices.dspread.Activity.models.BaseAppViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.bus.event.SingleLiveEvent;
import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.ToastUtils;

public class PaymentViewModel extends BaseAppViewModel {
    private static final String AUTHFROMISSUER_URL = "https://ypparbjfugzgwijijfnb.supabase.co/functions/v1/request-online-result";
    private DingTalkApiService apiService;

    public PaymentViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getInstance().create(DingTalkApiService.class);
    }

    public ObservableField<String> loadingText = new ObservableField<>("");
    public ObservableBoolean isLoading = new ObservableBoolean(false);
    public ObservableField<String> transactionResult = new ObservableField<>("");
    public ObservableField<String> amount = new ObservableField<>("");
    public ObservableField<String> titleText = new ObservableField<>("Payment");
    public ObservableBoolean isWaiting = new ObservableBoolean(true);
    public ObservableBoolean isSuccess = new ObservableBoolean(false);
    public ObservableBoolean isPrinting = new ObservableBoolean(false);
    public SingleLiveEvent<Boolean> isOnlineSuccess = new SingleLiveEvent();
    public ObservableBoolean showPinpad = new ObservableBoolean(false);
    public ObservableBoolean showResultStatus = new ObservableBoolean(false);
    public ObservableField<String> receiptContent = new ObservableField<>();
    private Bitmap receiptBitmap;
    private Context mContext;

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public PaymentModel setTransactionSuccess(String message) {
        setTransactionSuccess();
        message = message.substring(message.indexOf(":") + 2);
//        TRACE.i("data 2 = "+message);
        PaymentModel paymentModel = new PaymentModel();
        String transType = SPUtils.getInstance().getString("transactionType");
        paymentModel.setTransType(transType);
        List<TLV> tlvList = TLVParser.parse(message);
        if (tlvList == null || tlvList.size() == 0) {
            return paymentModel;
        }
        TLV dateTlv = TLVParser.searchTLV(tlvList, "9A");
//        TLV transTypeTlv = TLVParser.searchTLV(tlvList,"9C");
        TLV transCurrencyCodeTlv = TLVParser.searchTLV(tlvList, "5F2A");
        TLV transAmountTlv = TLVParser.searchTLV(tlvList, "9F02");
        TLV tvrTlv = TLVParser.searchTLV(tlvList, "95");
        TLV cvmReusltTlv = TLVParser.searchTLV(tlvList, "9F34");
        TLV cidTlv = TLVParser.searchTLV(tlvList, "9F27");
        paymentModel.setDate(dateTlv.value);
        paymentModel.setTransCurrencyCode(transCurrencyCodeTlv == null ? "" : transCurrencyCodeTlv.value);
        paymentModel.setAmount(transAmountTlv == null ? "" : transAmountTlv.value);
        paymentModel.setTvr(tvrTlv == null ? "" : tvrTlv.value);
        paymentModel.setCvmResults(cvmReusltTlv == null ? "" : cvmReusltTlv.value);
        paymentModel.setCidData(cidTlv == null ? "" : cidTlv.value);
        return paymentModel;
    }

    public void setTransactionFailed(String message) {
        titleText.set("Payment finished");
        stopLoading();
        showPinpad.set(false);
        isSuccess.set(false);
        showResultStatus.set(true);
        isWaiting.set(false);
        transactionResult.set(message);
    }

    public void clearErrorState() {
        showResultStatus.set(false);
//        transactionResult.set("");
//        isSuccess.set(false);
    }

    public void displayAmount(String newAmount) {
        amount.set("¥" + newAmount);
    }

    public void setWaitingStatus(boolean isWaitings) {
        isWaiting.set(isWaitings);
    }

    public void setTransactionSuccess() {
        titleText.set("Payment finished");
        stopLoading();
        showPinpad.set(false);
        isSuccess.set(true);
        isWaiting.set(false);
        showResultStatus.set(true);
    }

    public void startLoading(String text) {
        isWaiting.set(false);
        isLoading.set(true);
        loadingText.set(text);
    }

    public void stopLoading() {
        isLoading.set(false);
        isWaiting.set(false);
        loadingText.set("");
    }

    public BindingCommand continueTxnsCommand = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            finish();
        }
    });
//    public BindingCommand sendReceiptCommand = new BindingCommand(new BindingAction() {
//        @Override
//        public void call() {
//            isPrinting.set(true);
//            PrinterManager instance = PrinterManager.getInstance();
//            PrinterDevice mPrinter = instance.getPrinter();
//            PrinterHelper.getInstance().setPrinter(mPrinter);
//            PrinterHelper.getInstance().initPrinter(mContext);
//            TRACE.i("bitmap = " + receiptBitmap);
//            new Handler().postDelayed(() -> {
//                try {
//                    PrinterHelper.getInstance().printBitmap(getApplication(), receiptBitmap);
//                } catch (RemoteException e) {
//                    throw new RuntimeException(e);
//                }
//                PrinterHelper.getInstance().getmPrinter().setPrintListener(new PrintListener() {
//                    @Override
//                    public void printResult(boolean b, String s, PrinterDevice.ResultType resultType) {
//                        TRACE.i("resultType = " + resultType.getValue());
//                        if (!b && resultType.getValue() == -9) {
//                            if (mContext != null) {
//                                DialogUtils.showLowBatteryDialog(mContext, R.layout.dialog_low_battery, R.id.okButton, false, new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View view) {
//                                        isPrinting.set(false);
//                                        finish();
//                                    }
//                                });
//                                return;
//                            }
//                        }
//                        if (b) {
//                            ToastUtils.showShort("Print Finished!");
//                        } else {
//                            ToastUtils.showShort("Print Result: " + s);
//                        }
//                        isPrinting.set(false);
//                        finish();
//                    }
//                });
//            }, 100);
//        }
//    });

    public Bitmap convertReceiptToBitmap(TextView receiptView) {
        float originalTextSize = receiptView.getTextSize();
        int originalWidth = receiptView.getWidth();
        int originalHeight = receiptView.getHeight();
        receiptView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize * 1.5f);
        receiptView.measure(
                View.MeasureSpec.makeMeasureSpec(receiptView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        Bitmap bitmap = Bitmap.createBitmap(
                receiptView.getWidth(),
                receiptView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        receiptView.layout(0, 0, receiptView.getWidth(), receiptView.getMeasuredHeight());
        receiptView.draw(canvas);
        receiptView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
        receiptView.layout(0, 0, originalWidth, originalHeight);
        receiptBitmap = bitmap;
        return bitmap;
    }

    public void requestOnlineAuth(boolean isICC, String message) {
        if (!isICC) {
            isOnlineSuccess.setValue(true);
        } else {
            JSONObject object = new JSONObject();
            try {
                object.put("requestData", message);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            addSubscribe(apiService.sendMessage(AUTHFROMISSUER_URL, object)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        TRACE.i("online auth rsp code= " + response.getResult());
                        String onlineRspCode = (String) response.getResult();
                        if (response.isOk()) {
                            ToastUtils.showShort("Send online success");
                            POSManager.getInstance().sendOnlineProcessResult("8A02" + onlineRspCode);
                        } else {

                            POSManager.getInstance().sendOnlineProcessResult("8A023035");
                            transactionResult.set("Send online failed：" + response.getMessage());
                            ToastUtils.showShort("Send online failed：" + response.getMessage());
                        }
                    }, throwable -> {

                        POSManager.getInstance().sendOnlineProcessResult("8A023035");

                        ToastUtils.showShort("The network is failed：" + throwable.getMessage());
                        transactionResult.set("The network is failed：" + throwable.getMessage());
                    }));
        }

    }
}
