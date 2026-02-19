package com.isw.payapp.devices;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.isw.payapp.BuildConfig;
//import com.isw.payapp.devices.dspread.DSpreadPrinter;
//import com.isw.payapp.devices.dspread.DSpreadPrinter;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.model.TransactionData;
//import com.isw.payapp.devices.telpo.TelpoPrinter;

import java.lang.reflect.Constructor;

public class DeviceFactory {

    //Printer
    public static IPrinterProcessor createPrinter(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        String posBrand = BuildConfig.POS_BRAND;

        try {
            Class<?> printerClass;

            switch (posBrand) {
                case "TELPO":
                    printerClass = Class.forName("com.isw.payapp.devices.telpo.TelpoPrinter");
                    break;
                case "NEW_TELPO":
                    printerClass = Class.forName("com.isw.payapp.devices.newtelpo.NewTelpoPrinterAdapter");
                    break;
                case "DSPREAD":
                    printerClass = Class.forName("com.isw.payapp.devices.dspread.DSpreadPrinterAdapter");
                    break;
                case "FEITIAN":
                    printerClass = Class.forName("com.isw.payapp.devices.feitian.FeitianPrinterService");
                    break;
                default:
                    throw new UnsupportedOperationException("Device not supported: " + posBrand);
            }

            Constructor<?> constructor = printerClass.getConstructor(Context.class);
            return (IPrinterProcessor) constructor.newInstance(context);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("POS printer class not found for brand: " + posBrand, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create printer instance", e);
        }
    }

    //Card Reader
    public static IEmvProcessor createEmvFunc( Activity activity, TransactionData transactionData, EmvServiceCallback callback){
        String posBrand = BuildConfig.POS_BRAND;
        System.out.println("createCardReaderFunc : "+ posBrand+"||"+transactionData.getAmount());
        try {
            Class <?> cardReaderClass = null;
            switch (posBrand){
                case "TELPO":
                    cardReaderClass = Class.forName("com.isw.payapp.devices.telpo.TelpoEmvService");
                    break;

                case "NEW_TELPO":
                    cardReaderClass = Class.forName("com.isw.payapp.devices.newtelpo.NewTelpoEmvService");
                    break;
                case"DSPREAD":
                    cardReaderClass = Class.forName("com.isw.payapp.devices.dspread.DSpreadEmvService");
                    break;

                case"FEITIAN":
                    cardReaderClass = Class.forName("com.isw.payapp.devices.feitian.FeitianEmvService");
                    break;
                default:
                    throw new UnsupportedOperationException("Device not supported: " + posBrand);
            }
            Constructor<?> constructor = cardReaderClass.getConstructor(Activity.class,TransactionData.class, EmvServiceCallback.class);
            return (IEmvProcessor) constructor.newInstance( activity,transactionData, callback);
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("POS printer class not found for brand: " + posBrand, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create  Emv Service instance", e);
        }
    }

    /// PIN PAD
    public static IPinPadProcessor createPinPad(Context context){
        String posBrand = BuildConfig.POS_BRAND;
        Log.i("Pos-Brand",posBrand);
        try {
            Class <?> pinpadClass = null;
            switch (posBrand){
                case "TELPO":
                    pinpadClass = Class.forName("com.isw.payapp.devices.telpo.TelpoPinpad");
                    break;
                case "NEW_TELPO":
                    pinpadClass = Class.forName("com.isw.payapp.devices.newtelpo.NewTelpoPinPadService");
                    break;
                case"DSPREAD":
                    pinpadClass = Class.forName("com.isw.payapp.devices.dspread.DSpreadPinPadService");
                    break;
                case "FEITIAN":
                    pinpadClass = Class.forName("com.isw.payapp.devices.feitian.FeitianPinpadService");

                    break;
            }
            Constructor<?> constructor = pinpadClass.getConstructor(Context.class);
            return (IPinPadProcessor) constructor.newInstance(context);
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("POS PIN PAD class not found for brand: " + posBrand, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PinPad instance", e);
        }
    }
}

