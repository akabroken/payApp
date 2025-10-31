package com.isw.payapp.devices.feitian;

import static androidx.core.content.ContextCompat.getSystemService;
import static com.ftpos.library.smartpos.emv.IPinBlockFormat.BLOCK_FORMAT_0;
import static com.ftpos.library.smartpos.emv.IPinBlockFormat.BLOCK_FORMAT_1;
import static com.ftpos.library.smartpos.errcode.ErrCode.ERR_PIN_BYPASS;
import static com.ftpos.library.smartpos.errcode.ErrCode.ERR_SUCCESS;
import static com.ftpos.library.smartpos.keymanager.KeyType.KEY_TYPE_IPEK;
import static com.ftpos.library.smartpos.keymanager.KeyType.KEY_TYPE_PEK;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_CENTER;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_LEFT;
import static com.ftpos.library.smartpos.util.EncodeConversionUtil.EncodeConversion;
import static com.isw.payapp.devices.feitian.utils.FEMVTag.EMV_TAG95_MAP;
import static com.isw.payapp.devices.feitian.utils.FEMVTag.EMV_TAG_MAP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ftpos.apiservice.aidl.led.LedConfig;
import com.ftpos.library.smartpos.datautils.BytesTypeValue;
import com.ftpos.library.smartpos.device.Device;
import com.ftpos.library.smartpos.emv.Amount;
import com.ftpos.library.smartpos.emv.CAPublicKeyInfo;
import com.ftpos.library.smartpos.emv.CandidateAIDInfo;
import com.ftpos.library.smartpos.emv.Emv;
import com.ftpos.library.smartpos.emv.IActionFlag;
import com.ftpos.library.smartpos.emv.IKernelINSInfo;
import com.ftpos.library.smartpos.emv.IPinBlockFormat;
import com.ftpos.library.smartpos.emv.OnEmvResponse;
import com.ftpos.library.smartpos.emv.OnSearchCardCallback;
import com.ftpos.library.smartpos.emv.TrackData;
import com.ftpos.library.smartpos.emv.TransRequest;
import com.ftpos.library.smartpos.errcode.ErrCode;
import com.ftpos.library.smartpos.icreader.IcReader;
import com.ftpos.library.smartpos.keymanager.KeyManager;
import com.ftpos.library.smartpos.keymanager.KeyType;
import com.ftpos.library.smartpos.led.Led;
import com.ftpos.library.smartpos.magreader.MagReader;
import com.ftpos.library.smartpos.nfcreader.NfcReader;
import com.ftpos.library.smartpos.pin.PinSeting;
import com.ftpos.library.smartpos.posSystem.PosSystem;
import com.ftpos.library.smartpos.printer.OnPrinterCallback;
import com.ftpos.library.smartpos.printer.PrintStatus;
import com.ftpos.library.smartpos.printer.Printer;
import com.ftpos.library.smartpos.util.BytesUtils;
import com.google.android.material.snackbar.Snackbar;
import com.isw.payapp.R;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.feitian.configBean.CAPublicKeyBean;
import com.isw.payapp.devices.feitian.configBean.CRLBean;
import com.isw.payapp.devices.feitian.configBean.EMVAcquirerParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVAppParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVCLAppParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVCLDRLBean;
import com.isw.payapp.devices.feitian.configBean.ExceptionListBean;
import com.isw.payapp.devices.feitian.configBean.XmlDataBean;
import com.isw.payapp.devices.feitian.constants.ICardType;
import com.isw.payapp.devices.feitian.constants.IParamType;
import com.isw.payapp.devices.feitian.constants.IPinpadCode;
import com.isw.payapp.devices.feitian.helpers.SvrHelper;
import com.isw.payapp.devices.feitian.utils.FTLV;
import com.isw.payapp.devices.feitian.utils.FTLVElement;
import com.isw.payapp.devices.feitian.utils.FTLVList;
import com.isw.payapp.devices.feitian.utils.FXmlParser;
import com.isw.payapp.devices.feitian.views.FeitianPinPadView;
import com.isw.payapp.devices.feitian.views.RFLogoDialog;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.dialog.PrinterPreviewDialog;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.utils.BytesUtil;
import com.isw.payapp.devices.feitian.constants.TapLampColor;
import com.isw.payapp.utils.DUKPK2009_CBC;
import com.isw.payapp.utils.EmvTlvParser;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.XMLUtils;
import com.isw.payapp.utils.tlvs.EMVTag;
import com.isw.payapp.views.pinkeyboard.BasePinPadView;
import com.jirui.logger.Logger;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeitianEmvService implements IEmvProcessor {

    // Constants
    private static final String TAG = "FeitianEmvService";
    public static final int TIMEOUT_SEARCH_CARD = 10;
    public static final int TIMEOUT_PINPAD = 20;
    public static final int REQUEST_CODE_PIN = 1001;

    // CVM Flags
    private static final byte EMV_CVMFLAG_OLPIN_SIGN = 0x02;
    private static final byte EMV_CVMFLAG_PLOFFLINE_PIN_SIGN = 0x01;
    private static final byte EMV_CVMFLAG_PLOFFLINE_PIN_SIGNATURE_SIGN = 0x03;
    private static final byte EMV_CVMFLAG_ENOFFLINE_PIN_SIGN = 0x04;
    private static final byte EMV_CVMFLAG_ENOFFLINE_PIN_SIGNATURE_SIGN = 0x05;
    private static final byte EMV_CVMFLAG_SIGNATURE = 0x1E;
    private static final byte EMV_CVMFLAG_NO_CVM = 0x1F;

    // Device components
    protected Led led;
    protected Emv iemv;
    protected Printer printer;
    protected IcReader icReader;
    protected NfcReader nfcReader;
    protected MagReader magReader;
    protected KeyManager ikey;

    protected Device device;
    protected PinSeting pinSeting;

    // Transaction state
    protected boolean hasInitEmv = false;
    protected boolean prePINPhase = false;
    protected boolean emvStatus = false;
    protected boolean isSeePhone = false;

    // UI components
    private FeitianPinPadDialog feitianPinPadDialog;

    private PinPadDialog mPinpadDialog;
    private RFLogoDialog mRFLogoDialog;
    private Timer timer;
    private WeakReference<EditText> pinpadEditTextRef;
    private WeakReference<View> scvTextRef;
    private WeakReference<View> tvReceiptRef;
    private WeakReference<View> btnSendReceiptRef;
    private View rootView;

    // Application components
    private final WeakReference<Activity> classActivityRef;
    private final TransactionData classTransactionData;
    private final EmvServiceCallback classEmvCallBacks;
    private final Handler mainHandler;
    private final ExecutorService transactionExecutor;
    private final Context context;

    private String responseMessage;
    private CountDownLatch serviceBindingLatch;
    private int transType = 0;
    private CardModel cardModel;

    private EmvModel emvModel;
    private int selectedPosition = 0;

    // Transaction data
    private TransRequest transRequest;
    private Amount amount;

    private String pinBlockValue;
    private String mainPan;

    private String ksnValue;
    private CountDownLatch mCountDownLatch;

    private DeviceListener deviceListener;

    // UI components for app selection
    private ListView appList;
    private LinearLayout layoutList;




    public FeitianEmvService(Activity classActivity, TransactionData classTransactionData,
                             EmvServiceCallback classEmvCallBacks) {
        this.classActivityRef = new WeakReference<>(classActivity);
        this.classTransactionData = classTransactionData;
        this.classEmvCallBacks = classEmvCallBacks;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.transactionExecutor = Executors.newSingleThreadExecutor();
        this.responseMessage = "";
        this.serviceBindingLatch = new CountDownLatch(1);
        this.context = classActivity;
        this.deviceListener = new DeviceListener();
        //  initializeDeviceComponents();
    }

    //region Core Interface Implementation
    @Override
    public void initializeDevice() throws Exception {
        // Device initialization logic if needed
    }

    @Override
    public void initializeEmvService() throws Exception {
        Logger.v("Initializing transaction");
        Log.i(TAG, "initializeEmvService");
        classEmvCallBacks.onLoading("Initializing transaction");
        SvrHelper.instance().setServiceListener(deviceListener);
        if (!clearAllParameters()) return;
        if (!updateAllParameters()) return;
        pinSeting.setOnlinePinBlockFormat(IPinBlockFormat.BLOCK_FORMAT_0);
        pinSeting.setOnlinePinKeyType(KeyType.KEY_TYPE_IPEK);
        hasInitEmv = true;
    }

    @Override
    public void startEmvService() throws Exception {
        transaction();
        classEmvCallBacks.onLoading("Starting transaction process");
    }

    @Override
    public void cancelTransaction() {
        if (prePINPhase && emvStatus) {
            emvStatus = false;
            SvrHelper.instance().cancelOperation();
            stopEmv(IPinpadCode.USER_TURN_OFF, null);
            hideSoftKeyboard();
        }
    }

    @Override
    public String getResponse() {
        return responseMessage != null ? responseMessage : "";
    }

    @Override
    public void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt) {
        this.pinpadEditTextRef = new WeakReference<>(pinpadEditText);
        this.scvTextRef = new WeakReference<>(scvText);
        this.tvReceiptRef = new WeakReference<>(tvReceipt);
        this.btnSendReceiptRef = new WeakReference<>(btnSendReceipt);
        this.rootView = scvText;
    }
    //endregion

    //region Device Initialization
    private void initializeDeviceComponents() {
        try {
            led = SvrHelper.instance().getLED();
            iemv = SvrHelper.instance().getEmv();
            ikey = SvrHelper.instance().getKey();
            printer = SvrHelper.instance().getPrinter();
            icReader = SvrHelper.instance().getIcReader();
            nfcReader = SvrHelper.instance().getNfcReader();
            magReader = SvrHelper.instance().getMagReader();
            device = SvrHelper.instance().getDevice();
            pinSeting = SvrHelper.instance().getPinSetting();


            Log.d(TAG, "Device components initialized successfully");
            serviceBindingLatch.countDown();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing device components", e);
            classEmvCallBacks.onError("Device initialization failed: " + e.getMessage());
        }
    }
    //endregion

    //region Transaction Management
    private void transaction() {
        Log.d(TAG, "transaction: ");
        if (emvStatus) {
            Log.d(TAG, "transaction: emvStatus " + emvStatus);
            hideSoftKeyboard();
        }
        if (!hasInitEmv) {
            Log.d(TAG, "transaction: hasInitEmv " + hasInitEmv);
            initTransaction();
        }
        Log.d(TAG, "transaction: hasInitEmv " + hasInitEmv);
        Log.d(TAG, "transaction: emvStatus " + emvStatus);
        doTransaction();
        // Log.d(TAG, "transaction: doTransaction() " + emvStatus);
    }

    private void initTransaction() {
        if (emvStatus) {
            showTransactionBar();
            return;
        }

        Logger.v("Initializing transaction");
        Log.d(TAG, "initTransaction: ");
        if (!clearAllParameters()) return;
        if (!updateAllParameters()) return;

        hasInitEmv = true;
    }

    private void doTransaction() {
        Log.d(TAG, "doTransaction: ");
        if (emvStatus) {
            showTransactionBar();
            return;
        }

        Logger.v("Starting transaction");
        Log.d(TAG, "Starting transaction: ");

        String amt = classTransactionData.getAmount();
        int cardSupport = ICardType.TYPE_CARD_CONTACT; // Default to contact card

        Logger.v("Creating trade request");
        Amount amount = createAmount(amt);
        if (amount == null) return;

        TransRequest transRequest = createTransRequest(cardSupport);
        String formatAmount = formatTransactionAmount(amount.getmAmount());

        String[] titles = getActivity().getResources().getStringArray(R.array.trans_type);
        updateDockLCD(((String) Objects.requireNonNull(Array.get(titles, selectedPosition))).toUpperCase(),
                "Amt:" + formatAmount, "in trading ...");
        Log.d(TAG, "Amount: " + amount.getmAmount() + " " + amount.getmOtherAmount());

        startEMV(amount, transRequest);
        Log.d(TAG, "Starting EMV process");
        classEmvCallBacks.onLoading("Starting EMV process");
    }

    private Amount createAmount(String amt) {
        Log.d(TAG, "createAmount: " + amt);
        if (TextUtils.isEmpty(amt) || !TextUtils.isDigitsOnly(amt)) {
            Logger.e("Please enter a valid amount");
            Log.e(TAG, "createAmount: Please enter a valid amount");
            return null;
        }

        try {
            long lAmt = Long.parseLong(amt);
            if (lAmt < 0L) {
                Logger.e("Amount cannot be negative");
                return null;
            }
            return new Amount(lAmt, 0);
        } catch (NumberFormatException e) {
            Logger.e("Invalid amount format");
            Log.d(TAG, "createAmount: ", e);
            return null;
        }
    }

    private TransRequest createTransRequest(int cardSupport) {
        return new TransRequest(transType)
                .setmCurrencyCode("0404")
                .setCardType(cardSupport)
                .setVerifyPinSkip(false)
                .setMagTransQuickPass(false)
                .setMagTransServiceCodeProcess(true)
                .setMaxTimeoutEMVThreadWait(30)
                .setReadRecordCallback(true)
                .setEnableAppSelectCallback(true)
                .setNeedBeep(false)
                .setSeePhoneContinueTrans(isSeePhone);
    }

    private String formatTransactionAmount(long amount) {
        return String.format(Locale.getDefault(), "%d", amount);
    }
    //endregion

    //region EMV Process Management
    protected void startEMV(Amount amount, TransRequest transRequest) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        this.amount = amount;
        this.transRequest = transRequest;

        try {
            Logger.i("Starting EMV process");
            SvrHelper.instance().setLed(false, false, false, true);
            iemv.startEMV(amount, transRequest, emvHandler);
            emvStatus = true;
            prePINPhase = true;
        } catch (Exception e) {
            Logger.e("Exception occurred in EMV process");
            Log.e(TAG, "startEMV: ", e);
            classEmvCallBacks.onError("EMV process failed to start");
        }
    }

    protected void stopEMV() {
        try {
            Logger.i("Stopping EMV process");
            iemv.stopEMV();
        } catch (Exception e) {
            Logger.e("Exception occurred while stopping EMV process");
            Log.e(TAG, "stopEMV: ", e);
        }
    }

    public void stopEmv(int code, String data) {
        switch (code) {
            case IPinpadCode.PINPAD_EXCEPTION:
                SvrHelper.instance().cancelOperation();
                Logger.e("Input PIN exception: " + data);
                break;
            case IPinpadCode.PINPAD_TIMEOUT:
                Logger.e("Input PIN timeout");
                break;
            case IPinpadCode.PINPAD_CANCEL:
                Logger.e("Input PIN cancelled");
                break;
            case IPinpadCode.PINPAD_SCREEN_OFF:
                Logger.e("Screen off when creating PINPad");
                break;
            case IPinpadCode.USER_TURN_OFF:
                Logger.e("Transaction stopped by user");
                break;
            default:
                Logger.e("Unknown error occurred in EMV");
                break;
        }
        iemv.stopEMV();
    }
    //endregion

    //region Parameter Management
    protected boolean clearAllParameters() {
        if (iemv == null) {
            Logger.e("Clear All Parameters Fail, Emv is null");
            return false;
        }

        try {
            int ret = iemv.manageEmvAppParameters(IActionFlag.CLEAR, null);
            Log.d(TAG, "manageEmvAppParameters Clear application parameters of EMV contact card transaction:: " + ret);
            Logger.v("Clear application parameters of EMV contact card transaction: " + ret);

            ret = iemv.manageDRL(IActionFlag.CLEAR, null);
            Logger.v("Clear DRL parameters: " + ret);
            Log.d(TAG, "manageDRL Clear certificate revocation list: " + ret);

            ret = iemv.manageCAPubKey(IActionFlag.CLEAR, null);
            Logger.v("Clear CA public key parameters: " + ret);
            Log.d(TAG, "manageCAPubKey Clear CA public key parameters: " + ret);

            ret = iemv.manageEmvclAppParameters(IActionFlag.CLEAR, null);
            Logger.v("Clear application parameters of EMV contactless card transaction: " + ret);
            Log.d(TAG, "manageEmvclAppParameters Clear application parameters of EMV contactless card transaction: " + ret);

            ret = iemv.setCRL(IActionFlag.CLEAR, null);
            Logger.v("Clear certificate revocation list: " + ret);
            Log.d(TAG, "setCRL Clear certificate revocation list: " + ret);

            ret = iemv.setExceptionList(IActionFlag.CLEAR, null);
            Logger.v("Clear black list: " + ret);
            Log.d(TAG, "setExceptionList Clear black list: " + ret);

            Logger.i("Clear All Parameters Successfully");
            Log.d(TAG, "Clear All Parameters Successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing parameters: " + e.getMessage());
            Logger.e("Error clearing parameters: " + e.getMessage());
            return false;
        }
    }

    protected boolean updateAllParameters() {
        if (iemv == null) {
            Logger.e("Update All Parameters Fail, Emv is null");
            return false;
        }

        boolean success = true;
        success &= updateParametersByFile(IParamType.TYPE_EMV_ACQUIRER_PARAM);
        success &= updateParametersByFile(IParamType.TYPE_APP_PARAM_EMV);
        success &= updateParametersByFile(IParamType.TYPE_APP_PARAM_EMVCL);
        success &= updateParametersByFile(IParamType.TYPE_CA_PUBKEY);
        success &= updateParametersByFile(IParamType.TYPE_EMVCL_DRL);
        success &= updateParametersByFile(IParamType.TYPE_CRL);
        success &= updateParametersByFile(IParamType.TYPE_EXCEPTION_LIST);

        if (success) {
            Log.i(TAG, "All Parameters Updated Successfully");
            Logger.i("All Parameters Updated Successfully");
        } else {
            Log.e(TAG, "Some parameters failed to update");
            Logger.e("Some parameters failed to update");
        }

        return success;
    }

    private boolean updateParametersByFile(IParamType type) {
        try (InputStream inputStream = context.getAssets().open(type.getPath())) {
            byte[] rsvBuffer = new byte[inputStream.available()];
            int read = inputStream.read(rsvBuffer);
            if (read <= 0) {
                Logger.e("Read file fail, path: " + type.getPath());
                return false;
            }

            List<XmlDataBean> xmlList = FXmlParser.parseXmlFile(type, rsvBuffer);
            if (xmlList == null || xmlList.isEmpty()) {
                Logger.e("Parse file data fail, xml list is null");
                return false;
            }

            return processXmlData(type, xmlList);
        } catch (IOException | XmlPullParserException e) {
            Logger.e("Error updating parameters from file: " + e.getMessage());
            Log.e(TAG, "updateParametersByFile: ", e);
            return false;
        }
    }

    private boolean processXmlData(IParamType type, List<XmlDataBean> xmlList) {
        try {
            for (XmlDataBean bean : xmlList) {
                int result = processBeanData(type, bean);
                if (result != 0) {
                    Logger.e("Failed to add " + type.name() + " parameters, Code: " +
                            Integer.toHexString(result) + " - " + ErrCode.toString(result));
                }
            }
            return true;
        } catch (Exception e) {
            Logger.e("Error processing XML data: " + e.getMessage());
            return false;
        }
    }

    private int processBeanData(IParamType type, XmlDataBean bean) {
        switch (type) {
            case TYPE_EMV_ACQUIRER_PARAM:
                byte[] acquirerData = ((EMVAcquirerParamsBean) bean).getBytes();
                byte[] desAcquirerData = new byte[((EMVAcquirerParamsBean) bean).getTlvLens()];
                System.arraycopy(acquirerData, 0, desAcquirerData, 0, desAcquirerData.length);
                return iemv.setDefaultAppParameters(desAcquirerData);

            case TYPE_APP_PARAM_EMV:
                byte[] appData = ((EMVAppParamsBean) bean).getBytes();
                byte[] desAppData = new byte[((EMVAppParamsBean) bean).getTlvLens()];
                System.arraycopy(appData, 0, desAppData, 0, desAppData.length);
                return iemv.manageEmvAppParameters(IActionFlag.ADD, desAppData);

            case TYPE_APP_PARAM_EMVCL:
                byte[] clAppData = ((EMVCLAppParamsBean) bean).getBytes();
                byte[] desClAppData = new byte[((EMVCLAppParamsBean) bean).getTlvLens()];
                System.arraycopy(clAppData, 0, desClAppData, 0, desClAppData.length);
                return iemv.manageEmvclAppParameters(IActionFlag.ADD, desClAppData);

            case TYPE_CA_PUBKEY:
                CAPublicKeyInfo caPublicKey = ((CAPublicKeyBean) bean).getCAPublicKey();
                return iemv.manageCAPubKey(IActionFlag.ADD, caPublicKey);

            case TYPE_EMVCL_DRL:
                byte[] drlData = ((EMVCLDRLBean) bean).getBytes();
                byte[] desDrlData = new byte[((EMVCLDRLBean) bean).getTlvLens()];
                System.arraycopy(drlData, 0, desDrlData, 0, desDrlData.length);
                return iemv.manageDRL(IActionFlag.ADD, desDrlData);

            case TYPE_CRL:
                byte[] crlData = ((CRLBean) bean).getBytes();
                byte[] desCrlData = new byte[((CRLBean) bean).getTlvLens()];
                System.arraycopy(crlData, 0, desCrlData, 0, desCrlData.length);
                return iemv.setCRL(IActionFlag.ADD, desCrlData);

            case TYPE_EXCEPTION_LIST:
                byte[] exceptionData = ((ExceptionListBean) bean).getBytes();
                byte[] desExceptionData = new byte[((ExceptionListBean) bean).getTlvLens()];
                System.arraycopy(exceptionData, 0, desExceptionData, 0, desExceptionData.length);
                return iemv.setExceptionList(IActionFlag.ADD, desExceptionData);

            default:
                return -1;
        }
    }
    //endregion

    //region UI Management
    protected void updateDockLCD(String title, String amt, String tips) {
        if (printer == null) return;
        if (title != null) printer.showLineText(0, title, PRINT_STYLE_CENTER);
        if (amt != null) printer.showLineText(1, amt, PRINT_STYLE_CENTER);
        if (tips != null) printer.showLineText(2, tips, PRINT_STYLE_LEFT);
    }

    private void hideSoftKeyboard() {
        Activity activity = getActivity();
        if (activity != null) {
            View focusView = activity.getCurrentFocus();
            if (focusView != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }
    }

    @SuppressLint("ShowToast")
    public void showTransactionBar() {
        if (rootView != null) {
            Snackbar.make(rootView, "The transaction is running, please wait.", Snackbar.LENGTH_INDEFINITE)
                    .setDuration(3000)
                    .show();
        }
    }

    private Activity getActivity() {
        return classActivityRef != null ? classActivityRef.get() : null;
    }
    //endregion

    //region CVM Management
    private int getCVM(String tlvList) {
        FTLVList tlvs = FTLV.fromData(tlvList);
        byte[] value9F34 = tlvs.getTLV("9F34").getBytesValue();
        switch (value9F34[0] & 0x3F) {
            case 0x01:
                return EMV_CVMFLAG_PLOFFLINE_PIN_SIGN;
            case 0x02:
                return EMV_CVMFLAG_OLPIN_SIGN;
            case 0x03:
                return EMV_CVMFLAG_PLOFFLINE_PIN_SIGNATURE_SIGN;
            case 0x04:
                return EMV_CVMFLAG_ENOFFLINE_PIN_SIGN;
            case 0x05:
                return EMV_CVMFLAG_ENOFFLINE_PIN_SIGNATURE_SIGN;
            case 0x01E:
                return EMV_CVMFLAG_SIGNATURE;
            case 0x01F:
                return EMV_CVMFLAG_NO_CVM;
            default:
                return 0;
        }
    }
    //endregion

    //region PIN Pad Management
    private void doInputPin(int cvmFlag, int type, Long amount) {
        EmvTlvParser parser = new EmvTlvParser();
        if (!emvStatus) return;

        collapseStatusBar();
        PosSystem.getInstance(getActivity()).enableTurningOffScreen(false, 60);

        if (!isCompactDevice()) {
            setScreenOrientationBasedOnRotation();
        }

        Logger.i("Inputting PIN...");
        updateDockLCD(null, null, "Enter pin ...");

        mainHandler.post(() -> {
            FTLVList pan = FTLV.fromData(iemv.getTlvList("5A"));
            Logger.tlv(pan.toString(), EMV_TAG_MAP);
            Map<String, String >panMap = parser.extractAllTags(pan.toString());
            mainPan = panMap.get("5A");

            Log.d(TAG, "PAN: "+ mainPan);
            int ipekKeyIndex = 0x01;

            BytesTypeValue ksnBytesTypeValue = new BytesTypeValue();
            int ret = ikey.exportDukptKsn(KeyType.KEY_TYPE_IPEK, ipekKeyIndex, ksnBytesTypeValue);
            if (ret == ErrCode.ERR_SUCCESS) {
                // After import , exportDukptKsn will be FFFF9876543210E00001(hex format)
                Log.d(TAG, "Current 3 KSN:" + BytesUtils.byte2HexStr(ksnBytesTypeValue.getData()));
                ksnValue =  BytesUtils.byte2HexStr(ksnBytesTypeValue.getData());
            }

            showFeitianPinPad(mainPan);
            //ksnValue =  BytesUtils.byte2HexStr(ksnBytesTypeValue.getData());

        });
    }

    private void showFeitianPinPad(String cardNumber) {

        feitianPinPadDialog = new FeitianPinPadDialog(getActivity(), iemv, ikey, cardNumber,
                new FeitianPinPadView.OnPayClickListener() {
                    @Override
                    public void onCancel() {
                        handlePinpadResult(IPinpadCode.PINPAD_CANCEL, "");
                    }

                    @Override
                    public void onPayPass() {
                        handlePinpadResult(IPinpadCode.PIN_BYPASS, String.valueOf(ERR_PIN_BYPASS));
                    }

                    @Override
                    public void onConfirm(byte[] encryptedPinBlock) {

                        //String pinBlockHex = BytesUtils.byte2HexStr(encryptedPinBlock);
                        pinBlockValue = BytesUtils.byte2HexStr(encryptedPinBlock);
                        Log.d(TAG, "Encrypted PIN Block: " + pinBlockValue);
                        Logger.d(TAG, "Encrypted PIN Block: " + pinBlockValue);
                        handlePinpadResult(IPinpadCode.PINPAD_SUCCESS, pinBlockValue);
                    }
                });
        feitianPinPadDialog.show();
    }

    private boolean isCompactDevice() {
        String model = Build.MODEL;
        return "T50".equals(model) || "F350".equals(model) || "DT60".equals(model);
    }

    private void setScreenOrientationBasedOnRotation() {
        Activity activity = getActivity();
        if (activity == null) return;

        int angle = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();

        Logger.i("Screen rotation angle: " + angle);
        switch (angle) {
            case Surface.ROTATION_90:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            default:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    private void handlePinpadResult(int code, String pinData) {
        Logger.d("PinpadResult Code: " + code + ", Result: " + pinData);
        prePINPhase = false;

        // Restore screen settings
        PosSystem.getInstance(getActivity()).enableTurningOffScreen(true, 0);
        if (!isCompactDevice()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        if (code != IPinpadCode.PINPAD_SUCCESS && code != IPinpadCode.PIN_BYPASS) {
            stopEmv(code, pinData);
        } else {
            String command = buildPinCommand(code, pinData);
            Log.d(TAG, "command: " + command);
            Logger.d("command: " + command);
            iemv.respondEvent(command);
        }
    }

    private String buildPinCommand(int code, String pinData) {
        if (code == IPinpadCode.PINPAD_SUCCESS) {
            return String.format("1F6301%02x", IPinpadCode.PIN_NORMAL);
        } else {
            return buildBypassPinCommand(pinData);
        }
    }

    private String buildBypassPinCommand(String pinData) {
        if (pinData.length() > 18) {
            pinData = pinData.substring(0, 18);
        }

        try {
            long pinCode = Long.parseLong(pinData);
            if (pinCode == 0x63C0) {
                return String.format("1F6301%02x", IPinpadCode.OFFLINE_PIN_EXCEED_LIMIT);
            } else if (pinCode == 0x6983) {
                return String.format("1F6301%02x", IPinpadCode.OFFLINE_PIN_6983);
            } else if (pinCode == ERR_PIN_BYPASS) {
                return String.format("1F6301%02x", IPinpadCode.PIN_BYPASS);
            } else {
                return String.format("1F6301%02x", pinCode);
            }
        } catch (NumberFormatException e) {
            stopEmv(IPinpadCode.PINPAD_ERROR, pinData);
            return null;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PIN) {
            PosSystem.getInstance(getActivity()).enableTurningOffScreen(true, 0);
            if (!isCompactDevice()) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }

            if (resultCode == Activity.RESULT_OK && data != null) {
                int code = data.getIntExtra(IPinpadCode.PINPAD_BACK_CODE, IPinpadCode.PINPAD_UNKNOWN);
                String pinData = data.getStringExtra(IPinpadCode.PINPAD_BACK_DATA);
                Logger.i("PINPad ResultCode: " + code + ", Data: " + pinData);
                handlePinpadResult(code, pinData);
            }
        }
    }
    //endregion

    //region Status Bar Management
    protected void collapseStatusBar() {
        Activity activity = getActivity();
        if (activity == null) return;

        try {
            Object service = activity.getSystemService("statusbar");
            if (service == null) return;

            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            Method collapse = android.os.Build.VERSION.SDK_INT <= 16 ?
                    clazz.getMethod("collapse") : clazz.getMethod("collapsePanels");

            collapse.setAccessible(true);
            // collapse.invoke(service);
        } catch (Exception e) {
            Log.w(TAG, "Failed to collapse status bar", e);
        }
    }
    //endregion

    //region Application Selection
    protected void doAppSelect(List<CandidateAIDInfo> list) {
        if (list == null || list.isEmpty() || appList == null) return;

        mainHandler.post(() -> {
            layoutList.setVisibility(View.VISIBLE);
            List<String> listApp = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                String appLabel = EncodeConversion(
                        list.get(i).getApplicationLabel_tag50(),
                        list.get(i).getCodeTableIndex_tag9F11()
                );
                listApp.add(appLabel);
                Logger.v(i + " " + appLabel);
            }

            Logger.d("Please select application");
            appList.setOnItemClickListener(listListener);
            appList.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listApp));
        });
    }

    private final AdapterView.OnItemClickListener listListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            layoutList.setVisibility(View.GONE);
            Logger.d("Selected application: " + position);
            String tlvData = String.format("1F6601%02x", (byte) position);
            iemv.respondEvent(tlvData);
        }
    };
    //endregion

    //region Transaction Result Processing
    protected void doEndProcess(int code, String data) {
        Logger.d("onEndProcess: 0x" + Integer.toHexString(code) + " - " + ErrCode.toString(code));
        emvStatus = false;
        outputResult(code, data);

        if (code == ErrCode.ERR_RESTART_B || code == ErrCode.ERR_END_APP_B) {
            isSeePhone = false;
            doTransaction();
        } else if (code == ErrCode.ERR_SEE_PHONE) {
            isSeePhone = true;
            doTransaction();
        }

        led.ledCardIndicator(0x00, 0x00, 0x00, 0x00);
    }

    void outputResult(int code, String data) {
         cardModel = new CardModel();
         emvModel = new EmvModel();
        EmvTlvParser parser = new EmvTlvParser();
//        parser.parse(data);

        try{
            if (code == ErrCode.ERR_ONLINE_APPROVED || code == ErrCode.ERR_OFFLINE_APPROVED) {
                handleTransactionSuccess();
            } else {
                handleTransactionFailure();
            }

            mCountDownLatch = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
                SvrHelper.instance().setLed(false, false, false, false);
                led.readerLedStatus(0x03, false, false, false);
                mCountDownLatch.countDown();
            }).start();

            if (isTransactionCompleted(code)) {

                Log.d(TAG,"pinBlockValue-->> : "+ pinBlockValue);
                FTLVList list = FTLV.fromData(iemv.getTlvList("1F531F609C9A9F215A579F025F2A9F34959F339F409F669F1E"));
                Logger.tlv(list.toString(), EMV_TAG_MAP);

                FTLVList emvlist = FTLV.fromData(iemv.getTlvList("575A5F34829F369F269F279F349F105F2A959F1A9F359A9C9F37849F33"));
                Log.d(TAG, "EMV DATA\n"+emvlist.toString());
                parser.printAllTags(emvlist.toString());
                Map<String , String> tagMap = parser.extractAllTags(emvlist.toString());
                Log.d(TAG,"EMV tag 9A : "+ tagMap.get("9A"));
                //tagMap.get("57");
                Logger.tlv(emvlist.toString(), EMV_TAG_MAP);
                Log.i(TAG, "ksnValue : "+ ksnValue );
                cardModel.setKsn(ksnValue);
                cardModel.setPinBlock("T"+pinBlockValue);

                String clearPinData = DUKPK2009_CBC.getData(ksnValue, pinBlockValue,
                        DUKPK2009_CBC.Enum_key.PIN, DUKPK2009_CBC.Enum_mode.CBC);
                Log.i(TAG,"PIN CLEAR :"+ clearPinData);

                // EMV tag 57 (Track 2 Equivalent data)
                emvModel.setTrack2data(tagMap.get("57"));
                // EMV tag 5A (Application Primary Account Number (PAN))
                cardModel.setPan(tagMap.get("5A"));
                // EMV tag 5F34 (Application Primary Account Number (PAN) Sequence Number)
                emvModel.setCarSeqNo(tagMap.get("5F34"));
                // EMV tag 82 (application interchange profile)
                emvModel.setApplicationInterchangeProfile(tagMap.get("82"));
                // EMV tag 9F36 (Application Transaction Counter (ATC))
                emvModel.setAtc(tagMap.get("9F36"));
                // EMV tag 9F26 (Application Cryptogram)
                emvModel.setCryptogram(tagMap.get("9F26"));
                // EMV tag 9F27 (Cryptogram Information Data)
                emvModel.setCryptogramInformationData(tagMap.get("9F27"));
                // EMV tag 9F34 (Cardholder Verification Method (CVM))
                emvModel.setCvmResults(tagMap.get("9F34"));
                // EMV tag 9F10 (Issuer Application Data)
                emvModel.setIssuerApplicationData(tagMap.get("9F10"));
                // EMV tag 5F2A (Transaction Currency Code)
                emvModel.setTransactionCurrencyCode(tagMap.get("5F2A"));
                // EMV tag 95 (Terminal Verification Results)
                emvModel.setTerminalVerificationResult(tagMap.get("95"));
                //EMV tag 9F1A (Terminal Country Code)
                emvModel.setTerminalCountryCode(tagMap.get("9F1A"));
                //EMV tag 9F35 (Terminal Type)
                emvModel.setTerminalType(tagMap.get("9F35"));
                //EMV tag 9A (Transaction Date)
                emvModel.setTransactionDate(tagMap.get("9A"));
                //EMV tag 9C (Transaction Type)
                emvModel.setTransactionType(tagMap.get("9C"));
                //EMV tag 9F37 (Unpredictable Number)
                emvModel.setUnpredictableNumber(tagMap.get("9F37"));
                // EMV tag 84 (Dedicated File (DF) Name)
                emvModel.setDedicatedFileName(tagMap.get("84"));
                // EMV tag 9F33 (Terminal Capabilities)
                emvModel.setTerminalCapabilities(tagMap.get("9F33"));

                KsmgRequest pinchangeRequest = new KsmgRequest(emvModel, classTransactionData, cardModel);
                Log.d(TAG, "Payload \n"+ pinchangeRequest.generatePayload());

                if (list.contains("95")) {
                    Logger.bit(list.getTLV("95").getBytesValue(), EMV_TAG95_MAP);
                }

                processNetworkRequest(pinchangeRequest, emvModel);

//                if (shouldPrintReceipt(code)) {
//                    try {
//                        mCountDownLatch.await();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        throw new RuntimeException("Transaction interrupted", e);
//                    }
//
//                    if (!isCompactDevice()) {
//                        printReceipt(null);
//                    }
//                }
            }

            Logger.d("Result: [" + Integer.toHexString(code) + "] " + ErrCode.toString(code));
            Logger.i("Transaction Finished");
            updateDockLCD("WELCOME", " ", " ");
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private void processNetworkRequest(KsmgRequest pinchangeRequest, EmvModel emvModel) {
        ExecutorService networkExecutor = NetworkExecutor.getExecutor();

        networkExecutor.execute(() -> {
            try {
                Activity activity = getActivity();
                if (activity == null) return;

                String baseUrl = "https://" + TerminalConfig.loadTerminalDataFromJson(activity, "__transip") + ":"
                        + TerminalConfig.loadTerminalDataFromJson(activity, "__transport") + "/";

                NetworkService.initialize(activity, baseUrl);
                NetworkService networkService = NetworkService.getInstance();

                String response = networkService.postPayLoadSync(pinchangeRequest.generatePayload());

                mainHandler.post(() -> handleNetworkResponse(response, emvModel));

            } catch (Exception e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                mainHandler.post(() -> {
                    classEmvCallBacks.onStopLoading();
                    classEmvCallBacks.onError("Network error: " + e.getMessage());
                });
            }
        });
    }

    private void handleNetworkResponse(String response, EmvModel emvModel) {
        try {
            classEmvCallBacks.onStopLoading();

            String respMessage = XMLUtils.isErrorResponse(response);

            showPrinterPreviewDialog(respMessage, emvModel);

        } catch (Exception e) {
            Log.e(TAG, "Error handling network response: " + e.getMessage());
            e.printStackTrace();
            classEmvCallBacks.onError("Error processing response");
        }
    }

    private void showPrinterPreviewDialog(String respMessage, EmvModel emvModel) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;

        PrinterPreviewDialog previewDialog = new PrinterPreviewDialog(
                activity,
                cardModel,
                emvModel,
                classTransactionData,
                respMessage,
                new PrinterPreviewDialog.OnPrintClickListener() {
                    @Override
                    public void onPrintClick(String previewContent) {
                        printReceipt(createReceipt(respMessage, emvModel));
                        classEmvCallBacks.onTransactionSuccess(respMessage);
                    }

                    @Override
                    public void onCancelClick() {
                        Log.d(TAG,"Printing cancelled by user");
                    }
                }
        );

        previewDialog.show();
    }

    private Receipt createReceipt(String respMessage, EmvModel emvModel) {
        Activity activity = getActivity();
        Receipt receipt = new Receipt();


        receipt.setBank(TerminalConfig.loadTerminalDataFromJson(getActivity(), "__bank"));
        receipt.setMerchant(TerminalConfig.loadTerminalDataFromJson(getActivity(), "__merchantloc"));
        receipt.setTerminalId(TerminalConfig.loadTerminalDataFromJson(getActivity(), "__tid"));


        receipt.setAmount(classTransactionData.getAmount());
        receipt.setCurrency("KES");
        receipt.setDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        receipt.setTransactionType(classTransactionData.getTransactionType());

        String maskPan = (cardModel.getPan() != null ?
                cardModel.getPan().substring(0, 6) + "******" +
                        cardModel.getPan().substring(cardModel.getPan().length() - 4) : "N/A");
        receipt.setCardNumber(maskPan);
        receipt.setEntryMode("Chip");
        receipt.setAid(emvModel.getDedicatedFileName());
        receipt.setAtc(emvModel.getAtc());
        receipt.setTvr(emvModel.getTerminalVerificationResult());
        receipt.setResponse(respMessage);

        return receipt;
    }



    private void handleTransactionSuccess() {
        if ("F360".equals(Build.MODEL)) {
            LedConfig ledConfig = new LedConfig(TapLampColor.Success.RED,
                    TapLampColor.Success.GREEN, TapLampColor.Success.BLUE);
            led.tapeLampOn(ledConfig, 100);
            mainHandler.post(() -> {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        led.ledDefault();
                    }
                }, 3 * 1000);
            });
        }
        SvrHelper.instance().setLed(false, true, true, true);
        updateDockLCD(null, null, "Txn. approved");
    }

    private void handleTransactionFailure() {
        if ("F360".equals(Build.MODEL)) {
            LedConfig ledConfig = new LedConfig(TapLampColor.Failed.RED,
                    TapLampColor.Failed.GREEN, TapLampColor.Failed.BLUE);
            led.tapeLampOn(ledConfig, 100);
        }
        SvrHelper.instance().setLed(true, false, false, false);
        updateDockLCD(null, null, "Txn. fail");
    }

    private boolean isTransactionCompleted(int code) {
        return code == ErrCode.ERR_ONLINE_APPROVED || code == ErrCode.ERR_OFFLINE_APPROVED ||
                code == ErrCode.ERR_OFFLINE_DECLINED || code == ErrCode.ERR_ONLINE_DECLINED ||
                code == ErrCode.ERR_ONLINE_END_CARD_DECLINED;
    }

    private boolean shouldPrintReceipt(int code) {
        return code == ErrCode.ERR_ONLINE_APPROVED || code == ErrCode.ERR_OFFLINE_APPROVED ||
                code == ErrCode.ERR_OFFLINE_DECLINED;
    }
    //endregion

    //region Receipt Printing
    void printReceipt(Receipt tlvs) {
        try {
            int ret = printer.open();
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer open failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            ret = printer.startCaching();
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer start caching failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            ret = printer.setGray(3);
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer set gray failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            PrintStatus printStatus = new PrintStatus();
            ret = printer.getStatus(printStatus);
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer get status failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            Logger.i("Temperature = " + printStatus.getmTemperature() + ", Gray = " + printStatus.getmGray());
            if (!printStatus.getmIsHavePaper()) {
                Logger.i("Printer out of paper");
                return;
            }

            printReceiptContent(tlvs);

        } catch (Exception e) {
            Logger.i("Print failed: " + e.toString());
            Log.e(TAG, "printReceipt: ", e);
        }
    }

    private void printReceiptContent(Receipt tlvs) {
        printer.setAlignStyle(PRINT_STYLE_CENTER);
        printer.printStr("Receipt\n");

        printer.setAlignStyle(PRINT_STYLE_LEFT);
        printer.printStr("Please retain this receipt for your exchange.\n");
        printer.printStr("------------------------\n");

        printer.printStr("Bank: " + tlvs.getBank() + "\n");
        printer.printStr("Merchant: " + tlvs.getMerchant() + "\n");
        printer.printStr("Terminal ID: " + tlvs.getTerminalId() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Card Number: " + tlvs.getCardNumber() + "\n");
        printer.printStr("Amount: " + tlvs.getAmount() + " " + tlvs.getCurrency() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Entry Mode: " + tlvs.getEntryMode() + "\n");
        printer.printStr("AID: " + tlvs.getAid() + "\n");
        printer.printStr("ATC: " + tlvs.getAtc() + "\n");
        printer.printStr("TVR: " + tlvs.getTvr() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Date/Time: " + tlvs.getDateTime() + "\n");
        printer.printStr("Transaction Type: " + tlvs.getTransactionType() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Response: " + tlvs.getResponse() + "\n");
        printer.printStr("------------------------\n");
        printer.feed(1);
        printer.printStr("Thank you!\n");
        printer.feed(1);


        int usedPaperLen = printer.getUsedPaperLenManage();
        if (usedPaperLen < 0) {
            Logger.i("Get used paper length failed");
        } else {
            Logger.i("UsedPaperLenManage = " + usedPaperLen + "mm");
        }

        printer.print(new OnPrinterCallback() {
            @Override
            public void onSuccess() {
                Logger.i("Print success");
                printer.feed(32);
            }

            @Override
            public void onError(int errorCode) {
                Logger.i("Print failed: " + String.format(" errCode = 0x%x\n", errorCode));
            }
        });
    }
    //endregion

    //region Callbacks
    private final OnSearchCardCallback callback = new OnSearchCardCallback() {
        @Override
        public void onSuccess(int type, TrackData trackData) {
            led.ledCardIndicator(0x01, 0, 200, 200);
            if (mRFLogoDialog != null) {
                mRFLogoDialog.dismiss();
            }

            boolean isSuccess = handleCardSuccess(type, trackData);
            updateCardReaderStatus(isSuccess);

            if (isSuccess) {
                iemv.respondEvent(null);
            } else {
                iemv.stopEMV();
            }
        }

        @Override
        public void onError(int errCode) {
            Logger.e("Search card failed, Error Code [" + Integer.toHexString(errCode) + " ]: " + ErrCode.toString(errCode));
            if (mRFLogoDialog != null) {
                mRFLogoDialog.dismiss();
            }

            led.readerLedStatus(0x03, true, false, false);
            SvrHelper.instance().setLed(true, false, false, false);
            iemv.stopEMV();
        }
    };

    private boolean handleCardSuccess(int type, TrackData trackData) {
        switch (type) {
            case ICardType.TYPE_CARD_MAGNETIC:
                return handleMagneticCard(trackData);
            case 0x01: // Contact card
                Logger.d("Search card successful, type: Contact card");
                return true;
            case 0x02: // Contactless card
                Logger.d("Search card successful, type: Contactless card");
                return true;
            default:
                Logger.d("Search card successful, type: unknown");
                return false;
        }
    }

    private boolean handleMagneticCard(TrackData trackData) {
        Logger.d("Search card successful, type: Magnetic card");
        Logger.i("TR1:" + trackData.getTrack1Data());
        Logger.i("TR2:" + trackData.getTrack2Data());
        Logger.i("TR3:" + trackData.getTrack3Data());

        String tr2 = trackData.getTrack2Data();
        if (tr2 != null) {
            String[] data = tr2.split("=");
            if (data.length > 0) {
                Logger.i("PAN:" + data[0]);
                if (data.length > 1 && data[1].length() > 7) {
                    Logger.i("EXPIRED_DATE:" + data[1].substring(0, 4));
                    Logger.i("SERVICE_CODE:" + data[1].substring(4, 7));
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
                return true;
            } else {
                Logger.e("Track2 data missing!");
            }
        }
        return false;
    }

    private void updateCardReaderStatus(boolean isSuccess) {
        if (isSuccess) {
            led.readerLedStatus(0x03, false, true, false);
            SvrHelper.instance().setLed(false, true, false, true);
        } else {
            led.readerLedStatus(0x03, true, false, false);
            SvrHelper.instance().setLed(true, false, false, false);
        }
    }

    private class DeviceListener implements SvrHelper.ServiceListener {
        @Override
        public void onServerBinded() {
            Log.d(TAG, "Service successfully bound");
            // Initialize device components when service is bound
            initializeDeviceComponents();
        }
    }

    private final OnEmvResponse emvHandler = new OnEmvResponse() {
        @Override
        public void onAppSelect(boolean reselect, List<CandidateAIDInfo> list) {
            Logger.d("onAppSelect Whether to reselect: " + reselect);
            Logger.i("AID List: " + list.size());
            doAppSelect(list);
        }

        @Override
        public void onPinEntry(int cvm) {
            Logger.d("onPinEntry: " + cvm);
            if ((cvm & (Emv.EMV_CVMFLAG_PLOFFLINE_PIN_SIGN | Emv.EMV_CVMFLAG_OLPIN_SIGN | Emv.EMV_CVMFLAG_ENOFFLINE_PIN_SIGN)) == 0) {
                iemv.respondEvent(null);
                prePINPhase = false;
            } else {
                doInputPin(cvm, transRequest.getmTransType(), amount.getmAmount());
            }
        }

        @Override
        public void onOnlineProcess(String data) {
            Logger.d("onOnlineProcess");
            Logger.d("Trans Card Type: 0x%02X", iemv.getTransCardtype());
            updateDockLCD(null, null, "Online request ...");

            Log.i(TAG, "onOnlineProcess: "+ data);

            if (iemv.getTransCardtype() != ICardType.TYPE_CARD_MAGNETIC) {
                int cvm = getCVM(iemv.getTlvList("9F34"));
                Logger.d("Cardholder verify method: 0x%02X", cvm);
            }

            iemv.getTlvList("9C9A9F215A579F025F2A9F34959F339F409F669F1E");
            Logger.i("Simulated online interaction");

            iemv.setIssuerOnlineResponseData(0, null, "00", null, null, null);
            iemv.respondEvent(null);

        }

        @Override
        public void onEndProcess(int code, String data) {
            doEndProcess(code, data);
        }

        @Override
        public void onDisplayPanInfo(String pan) {
            Logger.d("onDisplayPanInfo, PAN: " + pan);
        }

        @Override
        public void onSearchCard() {
            Logger.d("onSearchCard");
            Logger.i("Build.MODEL: " + Build.MODEL);
            showRFLogoIfNeeded();
            setupCardSearchLED();
            led.readerLedStatus(0x03, false, false, true);
            iemv.searchCard(TIMEOUT_SEARCH_CARD, callback);
            Logger.i("Searching Card...");
        }

        @Override
        public void onSearchCardAgain() {
            Logger.d("onSearchCardAgain");
            setupCardSearchLED();

            if (shouldShowRFLogo()) {
                mainHandler.post(() -> {
                    mRFLogoDialog = new RFLogoDialog(getActivity());
                    mRFLogoDialog.show();
                });
            }

            led.readerLedStatus(0x03, false, false, true);
            updateDockLCD(null, null, "Present card");
            iemv.searchCard(TIMEOUT_SEARCH_CARD, callback);
            Logger.i("Searching Card Again...");
        }

        @Override
        public void onProcessInteractionPoint(int step) {
            Logger.d("onProcessInteractionPoint: " + step);
            if (step == 3) { // ReadRecord
                String df70 = iemv.getTlvList("9F4D");
                Log.i("yaojm", "9F4D String " + df70);
            }
            iemv.respondEvent(null);
        }

        @Override
        public void onObtainData(int code, byte[] data, byte[] dataInformation) {
            Logger.i("onObtainData, Kernel INS Info: " + code);
            String tag = BytesUtil.bytes2HexString(data);
            Log.i(TAG,"onObtainData : "+ tag);

            if (code == IKernelINSInfo.TAG_LIST) {
                handleTagListRequest(tag);
            } else if (code == IKernelINSInfo.TLV_DATA) {
                Logger.v("paypass get DET");
                iemv.setTLV("1F6A", "5A081122334455667788DF81100101");
            }

            iemv.respondEvent(null);
        }

        @Override
        public Amount onUpdateTransAmount() {
            Logger.v("onUpdateTransAmount:");
            Logger.i("Simulate update transaction amount");
            return new Amount(20, 0);
        }
    };

    private void showRFLogoIfNeeded() {
        if (shouldShowRFLogo()) {
            mainHandler.post(() -> {
                mRFLogoDialog = new RFLogoDialog(getActivity());
                mRFLogoDialog.show();
            });
        }
    }

    private boolean shouldShowRFLogo() {
        String model = Build.MODEL;
        return "M200".equals(model) || "F310".equals(model) ||
                "F310 P".equals(model) || "F360".equals(model);
    }

    private void setupCardSearchLED() {
        List<LedConfig> colorList = new ArrayList<>();
        colorList.add(new LedConfig(TapLampColor.Breath.RED_0, TapLampColor.Breath.GREEN_0, TapLampColor.Breath.BLUE_0));
        colorList.add(new LedConfig(TapLampColor.Breath.RED_1, TapLampColor.Breath.GREEN_1, TapLampColor.Breath.BLUE_1));
        colorList.add(new LedConfig(TapLampColor.Breath.RED_2, TapLampColor.Breath.GREEN_2, TapLampColor.Breath.BLUE_2));
        led.breathOn(colorList, 100, 5, 5, 30);
        led.ledCardIndicator(0x03, 0, 500, 500);
    }

    private void handleTagListRequest(String tag) {
        switch (tag) {
            case "1F3E":
                Logger.i("1F3E = Get the accumulated amount");
                iemv.setTLV("1F3E", "00000000");
                break;
            case "1F10":
                Logger.i("1F10 = Execution result of the issuing bank");
                iemv.setTLV("1F10", "01");
                break;
            default:
                break;
        }
    }
    //endregion

    /**
     * PIN Pad Dialog wrapper class
     */
    public static class PinPadDialog extends Dialog {
        private BasePinPadView pinPadView;

        private Context context;
        private BasePinPadView.OnPayClickListener listener;

        int ret;
        int ipekKeyIndex = 0x01;

        public PinPadDialog(Context context, BasePinPadView.OnPayClickListener listener) {
            super(context, R.style.FullScreenDialog);
            this.listener = listener;
            this.context = context;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            pinPadView = new BasePinPadView(context);
            pinPadView.setPayClickListener(new BasePinPadView.OnPayClickListener() {
                @Override
                public void onCancel() {
                    if (listener != null) {
                        listener.onCancel();
                    }
                    dismiss();
                }

                @Override
                public void onPayPass() {
                    if (listener != null) {
                        listener.onPayPass();
                    }
                }

                @Override
                public void onConfirm(String password) {
                    if (listener != null) {
                        listener.onConfirm(password);
                    }
                    dismiss();
                }
            });

            setContentView(pinPadView);

            Window window = getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setGravity(Gravity.CENTER);
            }

            setCancelable(false);
        }

        @Override
        public void dismiss() {
            if (pinPadView != null) {
                pinPadView.cleanup();
            }
            super.dismiss();
        }
    }

    /**
     * Feitian-specific PIN Pad Dialog
     */
    public static class FeitianPinPadDialog extends Dialog {
        private FeitianPinPadView pinPadView;
        private Context context;
        private boolean isPinCompleted = false;

        public FeitianPinPadDialog(Context context, Emv emv, KeyManager keyManager, String pan,
                                   FeitianPinPadView.OnPayClickListener listener) {
            super(context, R.style.FullScreenDialog);
            this.context = context;

            pinPadView = new FeitianPinPadView(context, emv, keyManager, pan);
            // Enable debugging to see actual PIN values
            pinPadView.setEnableDebugLogging(true);
            pinPadView.setPayClickListener(new FeitianPinPadView.OnPayClickListener() {
                @Override
                public void onCancel() {
                    Log.d(TAG, "Dialog: PIN input cancelled");
                    isPinCompleted = true;
                    listener.onCancel();
                }

                @Override
                public void onPayPass() {
                    Log.d(TAG, "Dialog: PIN bypassed");
                    isPinCompleted = true;
                    listener.onPayPass();
                }

                @Override
                public void onConfirm(byte[] encryptedPinBlock) {
                    Log.d(TAG, "Dialog: PIN confirmed with encrypted block");
                    isPinCompleted = true;
                    listener.onConfirm(encryptedPinBlock);
                }
            });

            pinPadView.setOnDismissListener(new FeitianPinPadView.OnDismissListener() {
                @Override
                public void onDismiss() {
                    Log.d(TAG, "Dialog: Dismiss requested");
                    FeitianPinPadDialog.this.dismiss();
                }
            });
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(pinPadView);

            Window window = getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setGravity(Gravity.BOTTOM);
            }

            setCancelable(true);

            setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Log.d(TAG, "Dialog: Cancelled by user");
                    if (!isPinCompleted) {
                        pinPadView.cleanup();
                        if (pinPadView.mPayClickListener != null) {
                            pinPadView.mPayClickListener.onCancel();
                        }
                    }
                }
            });

            // Add a slight delay and reset before starting PIN input
            pinPadView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Reset any auto-filled values before starting
                  //  pinPadView.resetPinInput();

                    // Small delay to ensure reset is complete
                    pinPadView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pinPadView.startPinInput();
                        }
                    }, 100);
                }
            }, 300);
        }

        @Override
        public void show() {
            Log.d(TAG, "Dialog: Showing Feitian PIN pad");
            super.show();
        }

        @Override
        public void dismiss() {
            Log.d(TAG, "Dialog: Dismissing Feitian PIN pad");
            if (pinPadView != null) {
                //pinPadView.cleanup();
            }
            super.dismiss();
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                Log.d(TAG, "Dialog: Back button pressed");
                // Let Feitian SDK handle the cancel
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }
}