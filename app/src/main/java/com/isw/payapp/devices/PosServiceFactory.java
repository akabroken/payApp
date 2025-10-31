package com.isw.payapp.devices;

import android.content.Context;

import com.isw.payapp.BuildConfig;
import com.isw.payapp.devices.interfaces.IPosService;

import java.lang.reflect.Constructor;

public class PosServiceFactory {

    public static IPosService createPinPadService(Context context) {
        String posBrand = BuildConfig.POS_BRAND;

        try {
            Class<?> pinPadServiceClass;

            switch (posBrand) {
                case "DSPREAD":
                    pinPadServiceClass = Class.forName("com.isw.payapp.devices.dspread.Activity.pinkeyboard.DSpreadPinPadServiceImpl");
                    break;
                case "TELPO":
                    pinPadServiceClass = Class.forName("com.isw.payapp.devices.telpo.TelpoPinPadServiceImpl");
                    break;
                case "FEITIAN":
                    pinPadServiceClass = Class.forName("com.isw.payapp.devices.feitian.FeitianPinPadServiceImpl");
                    break;
                case "PAX":
                    pinPadServiceClass = Class.forName("com.isw.payapp.devices.pax.PaxPinPadServiceImpl");
                    break;
                case "CASTLES":
                    pinPadServiceClass = Class.forName("com.isw.payapp.devices.castles.CastlesPinPadServiceImpl");
                    break;
                default:
                    throw new UnsupportedOperationException("POS brand not supported: " + posBrand);
            }

            Constructor<?> constructor = pinPadServiceClass.getConstructor(Context.class);
            return (IPosService) constructor.newInstance(context);

        } catch (ClassNotFoundException e) {
            // Instead of throwing exception, return a fallback implementation
            return createFallbackPinPadService(context, posBrand);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PIN pad service instance for brand: " + posBrand, e);
        }
    }

    // Fallback implementation when class is not found
    private static IPosService createFallbackPinPadService(Context context, String posBrand) {
        // You can return a default implementation or log a warning
        System.err.println("PIN pad service class not found for brand: " + posBrand + ", using fallback implementation");

        // Return a simple fallback implementation
        return new FallbackPinPadServiceImpl(context);
    }

    // You can also create other service factories here
    public static IPosService createPosService(Context context) {
        // Similar pattern for other POS services
        return createPinPadService(context); // For now, reuse the same instance
    }

    // Fallback implementation when the variant-specific class is not found
    private static class FallbackPinPadServiceImpl implements IPosService {
        private Context context;

        public FallbackPinPadServiceImpl(Context context) {
            this.context = context;
        }

        @Override
        public void initialize() {
            // Fallback initialization
        }

        @Override
        public String getCvmKeyList() {
            return "0123456789"; // Default key list
        }

        @Override
        public String convertHexToString(String hexData) {
            // Simple hex to string conversion
            try {
                StringBuilder output = new StringBuilder();
                for (int i = 0; i < hexData.length(); i += 2) {
                    String str = hexData.substring(i, i + 2);
                    output.append((char) Integer.parseInt(str, 16));
                }
                return output.toString();
            } catch (Exception e) {
                return hexData;
            }
        }

        @Override
        public String encryptPinData(String pinBlock, String cardNumber) {
            return pinBlock; // No encryption in fallback
        }

        @Override
        public boolean isDeviceConnected() {
            return false; // Assume not connected in fallback
        }

        @Override
        public void processPinEntry(String pinData) {
            // No operation in fallback
        }

        // Implement other required methods from IPosService interface
        // Add any other methods required by your IPosService interface
    }
}