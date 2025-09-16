package com.isw.payapp.terminal.controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class LoginController {

    public  String sendPostRequest(String urlString, String payload) throws Exception {
        // Create a URL object for the HTTPS endpoint
        URL url = new URL(urlString);

        // Open a connection to the URL
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        try {
            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Enable input and output streams
            connection.setDoOutput(true);

            // Set request headers
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");

            // Write the XML payload to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response from the server
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } finally {
            // Disconnect the connection
            connection.disconnect();
        }
    }

    public String sendPostRequest(String urlString, String payload, String test) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Submit a Callable task to the executor
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    // Create a URL object
                    URL url = new URL(urlString);

                    // Open a connection
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/xml");
                    connection.setDoOutput(true);

                    // Write the payload
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = payload.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    // Read the response
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream(), "utf-8"))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            return response.toString(); // Return the server's response
                        }
                    } else {
                        return "Error: " + responseCode;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Exception: " + e.getMessage();
                }
            }
        });

        // Retrieve the result from the Future
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
}
