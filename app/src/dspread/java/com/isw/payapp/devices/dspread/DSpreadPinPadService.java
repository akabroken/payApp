package com.isw.payapp.devices.dspread;

import static com.isw.payapp.devices.dspread.utils.Utils.getKeyIndex;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.MPOSService;
import com.dspread.xpos.QPOSService;

import com.isw.payapp.devices.dspread.Activity.DeviceApplication;
import com.isw.payapp.devices.dspread.serviceListeners.KeyInjection;
import com.isw.payapp.devices.dspread.utils.DUKPK2009_CBC;
import com.isw.payapp.devices.dspread.utils.DukptKeys;
import com.isw.payapp.devices.dspread.utils.Envelope;
import com.isw.payapp.devices.dspread.utils.FileUtils;
import com.isw.payapp.devices.dspread.utils.Poskeys;
import com.isw.payapp.devices.dspread.utils.Session;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.utils.ThreeDES;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DSpreadPinPadService implements IPinPadProcessor {
    private static final String TAG = "DSpreadPINPadService";
    private static final int OPERATION_TIMEOUT_SECONDS = 30;
    private static final int KEY_LENGTH_HEX = 32; // 16 bytes for double-length 3DES

    private final Context context;
    private static QPOSService pos = DeviceApplication.getPos();
    private final QPOSCallback callback;

    // Thread-safe operation state
    private static CountDownLatch operationLatch;
    private static final AtomicBoolean lastOperationSuccess = new AtomicBoolean(false);

    // Key state management
    private static class KeyState {
        static volatile boolean isTekLoaded = false;
        static volatile boolean isTmkLoaded = false;
        static volatile boolean isTpkLoaded = false;
        static volatile boolean isDukptLoaded = false;
        static volatile boolean remoteKeyResp = false;
        static volatile boolean isReset = false;
    }

    private static KeyInjection keyInjection;
    private String newTMK = null;
    private static String prevKey = null;

    public DSpreadPinPadService(Context context) {
        this.context = context.getApplicationContext();
        this.callback = new QPOSCallback();
        this.pos = QPOSService.getInstance(this.context, QPOSService.CommunicationMode.UART);
        this.pos.initListener(callback);
    }

    @Override
    public void initPinPad() {
        Log.i(TAG, "Initializing PIN Pad Service");
        try {
            pos.openUart();
            pos.setFormatId(QPOSService.FORMATID.DUKPT);
            Log.i(TAG, "PIN Pad initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize PIN Pad", e);
            throw new RuntimeException("PIN Pad initialization failed", e);
        }
    }

    @Override
    public int injectDukptKey(String key, String ksn, String kcv) {
        Log.i(TAG, "Injecting DUKPT key: KSN=" + ksn);
        Log.i(TAG, "Injecting DUKPT clear full key:=" + key);
        key = key.substring(8,40);
        Log.i(TAG, "Injecting DUKPT clear key:=" + key);
        try {
            validateInputParameters(key, ksn);
            resetOperationState();

            return executeDukptInjection(key, ksn);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid input parameters", e);
            return ErrorCode.INVALID_PARAMETERS;
        } catch (InterruptedException e) {
            Log.e(TAG, "DUKPT injection interrupted", e);
            Thread.currentThread().interrupt();
            return ErrorCode.OPERATION_INTERRUPTED;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during DUKPT injection", e);
            return ErrorCode.UNEXPECTED_ERROR;
        }
    }

    private void validateInputParameters(String key, String ksn) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (key.length() != KEY_LENGTH_HEX) {
            throw new IllegalArgumentException("Key must be " + KEY_LENGTH_HEX + " hex characters");
        }
        if (ksn == null || ksn.trim().isEmpty()) {
            throw new IllegalArgumentException("KSN cannot be null or empty");
        }
    }

    private int executeDukptInjection(String key, String ksn) throws InterruptedException {
        injectDukptKeys(key, ksn);

        if (!operationLatch.await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            Log.e(TAG, "DUKPT injection timeout");
            return ErrorCode.OPERATION_TIMEOUT;
        }

        if (!lastOperationSuccess.get()) {
            Log.e(TAG, "DUKPT injection failed");
            return ErrorCode.OPERATION_FAILED;
        }

        loadEmvConfigs();
        Log.i(TAG, "DUKPT key injection completed successfully");
        return ErrorCode.SUCCESS;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void injectDukptKeys(String key, String ksn) {
        try {
            String defaultMasterKey =key;//key ;//"D8DE53632DE273D3EF3D2AA35253F2DC";//"0123456789ABCDEFFEDCBA9876543210";

            // Generate IPEK and BDK for debugging
            byte[] ipekBytes = DUKPK2009_CBC.GenerateIPEK(
                    ThreeDES.hexStringToByteArray(ksn),
                    ThreeDES.hexStringToByteArray(key)
            );
            String ipek = ThreeDES.byteArrayToHexString(ipekBytes).toLowerCase();
            TRACE.i("IPEK: " + ipek);

            byte[] bdkBytes = DUKPK2009_CBC.GetDUKPTKey(
                    ThreeDES.hexStringToByteArray(ksn),
                    ThreeDES.hexStringToByteArray(key)
            );
            String bdk = ThreeDES.byteArrayToHexString(bdkBytes);
            TRACE.i("BDK: " + bdk);

            // Load configuration
            String configPem = loadConfiguration();

            // Create key sets for different encryption types
            DukptKeySet trackKeys = createDukptKeySet(ipek, ksn, defaultMasterKey, "Track");
            DukptKeySet emvKeys = createDukptKeySet(ipek, ksn, defaultMasterKey, "EMV");
            DukptKeySet pinKeys = createDukptKeySet(ipek, ksn, defaultMasterKey, "PIN");

            logKeyDetails(trackKeys, emvKeys, pinKeys);

            // Initialize POS service connection
            QPOSService posService = getPosService();
            connectService(posService, false);

        } catch (Exception e) {
            Log.e(TAG, "Error in DUKPT injection process", e);
            completeOperation(false);
        }
    }

    private String loadConfiguration() throws Exception {
        return new String( FileUtils.readAssetsLine("debug_certificate.pem", context))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "").trim();
    }

    private QPOSService getPosService() {
        return pos != null ? pos : DeviceApplication.getPos();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void connectService(QPOSService pos, boolean automaticDownload) {
        try {
            KeyState.isReset = true;
            QPOSService posService = pos != null ? pos : DeviceApplication.getPos();

            if (posService != null) {
                Log.w("UPDATE TMK", "Getting device TMK check value");
                posService.getKeyCheckValue(getKeyIndex(), QPOSService.CHECKVALUE_KEYTYPE.MKSK_TMK);
            } else {
                Log.w("connectService", "POS service is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error connecting service", e);
        }
    }

    private DukptKeySet createDukptKeySet(String key, String ksn, String masterKey, String keyType) {
        validateKeySetParameters(key, ksn, masterKey, keyType);

        try {
            String kcv = ThreeDES.generateKeyCheckValue(key, ThreeDES.KcvMethod.ANSI);
            String encryptedIpek = ThreeDES.encryptECB(masterKey, key);

            if (encryptedIpek == null || encryptedIpek.length() != KEY_LENGTH_HEX) {
                encryptedIpek = ThreeDES.encryptECBNoPadding(masterKey, key);
                validateEncryptedIpekLength(encryptedIpek);
            }

            logKeyCreationDetails(keyType, kcv, encryptedIpek, key, masterKey);
            return new DukptKeySet(ksn, encryptedIpek, kcv);

        } catch (Exception e) {
            Log.e(TAG, String.format("Error creating DUKPT key set for %s", keyType), e);
            throw new RuntimeException("Failed to create DUKPT key set", e);
        }
    }

    private void validateKeySetParameters(String key, String ksn, String masterKey, String keyType) {
        if (key == null || ksn == null || masterKey == null || keyType == null) {
            throw new IllegalArgumentException("All DUKPT key set parameters must be non-null");
        }
        if (key.length() != KEY_LENGTH_HEX) {
            throw new IllegalArgumentException("Key must be " + KEY_LENGTH_HEX + " hex characters");
        }
    }

    private void validateEncryptedIpekLength(String encryptedIpek) {
        if (encryptedIpek.length() != KEY_LENGTH_HEX) {
            throw new IllegalStateException(
                    String.format("Encrypted IPEK must be %d hex characters, got: %d",
                            KEY_LENGTH_HEX, encryptedIpek.length()));
        }
    }

    private void logKeyCreationDetails(String keyType, String kcv, String encryptedIpek, String key, String masterKey) {
        Log.i(TAG, String.format("%s IPEK KCV: %s", keyType, kcv));
        Log.i(TAG, String.format("Encrypted %s IPEK: %s (length: %d)", keyType, encryptedIpek, encryptedIpek.length()));
        Log.i(TAG, String.format("Key Type: %s", ThreeDES.getKeyType(key)));
        Log.i(TAG, String.format("Master Key Type: %s", ThreeDES.getKeyType(masterKey)));
    }

    private void logKeyDetails(DukptKeySet... keySets) {
        for (DukptKeySet keySet : keySets) {
            Log.d(TAG, "KeySet - KSN: " + keySet.ksn + ", KCV: " + keySet.kcv);
        }
    }

    private void resetOperationState() {
        operationLatch = new CountDownLatch(1);
        lastOperationSuccess.set(false);
    }

    private static void completeOperation(boolean success) {
        lastOperationSuccess.set(success);
        if (operationLatch != null) {
            operationLatch.countDown();
        }
    }

    @Override
    public int resetKey() {
        Log.i(TAG, "Resetting PIN Pad keys");
        try {
            pos.resetQPOS();
            return ErrorCode.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset keys", e);
            return ErrorCode.OPERATION_FAILED;
        }
    }

    @Override
    public int deleteKeys() {
        Log.i(TAG, "Deleting all keys");
        try {
            pos.resetQPOS();
            return ErrorCode.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete keys", e);
            return ErrorCode.OPERATION_FAILED;
        }
    }

    @Override
    public void deleteKey() {
        Log.w(TAG, "Single key deletion not implemented - use deleteKeys() instead");
    }

    @Override
    public void deviceClose() {
        Log.i(TAG, "Closing device connection");
        try {
            if (pos != null) {
                pos.closeUart();
                Log.i(TAG, "Device connection closed successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing device", e);
        }
    }

    private void loadEmvConfigs() {
        try {
            String configContent = new String(FileUtils.readAssetsLine("emv_profile_tlv.xml", context));
            pos.updateEMVConfigByXml(configContent);
            Log.i(TAG, "EMV configurations loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load EMV configurations", e);
        }
    }

    /**
     * Simplified callback handler focusing on essential operations
     */
    public static class QPOSCallback implements QPOSService.QPOSServiceListener {
        private static final String DEFAULT_KCV = "08D7B4";
        private String pubModel = "";

        @Override
        public void onReturnUpdateIPEKResult(boolean success) {
            Log.i(TAG, "IPEK update result: " + success);
            completeOperation(success);
        }

        @Override
        public void onReturnRSAResult(String s) {

        }

        @Override
        public void onReturnUpdateEMVResult(boolean b) {

        }

        @Override
        public void onReturnUpdateEMVResult(boolean b, List<String> list) {

        }

        @Override
        public void onReturnGetQuickEmvResult(boolean b) {

        }

        @Override
        public void onReturnGetEMVListResult(String s) {

        }

        @Override
        public void onReturnGetCustomEMVListResult(Map<String, String> map) {

        }

        @Override
        public void onReturnUpdateEMVRIDResult(boolean b) {

        }

        @Override
        public void onDeviceFound(BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> linkedHashMap) {

        }

        @Override
        public void onBluetoothBonding() {

        }

        @Override
        public void onBluetoothBonded() {

        }

        @Override
        public void onWaitingforData(String s) {

        }

        @Override
        public void onBluetoothBondFailed() {

        }

        @Override
        public void onBluetoothBondTimeout() {

        }

        @Override
        public void onReturniccCashBack(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onLcdShowCustomDisplay(boolean b) {

        }

        @Override
        public void onSetCustomLogoDisplay(boolean b) {

        }

        @Override
        public void onUpdatePosFirmwareResult(QPOSService.UpdateInformationResult updateInformationResult) {

        }

        @Override
        public void onReturnPosFirmwareUpdateProgressResult(int i) {

        }

        @Override
        public void onBluetoothBoardStateResult(boolean b) {

        }

        @Override
        public void onReturnDownloadRsaPublicKey(HashMap<String, String> hashMap) {

        }

        @Override
        public void onGetPosComm(int i, String s, String s1) {

        }

        @Override
        public void onUpdateMasterKeyResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onPinKey_TDES_Result(String s) {

        }

        @Override
        public void onEmvICCExceptionData(String s) {

        }

        @Override
        public void onSetParamsResult(boolean b, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onSetVendorIDResult(boolean b, Hashtable<String, Object> hashtable) {

        }

        @Override
        public void onGetInputAmountResult(boolean b, String s) {

        }

        @Override
        public void onReturnNFCApduResult(boolean b, String s, int i) {

        }

        @Override
        public void onReturnMPUCardInfo(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPowerOnNFCResult(boolean b, String s, String s1, int i) {

        }

        @Override
        public void onReturnPowerOnNFCResult(boolean b, String s, int i) {

        }

        @Override
        public void onReturnPowerOnNFCResult(boolean b, QPOSService.CardsType cardsType, String s, int i) {

        }

        @Override
        public void onReturnPowerOnCardResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPowerOffCardResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnSearchCardResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnReadCardResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnCheckCardResult(boolean b, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPowerOffNFCResult(boolean b) {

        }

        @Override
        public void onCbcMacResult(String s) {

        }

        @Override
        public void onReadBusinessCardResult(boolean b, String s) {

        }

        @Override
        public void onReadGasCardResult(boolean b, String s) {

        }

        @Override
        public void onWriteBusinessCardResult(boolean b) {

        }

        @Override
        public void onWriteGasCardResult(boolean b) {

        }

        @Override
        public void onConfirmAmountResult(boolean b) {

        }

        @Override
        public void onSetManagementKey(boolean b) {

        }

        @Override
        public void onSetSleepModeTime(boolean b) {

        }

        @Override
        public void onGetSleepModeTime(String s) {

        }

        @Override
        public void onGetShutDownTime(String s) {

        }

        @Override
        public void onEncryptData(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onAddKey(boolean b) {

        }

        @Override
        public void onSetBuzzerResult(boolean b) {

        }

        @Override
        public void onSetBuzzerTimeResult(boolean b) {

        }

        @Override
        public void onSetBuzzerStatusResult(boolean b) {

        }

        @Override
        public void onGetBuzzerStatusResult(String s) {

        }

        @Override
        public void onReturnPlayBuzzerByTypeResult(boolean b) {

        }

        @Override
        public void onReturnOperateLEDByTypeResult(boolean b) {

        }

        @Override
        public void onQposDoTradeLog(boolean b) {

        }

        @Override
        public void onQposDoGetTradeLogNum(String s) {

        }

        @Override
        public void onQposDoGetTradeLog(String s, String s1) {

        }

        @Override
        public void onRequestDevice() {

        }

        @Override
        public void onQposIdResult(Hashtable<String, String> hashtable) {
            TRACE.w("onQposIdResult(): " + hashtable.toString());

            String posId = getSafeValue(hashtable, "posId");
            String csn = getSafeValue(hashtable, "csn");
            String psamId = getSafeValue(hashtable, "psamId");
            String nfcId = getSafeValue(hashtable, "nfcID");

            Log.w("posId", "posId == " + posId);
            DeviceApplication.mposDeviceId = posId;

            Log.d("Key inject", "Starting key injection...");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectService(DeviceApplication.getPos(), false);
            }
            Log.d("Key inject", "Key injection completed");
        }

        @Override
        public void onQposKsnResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onQposIsCardExist(boolean b) {

        }

        @Override
        public void onRequestDeviceScanFinished() {

        }

        @Override
        public void onQposInfoResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onQposUpdateDataWithKeyResult(boolean b) {

        }

        @Override
        public void onQposGetDeviceECCPublicKeyResult(String s) {

        }

        @Override
        public void onQposUpdateServerECCPublicKeyResult(boolean b) {

        }

        @Override
        public void onQposTestResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onQposCertificateInfoResult(List<String> list) {

        }

        @Override
        public void onQposGenerateSessionKeysResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onQposDoSetRsaPublicKey(boolean b) {

        }

        @Override
        public void onSearchMifareCardResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onBatchReadMifareCardResult(String s, Hashtable<String, List<String>> hashtable) {

        }

        @Override
        public void onBatchWriteMifareCardResult(String s, Hashtable<String, List<String>> hashtable) {

        }

        @Override
        public void onDoTradeResult(QPOSService.DoTradeResult doTradeResult, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onFinishMifareCardResult(boolean b) {

        }

        @Override
        public void onVerifyMifareCardResult(boolean b) {

        }

        @Override
        public void onReadMifareCardResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onWriteMifareCardResult(boolean b) {

        }

        @Override
        public void onOperateMifareCardResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void getMifareCardVersion(Hashtable<String, String> hashtable) {

        }

        @Override
        public void getMifareReadData(Hashtable<String, String> hashtable) {

        }

        @Override
        public void getMifareFastReadData(Hashtable<String, String> hashtable) {

        }

        @Override
        public void writeMifareULData(String s) {

        }

        @Override
        public void verifyMifareULData(Hashtable<String, String> hashtable) {

        }

        @Override
        public void transferMifareData(String s) {

        }

        @Override
        public void onRequestSetAmount() {

        }

        @Override
        public void onRequestSelectEmvApp(ArrayList<String> arrayList) {

        }

        @Override
        public void onRequestIsServerConnected() {

        }

        @Override
        public void onRequestFinalConfirm() {

        }

        @Override
        public void onRequestOnlineProcess(String s) {

        }

        @Override
        public void onRequestTime() {

        }

        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {

        }

        @Override
        public void onRequestTransactionLog(String s) {

        }

        @Override
        public void onRequestBatchData(String s) {

        }

        private String getSafeValue(Hashtable<String, String> hashtable, String key) {
            return hashtable.get(key) != null ? hashtable.get(key) : "";
        }

        @Override
        public void onGetKeyCheckValue(Hashtable<String, String> checkValue) {
            if (!DeviceApplication.isKeyInjectionSuccessful) {
                Log.w("onGetKeyCheckValue", "Processing key check value");
                String mkskTmkKcv = checkValue.get("MKSK_TMK_KCV");
                System.out.println("checkValue: " + mkskTmkKcv);

                try {
                    handleKeyCheckValue(mkskTmkKcv);
                } catch (Exception e) {
                    prevKey = null;
                    Log.e(TAG, "Error processing key check value", e);
                }
            } else {
                TRACE.i("Key injection already successful");
                if (pos != null) {
                    pos.getDevicePublicKey(30);
                }
            }
        }

        private void handleKeyCheckValue(String mkskTmkKcv) {
            if (mkskTmkKcv == null) {
                prevKey = "NO_PREVIOUS_KEY";
            } else if (DEFAULT_KCV.equals(mkskTmkKcv)) {
                handleDefaultKey(mkskTmkKcv);
            } else {
                handleNonDefaultKey();
            }
        }

        private void handleDefaultKey(String mkskTmkKcv) {
            System.out.println("Device Has Default Key");
            TRACE.i("Default TMK KCV found: " + mkskTmkKcv);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String tmk = "5ADCAEF2E33CCDCC605934C77E06073C";//"5ADCAEF2E33CCDCC605934C77E06073C";
                String kvc = "C0B7927925177313";//"C0B7927925177313";
                KeyState.isTmkLoaded = keyInjection.loadD60MasterKey(
                        DeviceApplication.getPos(), tmk, kvc);
            } else {
                TRACE.i("Unsupported SDK Version");
            }
        }

        private void handleNonDefaultKey() {
            System.out.println("Device Does Not Have Default Key");
            TRACE.i("getDevicePublicKey:1 ---");
            if (pos != null) {
                pos.getDevicePublicKey(30);
            }
        }

        @Override
        public void onGetDevicePubKey(Hashtable<String, String> hashtable) {
            Log.w("GetPublicKeyclearKeys", hashtable.toString());
            TRACE.i("onGetDevicePubKey(clearKeys):" + hashtable);

            pubModel = hashtable.get("modulus");
            if (pubModel == null) {
                Log.e(TAG, "Public key modulus is null");
                return;
            }

            processDigitalEnvelope();
        }

        @Override
        public void onReturnUpdateCAPKResult(boolean b, List<String> list) {

        }

        @Override
        public void onSetPosBluConfig(boolean b) {

        }

        @Override
        public void onTradeCancelled() {

        }

        @Override
        public void onReturnSetAESResult(boolean b, String s) {

        }

        @Override
        public void onReturnAESTransmissonKeyResult(boolean b, String s) {

        }

        @Override
        public void onReturnSignature(boolean b, String s) {

        }

        @Override
        public void onReturnConverEncryptedBlockFormat(String s) {

        }

        @Override
        public void onQposIsCardExistInOnlineProcess(boolean b) {

        }

        @Override
        public void onReturnSetConnectedShutDownTimeResult(boolean b) {

        }

        @Override
        public void onReturnGetConnectedShutDownTimeResult(String s) {

        }

        @Override
        public void onRequestNFCBatchData(QPOSService.TransactionResult transactionResult, String s) {

        }

        @Override
        public void onReturnUpdateKeyByTR_31Result(boolean b) {

        }

        @Override
        public void onRequestGenerateTransportKey(Hashtable hashtable) {

        }

        @Override
        public void onReturnAnalyseDigEnvelop(String s) {

        }

        @Override
        public void onReturnDisplayQRCodeResult(boolean b) {

        }

        @Override
        public void onReturnDeviceCSRResult(String s) {

        }

        @Override
        public void onReturnStoreCertificatesResult(boolean b) {

        }

        @Override
        public void onReturnSignatureAndCertificatesResult(String s, String s1, String s2) {

        }

        @Override
        public void onReturnDeviceSigningCertResult(String s, String s1) {

        }

        @Override
        public void onReturnServerCertResult(String s, String s1) {

        }

        @Override
        public void onQposSetLEDColorResult(boolean b) {

        }

        @Override
        public void onQposGetLEDColorResult(String s) {

        }

        @Override
        public void onReturnDeviceCertAndSignatureResult(String s, String s1) {

        }

        private void processDigitalEnvelope() {
            int keyIndex = getKeyIndex();
            String digEnvelopStr = null;

            try {
                Log.d(TAG, "=== Starting digital envelope process ===");

                // FIX: Use the corrected static method
                if (!DeviceApplication.isApplicationReady()) {
                    Log.e(TAG, "❌ Application is not ready yet");
                    Session.setIsKeyReset(false);
                    return;
                }

                // FIX: Use the proper context getter
                Context context = DeviceApplication.getApplicationContextStatic();
                if (context == null) {
                    Log.e(TAG, "❌ Application context is null");
                    Session.setIsKeyReset(false);
                    return;
                }

                AssetManager assetManager = context.getAssets();
                if (assetManager == null) {
                    Log.e(TAG, "❌ AssetManager is null");
                    Session.setIsKeyReset(false);
                    return;
                }
                Log.d(TAG, "✅ Application context and AssetManager: OK");

                // Continue with your existing code...
                Poskeys posKeys = new DukptKeys();

                if (pubModel == null) {
                    Log.e(TAG, "❌ pubModel is null");
                    Session.setIsKeyReset(false);
                    return;
                }
                posKeys.setRSA_public_key(pubModel);

                String[] files = assetManager.list("");
                boolean fileExists = false;
                for (String file : files) {
                    if (file.equals("rsa_private_pkcs8_1024.pem")) {
                        fileExists = true;
                        break;
                    }
                }

                if (!fileExists) {
                    Log.e(TAG, "❌ PEM file not found in assets");
                    Session.setIsKeyReset(false);
                    return;
                }

                InputStream pemFile = assetManager.open("rsa_private_pkcs8_1024.pem");
                if (pemFile == null) {
                    Log.e(TAG, "❌ Failed to open PEM file");
                    Session.setIsKeyReset(false);
                    return;
                }

                digEnvelopStr = Envelope.getDigitalEnvelopStrByKey(
                        pemFile,
                        posKeys,
                        Poskeys.RSA_KEY_LEN.RSA_KEY_1024,
                        keyIndex
                );

                Log.d(TAG, "Digital envelope result: " + digEnvelopStr);

                if (digEnvelopStr == null) {
                    Log.e(TAG, "❌ Envelope method returned null");
                    Session.setIsKeyReset(false);
                    return;
                }

                Session.setIsKeyReset(true);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in processDigitalEnvelope", e);
                Session.setIsKeyReset(false);
                return;
            }

            updateWorkKey(digEnvelopStr);
        }
        private void updateWorkKey(String digEnvelopStr) {
            TRACE.i("onGetDevicePubKey(clearKeys):updateWorkKey");
            QPOSService posService = DeviceApplication.getPos();

            if (posService != null && digEnvelopStr != null) {
                posService.updateWorkKey(digEnvelopStr);
            } else {
                Log.e(TAG, "Cannot update work key - POS service or digital envelope is null");
            }
        }

        // Essential callback implementations
        @Override
        public void onReturnSetMasterKeyResult(boolean success) {
            Log.d(TAG, "Master key set result: " + success);
        }

        @Override
        public void onReturnSetMasterKeyResult(boolean success, Hashtable<String, String> details) {
            Log.d(TAG, "Master key set result: " + success);
        }

        @Override
        public void onRequestUpdateKey(String s) {

        }

        @Override
        public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult result) {
            Log.i(TAG, "Work key update result: " + result);
        }

        @Override
        public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult updateInformationResult, Hashtable<String, String> hashtable) {

        }

        @Override
        public void onRequestSendTR31KeyResult(boolean b) {

        }

        @Override
        public void onReturnCustomConfigResult(boolean b, String s) {

        }

        @Override
        public void onReturnDoInputCustomStr(boolean b, String s, String s1) {

        }

        @Override
        public void onRetuenGetTR31Token(String s) {

        }

        @Override
        public void onRequestSetPin() {

        }

        @Override
        public void onRequestSetPin(boolean b, int i) {

        }

        @Override
        public void onRequestQposConnected() {
            Log.i(TAG, "QPOS device connected");
        }

        @Override
        public void onRequestQposDisconnected() {
            Log.w(TAG, "QPOS device disconnected");
        }

        @Override
        public void onRequestNoQposDetected() {

        }

        @Override
        public void onRequestNoQposDetectedUnbond() {

        }

        @Override
        public void onError(QPOSService.Error error) {
            Log.e(TAG, "PIN Pad error: " + error);
            completeOperation(false);
        }

        @Override
        public void onRequestDisplay(QPOSService.Display display) {

        }

        @Override
        public void onReturnReversalData(String s) {

        }

        @Override
        public void onReturnGetPinInputResult(int i) {

        }

        @Override
        public void onReturnGetKeyBoardInputResult(String s) {

        }

        @Override
        public void onReturnGetPinResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onReturnPowerOnIccResult(boolean b, String s, String s1, int i) {

        }

        @Override
        public void onReturnPowerOffIccResult(boolean b) {

        }

        @Override
        public void onReturnApduResult(boolean b, String s, int i) {

        }

        @Override
        public void onReturnPowerOnFelicaResult(MPOSService.FelicaStatusCode felicaStatusCode) {

        }

        @Override
        public void onReturnPowerOffFelicaResult(MPOSService.FelicaStatusCode felicaStatusCode) {

        }

        @Override
        public void onReturnSendApduFelicaResult(MPOSService.FelicaStatusCode felicaStatusCode, String s, String s1) {

        }

        @Override
        public void onReturnSetSleepTimeResult(boolean b) {

        }

        @Override
        public void onGetCardNoResult(String s) {

        }

        @Override
        public void onGetCardInfoResult(Hashtable<String, String> hashtable) {

        }

        @Override
        public void onRequestSignatureResult(byte[] bytes) {

        }

        @Override
        public void onRequestCalculateMac(String s) {

        }

        // Empty implementations for unused callbacks
        @Override public void onRequestSetMPOCPin() {}
        @Override public void onReturnGetMPOCPinResult(String s, Hashtable<String, String> hashtable) {}
        @Override public void onGetDeviceTestResult(boolean b) {}
        @Override public void onQposRequestPinResult(List<String> list, int i) {}

        @Override
        public void onReturnD20SleepTimeResult(boolean b) {

        }

        @Override
        public void onQposRequestPinStartResult(List<String> list) {

        }

        @Override
        public void onQposPinMapSyncResult(boolean b, boolean b1) {

        }

        @Override
        public void onRequestWaitingUser() {

        }

        @Override
        public void onReturnSyncVersionInfo(QPOSService.FirmwareStatus firmwareStatus, String s, QPOSService.QposStatus qposStatus) {

        }

        @Override
        public void onReturnSpLogResult(String s) {

        }

        @Override
        public void onReturnRsaResult(String s) {

        }

        @Override
        public void onQposInitModeResult(boolean b) {

        }

        @Override
        public void onD20StatusResult(String s) {

        }

        @Override
        public void onQposTestSelfCommandResult(boolean b, String s) {

        }

        @Override
        public void onQposTestCommandResult(boolean b, String s) {

        }

        @Override
        public void onQposGetRealTimeSelfDestructStatus(boolean b, String s) {

        }

        @Override
        public void onReturPosSelfDestructRecords(boolean b, String s) {

        }
        // ... (keep other empty implementations as needed)

        // Note: The original code had many empty callback implementations.
        // They should be maintained for interface compatibility.
    }

    /**
     * Helper class for DUKPT key set management
     */
    private static class DukptKeySet {
        final String ksn;
        final String encryptedIpek;
        final String kcv;

        DukptKeySet(String ksn, String encryptedIpek, String kcv) {
            this.ksn = ksn;
            this.encryptedIpek = encryptedIpek;
            this.kcv = kcv;
        }
    }

    /**
     * Error codes for better error handling
     */
    private static class ErrorCode {
        static final int SUCCESS = 0;
        static final int OPERATION_FAILED = -1;
        static final int OPERATION_TIMEOUT = -2;
        static final int OPERATION_INTERRUPTED = -3;
        static final int UNEXPECTED_ERROR = -4;
        static final int INVALID_PARAMETERS = -5;
    }
}