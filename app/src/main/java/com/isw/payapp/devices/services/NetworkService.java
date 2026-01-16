package com.isw.payapp.devices.services;

import android.content.Context;
import android.util.Log;


import com.isw.payapp.BuildConfig;
import com.isw.payapp.utils.XMLUtils;

import okhttp3.tls.HandshakeCertificates;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class NetworkService {

    private static NetworkService instance;

    private final Context context;
    private  final IApiServices apiService;
    private static final String TAG = "NetworkService";
    private  final String baseUrl;
    private OkHttpClient httpClient;

    // Private constructor to create the Retrofit instance

    private NetworkService(Context context, String baseUrl) {
        // Use application context to prevent memory leaks
        this.context = context.getApplicationContext();
        this.baseUrl = normalizeBaseUrl(baseUrl);
//        if (instance == null) {
//            // Pass the custom client
//            instance = new NetworkService(httpClient);
//        }

        OkHttpClient httpClient;

        if (BuildConfig.DEBUG) {
            // Use the unsafe client for debugging/QA - THIS BYPASSES SSL VALIDATION
            httpClient = getUnsafeOkHttpClient();
            Log.w(TAG, "Using UNSAFE SSL configuration for debugging. DO NOT USE IN PRODUCTION.");
        } else {
            // Use safe client for production
            httpClient = getCustomCertOkHttpClient(context);

        }

        // Build the Retrofit instance with the chosen client
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        apiService = retrofit.create(IApiServices.class);
    }

    private OkHttpClient getCustomCertOkHttpClient(Context context) {
        try {
            // 1. Load your PEM certificate
            InputStream certInputStream = context.getAssets().open("kimono.pem");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate customCertificate = (X509Certificate) cf.generateCertificate(certInputStream);
            certInputStream.close();

            // 2. Create a KeyStore with your certificate
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null); // Initialize empty
            keyStore.setCertificateEntry("kimono", customCertificate);

            // 3. Get system default TrustManager
            TrustManagerFactory systemTmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            systemTmf.init((KeyStore) null); // Initialize with system defaults

            // 4. Create TrustManager for custom certificate
            TrustManagerFactory customTmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            customTmf.init(keyStore);

            // 5. Create composite TrustManager
            X509TrustManager systemTrustManager = (X509TrustManager) systemTmf.getTrustManagers()[0];
            X509TrustManager customTrustManager = (X509TrustManager) customTmf.getTrustManagers()[0];

            X509TrustManager compositeTrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    systemTrustManager.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    try {
                        // First try system trust manager
                        systemTrustManager.checkServerTrusted(chain, authType);
                    } catch (CertificateException systemException) {
                        try {
                            // If system fails, try custom certificate
                            customTrustManager.checkServerTrusted(chain, authType);
                        } catch (CertificateException customException) {
                            // Combine error messages
                            throw new CertificateException(
                                    "Server certificate not trusted by system or custom certificate. " +
                                            "System error: " + systemException.getMessage() +
                                            ", Custom error: " + customException.getMessage()
                            );
                        }
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    // Combine issuers from both trust managers
                    List<X509Certificate> issuers = new ArrayList<>();
                    issuers.addAll(Arrays.asList(systemTrustManager.getAcceptedIssuers()));
                    issuers.addAll(Arrays.asList(customTrustManager.getAcceptedIssuers()));
                    return issuers.toArray(new X509Certificate[0]);
                }
            };

            // 6. Create SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[]{compositeTrustManager}, null);

            // 7. Build OkHttpClient
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), compositeTrustManager)
                    .addInterceptor(new ResponseDebugInterceptor())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create custom certificate client. Falling back to standard trust.", e);
            // Fallback to standard client (will NOT trust your custom certificate)
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
    }
    private OkHttpClient getCustomCertOkHttpClient(Context context, Object obj) {
        try {
            // 1. Load your PEM certificate from the raw resources
            InputStream certInputStream = context.getAssets().open("kimono.pem");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate customCertificate = (X509Certificate) cf.generateCertificate(certInputStream);
            certInputStream.close();

            // 2. Build the HandshakeCertificates object
            //    This adds your certificate AND keeps the platform's trusted CAs [citation:4]
            HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
                    .addPlatformTrustedCertificates() // Trust standard CAs
                    .addTrustedCertificate(customCertificate) // Also trust your specific cert
                    .build();

            // 3. Build and return the OkHttpClient
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(
                            handshakeCertificates.sslSocketFactory(),
                            handshakeCertificates.trustManager()
                    )
                    // Hostname verification remains ENABLED and secure
                    .addInterceptor(new ResponseDebugInterceptor())
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to create custom certificate client. Falling back to standard trust.", e);
            // Fallback: return a client that only trusts platform CAs
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
    }

    // Initialize the singleton instance
    public static synchronized void initialize(Context context, String baseUrl) {
        if (instance == null) {
            instance = new NetworkService(context, baseUrl);
        } else {
            // Optional: Log if trying to reinitialize with different baseUrl
            if (!instance.baseUrl.equals(baseUrl)) {
                Log.w(TAG, "Reinitializing NetworkService with different base URL");
                instance = new NetworkService(context, baseUrl);
            }
        }
    }

    // Get the singleton instance (must be initialized first)
    public static NetworkService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkService must be initialized first! Call NetworkService.initialize() first.");
        }
        return instance;
    }

    // Bypass SSL - Unsafe OkHttpClient method
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Trusting all certificates
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true); // Bypass hostname verification

            // Add timeouts
            builder.connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // Add logging interceptor for debug builds
            if (!BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logging);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create unsafe OkHttpClient", e);
        }
    }

    // Add a retry interceptor to your OkHttpClient
    private static class RetryInterceptor implements Interceptor {
        private static final int MAX_RETRIES = 3;

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            okhttp3.Response response = null;
            IOException exception = null;

            // Try the request up to MAX_RETRIES times
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }
                } catch (IOException e) {
                    exception = e;
                    if (attempt == MAX_RETRIES) {
                        break;
                    }
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry", ie);
                    }
                }
            }

            if (exception != null) {
                throw exception;
            }

            if (response != null) {
                return response;
            }

            throw new IOException("Unknown error occurred");
        }
    }

    private static class ResponseDebugInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            okhttp3.Response response = null;

            try {
                response = chain.proceed(request);

                // Log response details
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());
                Log.d(TAG, "Content length: " + (response.body() != null ? response.body().contentLength() : "null"));

                // Peek at the response body without consuming it
                if (response.body() != null) {
                    ResponseBody responseBody = response.body();
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE);
                    Buffer buffer = source.buffer();

                    String responseText = buffer.clone().readString(Charset.defaultCharset());
                    Log.d(TAG, "Response preview: " + responseText.substring(0, Math.min(responseText.length(), 500)));
                }

                return response;
            } catch (IOException e) {
                Log.e(TAG, "Network error in interceptor: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Makes an asynchronous POST request to download keys.
     *
     * @param xmlRequest The XML request body.
     * @param callback   The callback to handle the response or failure.
     */
    public void downloadKeys(String xmlRequest, final NetworkCallback callback) {
        Call<String> call = apiService.downloadKeys(xmlRequest);
        executeCall(call, callback);
    }

    /**
     * Makes an asynchronous POST request with payload.
     *
     * @param xmlRequest The XML request body.
     * @param callback   The callback to handle the response or failure.
     */
    public void postPayLoad(String xmlRequest, final NetworkCallback callback) {
        Call<String> call = apiService.postPayloadString(xmlRequest);
        executeCall(call, callback);
    }

    /**
     * Makes an asynchronous GET request.
     * Note: GET requests typically don't have a request body.
     * If you need to send parameters, use query parameters instead.
     *
     * @param endpoint The endpoint to call (with query parameters if needed)
     * @param callback The callback to handle the response or failure.
     */
    public void getPayLoad(String endpoint, final NetworkCallback callback) {
        // Typically GET requests don't have a body, so we're using the string as endpoint
        // You might need to adjust your IApiServices interface
        Call<String> call = apiService.getPayloadString(endpoint);
        executeCall(call, callback);
    }

    /**
     * Generic method to execute API calls and handle responses
     */
    private void executeCall(Call<String> call, final NetworkCallback callback) {
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String tets = XMLUtils.isErrorResponse(response.body());
                Log.i(TAG,"executeCall::"+response.body()+"\n"+response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG,"executeCall::"+response.body()+"\n"+response.code()+"\nError message:::"+tets);
                    callback.onSuccess(response.body());
                } else {
                    // Handle HTTP error (e.g., 404, 500)
                    String errorMsg = "Server error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (IOException e) {
                            errorMsg += " - Could not read error body.";
                            Log.e(TAG, "Error reading error body", e);
                        }
                    } else {
                        errorMsg += " - No error body returned.";
                    }
                    Log.e(TAG, errorMsg);
                    callback.onFailure(new IOException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Handle network failures (e.g., no internet, timeout)
                String errorMsg = "Network request failed: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onFailure(new IOException(errorMsg, t));
            }
        });
    }

    public String postPayLoadSync(String xmlRequest) throws IOException {
        Call<String> call = apiService.postPayloadString(xmlRequest);
        Response<String> response = call.execute(); // This makes a synchronous call

        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG,"response:::>>>"+response.body());
            return response.body();
        } else {
            String errorMsg = "Server error: " + response.code();
            if (response.errorBody() != null) {
                try {
                    errorMsg += " - " + response.errorBody().string();
                } catch (IOException e) {
                    errorMsg += " - Could not read error body.";
                    Log.e(TAG, "Error reading error body", e);
                }
            }
            throw new IOException(errorMsg);
        }
    }

    public String postPayLoadSyncLogin(String xmlRequest) throws IOException {
        Call<String> call = apiService.doLogin(xmlRequest);
        Response<String> response = call.execute(); // This makes a synchronous call

        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG,"response:::>>>"+response.body());
            return response.body();
        } else {
            String errorMsg = "Server error: " + response.code();
            if (response.errorBody() != null) {
                try {
                    errorMsg += " - " + response.errorBody().string();
                } catch (IOException e) {
                    errorMsg += " - Could not read error body.";
                    Log.e(TAG, "Error reading error body", e);
                }
            }
            throw new IOException(errorMsg);
        }
    }

    public String getPayLoadSync(String xmlRequest) throws IOException {
        Call<String> call = apiService.postPayloadString(xmlRequest);
        Response<String> response = call.execute(); // This makes a synchronous call

        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG,"response:::>>>"+response.body());
            return response.body();
        } else {
            String errorMsg = "Server error: " + response.code();
            if (response.errorBody() != null) {
                try {
                    errorMsg += " - " + response.errorBody().string();
                } catch (IOException e) {
                    errorMsg += " - Could not read error body.";
                    Log.e(TAG, "Error reading error body", e);
                }
            }
            throw new IOException(errorMsg);
        }
    }

    // Add this method to your NetworkService class
    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL cannot be null");
        }

        // Ensure the URL has a protocol
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl; // Default to HTTPS
        }

        // Ensure the URL ends with a slash for Retrofit
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        return baseUrl;
    }

    public interface NetworkCallback {
        void onSuccess(String response);
        void onFailure(Exception e);
    }
}