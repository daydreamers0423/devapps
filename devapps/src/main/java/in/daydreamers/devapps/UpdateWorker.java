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
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateWorker  extends Worker {

    private static final String SCREEN_ANALYTICS = "SCREEN_ANALYTICS_DEVAPPS";
    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "/loganalytics";

    private ExecutorService executorService;

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

        if(prefs.getBoolean("dirty",false)) {
            try {
                requestPlayIntegrityToken(context,prefs);
                //callCloudFunction(gson.fromJson(prefs.getString("timeline", ""), HashMap.class), Objects.requireNonNull(prefs.getLong("usage", 0L)), getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS, prefs.getString("referer", ""));

            } catch (Exception e) {

                return Result.failure();
            }


            return Result.success();
        }

        return Result.success();
    }
    private String getNonce() {


        HttpTransport transport = new NetHttpTransport();


        // Create a request factory
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        // Create the POST request
        HttpRequest request = null;
        try {
            request = requestFactory.buildGetRequest(new GenericUrl(getServiceUrl() +"/getNonce"));

            // Execute the request
            HttpResponse response = request.execute();
            Log.i("DevApps","after getnounce");
            String rawJson = response.parseAsString();
            Log.d("DevApps", "Raw JSON: " + rawJson);

// Re-parse using JSON parser
            GenericJson json = new GsonFactory()
                    .createJsonParser(rawJson)
                    .parseAndClose(GenericJson.class);

// Check for null before accessing "nonce"
            if (json != null && json.containsKey("nonce")) {
                String nonce = (String) json.get("nonce");
                Log.d("DevApps", "Nonce received: " + nonce);
                return nonce;
            } else {
                Log.e("DevApps", "JSON is null or does not contain 'nonce'");
                return "";
            }
        } catch (IOException e) {
            Log.e("Devapps Error",e.toString());
        }catch (Exception ex)
        {
            Log.e("Devapps Error",ex.toString());
        }
        return "";
    }
    public void requestPlayIntegrityToken(Context context,final SharedPreferences prefs) {
        Log.i("DevApps::","requestPlayIntegrityToken");

        executorService =  executorService == null ? Executors.newSingleThreadExecutor():executorService;
        IntegrityManager integrityManager = IntegrityManagerFactory.create(context);
        String nonce = getNonce();
        Log.i("DevApps::","nonce="+nonce);
        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build();

        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
                    @Override
                    public void onSuccess(IntegrityTokenResponse response) {
                        Log.i("DevApps::","token="+response.token());
                        Log.i("DevApps::","nonce="+nonce);

                        Gson gson = new Gson();
                        try {
                            HashMap<String,Object> usage = gson.fromJson(prefs.getString("usage","{}"),HashMap.class);
//                            Long[] totalUsage = new Long[1];
//                            totalUsage[0]= 0L;
//                            if(!usage.isEmpty())
//                            {
//                                for(Object val :usage.values())
//                                {
//                                    totalUsage[0] += ((Number)val).longValue();
//                                }
//
//                            }
                            executorService.execute(()-> {
                                try {
                                    Log.i("DevApps","requestPlayIntegrityToken:executorService");
                                    callCloudFunction(gson.fromJson(prefs.getString("timeline",""), HashMap.class), usage, getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS,prefs.getString("referer",""),response.token(),nonce);
                                } catch (Exception e) {
                                    Log.e("Error",e.toString());
                                }
                            });

                        } catch (Exception e) {
                            Log.e("Error3333",e.toString());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure( Exception e) {
                        Log.e("DevApps","integrity failed");
                        Log.e("Error444",e.toString());
                    }
                });
    }

    public  void callCloudFunction(@NonNull Map data, @NonNull  Map usage , @NonNull String url, String refId, String token, String nonce) throws IOException {
        // Create an HTTP transport
        HttpTransport transport = new NetHttpTransport();
        data.put("usage",usage);
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
        SharedPreferences prefs = context.getSharedPreferences(SCREEN_ANALYTICS, Context.MODE_PRIVATE);
        SharedPreferences.Editor es = prefs.edit();
        // Handle the response
        if (response.isSuccessStatusCode()) {
            es.putBoolean("lastupdated", true);
            prefs.edit().putBoolean("dirty",false).apply();
            es.apply();
            String responseBody = response.parseAsString();
            System.out.println("Response: " + responseBody);
        } else {
            es.putBoolean("lastupdated", false);
            es.apply();
            System.err.println();
        }
    }
}
