package com.isw.payapp.devices.newtelpo.EMV;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.model.TransactionData;
import com.telpo.emv.AmexAmount;
import com.telpo.emv.AmexLimits;
import com.telpo.emv.AmexListener;
import com.telpo.emv.AmexParam;
import com.telpo.emv.AmexResult;
import com.telpo.emv.EmvCertRevo;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.util.StringUtil;

import java.util.List;

public class AmexExpress {
    private static final String TAG = "AmexScheme";

    // AMEX-specific constants
    private static final byte[] AMEX_AID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x25};
    private static final byte[] DEFAULT_AMEX_VERSION = new byte[]{0x00, 0x01};
    private static final int DEFAULT_HOLD_TIME_MS = 300;
    private static final String DEFAULT_MERCHANT_NAME = "Amex";

    private final Context context;
    private final EMVHandler emvHandler;
    private TransactionData transactionData;
    private int lastErrorCode = 0;

    // Transaction configuration
    private long transactionAmount = 0;
    private int currencyCode = 840; // USD
    private int currencyExponent = 2;
    //private final AmexEmvCombinedListener combinedListener;

    public AmexExpress(Context context) {
        this.context = context;
        this.emvHandler = new EMVHandler(context);
    }

    // Configuration methods
    public void setCurrency(int currencyCode, int currencyExponent) {
        this.currencyCode = currencyCode;
        this.currencyExponent = currencyExponent;
    }

    // Main transaction method
    public boolean StartTransaction(double amount) {
        this.transactionAmount = (long) (amount * 100);
        logInfo("Starting AMEX transaction for amount: " + amount);

        try {
            // 1. Setup EMV service
            if (!setupEmvService()) {
                return false;
            }

            // 2. Initialize AMEX transaction
            if (!initializeAmexTransaction()) {
                return false;
            }

            // 3. Configure limits
            if (!configureLimits()) {
                return false;
            }

            // 4. Process AMEX application
            return processAmexApplication();

        } catch (Exception e) {
            logError("Transaction failed with exception: " + e.getMessage());
            return false;
        }
    }

    private boolean setupEmvService() {
        // Set listener
        emvHandler.emvService.setListener(amexListener);

        // Set EMV parameters
        EmvParam emvParam = new EmvParam();
        lastErrorCode = emvHandler.emvService.Emv_SetParam(emvParam);

        if (!isSuccess(lastErrorCode)) {
            logError("Emv_SetParam failed with code: " + lastErrorCode);
            return false;
        }

        logInfo("Emv_SetParam successful");

        // Add certificate revocation list
        EmvCertRevo emvCertRevo = new EmvCertRevo();
        lastErrorCode = emvHandler.emvService.Emv_CertRevoList_Add(emvCertRevo);

        if (!isSuccess(lastErrorCode)) {
            logError("Emv_CertRevoList_Add failed with code: " + lastErrorCode);
            return false;
        }

        logInfo("Emv_CertRevoList_Add successful");
        return true;
    }

    private boolean initializeAmexTransaction() {
        // Create AMEX transaction parameters
        AmexParam amexParam = createAmexParam();
        AmexAmount amexAmount = createAmexAmount();

        // Initialize AMEX transaction
        lastErrorCode = emvHandler.emvService.Amex_TransInit(amexParam, amexAmount);

        if (!isSuccess(lastErrorCode)) {
            logError("Amex_TransInit failed with code: " + lastErrorCode);
            return false;
        }

        logInfo("Amex_TransInit successful");
        return true;
    }

    private AmexParam createAmexParam() {
        AmexParam param = new AmexParam();

        // Basic terminal configuration
        param.AmexVersion = DEFAULT_AMEX_VERSION.clone();
        param.TermCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xC8};
        param.TermCountryCode = 404; // Example country code
        param.TermType = 0x22;
        param.ContactlessCap = 0xC0;
        param.EnhanceContactlessCap = new byte[]{(byte) 0x58, (byte) 0xE0, 0x00, (byte) 0x83};
        param.MerchantName = DEFAULT_MERCHANT_NAME;
        param.MerchantCode = new byte[]{0x41, 0x12};
        param.HoldTimeMs = DEFAULT_HOLD_TIME_MS;
        param.MagStripeRangeNumber = 60;
        param.TAC_Denial = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
        param.TAC_OnLine = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
        param.TAC_Default = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
        param.IsUnableOnline = 0;
        param.CheckCDAMode = 1;

        return param;
    }

    private AmexAmount createAmexAmount() {
        AmexAmount amount = new AmexAmount();
        amount.Amount = transactionAmount;
        amount.CashbackAmount = 0;
        amount.CurrCode = currencyCode;
        amount.CurrExp = currencyExponent;
        return amount;
    }

    private boolean configureLimits() {
        // Set default limits
        AmexLimits defaultLimits = new AmexLimits();
        defaultLimits.CVMLimit = 10000;
        defaultLimits.FloorLimit = 100;
        defaultLimits.TransLimit = 100000;

        lastErrorCode = emvHandler.emvService.Amex_DefaultLimit_Set(defaultLimits);
        if (!isSuccess(lastErrorCode)) {
            logError("Amex_DefaultLimit_Set failed with code: " + lastErrorCode);
            return false;
        }
        logInfo("Amex_DefaultLimit_Set successful");

        // Add AID-specific limits
        lastErrorCode = emvHandler.emvService.Amex_AidLimit_Add(defaultLimits, AMEX_AID);
        if (!isSuccess(lastErrorCode)) {
            logError("Amex_AidLimit_Add failed with code: " + lastErrorCode);
            return false;
        }
        logInfo("Amex_AidLimit_Add successful");

        // Add dynamic limits
        AmexLimits dynamicLimits = new AmexLimits();
        dynamicLimits.CVMLimit = 10000;
        dynamicLimits.FloorLimit = 100;
        dynamicLimits.TransLimit = 10000;

        lastErrorCode = emvHandler.emvService.Amex_DynamicLimit_Add(dynamicLimits, 0);
        if (!isSuccess(lastErrorCode)) {
            logError("Amex_DynamicLimit_Add failed with code: " + lastErrorCode);
            return false;
        }
        logInfo("Amex_DynamicLimit_Add successful");

        return true;
    }

    private boolean processAmexApplication() {
        // Preprocess
        lastErrorCode = emvHandler.emvService.Amex_Preprocess();
        if (!isSuccess(lastErrorCode)) {
            logError("Amex_Preprocess failed with code: " + lastErrorCode);
            return false;
        }
        logInfo("Amex_Preprocess successful");

        // Start AMEX application
        lastErrorCode = emvHandler.emvService.Amex_StartApp();

        // Retrieve card information if needed
        if (!retrieveCardInformation()) {
            logError("Failed to retrieve card information");
            return false;
        }

        // Get additional TLV data
        retrieveAdditionalTLVData();

        // Process outcome
        return handleTransactionOutcome();
    }

    private boolean retrieveCardInformation() {
        if (emvHandler.cardNum == null || emvHandler.cardNum.isEmpty()) {
            String cardNum = readCardNumberFromEmv();
            if (cardNum == null || cardNum.isEmpty()) {
                logError("Failed to read card number");
                return false;
            }
            emvHandler.cardNum = cardNum;
            logInfo("Card number retrieved: " + maskCardNumber(cardNum));
        }
        return true;
    }

    private String readCardNumberFromEmv() {
        // Try to read from tag 0x5A
        EmvTLV tlv = new EmvTLV(0x5A);
        int ret = emvHandler.emvService.Emv_GetTLV(tlv);

        if (isSuccess(ret)) {
            String cardNum = StringUtil.bytesToHexString(tlv.Value).replace("F", "");
            logInfo("Card number from tag 0x5A: " + maskCardNumber(cardNum));
            return cardNum;
        }

        // Fallback to tag 0x57
        tlv = new EmvTLV(0x57);
        ret = emvHandler.emvService.Emv_GetTLV(tlv);

        if (isSuccess(ret)) {
            String str57 = StringUtil.bytesToHexString(tlv.Value);
            String cardNum = str57.substring(0, str57.indexOf('D'));
            logInfo("Card number from tag 0x57: " + maskCardNumber(cardNum));
            return cardNum;
        }

        return null;
    }

    private void retrieveAdditionalTLVData() {
        List<EmvTLV> tagList = EMVUtilsConfigs.getTLVContactlessCardDataTags();
        if (tagList == null) return;

        for (EmvTLV emvTLV : tagList) {
            int ret = emvHandler.emvService.Emv_GetTLV(emvTLV);
            String tagHex = Integer.toHexString(emvTLV.Tag).toUpperCase();

            if (isSuccess(ret)) {
                logInfo("Tag " + tagHex + ": " + StringUtil.bytesToHexString(emvTLV.Value));
            } else {
                logInfo("Tag " + tagHex + ": Not available");
            }
        }
    }

    private boolean handleTransactionOutcome() {
        lastErrorCode = emvHandler.emvService.Amex_GetOutComeResult();
        logInfo("Amex_GetOutComeResult code: " + lastErrorCode);

        if (lastErrorCode == AmexResult.AMEX_RESULT_AGAIN) {
            lastErrorCode = emvHandler.emvService.Amex_RetryApp();
        }

        switch (lastErrorCode) {
            case AmexResult.AMEX_RESULT_APPROVED:
                logInfo("Transaction approved");
                return true;

            case AmexResult.AMEX_RESULT_ONLINE:
                logInfo("Online processing required");
                // Online processing would be handled by the listener
                return true;

            default:
                logError("Transaction failed with code: " + lastErrorCode);
                return false;
        }
    }

    private final AmexListener amexListener = new AmexListener() {
        @Override
        public int OnAmexMessage(int messageId, int holdTimeMs) {
            logInfo("OnAmexMessage, MessageID: " + messageId + ", HoldTimesMs: " + holdTimeMs);
            return EmvService.EMV_TRUE;
        }

        @Override
        public int OnAmexCheckException(int errorCode, String errorMessage) {
            logError("OnAmexCheckException - Code: " + errorCode + ", Message: " + errorMessage);
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnAmexRequireOnline() {
            logInfo("Online processing required");
            return processOnlineTransaction() ?
                    AmexResult.AMEX_ONLINE_APPROVED :
                    AmexResult.AMEX_ONLINE_DECLINED;
        }

        @Override
        public int OnAmexInputPin() {
            return handlePinInput();
        }
    };

    private boolean processOnlineTransaction() {
        // Implement your online processing logic here
        // This would typically involve communicating with your payment gateway
        try {
            // Simulate online processing
            return true; // Return true for approved, false for declined
        } catch (Exception e) {
            logError("Online processing failed: " + e.getMessage());
            return false;
        }
    }

    private int handlePinInput() {
        // Ensure we have card number
        if (emvHandler.cardNum == null || emvHandler.cardNum.isEmpty()) {
            String cardNum = readCardNumberFromEmv();
            if (cardNum == null || cardNum.isEmpty()) {
                logError("Transaction failed: No card information");
                return EmvService.EMV_FALSE;
            }
            emvHandler.cardNum = cardNum;
        }

        logInfo("Processing PIN for card: " + maskCardNumber(emvHandler.cardNum));

        // Show card number in UI (masked)
        String maskedPan = maskCardNumber(emvHandler.cardNum);
        emvHandler.changePanUIVisibility(true, "PAN: " + maskedPan);

        // Get PIN block based on encryption mode
        String pinBlock = getPinBlock();
        if (pinBlock == null || pinBlock.isEmpty()) {
            emvHandler.changePanUIVisibility(false, null);
            logError("Failed to generate PIN block");
            return EmvService.EMV_FALSE;
        }

        emvHandler.pinBlock = pinBlock;
        emvHandler.changePanUIVisibility(false, null);
        logInfo("PIN block generated successfully");

        return EmvService.EMV_TRUE;
    }

    private String getPinBlock() {
        if (emvHandler.isMkMode) {
            // MK/SK mode
            return emvHandler.getMkPin(emvHandler.cardNum);
        } else {
            // DUKPT mode
            return emvHandler.getDukptPin(emvHandler.cardNum);
        }
    }

    // Utility methods
    private boolean isSuccess(int resultCode) {
        return resultCode == EmvService.EMV_TRUE;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return cardNumber;
        }
        return cardNumber.substring(0, 6) + "******" +
                cardNumber.substring(cardNumber.length() - 4);
    }

    private void logInfo(String message) {
        Log.i(TAG, message);
        emvHandler.appendDisplay(message);
    }

    private void logError(String message) {
        Log.e(TAG, message);
        emvHandler.appendDisplay("ERROR: " + message);
    }

    public int getLastErrorCode() {
        return lastErrorCode;
    }
}