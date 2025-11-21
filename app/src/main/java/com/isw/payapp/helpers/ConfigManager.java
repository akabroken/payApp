package com.isw.payapp.helpers;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.isw.payapp.model.TerminalConfigModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static TerminalConfigModel cachedConfig;

    public static TerminalConfigModel getConfig(Context context) {
        if (cachedConfig == null) {
            cachedConfig = loadConfig(context);
        }
        return cachedConfig;
    }

    public static void refreshConfig(Context context) {
        cachedConfig = loadConfig(context);
    }

    private static TerminalConfigModel loadConfig(Context context) {
        // Try internal storage first
        TerminalConfigModel config = loadFromInternalStorage(context);
        if (config != null) {
            return config;
        }

        // Fall back to assets
        return loadFromAssets(context);
    }

    private static TerminalConfigModel loadFromInternalStorage(Context context) {
        try (InputStream inputStream = context.openFileInput("config.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String json = stringBuilder.toString();
            Gson gson = new Gson();
            return gson.fromJson(json, TerminalConfigModel.class);

        } catch (Exception e) {
            Log.d(TAG, "No config found in internal storage");
            return null;
        }
    }

    private static TerminalConfigModel loadFromAssets(Context context) {
        try (InputStream inputStream = context.getAssets().open("config.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String json = stringBuilder.toString();
            json = json.replace("__", ""); // Remove underscores for Gson
            Gson gson = new Gson();
            return gson.fromJson(json, TerminalConfigModel.class);

        } catch (Exception e) {
            Log.e(TAG, "Error loading config from assets", e);
            return createDefaultConfig();
        }
    }

    private static TerminalConfigModel createDefaultConfig() {
        TerminalConfigModel config = new TerminalConfigModel();
        config.setBank("Sidian Bank");
        config.setMid("CBLKE0000000001");
        config.setTid("CBLKE001");
        config.setMerchantloc("Busia Branch");
        config.setAddress1("Busia Branch");
        config.setAddress2("Busia Branch");
        config.setCity("Nairobi");
        config.setState("Nairobi");
        config.setZip("");
        config.setCurrencycode("404");
        config.setPosCode("00");
        config.setMtype("7011");
        config.setTransip("apps.qa.interswitch-ke.com");
        config.setTransport("7075");
        config.setLoginurl("smarttrans.interswitch-ke.com");
        config.setLoginport("81");
        config.setKeysetid("000006");
        return config;
    }
}
