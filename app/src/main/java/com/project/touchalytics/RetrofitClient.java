package com.project.touchalytics;

import com.google.gson.JsonObject;
import com.project.touchalytics.data.Features;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Manages the Retrofit client for making API calls to the server.
 * This class provides a singleton instance of Retrofit and defines the API service interface.
 */
public class RetrofitClient {

    private static Retrofit retrofit = null;

    /**
     * Gets the singleton instance of the Retrofit client.
     * If the instance does not exist, it creates a new one with the base URL and Gson converter.
     * @return The Retrofit client instance.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://" + Constants.SERVER_BASE_URL + ":5000") // Base URL for the API
                    .addConverterFactory(GsonConverterFactory.create()) // Convert JSON to Java object
                    .build();
        }
        return retrofit;
    }

    /**
     * Interface defining the API endpoints.
     */
    public interface ApiService {
        /**
         * Sends swipe features to the server for authentication/classification.
         * @param userID The ID of the user whose features are being sent.
         * @param features The {@link Features} object containing the swipe data.
         * @return A Retrofit {@link Call} object for the API request, expecting a {@link JsonObject} response.
         */
        @POST("/authenticate/{userID}")
        Call<JsonObject> sendFeatures(@Path("userID") int userID, @Body Features features);
    }

}
