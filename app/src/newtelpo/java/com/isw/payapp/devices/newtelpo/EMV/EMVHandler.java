package com.isw.payapp.devices.newtelpo.EMV;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.common.sdk.emv.PinpadBytesOut;
import com.common.sdk.emv.PinpadEnum;
import com.common.sdk.emv.PinpadService;
import com.isw.payapp.database.TransactionDatabaseHelper;
import com.isw.payapp.databinding.DetectDialogBinding;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.newtelpo.NewTelpoPrinterService;
import com.isw.payapp.devices.newtelpo.utils.EmvTLVExtractor;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.dialog.PrinterPreviewDialog;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.paymentsRequests.KxmlRequest;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.utils.StringUtils;
import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.emv.EmvTLV;
import com.telpo.pinpad.PinParam;
import com.telpo.util.ErrMsg;
import com.telpo.util.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;

public class EMVHandler {

    // Constants
    private static final String TAG = "EMVHandler";
    private static final String SUCCESS_RESPONSE_CODE = "00";
    private static final String ASCII_CHARSET = "ASCII";
    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";

    // Key indices
    private static final int PIN_KEY_INDEX = 1;
    private static final int PAN_KEY_INDEX = 2;
    private static final int MAC_KEY_INDEX = 3;

    // Dependencies
    private final Context context;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ReentrantLock threadLock = new ReentrantLock();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_TIME_FORMAT);

    // Services
    private  PinpadService pinpadService;
    public   EmvService emvService;

    // Data models
    private TransactionData transactionData;
    private EmvModel emvData;
    private CardModel cardModel;

    // UI components
    private DetectDialogBinding detectDialogBinding;
    private AlertDialog processDialog;
    private AlertDialog panDialog;

    // Transaction state
    private final AtomicBoolean stopDetect = new AtomicBoolean(false);
    private final AtomicBoolean uiThreadRunning = new AtomicBoolean(false);
    private final AtomicBoolean isTransactionCompleted = new AtomicBoolean(false);

    // Card data
    public String cardNum = "";
    public String pinBlock = "";
    private String currentKSN = "";
    private String panCurrentKSN = "";
    private String track1 = "";
    private String track2 = "";

    private String scriptProcDoc;

    // Flags
    private boolean isDevInit = false;
    public boolean isMkMode = false;
    private boolean isDesMode = true;
    private boolean isPanDesMode = true;
    private boolean isPanMkMode = false;
    private boolean isOnlineTransaction = false;
    private boolean isNFC = false;
    private boolean isMag = false;
    private boolean isNoErr = true;

    private EmvServiceCallback classEmvCallBacks;

    // Amount
    private double amount = 0.00;

    // Callbacks
    private UIUpdateListener uiUpdateListener;
    private EmvCallback emvCallback;

    // EMV listener
    private final EmvServiceListener emvServiceListener = new EmvServiceListenerImpl();

    // Interfaces
    public interface UIUpdateListener {
        void onDisplayUpdated(String message);
        void onDisplayCleared();
    }

    public interface EmvCallback {
        void onEmvResult(boolean success, String message);
        void onPinRequired();
        void onAmountRequired();
        void onAppSelectionRequired(EmvCandidateApp[] apps);
        void onOnlineProcessingRequired(EmvOnlineData onlineData);
    }

    // Constructor
    public EMVHandler(Context context){
        this.context = context;
    }

    public EMVHandler(Context context, TransactionData transactionData,
                      EmvService emvService, PinpadService pinpadService, EmvServiceCallback classEmvCallBacks) {
        this.context = context;
        this.transactionData = transactionData;
        this.emvService = emvService;
        this.pinpadService = pinpadService;
        this.classEmvCallBacks = classEmvCallBacks;
        this.emvData = new EmvModel();
        this.cardModel = new CardModel();

        initializeDialogs();
    }

    // Initialization methods
    private void initializeDialogs() {
        panDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .create();

        processDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .create();
    }

    // UI Methods
    public void setUIUpdateListener(UIUpdateListener listener) {
        this.uiUpdateListener = listener;
    }

    public void setEmvCallback(EmvCallback callback) {
        this.emvCallback = callback;
    }

    public void clearDisplay() {
        uiHandler.post(() -> {
            if (uiUpdateListener != null) {
                uiUpdateListener.onDisplayCleared();
            }
        });
    }

    public void appendDisplay(final String message) {
        Log.i(TAG, message);
        uiHandler.post(() -> {
            if (uiUpdateListener != null) {
                uiUpdateListener.onDisplayUpdated(message);
            }
        });
    }

    public void changeDialogText(String message) {
        uiHandler.post(() -> {
            ensureDialogBinding();
            if (detectDialogBinding != null) {
                detectDialogBinding.tvCancel.setVisibility(View.GONE);
                detectDialogBinding.tvMessage.setText(message);
            }
        });
    }

    private void ensureDialogBinding() {
        if (detectDialogBinding == null && context != null) {
            detectDialogBinding = DetectDialogBinding.inflate(LayoutInflater.from(context));
        }
    }

    public void showPanDialog(String text) {
        uiHandler.post(() -> {
            if (text != null && !text.isEmpty()) {
                panDialog.setMessage(text);
                panDialog.getWindow().setGravity(Gravity.TOP);
                panDialog.show();
            } else {
                dismissPanDialog();
            }
        });
    }

    public void dismissPanDialog() {
        uiHandler.post(() -> {
            if (panDialog != null && panDialog.isShowing()) {
                panDialog.dismiss();
            }
        });
    }

    // Encryption Methods
    public String encryptPan(String data) {
        if (data == null || data.isEmpty()) {
            appendDisplay("Empty data for encryption");
            return "";
        }
        appendDisplay("No Empty data for encryption");
        String paddedData = panPadding(data);
        String cipherText = "";

        try {
            if (isPanMkMode) {
                cipherText = encryptWithMkMode(paddedData);
            } else {
                cipherText = encryptWithDukptMode(paddedData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Encryption error", e);
            appendDisplay("Encryption failed: " + e.getMessage());
        }

        return cipherText;
    }

    private String encryptWithMkMode(String paddedData) {
        PinpadBytesOut output = new PinpadBytesOut();
        byte[] iv = isPanDesMode ?
                new byte[8] : // 8 zeros for DES
                new byte[16]; // 16 zeros for AES

        int result;
        if (isPanDesMode) {
            result = pinpadService.Pinpad_Calculate_Normal_DES(
                    PAN_KEY_INDEX,
                    StringUtil.hexStringToByte(paddedData),
                    output,
                    iv,
                    PinpadEnum.ENUM_ENC_MODE.PIN_ENC_ENCRYPT,
                    PinpadEnum.ENUM_ECB_MODE.PIN_ECB_CBC);
        } else {
            result = pinpadService.Pinpad_Calculate_Normal_AES(
                    PAN_KEY_INDEX,
                    StringUtil.hexStringToByte(paddedData),
                    output,
                    iv,
                    PinpadEnum.ENUM_ENC_MODE.PIN_ENC_ENCRYPT,
                    PinpadEnum.ENUM_ECB_MODE.PIN_ECB_CBC);
        }

        return handleEncryptionResult(result, output, isPanDesMode ? "DES" : "AES");
    }

    private String encryptWithDukptMode(String paddedData) {
        PinpadBytesOut ksn = new PinpadBytesOut();
        appendDisplay("encryptWithDukptMode(String paddedData)"+StringUtil.bytesToHexString(ksn.outResult));
        int startResult;
        String cipherText = "";

        if (isPanDesMode) {
            appendDisplay("isPanDesMode ="+isPanDesMode);
            startResult = pinpadService.Pinpad_DEA_DUKPT_Session_Start(1, ksn);
            if (startResult == PinpadService.PIN_OK) {
                cipherText = processDesDukptEncryption(paddedData, ksn);
                pinpadService.Pinpad_DEA_DUKPT_Session_End();
            } else {
                appendDisplay("DES DUKPT session start failed: " + ErrMsg.GetPinPadErrMsg(startResult));
            }
        } else {
            startResult = pinpadService.Pinpad_AES_DUKPT_Session_Start(0, ksn);
            if (startResult == PinpadService.PIN_OK) {
                cipherText = processAesDukptEncryption(paddedData, ksn);
                pinpadService.Pinpad_AES_DUKPT_Session_End();
            } else {
                appendDisplay("AES DUKPT session start failed: " + ErrMsg.GetPinPadErrMsg(startResult));
            }
        }

        return cipherText;
    }

    private String processDesDukptEncryption(String paddedData, PinpadBytesOut ksn) {
        appendDisplay("processDesDukptEncryption(String paddedData, PinpadBytesOut ksn)"+paddedData);
        PinpadBytesOut output = new PinpadBytesOut();
        byte[] iv = new byte[8];

        int result = pinpadService.Pinpad_DEA_DUKPT_Calculate_Des(
                StringUtil.hexStringToByte(paddedData),
                output,
                iv,
                PinpadEnum.ENUM_ENC_MODE.PIN_ENC_ENCRYPT,
                PinpadEnum.ENUM_ECB_MODE.PIN_ECB_CBC);

        if (result == PinpadService.PIN_OK) {
            panCurrentKSN = StringUtil.bytesToHexString(ksn.outResult);
        }

        return handleEncryptionResult(result, output, "DES DUKPT");
    }

    private String processAesDukptEncryption(String paddedData, PinpadBytesOut ksn) {
        PinpadBytesOut output = new PinpadBytesOut();
        byte[] iv = new byte[8];

        int result = pinpadService.Pinpad_AES_DUKPT_Calculate_Des(
                PinpadEnum.ENUM_AES_DUKPT_KeyUsage._DataEncryptionEncrypt,
                StringUtil.hexStringToByte(paddedData),
                output,
                iv,
                PinpadEnum.ENUM_ENC_MODE.PIN_ENC_ENCRYPT,
                PinpadEnum.ENUM_ECB_MODE.PIN_ECB_CBC);

        if (result == PinpadService.PIN_OK) {
            panCurrentKSN = StringUtil.bytesToHexString(ksn.outResult);
        }

        return handleEncryptionResult(result, output, "AES DUKPT");
    }

    private String handleEncryptionResult(int result, PinpadBytesOut output, String mode) {
        if (result == PinpadService.PIN_OK) {
            return StringUtil.bytesToHexString(output.outResult);
        } else {
            appendDisplay(mode + " encryption error: " + ErrMsg.GetPinPadErrMsg(result));
            return "";
        }
    }

    private String panPadding(String data) {
        int dataLen = data.length();
        int paddingSize = 16 - (dataLen % 16);
        byte[] padded = new byte[dataLen + paddingSize];

        System.arraycopy(data.getBytes(StandardCharsets.UTF_8), 0, padded, 0, dataLen);
        for (int i = dataLen; i < padded.length; i++) {
            padded[i] = (byte) paddingSize;
        }

        return StringUtil.bytesToHexString(padded);
    }

    // PIN Methods
    public String getPin(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            appendDisplay("Invalid card number for PIN");
            return "";
        }

        if (isMkMode) {
            return getMkPin(cardNumber);
        } else {
            return getDukptPin(cardNumber);
        }
    }

    public String getMkPin(String cardNumber) {
        PinpadBytesOut pinBlockOut = new PinpadBytesOut();
        int result = pinpadService.Pinpad_GetPin(
                PIN_KEY_INDEX,
                cardNumber,
                PinpadEnum.ENUM_PIN_BLOCK_FORMAT.ISO_9564_FORMAT_0,
                6, 0, 60, pinBlockOut);

        return handlePinResult(result, pinBlockOut, "MK/SK");
    }

    public String getDukptPin(String cardNumber) {
        PinpadBytesOut ksn = new PinpadBytesOut();
        int startResult;
        String pinBlockStr = "";

        if (isDesMode) {
            startResult = pinpadService.Pinpad_DEA_DUKPT_Session_Start(1, ksn);
            if (startResult == PinpadService.PIN_OK) {
                pinBlockStr = processDesDukptPin(cardNumber, ksn);
                pinpadService.Pinpad_DEA_DUKPT_Session_End();
            }
        } else {
            startResult = pinpadService.Pinpad_AES_DUKPT_Session_Start(0, ksn);
            if (startResult == PinpadService.PIN_OK) {
                pinBlockStr = processAesDukptPin(cardNumber, ksn);
                pinpadService.Pinpad_AES_DUKPT_Session_End();
            }
        }

        return pinBlockStr;
    }

    private String processDesDukptPin(String cardNumber, PinpadBytesOut ksn) {
        PinpadBytesOut pinBlockOut = new PinpadBytesOut();
        appendDisplay("amount:-"+amount);

        int result = pinpadService.Pinpad_DEA_DUKPT_GetPin(
                cardNumber,
                PinpadEnum.ENUM_PIN_BLOCK_FORMAT.ISO_9564_FORMAT_0,
                12, 4, 60, pinBlockOut);

        if (result == PinpadService.PIN_OK) {
            currentKSN = StringUtil.bytesToHexString(ksn.outResult);

            cardModel.setKsnd("605");
            cardModel.setKsn(currentKSN);
            cardModel.setPan(cardNumber);

            Log.i(TAG, "DES DUKPT KSN: " + currentKSN);
        }

        return handlePinResult(result, pinBlockOut, "DES DUKPT");
    }

    private String processAesDukptPin(String cardNumber, PinpadBytesOut ksn) {
        PinpadBytesOut pinBlockOut = new PinpadBytesOut();
        int result = pinpadService.Pinpad_AES_DUKPT_GetPin(
                PinpadEnum.ENUM_AES_DUKPT_KeyUsage._PINEncryption,
                cardNumber, 6, 0, 60, pinBlockOut);

        if (result == PinpadService.PIN_OK) {
            currentKSN = StringUtil.bytesToHexString(ksn.outResult);
            Log.i(TAG, "AES DUKPT KSN: " + currentKSN);
        }

        return handlePinResult(result, pinBlockOut, "AES DUKPT");
    }

    private String handlePinResult(int result, PinpadBytesOut pinBlockOut, String mode) {
        switch (result) {
            case PinpadService.PIN_OK:

                String pinBlockStr = StringUtil.bytesToHexString(pinBlockOut.outResult);
                cardModel.setPinBlock("T"+pinBlockStr);
                appendDisplay(mode + " PIN Block: " + pinBlockStr);
                return pinBlockStr;

            case PinpadService.PIN_ERROR_CANCEL:
                appendDisplay("PIN entry cancelled by user");
                isNoErr = false;
                return "";

            case PinpadService.PIN_ERROR_TIMEOUT:
                appendDisplay("PIN entry timeout");
                isNoErr = false;
                return "";

            default:
                appendDisplay(mode + " PIN error: " + ErrMsg.GetPinPadErrMsg(result));
                isNoErr = false;
                return "";
        }
    }

    // Card Detection Methods
    public void startCardDetection() {
        stopDetect.set(false);
        classEmvCallBacks.onLoading("Present Card...");
        Thread detectionThread = new Thread(new CardDetectionRunnable());
        detectionThread.start();
    }

    public void stopCardDetection() {
        stopDetect.set(true);
        dismissAllDialogs();
    }

    /**
     * transaction request
     */
    public boolean pay() {
        return true;
    }

    private class CardDetectionRunnable implements Runnable {
        @Override
        public void run() {
            threadLock.lock();
            try {
                resetTransactionData();
                initializeCardReaders();
                detectCards();
            } catch (InterruptedException e) {
                Log.w(TAG, "Card detection interrupted", e);
                Thread.currentThread().interrupt();
            } finally {
                threadLock.unlock();
                dismissAllDialogs();
            }
        }

        private void initializeCardReaders() {
            // Close all readers first
            EmvService.NfcCloseReader();
            EmvService.IccCloseReader();

            // Open NFC reader
            int nfcResult = EmvService.NfcOpenReader(200);
            if (nfcResult != EmvService.EMV_DEVICE_TRUE) {
                appendDisplay("NFC reader open failed: " + nfcResult);
            }

            // Open ICC reader
            int iccResult = EmvService.IccOpenReader();
            if (iccResult != EmvService.EMV_DEVICE_TRUE) {
                appendDisplay("ICC reader open failed: " + iccResult);
            }
        }

        private void detectCards() throws InterruptedException {
            appendDisplay("Starting card detection...");

            while (!stopDetect.get()) {
                // Check for NFC card
                if (EmvService.NfcCheckCard(300) == EmvService.EMV_DEVICE_TRUE) {
                    handleNfcCard();
                    break;
                }

                // Check for ICC card
                if (EmvService.IccCheckCard(300) == EmvService.EMV_DEVICE_TRUE) {
                    handleIccCard();
                    break;
                }

                // Small delay to prevent CPU spinning
                Thread.sleep(50);
            }
        }

        private void handleNfcCard() {
            stopDetect.set(true);
            changeDialogText("NFC card detected, starting transaction...");
            appendDisplay("NFC card detected");
            startNfcTransaction();
        }

        private void handleIccCard() {
            stopDetect.set(true);
            if (EmvService.IccCard_Poweron() == EmvService.EMV_DEVICE_TRUE) {
                changeDialogText("Chip card detected, starting transaction...");
                appendDisplay("Chip card detected");
                startIcTransaction();
                EmvService.IccCard_Poweroff();
            } else {
                appendDisplay("Failed to power on ICC card");
            }
        }
    }

    // Transaction Methods
    private void startNfcTransaction() {
        isNFC = true;
        amount = Double.parseDouble(transactionData.getAmount());

        int kernelId = emvService.NFC_CheckKernelID();
        switch (kernelId) {
            case EmvService.NFC_KERNEL_DEFAUT_CARD_VISA:
                appendDisplay("Card Type: VISA");
                // TODO: Implement VISA PayWave
                break;
            case EmvService.NFC_KERNEL_DEFAUT_CARD_MASTER:
                appendDisplay("Card Type: MASTER");
                // TODO: Implement MasterCard PayPass
                break;
            case EmvService.NFC_KERNEL_DEFAUT_CARD_UNIONPAY:
                appendDisplay("Card Type: UNIONPAY");
                // TODO: Implement UnionPay
                break;
            default:
                appendDisplay("Unsupported card type: " + kernelId);
                break;
        }

        dismissPanDialog();
    }

    private void startIcTransaction() {

        classEmvCallBacks.onLoading("Please Wait..");
        emvService.setListener(emvServiceListener);

        int result = emvService.Emv_TransInit();
        if (result != EmvService.EMV_TRUE) {
            appendDisplay("EMV transaction init failed: " + result);
            return;
        }

        appendDisplay("EMV transaction initialized");

        EmvParam param = createEmvParam();
        result = emvService.Emv_SetParam(param);
        if (result != EmvService.EMV_TRUE) {
            appendDisplay("EMV parameter set failed: " + result);
            return;
        }
        configureEmvParameters();

        appendDisplay("EMV parameters set");

        result = emvService.Emv_StartApp(EmvService.EMV_FALSE); // Online transaction


        if (result == EmvService.EMV_TRUE) {
            appendDisplay("Transaction successful");
            classEmvCallBacks.onTransactionSuccess("Transaction successful");
           // processTransactionResult();
        } else {
            appendDisplay("EMV transaction failed: " + ErrMsg.GetEmvErrMsg(result));
           // classEmvCallBacks.onTransactionCancelled();
            isNoErr = false;
        }

        dismissPanDialog();
    }

    private EmvParam createEmvParam() {
        EmvParam param = new EmvParam();
        param.CountryCode = new byte[]{(byte) 0x01, (byte) 0x56}; // Kenya (156)
        param.Capability = new byte[]{(byte) 0xE0, (byte) 0xF9, (byte) 0xC8};
        return param;
    }

    private boolean processTransactionResult() {
        if (!isOnlineTransaction) {
            // Process offline transaction
            processOfflineTransaction();
        } else {
            // Process online transaction
            boolean isSuccess = processOnlineTransaction();
            if (isSuccess) {
                return true;
            }
        }
        return false;
    }

    private void processOfflineTransaction() {
        String payload = prepareTransactionPayload();
        if (payload != null) {
            boolean success = sendTransactionToGateway(payload);
            if (success) {
                appendDisplay("Offline transaction processed successfully");
            } else {
                appendDisplay("Offline transaction failed");
            }
        }
    }

    private boolean processOnlineTransaction() {
        // TODO: Implement online transaction processing
        appendDisplay("Online transaction processing required");
        String payload = prepareTransactionPayload();
        appendDisplay(payload);
        boolean success = sendTransactionToGateway(payload);
        if (success) {
            appendDisplay("Online transaction processed successfully");
            return true;
        } else {
            appendDisplay("Online transaction failed");
        }
        return false;
    }

    // Network Methods
    private String prepareTransactionPayload() {
        try {
            EmvTLVExtractor emvTLVExtractor = new EmvTLVExtractor(emvService, transactionData);
            emvData = emvTLVExtractor.extractEmvData();

            String paymentApp = transactionData.getPaymentApp();
            if ("selectpin".equals(paymentApp)) {
                return preparePinChangePayload();
            } else {
                return preparePurchasePayload();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing transaction payload", e);
            appendDisplay("Failed to prepare transaction: " + e.getMessage());
            return null;
        }
    }

    private String preparePinChangePayload() {
        try {
            KsmgRequest pinChangeRequest = new KsmgRequest(emvData, transactionData, cardModel);
            return pinChangeRequest.Payload();
        } catch (Exception e) {
            Log.e(TAG, "Error preparing PIN change payload", e);
            appendDisplay("PIN change preparation failed");
            return null;
        }
    }

    private String preparePurchasePayload() {
        try {
            KxmlRequest purchaseRequest = new KxmlRequest(emvData, transactionData, cardModel);
            return purchaseRequest.Payload();
        } catch (Exception e) {
            Log.e(TAG, "Error preparing purchase payload", e);
            appendDisplay("Payment preparation failed");
            return null;
        }
    }

    private boolean sendTransactionToGateway(String payload) {
        try {
            initializeNetworkService();
            NetworkService networkService = NetworkService.getInstance();

            if (networkService == null) {
                appendDisplay("Network service not available");
                return false;
            }

            appendDisplay("Sending transaction to gateway...");
            String response = networkService.postPayLoadSync(payload);

            scriptProcDoc = response;

            if (response != null) {
                return processGatewayResponse(response);
            } else {
                appendDisplay("No response from gateway\n"+response);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending transaction", e);
            appendDisplay("Network communication failed: " + e.getMessage());
            return false;
        }
    }

    private boolean processGatewayResponse(String response) {
        appendDisplay(response);
        try {
            Document document = parseXmlResponse(response);
            if (document == null) {
                appendDisplay("Invalid gateway response");
                return false;
            }

            String responseCode = extractResponseCode(document);
            String responseMessage = extractResponseMessage(document);
          //  classEmvCallBacks.onTransactionFailed(responseMessage);

            if (SUCCESS_RESPONSE_CODE.equals(responseCode)) {
                appendDisplay("Transaction approved: " + responseMessage);
                showPrinterPreviewDialog("",emvData,responseMessage);
                return true;
            } else {
                appendDisplay("Transaction declined: " + responseMessage);

                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing gateway response", e);
            appendDisplay("Response processing error");
            return false;
        }
    }

    private String extractResponseCode(Document doc) {
        String code = getAttributeValue(doc, "var", "name", "responsecode");
        return code != null ? code : "96"; // Default to 96 if not found
    }

    private String extractResponseMessage(Document doc) {
        String message = getAttributeValue(doc, "var", "name", "responsemessage");
        if (message == null) {
            message = getValue(doc, "label");
        }
        return message != null ? message : "Unknown response";
    }

    // Utility Methods
    private void resetTransactionData() {
        currentKSN = "";
        panCurrentKSN = "";
        pinBlock = "";
        cardNum = "";
        track1 = "";
        track2 = "";

        isNFC = false;
        isMag = false;
        isNoErr = true;
        isOnlineTransaction = false;
    }

    private void dismissAllDialogs() {
        uiHandler.post(() -> {
            if (processDialog != null && processDialog.isShowing()) {
                processDialog.dismiss();
            }
            dismissPanDialog();
        });
    }

    private Activity getActivity() {
        return (context instanceof Activity) ? (Activity) context : null;
    }

    private void initializeNetworkService() {
        try {
            ConfigManager.refreshConfig(context);
            TerminalConfigModel config = ConfigManager.getConfig(context);

            String ip = config.getTransip();
            String port = config.getTransport();

            if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
                throw new IllegalArgumentException("Invalid terminal configuration");
            }

            String baseUrl = "https://" + ip + ":" + port + "/";
            NetworkService.initialize(context, baseUrl);
            Log.d(TAG, "Network service initialized");
        } catch (Exception e) {
            Log.e(TAG, "Network service initialization failed", e);
            throw new RuntimeException("Failed to initialize network service", e);
        }
    }

    // XML Parsing Methods
    private Document parseXmlResponse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String getAttributeValue(Document doc, String tagName,
                                            String attributeName, String attributeValue) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (attributeValue.equals(element.getAttribute(attributeName))) {
                        return element.getTextContent().trim();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting attribute value", e);
        }
        return null;
    }

    private static String getValue(Document doc, String tagName) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    return node.getTextContent().trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting value for tag: " + tagName, e);
        }
        return null;
    }

    // EMV Service Listener Implementation
    private class EmvServiceListenerImpl extends EmvServiceListener {
        @Override
        public int onInputAmount(EmvAmountData emvAmountData) {
            uiHandler.post(() -> {
                if (emvCallback != null) {
                    emvCallback.onAmountRequired();
                }
            });

            amount = Double.parseDouble(transactionData.getAmount());
            appendDisplay("Amount: "+ amount);
            emvAmountData.Amount = (long) (amount * 100);
            emvAmountData.TransCurrCode = 404; // KES
            emvAmountData.ReferCurrCode = 404;
            emvAmountData.TransCurrExp = 2;
            emvAmountData.ReferCurrExp = 2;
            emvAmountData.ReferCurrCon = 1;
            emvAmountData.CashbackAmount = 0;

            return EmvService.EMV_TRUE;
        }

        @Override
        public int onInputPin(EmvPinData emvPinData) {
            uiHandler.post(() -> {
                if (emvCallback != null) {
                    emvCallback.onPinRequired();
                }
            });

            dismissPanDialog();

            if (emvPinData.IsRetry == 1) {
                changeDialogText("Wrong PIN. Please try again");
            } else {
                changeDialogText("Please enter PIN");
            }

            String maskedPan = cardNum.length() > 10 ?
                    cardNum.substring(0, 6) + "******" + cardNum.substring(cardNum.length() - 4) :
                    "******";

            showPanDialog("PAN: " + maskedPan);

            if (emvPinData.type == EmvService.ONLIEN_ENCIPHER_PIN) {
                pinBlock = getPin(cardNum);
                if (!pinBlock.isEmpty()) {
                    emvPinData.Pin = StringUtil.hexStringToByte(pinBlock);
                    changeDialogText("EMV Processing...");
                    return EmvService.EMV_TRUE;
                }
            }

            changeDialogText("EMV Processing...");
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
            if (emvCallback != null) {
                uiHandler.post(() -> emvCallback.onAppSelectionRequired(emvCandidateApps));
            }

            if (emvCandidateApps == null || emvCandidateApps.length == 0) {
                return EmvService.ERR_USERCANCEL;
            }

            final int[] selectedIndex = {emvCandidateApps[0].index};
            uiThreadRunning.set(true);

            uiHandler.post(() -> showAppSelectionDialog(emvCandidateApps, selectedIndex));

            while (uiThreadRunning.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return EmvService.ERR_USERCANCEL;
                }
            }

            return selectedIndex[0];
        }

        @Override
        public int onSelectAppFail(int errorCode) {
            appendDisplay("Application selection failed: " + errorCode);
            if (emvCallback != null) {
                uiHandler.post(() -> emvCallback.onEmvResult(false,
                        "Application selection failed: " + errorCode));
            }
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onFinishReadAppData() {
            EmvTLV tlv = new EmvTLV(0x9F06);
            emvService.Emv_GetTLV(tlv);
            appendDisplay("AID: " + StringUtil.bytesToHexString(tlv.Value));

            extractCardNumber();
            extractAdditionalData();

            return EmvService.EMV_TRUE;
        }

        @Override
        public int onVerifyCert() {
            dismissPanDialog();
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onOnlineProcess(EmvOnlineData emvOnlineData) {
            appendDisplay("Online processing required");
            isOnlineTransaction = true;
           boolean isProcessSuccessful = processTransactionResult();

            if (emvCallback != null) {
                uiHandler.post(() -> emvCallback.onOnlineProcessingRequired(emvOnlineData));
            }


            if (isProcessSuccessful) {
                try {
                    Document document = parseXmlResponse(scriptProcDoc);
                    configureApprovedTransaction(emvOnlineData, document);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return EmvService.EMV_TRUE;
            }

            return EmvService.EMV_FALSE;
        }

        @Override
        public int onRequireTagValue(int tag, int source, byte[] bytes) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onRequireDatetime(byte[] bytes) {
            try {
                String currentTime = dateFormatter.format(new Date());
                byte[] timeBytes = currentTime.getBytes(ASCII_CHARSET);
                System.arraycopy(timeBytes, 0, bytes, 0, Math.min(timeBytes.length, bytes.length));
                return EmvService.EMV_TRUE;
            } catch (Exception e) {
                Log.e(TAG, "Error setting date/time", e);
                return EmvService.EMV_FALSE;
            }
        }

        @Override
        public int onReferProc() {
            dismissPanDialog();
            return EmvService.EMV_TRUE;
        }

        @Override
        public int OnCheckException(String s) {
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnCheckException_qvsdc(int i, String s) {
            return EmvService.EMV_FALSE;
        }

        private void showAppSelectionDialog(EmvCandidateApp[] apps, int[] selectedIndex) {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                uiThreadRunning.set(false);
                return;
            }

            String[] items = new String[apps.length];
            for (int i = 0; i < apps.length; i++) {
                items[i] = apps[i].appName;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle("Select Application")
                    .setCancelable(false)
                    .setSingleChoiceItems(items, 0, null)
                    .setPositiveButton("OK", (dialog, which) -> {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition >= 0 && selectedPosition < apps.length) {
                            selectedIndex[0] = apps[selectedPosition].index;
                            appendDisplay("Selected: " + items[selectedPosition]);
                        }
                        uiThreadRunning.set(false);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        selectedIndex[0] = EmvService.ERR_USERCANCEL;
                        uiThreadRunning.set(false);
                    });

            builder.create().show();
        }

        private void extractCardNumber() {
            EmvTLV tlv = new EmvTLV(0x5A);
            int result = emvService.Emv_GetTLV(tlv);

            if (result == EmvService.EMV_TRUE) {
                cardNum = StringUtil.bytesToHexString(tlv.Value).replace("F", "");
                appendDisplay("PAN: " + cardNum);
            } else {
                tlv = new EmvTLV(0x57);
                result = emvService.Emv_GetTLV(tlv);
                if (result == EmvService.EMV_TRUE) {
                    String track2Data = StringUtil.bytesToHexString(tlv.Value);
                    int delimiterIndex = track2Data.indexOf('D');
                    if (delimiterIndex > 0) {
                        cardNum = track2Data.substring(0, delimiterIndex);
                        appendDisplay("Track2 PAN: " + cardNum);
                    }
                }
            }

            if (!cardNum.isEmpty()) {
                appendDisplay("Encrypted PAN: " + encryptPan(cardNum));
            }
        }

        private void extractAdditionalData() {
            List<EmvTLV> tagList = EMVUtilsConfigs.getTLVCardDataTags();
            for (EmvTLV tag : tagList) {
                int result = emvService.Emv_GetTLV(tag);
                String tagHex = Integer.toHexString(tag.Tag).toUpperCase();
                if (result == EmvService.EMV_TRUE) {
                    appendDisplay(String.format("Tag -> %s: %s", tagHex,
                            StringUtil.bytesToHexString(tag.Value)));
                } else {
                    appendDisplay(String.format("Tag --> %s: N/A", tagHex));
                }
            }
        }
    }

    public void changePanUIVisibility(Boolean isShow, String text) {
        uiHandler.post (new Runnable() {
            @Override
            public void run() {
                if (isShow) {
                    panDialog.setMessage(text);
                    panDialog.getWindow().setGravity(Gravity.TOP);
                    panDialog.show();
                } else {
                    if (panDialog.isShowing()) {
                        panDialog.cancel();
                    }
                }
            }
        });
    }

    private int configureEmvParameters() {
        try {
            EmvParam emvParam = new EmvParam();
            ConfigManager.refreshConfig(context);
            TerminalConfigModel config = ConfigManager.getConfig(context);

            emvParam.MerchName = config.getMerchantloc().getBytes();
            emvParam.MerchId = config.getMid().getBytes();
            emvParam.TermId = config.getTid().getBytes();
            emvParam.TerminalType = 0x22;
            emvParam.Capability = new byte[]{(byte) 0xE0, (byte) 0x40, (byte) 0xC8};
            emvParam.ExCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01};
            emvParam.CountryCode = new byte[]{(byte) 0x04, (byte) 0x04};
            emvParam.TransType = 0x00;

            emvService.Emv_SetParam(emvParam);
            emvService.Emv_SetOfflinePinCBenable(1);

            Log.d(TAG, "EMV parameters configured successfully");
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "EMV parameter configuration failed", e);
            return -1;
        }
    }

    //Completion of transaction

    private static final SimpleDateFormat DATE_TIME_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private void showPrinterPreviewDialog(String gatewayResponse, EmvModel emvModel, String message) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "Cannot show printer preview - activity not available");
            return;
        }

        // Run on UI thread
        activity.runOnUiThread(() -> {
            try {
                PrinterPreviewDialog previewDialog = new PrinterPreviewDialog(
                        activity,
                        cardModel,
                        emvModel,
                        transactionData,
                        message,
                        new PrinterPreviewDialog.OnPrintClickListener() {
                            @Override
                            public void onPrintClick(String previewContent) {
                                printReceipt(createReceipt(gatewayResponse, emvModel, message));
                            }

                            @Override
                            public void onCancelClick() {
                                Log.d(TAG, "Printing cancelled by user");
                            }
                        }
                );

                previewDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing printer preview dialog", e);
            }
        });
    }


    private void saveReceiptToDatabase(Receipt receipt) {
        try {
            TransactionDatabaseHelper dbHelper = new TransactionDatabaseHelper(context);
            long savedId = dbHelper.saveTransaction(receipt);
            Log.d(TAG, "Receipt saved to database with ID: " + savedId);
        } catch (Exception e) {
            Log.e(TAG, "Error saving receipt to database", e);
        }
    }
    private Receipt createReceipt(String gatewayResponse, EmvModel emvModel, String theMessage) {
        Activity activity = getActivity();
        Receipt receipt = new Receipt();

        if (activity != null) {
            receipt.setBank(TerminalConfig.loadTerminalDataFromJson(activity, "bank"));
            receipt.setMerchant(TerminalConfig.loadTerminalDataFromJson(activity, "merchantloc"));
            receipt.setTerminalId(TerminalConfig.loadTerminalDataFromJson(activity, "tid"));
        }

        receipt.setAmount(transactionData.getAmount());
        receipt.setCurrency("KES");
        receipt.setDateTime(DATE_TIME_FORMATTER.format(new Date()));
        receipt.setTransactionType(transactionData.getTransactionType());
        receipt.setEntryMode("Chip");
        receipt.setAid(emvModel.getDedicatedFileName());
        receipt.setAtc(emvModel.getAtc());
        receipt.setTvr(emvModel.getTerminalVerificationResult());
        receipt.setResponse(theMessage);
        receipt.setTeller(transactionData.getTellerdetail());
        receipt.setCardNumber(cardNum != null ? maskPan(cardNum) : "N/A");
        saveReceiptToDatabase(receipt);
        return receipt;
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "N/A";
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    private void printReceipt(Receipt receipt) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "Cannot print receipt - activity not available");
            return;
        }

        try {
            NewTelpoPrinterService printerService =
                    new NewTelpoPrinterService(context,data -> uiHandler.post(() -> {
                        switch (data) {
                            case 0:
                                //tv_printerinfo.setText("Printer status:Normal");

                                break;
                            case 16:
                                classEmvCallBacks.onLoading("Printer status:No paper");
                                break;
                            default:
                                classEmvCallBacks.onError("Printer status:Error");
                                break;

                        }
                    }));
           // printerService.initializePrinter();
            printerService.printReceipt(receipt);

           // printerService.printReceiptMerchant(receipt);


            Log.i(TAG, "Receipt printed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Printing error: " + e.getMessage(), e);
        }
    }

    private Document parseXmlResponse(String xml, String charset) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void configureApprovedTransaction(EmvOnlineData emvOnlineData, Document document) {
        try {
            String script = getValue(document, "script");
            String st2 = getValue(document, "st2");
            String iad = getValue(document, "iad");
            String rc = getValue(document, "rc");

            String st1 = buildScriptData("71", script);
            st2 = buildScriptData("72", st2);

            emvOnlineData.ScriptData71 = StringUtils.hexStringToByte(st1);
            emvOnlineData.ScriptData72 = StringUtils.hexStringToByte(st2);
            emvOnlineData.IssuAuthenData = StringUtils.hexStringToByte(iad);
            emvOnlineData.AuthenCode = "000000".getBytes(ASCII_CHARSET);
            emvOnlineData.ResponeCode = rc != null ? rc.getBytes(ASCII_CHARSET) : new byte[0];

            Log.d(TAG, "Approved transaction configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring approved transaction", e);
        }
    }

    private String buildScriptData(String prefix, String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        String length = padStart(Integer.toHexString(data.length() / 2), 2, '0');
        return prefix + length + data;
    }


    private static String padStart(String input, int minLength, char padChar) {
        if (input == null) {
            input = "";
        }
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < minLength) {
            sb.insert(0, padChar);
        }
        return sb.toString();
    }
}