package in.daydreamers.devapps;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
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
import java.util.Objects;
import java.util.UUID;

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
        SharedPreferences.Editor es = prefs.edit();
        if(prefs.getBoolean("dirty",false)) {
            try {
                requestPlayIntegrityToken(context,prefs);
                //callCloudFunction(gson.fromJson(prefs.getString("timeline", ""), HashMap.class), Objects.requireNonNull(prefs.getLong("usage", 0L)), getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS, prefs.getString("referer", ""));

            } catch (Exception e) {
                es.putBoolean("lastupdated", false);
                es.apply();
                return Result.failure();
            }

            es.putBoolean("lastupdated", true);
            prefs.edit().putBoolean("dirty",false).apply();
            es.apply();
            return Result.success();
        }

        return Result.success();
    }

    public void requestPlayIntegrityToken(Context context,final SharedPreferences prefs) {
        Log.i("DevApps::","requestPlayIntegrityToken");
        final String nonce = UUID.randomUUID().toString();

        IntegrityManager integrityManager = IntegrityManagerFactory.create(context);

        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build();

        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
                    @Override
                    public void onSuccess(IntegrityTokenResponse response) {
                        Log.i("DevApps::","token="+response.token());
                        Gson gson = new Gson();
                        try {
                            callCloudFunction(gson.fromJson(prefs.getString("timeline",""), HashMap.class), Objects.requireNonNull(prefs.getLong("usage", 0L)), getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS,prefs.getString("referer",""),response.token(),nonce);
                        } catch (IOException e) {
                            Log.e("Error3333",e.toString());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {


                    }
                });
    }

    public static void callCloudFunction(@NonNull Map data, @NonNull  Long usage , @NonNull String url, String refId, String token, String nonce) throws IOException {
        // Create an HTTP transport
        HttpTransport transport = new NetHttpTransport();
        data.put("usage",usage/1000);
        data.put("refId",refId);
        // Create a request factory
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        // Prepare the JSON payload
        JsonHttpContent content = new JsonHttpContent(new GsonFactory(),
                data);

        // Create the POST request
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url), content);
        HttpHeaders headers = request.getHeaders();
        headers.put("token",token);
        headers.put("nounce",nonce);
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
