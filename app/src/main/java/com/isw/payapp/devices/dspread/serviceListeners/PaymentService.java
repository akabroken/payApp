package com.isw.payapp.devices.dspread.serviceListeners;

import android.bluetooth.BluetoothDevice;

import com.dspread.xpos.MPOSService;
import com.dspread.xpos.QPOSService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentService implements QPOSService.QPOSServiceListener {
    @Override
    public void onRequestSetMPOCPin() {

    }

    @Override
    public void onReturnGetMPOCPinResult(String s, Hashtable<String, String> hashtable) {

    }

    @Override
    public void onGetDeviceTestResult(boolean b) {

    }

    @Override
    public void onQposRequestPinResult(List<String> list, int i) {

    }

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
    public void onReturnSyncVersionInfo(MPOSService.FirmwareStatus firmwareStatus, String s, MPOSService.QposStatus qposStatus) {

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

    @Override
    public void onQposIdResult(Hashtable<String, String> hashtable) {

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

    @Override
    public void onRequestQposConnected() {

    }

    @Override
    public void onRequestQposDisconnected() {

    }

    @Override
    public void onRequestNoQposDetected() {

    }

    @Override
    public void onRequestNoQposDetectedUnbond() {

    }

    @Override
    public void onError(QPOSService.Error error) {

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

    @Override
    public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult updateInformationResult) {

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
    public void onReturnSetMasterKeyResult(boolean b) {

    }

    @Override
    public void onReturnSetMasterKeyResult(boolean b, Hashtable<String, String> hashtable) {

    }

    @Override
    public void onRequestUpdateKey(String s) {

    }

    @Override
    public void onReturnUpdateIPEKResult(boolean b) {

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
    public void onGetKeyCheckValue(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onGetDevicePubKey(Hashtable<String, String> hashtable) {

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
}
