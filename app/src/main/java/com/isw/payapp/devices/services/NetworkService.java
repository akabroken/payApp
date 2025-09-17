package com.isw.payapp.devices.services;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetworkService {

    private static NetworkService instance;
    private final IApiServices apiService;
    private static final String TAG = "NetworkService";
    private final String baseUrl; // Store the base URL

    // Private constructor to create the Retrofit instance
    private NetworkService(Context context, String baseUrl) {
        this.baseUrl = baseUrl;

        OkHttpClient httpClient;

        if (!BuildConfig.DEBUG) {
            // Use the unsafe client for debugging/QA - THIS BYPASSES SSL VALIDATION
            httpClient = getUnsafeOkHttpClient();
            Log.w(TAG, "Using UNSAFE SSL configuration for debugging. DO NOT USE IN PRODUCTION.");
        } else {
            // Use safe client for production
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            // Add logging interceptor for debug builds in production config too
            if (!BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClientBuilder.addInterceptor(logging);
            }

            httpClient = httpClientBuilder.build();
        }

        // Build the Retrofit instance with the chosen client
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient) // This is the key - using the custom client
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        apiService = retrofit.create(IApiServices.class); // Fixed: Changed ApiService to IApiServices
    }

    // Initialize the singleton instance
    public static synchronized void initialize(Context context, String baseUrl) {
        if (instance == null) {
            instance = new NetworkService(context, baseUrl);
        }
    }

    // Get the singleton instance (must be initialized first)
    public static NetworkService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkService must be initialized first!");
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
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logging);
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create unsafe OkHttpClient", e);
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
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    // Handle HTTP error (e.g., 404, 500)
                    String errorMsg = "Server error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += " - Could not read error body.";
                        }
                    }
                    callback.onFailure(new IOException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Handle network failures (e.g., no internet, timeout)
                callback.onFailure(new IOException("Network request failed: " + t.getMessage(), t));
            }
        });
    }

    public interface NetworkCallback {
        void onSuccess(String response);
        void onFailure(Exception e);
    }
}