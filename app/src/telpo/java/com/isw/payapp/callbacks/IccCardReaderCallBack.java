package com.isw.payapp.callbacks;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.devices.telpo.TelpoEmvService;
import com.isw.payapp.devices.telpo.TelpoPrinter;
import com.isw.payapp.dialog.PrinterPreviewDialog;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.paymentsRequests.KxmlRequest;
import com.isw.payapp.tasks.EmvTLVExtractor;
import com.isw.payapp.tasks.PinPadTasks;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.StringUtils;
import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.pinpad.PinpadService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IccCardReaderCallBack extends EmvServiceListener {
    private static final String TAG = "IccCardReaderCallBack";
    public static final int MAGNETIC_STRIPE = 0;
    public static final int INTEGRATED_CIRCUIT = 1;
    public static final int NFC = 2;

    private static final short CURRENCY_CODE = 404;
    private static final byte CURRENCY_EXPONENT = 2;
    private static final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
    private static final String SUCCESS_RESPONSE_CODE = "00";
    private static final SimpleDateFormat DATE_TIME_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat EMV_DATE_TIME_FORMATTER =
            new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

    private final Context context;
    private final EmvService emvService;
    private EmvModel emvData;
    private final TransactionData transactionData;
    private final int eventType;
    private final EmvServiceCallback emvCallback; // Add this field

    private CardModel cardModel;
    private String kimonoData;
    private String pan;
    private String responseCode;
    private String responseMessage;

    private final AtomicBoolean isUiThreadRunning = new AtomicBoolean(false);
    private CountDownLatch transactionLatch;
    private TelpoEmvService cardReader; // Reference to parent card reader

    public IccCardReaderCallBack(Context context, EmvService emvService,
                                 TransactionData transactionData, int eventType) {
        this.context = context;
        this.emvService = emvService;
        this.transactionData = transactionData;
        this.eventType = eventType;
        this.emvCallback = null; // Will be set via setter
    }

    // Add this constructor to accept EmvServiceCallback
    public IccCardReaderCallBack(Context context, EmvService emvService,
                                 TransactionData transactionData, int eventType,
                                 EmvServiceCallback emvCallback) {
        this.context = context;
        this.emvService = emvService;
        this.transactionData = transactionData;
        this.eventType = eventType;
        this.emvCallback = emvCallback;
    }

    public void setCardReader(TelpoEmvService cardReader) {
        this.cardReader = cardReader;
    }

    public void setTransactionLatch(CountDownLatch transactionLatch) {
        this.transactionLatch = transactionLatch;
    }

    /**
     * Get activity from context if it's an Activity
     */
    private Activity getActivity() {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    /**
     * Helper method to update progress through callback
     */
    private void updateProgress(String message) {
        if (emvCallback != null) {
            emvCallback.onLoading(message);
        } else if (cardReader != null) {
            // Fallback: try to access the callback through card reader
            // This might require adding a getter in TelpoEmvService
            Log.d(TAG, "Progress update: " + message);
        } else {
            Log.d(TAG, "Progress update (no callback): " + message);
        }
    }

    /**
     * Helper method to stop loading progress
     */
    private void stopProgress() {
        if (emvCallback != null) {
            emvCallback.onStopLoading();
        }
    }

    /**
     * Helper method to handle errors
     */
    private void handleError(String error) {
        if (emvCallback != null) {
            emvCallback.onError(error);
        }
    }

    @Override
    public int onInputAmount(EmvAmountData emvAmountData) {
        Log.d(TAG, "onInputAmount called");
        try {
            updateProgress("Setting transaction amount...");

            long amount = convertAmountToMinorUnits(transactionData.getAmount());
            emvAmountData.Amount =  100;//amount;
            emvAmountData.TransCurrCode = CURRENCY_CODE;
            emvAmountData.ReferCurrCode = CURRENCY_CODE;
            emvAmountData.TransCurrExp = CURRENCY_EXPONENT;
            emvAmountData.ReferCurrExp = CURRENCY_EXPONENT;

            Log.d(TAG, "Amount set: " + amount + " minor units");
            return EmvService.EMV_TRUE;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid amount format: " + transactionData.getAmount(), e);
            handleError("Invalid amount format: " + transactionData.getAmount());
            return EmvService.EMV_FALSE;
        }
    }

    private long convertAmountToMinorUnits(String amount) throws NumberFormatException {
        double amountValue = Double.parseDouble(amount);
        if (amountValue < 0) {
            throw new NumberFormatException("Amount cannot be negative");
        }
        return (long) (amountValue * 100);
    }

    @Override
    public int onInputPin(EmvPinData emvPinData) {
        Log.d(TAG, "onInputPin callback type: " + emvPinData.type);

        if (!isUiThreadRunning.compareAndSet(false, true)) {
            Log.w(TAG, "PIN input already in progress");
            return EmvService.EMV_FALSE;
        }

        try {
            updateProgress("Please enter PIN...");
            return processPinInput(emvPinData);
        } catch (Exception e) {
            Log.e(TAG, "PIN input processing failed", e);
            handleError("PIN input failed: " + e.getMessage());
            return EmvService.EMV_FALSE;
        } finally {
            isUiThreadRunning.set(false);
            Log.d(TAG, "onInputPIN callback completed");
        }
    }

    private int processPinInput(EmvPinData emvPinData) {
        try {
            updateProgress("Initializing PIN pad...");

            // Ensure PIN pad is open
            int ret = PinpadService.Open(context);
            if (ret != 0) {
                Log.e(TAG, "Failed to open PIN pad: " + ret);
                handleError("PIN pad initialization failed");
                return EmvService.EMV_FALSE;
            }

            updateProgress("Reading card data...");
            PinPadTasks pinPadTask = new PinPadTasks(context, emvPinData,
                    transactionData.getAmount(), eventType);
            cardModel = pinPadTask.extractCardData();

            if (cardModel == null) {
                Log.e(TAG, "Card model extraction failed");
                handleError("Failed to read card data");
                return EmvService.EMV_FALSE;
            }

            pan = cardModel.getPan();
            Log.d(TAG, "Card data extracted - PAN: " + maskPan(pan) +
                    ", PIN Block: " + cardModel.getPinBlock() + ", KSN: " + cardModel.getKsn());

            updateProgress("Card data read successfully");
            return EmvService.EMV_TRUE;
        } catch (Exception e) {
            Log.e(TAG, "PIN input processing failed", e);
            handleError("Card reading failed: " + e.getMessage());
            return EmvService.EMV_FALSE;
        }
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "N/A";
        }
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    @Override
    public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
        Log.d(TAG, "onSelectApp called with " + emvCandidateApps.length + " candidates");
        updateProgress("Selecting payment application...");

        if (emvCandidateApps.length > 0) {
            Log.d(TAG, "Selected app: " + emvCandidateApps[0].appName);
            updateProgress("Application selected: " + emvCandidateApps[0].appName);
            return emvCandidateApps[0].index;
        }

        handleError("No suitable payment application found");
        return EmvService.EMV_FALSE;
    }

    @Override
    public int onOnlineProcess(EmvOnlineData emvOnlineData) {
        Log.d(TAG, "onOnlineProcess called");
        updateProgress("Processing online transaction...");

        if (eventType != INTEGRATED_CIRCUIT) {
            Log.w(TAG, "Online process skipped - not an ICC transaction");
            handleError("Invalid transaction type");
            return EmvService.ONLINE_FAILED;
        }

        try {
            processOnlineTransactionAsync(emvOnlineData);
            return EmvService.ONLINE_APPROVE;
        } catch (Exception e) {
            Log.e(TAG, "Online processing initialization failed", e);
            handleError("Transaction initialization failed: " + e.getMessage());
            completeTransaction(false, "Online processing failed: " + e.getMessage());
            return EmvService.ONLINE_FAILED;
        }
    }

    private void processOnlineTransactionAsync(EmvOnlineData emvOnlineData) {
        ExecutorService networkExecutor = NetworkExecutor.getExecutor();
        networkExecutor.execute(() -> {
            try {
                updateProgress("Connecting to payment gateway...");
                boolean success = processOnlineTransaction(emvOnlineData);
                handleOnlineTransactionResult(success, emvOnlineData);
            } catch (Exception e) {
                Log.e(TAG, "Online transaction processing failed", e);
                handleError("Transaction processing error: " + e.getMessage());
                completeTransaction(false, "Transaction processing error: " + e.getMessage());
            }
        });
    }

    private boolean processOnlineTransaction(EmvOnlineData emvOnlineData) {
        try {
            Log.d(TAG, "Starting online transaction processing");
            updateProgress("Initializing network...");

            initializeNetworkService();

            updateProgress("Preparing transaction data...");
            String transactionPayload = prepareTransactionPayload();
            if (transactionPayload == null || transactionPayload.isEmpty()) {
                Log.e(TAG, "Empty or null transaction payload");
                handleError("Failed to prepare transaction data");
                return false;
            }

            Log.d(TAG, "Transaction payload prepared, length: " + transactionPayload.length());

            updateProgress("Sending transaction to gateway...");
            String gatewayResponse = sendTransactionToGateway(transactionPayload);
            if (gatewayResponse == null || gatewayResponse.isEmpty()) {
                Log.e(TAG, "Empty gateway response");
                handleError("No response from payment gateway");
                return false;
            }

            Log.d(TAG, "Gateway response received, length: " + gatewayResponse.length());
            updateProgress("Processing gateway response...");
            return processGatewayResponse(gatewayResponse, emvOnlineData);

        } catch (Exception e) {
            Log.e(TAG, "Online transaction processing error", e);
            handleError("Transaction processing error: " + e.getMessage());
            return false;
        }
    }

    private void initializeNetworkService() {
        try {
            String baseUrl = buildBaseUrl();
            NetworkService.initialize(context, baseUrl);
            Log.d(TAG, "Network service initialized with URL: " + baseUrl);
        } catch (Exception e) {
            Log.e(TAG, "Network service initialization failed", e);
            throw new RuntimeException("Failed to initialize network service", e);
        }
    }

    private String buildBaseUrl() {
        String ip = TerminalConfig.loadTerminalDataFromJson(context, "__transip");
        String port = TerminalConfig.loadTerminalDataFromJson(context, "__transport");

        if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
            throw new IllegalArgumentException("Invalid terminal configuration: IP or Port is missing");
        }

        return "https://" + ip + ":" + port + "/";
    }

    private String prepareTransactionPayload() {
        try {
            updateProgress("Extracting EMV data...");
            EmvTLVExtractor emvTLVExtractor = new EmvTLVExtractor(emvService, transactionData);
            emvData = emvTLVExtractor.extractEmvData();

            String paymentApp = transactionData.getPaymentApp();
            Log.d(TAG, "Preparing payload for payment app: " + paymentApp);

            if ("selectpin".equals(paymentApp)) {
                updateProgress("Preparing PIN change request...");
                return preparePinChangePayload(emvData);
            } else {
                updateProgress("Preparing payment request...");
                return preparePurchasePayload(emvData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing transaction payload", e);
            handleError("Failed to prepare transaction: " + e.getMessage());
            return null;
        }
    }

    private String preparePinChangePayload(EmvModel emvData) {
        try {
            KsmgRequest pinChangeRequest = new KsmgRequest(emvData, transactionData, cardModel);
            return pinChangeRequest.Payload();
        } catch (Exception e) {
            Log.e(TAG, "Error preparing PIN change payload", e);
            handleError("PIN change preparation failed");
            return null;
        }
    }

    private String preparePurchasePayload(EmvModel emvData) {
        try {
            KxmlRequest purchaseRequest = new KxmlRequest(emvData, transactionData, cardModel);
            return purchaseRequest.Payload();
        } catch (Exception e) {
            Log.e(TAG, "Error preparing purchase payload", e);
            handleError("Payment preparation failed");
            return null;
        }
    }

    private String sendTransactionToGateway(String transactionPayload) {
        try {
            NetworkService networkService = NetworkService.getInstance();
            if (networkService == null) {
                Log.e(TAG, "Network service not initialized");
                handleError("Network service unavailable");
                return null;
            }

            Log.d(TAG, "Sending transaction to gateway");
            String response = networkService.postPayLoadSync(transactionPayload);
            Log.d(TAG, "Gateway response received successfully");
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error sending transaction to gateway", e);
            handleError("Network communication failed");
            return null;
        }
    }

    private boolean processGatewayResponse(String gatewayResponse, EmvOnlineData emvOnlineData) {
        try {
            this.kimonoData = gatewayResponse;
            Log.d(TAG, "Processing gateway response");

            Document document = parseXmlResponse(gatewayResponse);
            if (document == null) {
                Log.e(TAG, "Failed to parse gateway response XML");
                handleError("Invalid gateway response format");
                completeTransaction(false, "Invalid gateway response");
                return false;
            }

            String responseCode = getAttributeValue(document, "var", "name", "responsecode");
            String responseMessage = getAttributeValue(document, "var", "name", "responsemessage");

            Log.d(TAG, "Gateway response - Code: " + responseCode + ", Message: " + responseMessage);

            if (SUCCESS_RESPONSE_CODE.equals(responseCode)) {
                updateProgress("Transaction approved");
                configureApprovedTransaction(emvOnlineData, document);
                //
                showPrinterPreviewDialog(gatewayResponse, emvData, "Transaction approved");
                completeTransaction(true, gatewayResponse);
                return true;
            } else {
                updateProgress("Transaction declined");
                showPrinterPreviewDialog(gatewayResponse, emvData, "Transaction declined");
                Log.w(TAG, "Transaction declined - Response code: " + responseCode);
                completeTransaction(false, "Transaction declined: " + responseMessage);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing gateway response", e);
            handleError("Response processing error");
            completeTransaction(false, "Gateway response processing error");
            return false;
        }
    }

    private void handleOnlineTransactionResult(boolean success, EmvOnlineData emvOnlineData) {
        if (success) {
            Log.i(TAG, "Online transaction processed successfully");
            stopProgress();
        } else {
            Log.w(TAG, "Online transaction processing failed");
            stopProgress();
        }
    }

    private void completeTransaction(boolean success, String responseData) {
        Log.d(TAG, "Completing transaction - Success: " + success);
        stopProgress();

        // Notify parent card reader
        if (cardReader != null) {
            cardReader.onTransactionCompleted(success, responseData);
        }

        // Count down the latch if it exists
        if (transactionLatch != null && transactionLatch.getCount() > 0) {
            transactionLatch.countDown();
        }
    }

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
                                updateProgress("Printing receipt...");
                                printReceipt(createReceipt(gatewayResponse, emvModel, message));
                                setKimonoData("00");
                                completeTransaction(true, "Print Successful");
                                stopProgress();
                            }

                            @Override
                            public void onCancelClick() {
                                Log.d(TAG, "Printing cancelled by user");
                                setKimonoData("01");
                                completeTransaction(true, "Canceled Printing");
                                stopProgress();
                            }
                        }
                );

                previewDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing printer preview dialog", e);
                stopProgress();
            }
        });
    }

    private Receipt createReceipt(String gatewayResponse, EmvModel emvModel, String theMessage) {
        Activity activity = getActivity();
        Receipt receipt = new Receipt();

        if (activity != null) {
            receipt.setBank(TerminalConfig.loadTerminalDataFromJson(activity, "__bank"));
            receipt.setMerchant(TerminalConfig.loadTerminalDataFromJson(activity, "__merchantloc"));
            receipt.setTerminalId(TerminalConfig.loadTerminalDataFromJson(activity, "__tid"));
        }
        if(transactionData.getPaymentApp().equals("selectpin")){
            receipt.setAmount("0.00");
        }else {
            receipt.setAmount(transactionData.getAmount());
        }
        receipt.setCurrency("KES");
        receipt.setDateTime(DATE_TIME_FORMATTER.format(new Date()));
        receipt.setTransactionType(transactionData.getTransactionType());
        receipt.setEntryMode("Chip");
        receipt.setAid(emvModel.getDedicatedFileName());
        receipt.setAtc(emvModel.getAtc());
        receipt.setTvr(emvModel.getTerminalVerificationResult());
        receipt.setResponse(theMessage);
        receipt.setCardNumber(pan != null ? maskPan(pan) : "N/A");

        return receipt;
    }

    private void printReceipt(Receipt receipt) {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "Cannot print receipt - activity not available");
            return;
        }

        try {
            TelpoPrinter printerService = new TelpoPrinter(context);
            printerService.initializePrinter();
            printerService.printReceipt(receipt);
            Log.i(TAG, "Receipt printed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Printing error: " + e.getMessage(), e);
            handleError("Printing failed: " + e.getMessage());
        }
    }

    private Document parseXmlResponse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void configureApprovedTransaction(EmvOnlineData emvOnlineData, Document document) {
        try {
            updateProgress("Finalizing transaction...");

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
            handleError("Transaction finalization failed");
        }
    }

    private String buildScriptData(String prefix, String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        String length = padStart(Integer.toHexString(data.length() / 2), 2, '0');
        return prefix + length + data;
    }

    @Override
    public int onRequireDatetime(byte[] datetime) {
        try {
            updateProgress("Setting transaction time...");
            String timestamp = EMV_DATE_TIME_FORMATTER.format(new Date());
            byte[] timeBytes = timestamp.getBytes(ASCII_CHARSET);
            System.arraycopy(timeBytes, 0, datetime, 0, Math.min(timeBytes.length, datetime.length));
            return EmvService.EMV_TRUE;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set datetime", e);
            return EmvService.EMV_FALSE;
        }
    }

    // Other required overrides with proper logging and progress updates
    @Override
    public int onSelectAppFail(int reason) {
        Log.d(TAG, "onSelectAppFail: " + reason);
        handleError("Application selection failed");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onFinishReadAppData() {
        Log.d(TAG, "onFinishReadAppData");
        updateProgress("Application data read successfully");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onVerifyCert() {
        Log.d(TAG, "onVerifyCert");
        updateProgress("Verifying certificate...");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onRequireTagValue(int tag, int source, byte[] value) {
        Log.d(TAG, "onRequireTagValue - tag: " + tag + ", source: " + source);
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onReferProc() {
        Log.d(TAG, "onReferProc");
        updateProgress("Processing referral...");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int OnCheckException(String data) {
        Log.d(TAG, "OnCheckException: " + data);
        //handleError("Transaction exception: " + data.substring(0,6)+"**"+data.substring(data.length()-4));
        return EmvService.EMV_TRUE;
    }

    @Override
    public int OnCheckException_qvsdc(int type, String data) {
        Log.d(TAG, "OnCheckException_qvsdc - type: " + type + ", data: " + data);
        //handleError("Transaction exception: " + data);
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onMir_FinishReadAppData() {
        Log.d(TAG, "onMir_FinishReadAppData");
        updateProgress("Mir application data read");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onMir_DataExchange() {
        Log.d(TAG, "onMir_DataExchange");
        updateProgress("Exchanging data with card...");
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onMir_Hint() {
        Log.d(TAG, "onMir_Hint");
        return EmvService.EMV_TRUE;
    }

    public void preProcessDataRequest() {
        try {
            updateProgress("Preparing transaction request...");
            EmvTLVExtractor extractor = new EmvTLVExtractor(emvService, transactionData);
            KxmlRequest purchaseData = new KxmlRequest(extractor.extractEmvData(), transactionData, cardModel);
            kimonoData = purchaseData.Payload();
            transactionData.setKimonoData(kimonoData);
            Log.d(TAG, "Transaction request data prepared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Pre-process data request failed", e);
            handleError("Transaction preparation failed");
        }
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