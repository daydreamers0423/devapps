package in.daydreamers.devapps;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UpdateWorker  extends Worker {

    private static final String SCREEN_ANALYTICS = "SCREEN_ANALYTICS_DEVAPPS";
    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "/loganalytics";

    public native String getServiceUrl();

    Context context;
    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = context.getSharedPreferences(SCREEN_ANALYTICS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        try {
            callCloudFunction(gson.fromJson(prefs.getString("timeline",""), HashMap.class),getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS);
        } catch (IOException e) {
            return Result.failure();
        }
        return Result.success();
    }

    public static void callCloudFunction(@NonNull Map data, @NonNull String url) throws IOException {
        // Create an HTTP transport
        HttpTransport transport = new NetHttpTransport();

        // Create a request factory
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        // Prepare the JSON payload
        JsonHttpContent content = new JsonHttpContent(new GsonFactory(),
                data);

        // Create the POST request
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url), content);

        // Execute the request
        HttpResponse response = request.execute();

        // Handle the response
        if (response.isSuccessStatusCode()) {
            String responseBody = response.parseAsString();
            System.out.println("Response: " + responseBody);
        } else {
            System.err.println();
        }
    }
}
