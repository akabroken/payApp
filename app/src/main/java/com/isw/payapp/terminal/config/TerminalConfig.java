package com.isw.payapp.terminal.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TerminalConfig {

    private Map<String,Object>conf;

    private  final String PREFS_NAME = "MyPrefs";
    private static final String CONFIG_FILE = "config.json";
    private  final String JSON_FILE_NAME = "config.json";
    private  final String TAG = "JsonConfigManager";

    public TerminalConfig(){

    }

    public Map<String,Object> readConfig(String jsonConfig){
        conf = new HashMap<>();
        return conf;
    }

    // Save JSON data to SharedPreferences
    public void saveTerminalConfigJsonData(Context context, JSONObject jsonData) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("jsonConfig", jsonData.toString());
        editor.apply();
    }

    // Read JSON data from SharedPreferences
    public  JSONObject readTerminalConfigJsonData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonConfigString = prefs.getString("jsonConfig", "{}");

        try {
            return new JSONObject(jsonConfigString);
        } catch (JSONException e) {
            Log.e(TAG, "Error reading JSON data", e);
            return new JSONObject();
        }
    }

    // Save JSON data to a file
    public  void saveTerminalConfigJsonToFile(Context context, JSONObject jsonData) {
        File externalStorage = Environment.getExternalStorageDirectory();
        File jsonFile = new File(externalStorage, JSON_FILE_NAME);

        try (FileWriter fileWriter = new FileWriter(jsonFile)) {
            fileWriter.write(jsonData.toString());
            Log.i(TAG, "JSON data saved to file: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing JSON data to file", e);
        }
    }

    // Read JSON data from a file
    public  JSONObject readTerminalConfigJsonFromFile(Context context) {
        File externalStorage = Environment.getExternalStorageDirectory();
        File jsonFile = new File(externalStorage, JSON_FILE_NAME);

        try {
            // Read the entire file into a string
            StringBuilder text = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(jsonFile);
            while (scanner.hasNextLine()) {
                text.append(scanner.nextLine());
            }

            // Parse the string into a JSONObject
            return new JSONObject(text.toString());
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error reading JSON data from file", e);
            return new JSONObject();
        }
    }

    public  String loadTerminalDataFromJson(Context context, String getStringValue) {
        String terminalConfig = "";

        try {
            // Load the JSON file from the assets folder

            InputStream inputStream = context.getAssets().open(CONFIG_FILE);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            // Convert the JSON file content to a string
            String jsonString = new String(buffer, "UTF-8");

            // Parse the JSON string into a JSONObject
            JSONObject jsonObject = new JSONObject(jsonString);
            terminalConfig = jsonObject.getString(getStringValue);
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
        return terminalConfig;
    }

}
