package com.isw.payapp.devices.dspread;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.CQPOSService;
import com.dspread.xpos.QPOSService;
//import com.isw.payapp.Manifest;
import com.isw.payapp.devices.dspread.baseService.BasePOSService;
import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.PaymentResult;
import com.isw.payapp.devices.dspread.enumarators.POS_TYPE;
import com.isw.payapp.devices.dspread.enumarators.TransCardMode;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.FileUtils;
import com.isw.payapp.devices.dspread.utils.HandleTxnsResultUtils;
import com.isw.payapp.devices.dspread.utils.USBClass;
import com.isw.payapp.utils.ThreeDES;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.goldze.mvvmhabit.utils.SPUtils;
import me.goldze.mvvmhabit.utils.Utils;

public final class POSManager {
    private static final String TAG = "POS";
    private static volatile POSManager instance;
    private QPOSService pos;
    private Context context;
    private QPOSServiceListener listener;
    // Callback management
    private final List<IConnectionServiceCallback> connectionCallbacks = new CopyOnWriteArrayList<>();
    private final List<IPaymentServiceCallback> transactionCallbacks = new CopyOnWriteArrayList<>();
    private Handler mainHandler;
    private CountDownLatch connectLatch;
    private PaymentResult paymentResult;
    private POS_TYPE posType;
    private boolean isICC;

    private ThreeDES threeDES;

    private POSManager(Context context) {
        this.context = context.getApplicationContext();
        this.listener = new QPOSServiceListener();
        mainHandler = new Handler(Looper.getMainLooper());
        paymentResult = new PaymentResult();
    }

    /**
     * Initialize POSManager with application context
     * @param context Application context
     */
    public static void init(Context context) {
        getInstance(context);
        Utils.init(context);
    }

    public static POSManager getInstance(Context context) {
        if (instance == null) {
            synchronized (POSManager.class) {
                if (instance == null) {
                    instance = new POSManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Get singleton instance of POSManager
     * @return POSManager instance
     */
    public static POSManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("POS must be initialized with context first");
        }
        return instance;
    }

    /**
     * Connect to POS device
     * @param deviceAddress Device address (Bluetooth address or USB port)
     * @param callback Callback to handle connection events
     */
    public void connect(String deviceAddress, IConnectionServiceCallback callback) {
        connectLatch = new CountDownLatch(1);
        registerConnectionCallback(callback);

        // start connect
        connect(deviceAddress);
        try {
            boolean waitSuccess = connectLatch.await(5, TimeUnit.SECONDS);
            if (!waitSuccess) {
                TRACE.i("Connection timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Connection interrupted", e);
        }

    }

    public void connect(String deviceAddress){
        if(!deviceAddress.isEmpty()){
            if(deviceAddress.contains(":")){
                posType = POS_TYPE.BLUETOOTH;
                initMode(QPOSService.CommunicationMode.BLUETOOTH);
                pos.setDeviceAddress(deviceAddress);
                pos.connectBluetoothDevice(true, 25, deviceAddress);
            }else {
                posType = POS_TYPE.USB;
                UsbDevice usbDevice = USBClass.getMdevices().get(deviceAddress);
                initMode(QPOSService.CommunicationMode.USB);
                pos.openUsb(usbDevice);
            }
        }else {
            Log.i("POSManager","UART");

            posType = POS_TYPE.UART;

            initMode(QPOSService.CommunicationMode.UART);
            pos.forceOpenUart(30);
           // pos.openUart();
            pos.openLog(true);

        }
    }

    public void forceOpenUart(int connectTime){
        pos.forceOpenUart(connectTime);
    }

    public void initMode(QPOSService.CommunicationMode mode) {
        pos = QPOSService.getInstance(context, mode);
        if (pos == null) {
            return;
        }
        if (mode == QPOSService.CommunicationMode.USB_OTG_CDC_ACM) {
            pos.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM);
        }
        pos.setContext(context);
        pos.initListener(listener);
       pos.setFormatId(QPOSService.FORMATID.DUKPT);

    }


    public QPOSService getQPOSService() {
        return pos;
    }

    public void clearPosService(){
        pos = null;
    }


    public void setICC(boolean ICC) {
        isICC = ICC;
    }

    /**
     * Check if device is ready for transaction
     * @return true if device is connected and ready
     */
    public boolean isDeviceReady() {
        return pos != null;
    }

    public void setDeviceAddress(String address){
        pos.setDeviceAddress(address);
    }

    public QPOSService.TransactionType getTransType(){
        String transactionTypeString = SPUtils.getInstance().getString("transactionType","");
        Log.i("POSManager", transactionTypeString);
        if (transactionTypeString.isEmpty()) {
            transactionTypeString = "GOODS";
        }
        return HandleTxnsResultUtils.getTransactionType(transactionTypeString);
    }
    public QPOSService.CardTradeMode getCardTradeMode(){
        String modeName = SPUtils.getInstance().getString("cardMode","");
        Log.i("POSManager", modeName);
        QPOSService.CardTradeMode cardTradeMode;
        if(modeName.isEmpty()){
            if(DeviceUtils.isSmartDevices()){
//                pos.setCardTradeMode(QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD_NOTUP);
                cardTradeMode = QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD_NOTUP;
            }else {
                cardTradeMode = QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD;
            }
        }else {
            cardTradeMode = TransCardMode.valueOf(modeName).getCardTradeModeValue();
        }
        return cardTradeMode;
    }

    /**
     * Start a payment transaction
     * @param amount Transaction amount
     * @param callback Callback to handle payment events
     */
    public void startTransaction(String amount,IPaymentServiceCallback callback){
        if(!isDeviceReady()){
            return;
        }
        getDeviceId();
        if(callback != null) {
            registerPaymentCallback(callback);
        }
        //String devId = getDeviceId();

        int currencyCode = SPUtils.getInstance().getInt("currencyCode",404);
        Log.i("POSManager", "Amount "+amount);
        Log.i("POSManager", "getTransType "+getTransType());
        Log.i("POSManager", "getCardTradeMode "+getCardTradeMode());
        Log.i("POSManager", "Amount "+amount);
        pos.setCardTradeMode(getCardTradeMode());
        //Format Id
        //pos.setFormatId (QPOSService.FORMATID.MKSK);
        pos.setAmount(amount,"",String.valueOf(currencyCode),getTransType());
        pos.doTrade(60);
    }

    public void getDeviceId(){
        Hashtable<String, Object> posIdTable = pos.syncGetQposId(5);
        String posId = posIdTable.get("posId") == null ? "" : (String) posIdTable.get("posId");
        SPUtils.getInstance().put("posID",posId);
        TRACE.i("posid :" + SPUtils.getInstance().getString("posID"));
    }

    /**
     * Cancel ongoing transaction
     */
    public void cancelTransaction(){
        if(pos != null){
            pos.cancelTrade();
        }
    }

    public void sendTime(String terminalTime){
        pos.sendTime(terminalTime);
    }

    public void selectEmvApp(int position){
        TRACE.i("selectEmvApp::"+position);
        pos.selectEmvApp(position);
    }

    public void cancelSelectEmvApp(){
        if(pos!=null) {
            pos.cancelSelectEmvApp();
        }
    }

    public void pinMapSync(String value, int timeout){
        if(pos!=null) {
            pos.pinMapSync(value, timeout);
        }
    }

    public void getIccCardNo(){
        String terminalTime = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance().getTime());
        pos.getIccCardNo(terminalTime);
    }

    public void cancelPin(){
        if(pos!=null) {
            pos.cancelPin();
        }
    }

    public boolean isOnlinePin(){
        return pos.isOnlinePin();
    }

    public int getCvmPinTryLimit(){
        return pos.getCvmPinTryLimit();
    }

    public void bypassPin(){
        if (pos!=null) {
            pos.sendPin("".getBytes());
        }
    }

    public void sendCvmPin(String pinBlock, boolean isEncrypted){
        if(pos!=null) {
            pos.sendCvmPin(pinBlock, isEncrypted);
        }
    }

    public Hashtable<String,String> getEncryptData(){
        return pos.getEncryptData();
    }

    public Hashtable<String, String> getNFCBatchData(){
        return pos.getNFCBatchData();
    }

    public void sendOnlineProcessResult(String tlv){
        if(pos!=null) {
            pos.sendOnlineProcessResult(tlv);
        }
    }

    public Hashtable<String, String> anlysEmvIccData(String tlv){
        return pos.anlysEmvIccData(tlv);
    }

    public void updateEMVConfig(String fileName){
        //ex: emv_profile_tlv.xml
      //  System.out.println(FileUtils.readAssetsLine(fileName,context));
        pos.updateEMVConfigByXml(new String(FileUtils.readAssetsLine(fileName,context)));
    }

    public Hashtable getENCDataBlock(){
        return pos.getEncryptData();
    }

    public void updateDeviceFirmware(Activity activity, String blueTootchAddress){
//        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION) //to be fixed later WRITE_EXTERNAL_STORAGE
//                != PackageManager.PERMISSION_GRANTED) {
//            //request permission
//            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION}, 1001);// To be fixed later WRITE_EXTERNAL_STORAGE
//        } else {
//            byte[] data = FileUtils.readAssetsLine("CR100_master.asc", activity);
//            if(data != null){
//                int updateResult = pos.updatePosFirmware(data, blueTootchAddress);
//                if(updateResult== -1){
//                    Toast.makeText(activity, "please keep the device charging", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
    }

    public void  updateWorkKey(String fileName){
        pos.updateWorkKey(new String(FileUtils.readAssetsLine(fileName,context)));
    }

    public void doUpdateIpekKey(String key, String iKsn, String kcv) throws InterruptedException {

        pos.resetQPOS();
        updateWorkKey("debug_certificate.pem");
       // pos.wait(99);
        String tmk = "0123456789ABCDEFFEDCBA9876543210";
        threeDES = new ThreeDES();
        String demoTrackKsn = iKsn;
        String demoTrackIpek = key;
        // Calculate KCV (encrypt zero data)
        String demoIpekKcv = threeDES.tdesECBEncrypt(demoTrackIpek, "0000000000000000");
        String kcvOut = threeDES.extractKcv(demoIpekKcv);
        System.out.println("Track IPEK KCV: " + kcvOut); // Should be: 377EE0

        // Encrypt IPEK with TMK
        String encDemoTrackIpek = threeDES.tdesECBEncrypt(tmk, demoTrackIpek);
        System.out.println("Encrypted Track IPEK: " + encDemoTrackIpek);

        // Similar operations for EMV and PIN keys
        String demoEmvKsn = iKsn;
        String demoEmvIpek = key;
        String demoEmvIpekKcv = threeDES.tdesECBEncrypt(demoEmvIpek, "0000000000000000");
        String emvKcv = threeDES.extractKcv(demoEmvIpekKcv);
        System.out.println("EMV IPEK KCV: " + emvKcv); // Should be: AE8F91

        String encDemoEmvIpek = threeDES.tdesECBEncrypt(tmk, demoEmvIpek);
        System.out.println("Encrypted EMV IPEK: " + encDemoEmvIpek);

        String demoPinKsn = iKsn;
        String demoPinIpek = key;
        String demoPinIpekKcv = threeDES.tdesECBEncrypt(demoPinIpek, "0000000000000000");
        String pinKcv = threeDES.extractKcv(demoPinIpekKcv);
        System.out.println("PIN IPEK KCV: " + pinKcv); // Should be: 7DD75C

        String encDemoPinIpek = threeDES.tdesECBEncrypt(tmk, demoPinIpek);
        System.out.println("Encrypted PIN IPEK: " + encDemoPinIpek);

       // pos.updateWorkKey("");
        pos.doUpdateIPEKOperation("1", demoTrackKsn, encDemoTrackIpek, kcvOut,
                demoEmvKsn, encDemoEmvIpek, emvKcv, demoPinKsn, encDemoPinIpek, pinKcv);

    }

    public void close() {
        TRACE.d("start close");
        if (pos == null || posType == null) {
            TRACE.d("return close");
        } else if (posType == POS_TYPE.BLUETOOTH) {
            pos.disconnectBT();
        } else if (posType == POS_TYPE.BLUETOOTH_BLE) {
            pos.disconnectBLE();
        } else if (posType == POS_TYPE.UART) {
            pos.closeUart();
        } else if (posType == POS_TYPE.USB) {
            pos.closeUsb();
        }else {
            pos.disconnectBT();
        }
        SPUtils.getInstance().put("deviceAddress","");
        clearPosService();
    }

    /**
     * register payment callback
     */
    public void registerPaymentCallback(IPaymentServiceCallback callback) {
        if (callback != null && !transactionCallbacks.contains(callback)) {
            transactionCallbacks.add(callback);
        }
    }

    /**
     * register connection service callback
     */
    public void registerConnectionCallback(IConnectionServiceCallback callback) {
        if (callback != null && !connectionCallbacks.contains(callback)) {
            connectionCallbacks.add(callback);
        }
    }

    public void unregisterCallbacks() {
        connectionCallbacks.clear();
        transactionCallbacks.clear();
    }

    private void notifyConnectionCallbacks(CallbackAction<IConnectionServiceCallback> action) {
        mainHandler.post(() -> {
            for (IConnectionServiceCallback  callback : connectionCallbacks) {
                try {
                    action.execute(callback);
                } catch (Exception e) {
                    TRACE.e("Error in connection callback: " + e.getMessage());
                }
            }
        });
    }

    private void notifyTransactionCallbacks(CallbackAction<IPaymentServiceCallback> action) {
        mainHandler.post(() -> {
            for (IPaymentServiceCallback callback : transactionCallbacks) {
                try {
                    action.execute(callback);
                } catch (Exception e) {
                    TRACE.e("Error in transaction callback: " + e.getMessage());
                }
            }
        });
    }



    @FunctionalInterface
    private interface CallbackAction<T> {
        void execute(T callback) throws Exception;
    }

    private class QPOSServiceListener extends CQPOSService {

        @Override
        public void onRequestQposConnected() {
            connectLatch.countDown();

            SPUtils.getInstance().put("isConnected",true);
            notifyConnectionCallbacks(cb -> cb.onRequestQposConnected());
        }

        @Override
        public void onRequestQposDisconnected() {
            SPUtils.getInstance().put("isConnected",false);
            clearPosService();
            connectLatch.countDown();
            notifyConnectionCallbacks(cb -> cb.onRequestQposDisconnected());
        }

        @Override
        public void onRequestNoQposDetected() {
            SPUtils.getInstance().put("isConnected",false);
            clearPosService();
            connectLatch.countDown();
            notifyConnectionCallbacks(cb -> cb.onRequestNoQposDetected());
        }

        @Override
        public void onDoTradeResult(QPOSService.DoTradeResult result, Hashtable<String, String> decodeData) {
            TRACE.d("(DoTradeResult result, Hashtable<String, String> decodeData) " + result.toString() + "\n" + "decodeData:" + decodeData);
            // Handle ICC card for EMV processing
            setICC(false);
            if (result == QPOSService.DoTradeResult.ICC) {
                setICC(true);
                TRACE.d("result.name()::"+result.name());
                paymentResult.setTransactionType(result.name());

                if (pos != null) {
                    TRACE.d("Doing doEmvApp::"+result.name());
                    pos.doEmvApp(QPOSService.EmvOption.START);
                    TRACE.d("Doing doEmvApp::OK OK OK");
                    //getIccCardNo();
                }
            }else if(result == QPOSService.DoTradeResult.NFC_OFFLINE || result == QPOSService.DoTradeResult.NFC_ONLINE ||result == QPOSService.DoTradeResult.MCR){
                paymentResult = HandleTxnsResultUtils.handleTransactionResult(paymentResult,decodeData);
                paymentResult.setTransactionType(result.name());
                notifyTransactionCallbacks(cb -> cb.onTransactionCompleted(paymentResult));
            } else {
                String msg = HandleTxnsResultUtils.getTradeResultMessage(result,context);
                notifyTransactionCallbacks(cb -> cb.onTransactionFailed(msg, null));
            }
        }

        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
            String msg = HandleTxnsResultUtils.getTransactionResultMessage(transactionResult, context);
            TRACE.d("onRequestTransactionResult: "+msg);
            String ipektw = "33707E4927C4A0D5"; //live
            String iksn_live = "FFFF000002DDDDE00000";
            String kcv_live = "10B9824432E458DD";
            paymentResult.setStatus(msg);
            if(msg.equals("trans fallback")){
                updateEMVConfig("emv_profile_tlv.xml");
            }else if(msg.equals("TRANSACTION_TERMINATED")){
                pos.doUpdateIPEKOperation("0", "", "", "", "", "", "", iksn_live, ipektw, kcv_live);
            }
            if (!msg.isEmpty()) {
                notifyTransactionCallbacks(cb -> cb.onTransactionFailed(msg,null));
            }else {
                notifyTransactionCallbacks(cb -> cb.onTransactionCompleted(paymentResult));
            }

        }

        @Override
        public void onRequestWaitingUser() {
            notifyTransactionCallbacks(cb -> cb.onRequestWaitingUser());
        }

        @Override
        public void onRequestTime() {
            notifyTransactionCallbacks(cb -> cb.onRequestTime());
        }

        @Override
        public void onRequestSelectEmvApp(ArrayList<String> appList) {
            notifyTransactionCallbacks(cb -> cb.onRequestSelectEmvApp(appList));
        }

        @Override
        public void onRequestOnlineProcess(String tlv) {
            notifyTransactionCallbacks(cb -> cb.onRequestOnlineProcess(tlv));
        }

        @Override
        public void onRequestBatchData(String tlv) {
            paymentResult.setTlv(tlv);
            notifyTransactionCallbacks(cb -> cb.onTransactionCompleted(paymentResult));
        }

        @Override
        public void onRequestSetPin(boolean isOfflinePin, int tryNum) {
            TRACE.d("onRequestSetPin = " + isOfflinePin + "\ntryNum: " + tryNum);
            notifyTransactionCallbacks(cb -> cb.onRequestSetPin(isOfflinePin, tryNum));
        }

        @Override
        public void onReturnCustomConfigResult(boolean isSuccess,String result){
            TRACE.d("onReturnCustomConfigResult "+ "isSuccess:"+isSuccess +" result:"+result +" Is Offline pin"+ isOnlinePin());
            notifyTransactionCallbacks(cb -> cb.onReturnCustomConfigResult(isSuccess, result));
        }

        @Override
        public void onRequestDisplay(QPOSService.Display displayMsg) {
            TRACE.i("parent onRequestDisplay" );
            notifyTransactionCallbacks(cb -> cb.onRequestDisplay(displayMsg));
        }

        @Override
        public void onError(QPOSService.Error errorState) {
            notifyTransactionCallbacks(cb -> cb.onTransactionFailed(errorState.name(),null));
        }

        @Override
        public void onReturnReversalData(String tlv) {
            paymentResult.setTlv(tlv);
            notifyTransactionCallbacks(cb -> cb.onTransactionCompleted(paymentResult));
        }

        @Override
        public void onEmvICCExceptionData(String tlv) {
            notifyTransactionCallbacks(cb -> cb.onTransactionFailed("Decline",tlv));
        }

        @Override
        public void onGetCardInfoResult(Hashtable<String, String> cardInfo) {
            notifyTransactionCallbacks(cb -> cb.onGetCardInfoResult(cardInfo));
        }

        @Override
        public void onRequestSetPin() {
            TRACE.d("onRequestSetPin()::::ONLINE PIN");
            notifyTransactionCallbacks(cb -> cb.onRequestSetPin());
        }

        @Override
        public void onReturnGetPinInputResult(int num) {
            notifyTransactionCallbacks(cb -> cb.onReturnGetPinInputResult(num));
        }

        @Override
        public void onQposRequestPinResult(List<String> dataList, int offlineTime) {
            TRACE.d("onQposRequestPinResult()"+dataList.toString()+" offlineTime:"+offlineTime);
            notifyTransactionCallbacks(cb -> cb.onQposRequestPinResult(dataList,offlineTime));
        }

        @Override
        public void onTradeCancelled() {
            notifyTransactionCallbacks(cb -> cb.onTransactionFailed("Cancel",null));
        }

        @Override
        public void onReturnGetPinResult(Hashtable<String, String> result){
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):" + result.toString());
            notifyTransactionCallbacks(cb -> cb.onReturnGetPinResult(result));
        }

        @Override
        public void onGetCardNoResult(String cardNo){
            TRACE.d("onGetCardNoResult(String cardNo):" + cardNo);
            notifyTransactionCallbacks(cb -> cb.onGetCardNoResult(cardNo));
        }

        @Override
        public void onReturnUpdateIPEKResult(boolean b){
            TRACE.d("onReturnUpdateIPEKResult(boolean b):" + b);
            notifyTransactionCallbacks(cb -> cb.onReturnUpdateIPEKResult(b));
        }

        @Override
        public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult result) {
            TRACE.d("onRequestUpdateWorkKeyResult(boolean b):" + result);
            notifyTransactionCallbacks(cb -> cb.onRequestUpdateWorkKeyResult(result));
        }
    }
}