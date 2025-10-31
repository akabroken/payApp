package com.isw.payapp.commonActions;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class TerminalReadConfig {

    private String param;
    private String path;
    private Context context;
    private String fileName;

    //String picturePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bluetooth/Interswtch_new_logo.bmp";
    public TerminalReadConfig(String param, String path, Context context) {
        this.param = param;
        this.path = path;

        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String getConfigValue() {
        String valuee = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(path));
            if (rootNode.isObject()) {
                rootNode.fields().forEachRemaining(entry -> {
                    String tag = entry.getKey();
                    JsonNode value = entry.getValue();
                });
            } else if (rootNode.isArray()) {
                for (int i = 0; i < rootNode.size(); i++) {
                    JsonNode arrayElement = rootNode.get(i);

                }
            }
        } catch (IOException e) {

        }
        return valuee;
    }

    public String getValue(String configValue) {
        String value = "";
        context = context.getApplicationContext();
        try {

            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(path);

            JsonParser parser = new JsonParser();
            JsonObject jsnoRoot = parser.parse(new InputStreamReader(inputStream)).getAsJsonObject();
            value = jsnoRoot.get(configValue).getAsString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

}
