package com.isw.payapp.devices.dspread.utils;

import java.util.Hashtable;
import java.util.Map;
public class EmvTLVTags {

    // -------------------------------
    // Common EMV tag constants
    // -------------------------------
    public static final String CardholderName            = "5F20";
    public static final String ApplicationExpiryDate     = "5F24";
    public static final String ApplicationEffectiveDate  = "5F25";
    public static final String IssuerCountryCode         = "5F28";
    public static final String TransactionCurrencyCode    = "5F2A";
    public static final String ServiceCode               = "5F30";
    public static final String PANSequenceNumber         = "5F34";
    public static final String AIDCard                   = "4F";
    public static final String ApplicationLabel          = "50";
    public static final String Track2EquivalentData      = "57";
    public static final String PAN                       = "5A";

    public static final String CVMList                   = "8E";
    public static final String ApplicationInterchangeProfile = "82";
    public static final String DedicatedFileName         = "84";
    public static final String TVR                       = "95"; // Terminal Verification Results

    public static final String TransactionDate           = "9A";
    public static final String TransactionStatusInfo     = "9B";
    public static final String TransactionType           = "9C";

    public static final String AmountAuthorised          = "9F02";
    public static final String AmountOther               = "9F03";
    public static final String ApplicationIdentifier     = "9F06"; // terminal AID
    public static final String ApplicationUsageControl   = "9F07";
    public static final String ApplicationVersionICC     = "9F08";
    public static final String ApplicationVersionTerminal= "9F09";

    public static final String IssuerActionCodeDefault   = "9F0D";
    public static final String IssuerActionCodeDenial    = "9F0E";
    public static final String IssuerActionCodeOnline    = "9F0F";
    public static final String IssuerApplicationData     = "9F10";

    public static final String ApplicationPreferredName  = "9F12";
    public static final String MerchantCategoryCode      = "9F15";
    public static final String MerchantIdentifier        = "9F16";
    public static final String TerminalCountryCode       = "9F1A";
    public static final String TerminalIdentification   = "9F1C";
    public static final String IFDSN                     = "9F1E"; // IFD Serial Number
    public static final String Track1Discretionary       = "9F1F";

    public static final String TransactionTime           = "9F21";
    public static final String ApplicationCryptogram     = "9F26";
    public static final String CryptogramInfoData        = "9F27";
    public static final String TerminalCapabilities      = "9F33";
    public static final String CVMResults                = "9F34";
    public static final String TerminalType              = "9F35";
    public static final String ATC                       = "9F36"; // Application Transaction Counter
    public static final String UnpredictableNumber       = "9F37";
    public static final String POSEntryMode              = "9F39";
    public static final String AdditionalTerminalCaps    = "9F40";
    public static final String TransactionSequenceCounter= "9F41";
    public static final String ICCDynamicNumber          = "9F4C";
    public static final String MerchantNameLocation      = "9F4E";
    public static final String TransactionCategoryCode   = "9F53";

    // -------------------------------
    // Templates / Structural
    // -------------------------------
    public static final String TemplateFCI               = "6F";
    public static final String TemplateProprietary       = "70";
    public static final String TemplateFCIProp           = "A5";
    public static final String TemplateRspMsg1           = "80";
    public static final String TemplateRspMsg2           = "77";

    // -------------------------------
    // Proprietary (issuer-reserved)
    // -------------------------------
    public static final String ProprietaryC0             = "C0";
    public static final String ProprietaryC1             = "C1";
    public static final String ProprietaryC2             = "C2";
    public static final String ProprietaryC4             = "C4";
    public static final String ProprietaryC7             = "C7";

    // -------------------------------
    // Miscellaneous
    // -------------------------------
    public static final String NullPadding               = "00";

    private static final Map<String, String> TAG_DICTIONARY = new Hashtable<>();

    static {
        // Structural
        TAG_DICTIONARY.put(TemplateFCI, "File Control Information (FCI) Template");
        TAG_DICTIONARY.put(TemplateProprietary, "EMV Proprietary Template");
        TAG_DICTIONARY.put(TemplateFCIProp, "FCI Proprietary Template");
        TAG_DICTIONARY.put(TemplateRspMsg1, "Response Message Template Format 1");
        TAG_DICTIONARY.put(TemplateRspMsg2, "Response Message Template Format 2");

        // Application data
        TAG_DICTIONARY.put(AIDCard, "Application Identifier (AID) – Card");
        TAG_DICTIONARY.put(ApplicationLabel, "Application Label");
        TAG_DICTIONARY.put(Track2EquivalentData, "Track 2 Equivalent Data");
        TAG_DICTIONARY.put(PAN, "Application Primary Account Number (PAN)");
        TAG_DICTIONARY.put(CardholderName, "Cardholder Name");
        TAG_DICTIONARY.put(ApplicationExpiryDate, "Application Expiry Date");
        TAG_DICTIONARY.put(ApplicationEffectiveDate, "Application Effective Date");
        TAG_DICTIONARY.put(IssuerCountryCode, "Issuer Country Code");
        TAG_DICTIONARY.put(TransactionCurrencyCode,"Transaction Currency Code");
        TAG_DICTIONARY.put(ServiceCode, "Service Code");
        TAG_DICTIONARY.put(PANSequenceNumber, "PAN Sequence Number");

        // Crypto / issuer
        TAG_DICTIONARY.put(ApplicationInterchangeProfile, "Application Interchange Profile");
        TAG_DICTIONARY.put(DedicatedFileName, "Dedicated File (DF) Name");
        TAG_DICTIONARY.put(CVMList, "CVM List");
        TAG_DICTIONARY.put(TVR, "Terminal Verification Results");
        TAG_DICTIONARY.put(TransactionDate, "Transaction Date");
        TAG_DICTIONARY.put(TransactionStatusInfo, "Transaction Status Information");
        TAG_DICTIONARY.put(TransactionType, "Transaction Type");
        TAG_DICTIONARY.put(IssuerActionCodeDefault, "Issuer Action Code - Default");
        TAG_DICTIONARY.put(IssuerActionCodeDenial, "Issuer Action Code - Denial");
        TAG_DICTIONARY.put(IssuerActionCodeOnline, "Issuer Action Code - Online");
        TAG_DICTIONARY.put(IssuerApplicationData, "Issuer Application Data");

        // Amounts
        TAG_DICTIONARY.put(AmountAuthorised, "Amount, Authorised");
        TAG_DICTIONARY.put(AmountOther, "Amount, Other");
        TAG_DICTIONARY.put(ApplicationIdentifier, "Application Identifier (AID)");
        TAG_DICTIONARY.put(ApplicationUsageControl, "Application Usage Control");
        TAG_DICTIONARY.put(ApplicationVersionICC, "Application Version Number (ICC)");
        TAG_DICTIONARY.put(ApplicationVersionTerminal, "Application Version Number (Terminal)");

        // Merchant / Terminal
        TAG_DICTIONARY.put(ApplicationPreferredName, "Application Preferred Name");
        TAG_DICTIONARY.put(MerchantCategoryCode, "Merchant Category Code");
        TAG_DICTIONARY.put(MerchantIdentifier, "Merchant Identifier");
        TAG_DICTIONARY.put(TerminalCountryCode, "Terminal Country Code");
        TAG_DICTIONARY.put(TerminalIdentification, "Terminal Identification");
        TAG_DICTIONARY.put(IFDSN, "Interface Device (IFD) Serial Number");
        TAG_DICTIONARY.put(Track1Discretionary, "Track 1 Discretionary Data");
        TAG_DICTIONARY.put(TransactionTime, "Transaction Time");

        // Crypto results
        TAG_DICTIONARY.put(ApplicationCryptogram, "Application Cryptogram");
        TAG_DICTIONARY.put(CryptogramInfoData, "Cryptogram Information Data");

        // Terminal details
        TAG_DICTIONARY.put(TerminalCapabilities, "Terminal Capabilities");
        TAG_DICTIONARY.put(CVMResults, "CVM Results");
        TAG_DICTIONARY.put(TerminalType, "Terminal Type");
        TAG_DICTIONARY.put(ATC, "Application Transaction Counter (ATC)");
        TAG_DICTIONARY.put(UnpredictableNumber, "Unpredictable Number");
        TAG_DICTIONARY.put(POSEntryMode, "POS Entry Mode");
        TAG_DICTIONARY.put(AdditionalTerminalCaps, "Additional Terminal Capabilities");
        TAG_DICTIONARY.put(TransactionSequenceCounter, "Transaction Sequence Counter");
        TAG_DICTIONARY.put(ICCDynamicNumber, "ICC Dynamic Number");
        TAG_DICTIONARY.put(MerchantNameLocation, "Merchant Name & Location");
        TAG_DICTIONARY.put(TransactionCategoryCode, "Transaction Category Code");

        // Proprietary
        TAG_DICTIONARY.put(ProprietaryC0, "Issuer Proprietary Data");
        TAG_DICTIONARY.put(ProprietaryC1, "Issuer Proprietary Data");
        TAG_DICTIONARY.put(ProprietaryC2, "Issuer Authentication Data (ARQC Blob)");
        TAG_DICTIONARY.put(ProprietaryC4, "Issuer Script / Dynamic Data");
        TAG_DICTIONARY.put(ProprietaryC7, "Issuer Proprietary Cryptogram");

        // Null
        TAG_DICTIONARY.put(NullPadding, "Null / Padding");
    }

    public static String decodeTag(String tag) {
        return TAG_DICTIONARY.getOrDefault(tag.toUpperCase(), "Unknown Tag");
    }
}
