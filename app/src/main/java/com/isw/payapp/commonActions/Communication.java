package  com.isw.payapp.commonActions;

import android.os.AsyncTask;
import android.util.Log;


import com.isw.payapp.utils.RSAUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Communication {

    private static final String TAG = Communication.class.getSimpleName();
    private static final String TAGG = "HTTP_GET";
    private String data;
    // Log.e( TAG, "IOException: " + e.getMessage() );
    public static final int CONNECTON_TIMEOUT_MILLISECONDS = 60000;
//    RSAUtil security_utils = new RSAUtil();

    public static String token;

    public Communication() {
    }


    public void getRequest(){

    }

    public void postRequest(String postUrl, Map<String, String> postData,String payload){
        // Create a HashMap for the POST data
//        Map<String, String> postData = new HashMap<>();
//        postData.put("param1", "value1");
//        postData.put("param2", "value2");

        // Make an HTTPS POST request
        new HttpPostTask().execute(postUrl, payload);
    }

    public void setResponseData(String data){
        this.data = data;
    }

    public String getResponseData(){
        return data;
    }

    public static class HttpPostTask extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... params) {
            String postUrl = (String) params[0];
            String xmlPayload = (String) params[1]; // Use xmlPayload instead of postData
            String contentType = "application/xml"; // Set the Content-Type header
            String userAgent  = "kimono";

            try {
                // Create a custom TrustManager to trust all certificates
                TrustManager[] trustAllCertificates = {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // Create a custom HostnameVerifier to trust all hosts
                HostnameVerifier trustAllHosts = (hostname, sslSession) -> true;

                // Set the custom TrustManager and HostnameVerifier
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(trustAllHosts);

                // Create a URL object with the given URL
                URL url = new URL(postUrl);

                // Open a connection to the URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set up the request method and other properties
                connection.setRequestMethod("POST");
                connection.setReadTimeout(10000 /* milliseconds */);
                connection.setConnectTimeout(15000 /* milliseconds */);
                connection.setDoOutput(true);

                // Set the Content-Type header
                connection.setRequestProperty("Content-Type", contentType);
                connection.setRequestProperty("User-Agent",userAgent);

                // Set the content length of the XML payload
                byte[] postDataBytes = xmlPayload.getBytes("UTF-8");
                connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

                // Write the XML payload to the output stream
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.write(postDataBytes);
                }

                // Connect to the server
                connection.connect();

                // Check for a successful response code (HTTP 200 OK)
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response data
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        //data = stringBuilder.toString();
                        return stringBuilder.toString();
                    }
                } else {
                    Log.e(TAG, "HTTP POST request failed with response code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error while making HTTP POST request: " + e.getMessage());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
                Log.e(TAG, "SSL Error: " + e.getMessage());
            }

            return null;
        }

//        @Override
//        protected void onPostExecute(String result) {
//            if (result != null) {
//                // Handle the response data here
//                //Log.d(TAG, "Response Data: " + result);
//                //setResponseData(result);
////                handleResult(result);
//            } else {
//                // Handle the error or no data case here
//                Log.e(TAG, "HTTP POST request failed or returned no data.");
//            }
//        }
    }
//    private void handleResult(String result) {
//        // Perform actions with the result, e.g., update UI, call other methods, etc.
//        //setResponseData(result);
//    }

    public String KimonoPost(String ...params){
        String postUrl = (String) params[0];
        String xmlPayload = (String) params[1]; // Use xmlPayload instead of postData
        String contentType = "application/xml"; // Set the Content-Type header
        String userAgent  = "kimono";

        try {
            // Create a custom TrustManager to trust all certificates
            TrustManager[] trustAllCertificates = {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Create a custom HostnameVerifier to trust all hosts
            HostnameVerifier trustAllHosts = (hostname, sslSession) -> true;

            // Set the custom TrustManager and HostnameVerifier
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(trustAllHosts);

            // Create a URL object with the given URL
            URL url = new URL(postUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set up the request method and other properties
            connection.setRequestMethod("POST");
            connection.setReadTimeout(100000 /* milliseconds */);
            connection.setConnectTimeout(150000 /* milliseconds */);
            connection.setDoOutput(true);

            // Set the Content-Type header
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent",userAgent);

            // Set the content length of the XML payload
            byte[] postDataBytes = xmlPayload.getBytes("UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

            // Write the XML payload to the output stream
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(postDataBytes);
            }

            // Connect to the server
            connection.connect();

            // Check for a successful response code (HTTP 200 OK)
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response data
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    //data = stringBuilder.toString();
                    return stringBuilder.toString();
                }
            } else {
                Log.e(TAG, "HTTP POST request failed with response code: " + responseCode);
            }
        }
        catch (SocketTimeoutException e){
            return "<purchaseResponse xmlns:ns5=\"http://interswitchng.com\" xmlns:ns2=\"xmlns:m0=http://schemas.compassplus.com/two/1.0/fimi_types.xsd\" xmlns:ns4=\"http://ws.waei.uba.com/\" xmlns:ns3=\"http://tempuri.org/ns.xsd\"><description>Read timed out</description><field39>96</field39><referenceNumber>000014186219</referenceNumber><stan>4001</stan><transactionChannelName>MY_NEW_PB</transactionChannelName><wasReceive>true</wasReceive><wasSend>true</wasSend></purchaseResponse>";
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error while making HTTP POST request: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            Log.e(TAG, "SSL Error: " + e.getMessage());
        }
        return null;
    }

}
