package com.isw.payapp.devices.services;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Retrofit interface defining the API endpoints for the application.
 * This interface declares the possible network operations (like key download)
 * without implementing them. Retrofit generates the implementation at runtime.
 */
public interface IApiServices {

    /**
     * Sends an XML payload to the key management service to download terminal keys.
     * This is a generic method that sends a String and expects a String response.
     *
     * @param xmlRequest The full XML request body as a String.
     * @return A Retrofit {@link Call} object that can be executed to get a String response.
     */
    @POST("kmw/kimonoservice/kenya")
    @Headers({
            "Content-Type: application/xml", // Explicitly sets the Content-Type header
            "Accept: application/xml"        // Explicitly tells the server we expect XML back
    })
    Call<String> downloadKeys(@Body String xmlRequest);

    /**
     * (ALTERNATIVE VERSION - More Flexible)
     * Sends an XML payload using OkHttp's RequestBody.
     * This is useful if you need more control over the content type or want to send raw bytes.
     *
     * @param xmlRequestBody The request body, explicitly defined as XML.
     * @return A Retrofit {@link Call} object that can be executed to get a String response.
     */
    @POST("kmw/kimonoservice/kenya")
    Call<String> downloadKeysWithRequestBody(@Body RequestBody xmlRequestBody);

    /**
     * (ALTERNATIVE VERSION - For Binary Data Response)
     * If the server response might not be a simple string (e.g., binary data in the XML),
     * it's safer to use Retrofit's ResponseBody and handle the parsing manually.
     *
     * @param xmlRequest The full XML request body as a String.
     * @return A Retrofit {@link Call} object that can be executed to get a raw {@link ResponseBody}.
     */
    @POST("kmw/kimonoservice/kenya")
    @Headers("Content-Type: application/xml")
    Call<ResponseBody> downloadKeysRawResponse(@Body String xmlRequest);
}
