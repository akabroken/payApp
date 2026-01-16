package com.isw.payapp.utils;

public class PaddingUtils {

    /**
     * Pads a string to a fixed length of 6 with leading zeros
     * @param cardAuthRc The input string to pad
     * @return Padded string of exactly 6 characters
     */
    public static String padToSixChars(String cardAuthRc) {
        if (cardAuthRc == null) {
            return "000000";
        }

        // If input is already 6 or more characters, return the first 6
        if (cardAuthRc.length() >= 6) {
            return cardAuthRc.substring(0, 6);
        }

        // Pad with leading zeros to make exactly 6 characters
        return String.format("%6s", cardAuthRc).replace(' ', '0');
    }

    /**
     * Alternative implementation using StringBuilder
     * @param cardAuthRc The input string to pad
     * @return Padded string of exactly 6 characters
     */
    public static String padToSixCharsV2(String cardAuthRc) {
        if (cardAuthRc == null) {
            return "000000";
        }

        StringBuilder result = new StringBuilder();
        int paddingNeeded = 6 - cardAuthRc.length();

        // Add leading zeros
        for (int i = 0; i < paddingNeeded; i++) {
            result.append('0');
        }

        // Add the original string
        result.append(cardAuthRc);

        return result.toString();
    }

    /**
     * More generic version that pads to any specified length
     * @param input The string to pad
     * @param length Desired total length
     * @param padChar Character to use for padding
     * @param padLeft true to pad on left, false to pad on right
     * @return Padded string
     */
    public static String padString(String input, int length, char padChar, boolean padLeft) {
        if (input == null) {
            input = "";
        }

        if (input.length() >= length) {
            return input.substring(0, length);
        }

        StringBuilder result = new StringBuilder();
        int paddingNeeded = length - input.length();

        if (padLeft) {
            // Pad on left
            for (int i = 0; i < paddingNeeded; i++) {
                result.append(padChar);
            }
            result.append(input);
        } else {
            // Pad on right
            result.append(input);
            for (int i = 0; i < paddingNeeded; i++) {
                result.append(padChar);
            }
        }

        return result.toString();
    }

    /**
     * Specifically pads cardAuthRc to 6 characters with leading zeros
     * @param cardAuthRc The input string
     * @return Padded string
     */
    public static String padCardAuthRc(String cardAuthRc) {
        return padString(cardAuthRc, 6, '0', true);
    }

    /**
     * Example usage in your context
     */
    public static void main(String[] args) {
        // Test cases
        String[] testCases = {"1", "12", "123", "1234", "12345", "123456", "1234567", null, ""};

        System.out.println("Testing padToSixChars():");
        for (String test : testCases) {
            String result = padToSixChars(test);
            System.out.printf("Input: %-10s -> Output: %s (length: %d)%n",
                    test == null ? "null" : "\"" + test + "\"",
                    result,
                    result.length());
        }

        System.out.println("\nTesting padCardAuthRc() [Specific for your use case]:");
        for (String test : testCases) {
            String result = padCardAuthRc(test);
            System.out.printf("cardAuthRc: %-10s -> Padded: %s%n",
                    test == null ? "null" : "\"" + test + "\"",
                    result);
        }

        System.out.println("\nTesting generic padString():");
        // Pad on right as requested in your title
        String input = "123";
        String rightPadded = padString(input, 6, '0', false);
        System.out.println("Right padded: " + rightPadded);

        // Pad on left (default for cardAuthRc)
        String leftPadded = padString(input, 6, '0', true);
        System.out.println("Left padded: " + leftPadded);
    }
}