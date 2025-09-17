package com.isw.payapp.terminal.controllers;

import android.content.Context;

import com.isw.payapp.devices.services.NetworkService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoginController {

    private final Context context;
    private final NetworkService networkService;

    public LoginController(Context context) {
        this.context = context.getApplicationContext();
        // Initialize NetworkService if not already initialized
        String baseUrl = "https://apps.qa.interswitch-ke.com:7075/"; // You might want to make this configurable
        NetworkService.initialize(context, baseUrl);
        this.networkService = NetworkService.getInstance();
    }

    /**
     * Sends a POST request using the NetworkService
     */
    public void sendPostRequest(String payload, final NetworkService.NetworkCallback callback) {
        networkService.downloadKeys(payload, callback);
    }

    /**
     * Synchronous version using ExecutorService for backward compatibility
     * Note: This is not recommended - prefer the async version above
     */
    public String sendPostRequest(String urlString, String payload, String test) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                final String[] result = new String[1];
                final Exception[] exception = new Exception[1];

                // Use NetworkService asynchronously and wait for result
                networkService.downloadKeys(payload, new NetworkService.NetworkCallback() {
                    @Override
                    public void onSuccess(String response) {
                        synchronized (result) {
                            result[0] = response;
                            result.notify();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        synchronized (exception) {
                            exception[0] = e;
                            exception.notify();
                        }
                    }
                });

                // Wait for the async operation to complete
                synchronized (result) {
                    if (result[0] == null && exception[0] == null) {
                        try {
                            result.wait(30000); // 30 second timeout
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return "Exception: " + e.getMessage();
                        }
                    }
                }

                if (exception[0] != null) {
                    return "Exception: " + exception[0].getMessage();
                }

                return result[0] != null ? result[0] : "Timeout: No response received";
            }
        });

        try {
            String result = future.get(); // Blocks until the task completes
            executor.shutdown();
            return result;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            executor.shutdown();
            return "Exception: " + e.getMessage();
        }
    }

    /**
     * Alternative synchronous method without the legacy signature
     */
    public String sendPostRequestSync(String payload) {
        final String[] result = new String[1];
        final Exception[] exception = new Exception[1];
        final Object lock = new Object();

        networkService.downloadKeys(payload, new NetworkService.NetworkCallback() {
            @Override
            public void onSuccess(String response) {
                synchronized (lock) {
                    result[0] = response;
                    lock.notifyAll();
                }
            }

            @Override
            public void onFailure(Exception e) {
                synchronized (lock) {
                    exception[0] = e;
                    lock.notifyAll();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(30000); // 30 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Exception: " + e.getMessage();
            }
        }

        if (exception[0] != null) {
            return "Exception: " + exception[0].getMessage();
        }

        return result[0] != null ? result[0] : "Timeout: No response received";
    }

    /**
     * Legacy method for backward compatibility - uses the old HttpURLConnection approach
     * This is kept only if you absolutely need the old behavior
     */
    @Deprecated
    public String sendPostRequestLegacy(String urlString, String payload) throws Exception {
        // This method is deprecated - you should use the NetworkService version instead
        // Keeping it only for extreme backward compatibility cases

        java.net.URL url = new java.net.URL(urlString);
        javax.net.ssl.HttpsURLConnection connection = (javax.net.ssl.HttpsURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");

            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } finally {
            connection.disconnect();
        }
    }
}