package in.daydreamers.devapps;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.GenericUrl;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;

import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;


public class UpdateWorker  extends Worker {

    public native String getScreenAnalytics();

    public native String getappid();
    public native String getapik();

    public native String getprid();
    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "loganalytics";

    private ExecutorService executorService;

    FirebaseAppCheck firebaseAppCheck;

    FirebaseApp app;

    public native String getServiceUrl();

    Context context;
    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = context.getSharedPreferences(getScreenAnalytics(), Context.MODE_PRIVATE);
        Gson gson = new Gson();
        HashMap<String,Object> usage = gson.fromJson(prefs.getString("usage","{}"),HashMap.class);
        if(prefs.getBoolean("dirty",false)) {
            try {

                callCloudFunction(gson.fromJson(prefs.getString("timeline", ""), HashMap.class), usage, getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS, prefs.getString("referer", ""));

            } catch (Exception e) {
                Log.e("DevApps:E",e.toString());
                return Result.failure();
            }


            return Result.success();
        }

        return Result.success();
    }

    public  void callCloudFunction(@NonNull Map data, @NonNull  Map usage , @NonNull String url, String refId) throws Exception{
        // Create an HTTP transport
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(getapik())
                .setApplicationId(getappid())
                .setProjectId(getprid())
                .build();
        try
        {
            app = FirebaseApp.getInstance("DEVAPPS");
        }catch (IllegalStateException  e) {
            app = FirebaseApp.initializeApp(this.getApplicationContext(), options, "DEVAPPS");
        }


        firebaseAppCheck = FirebaseAppCheck.getInstance();

        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );
        Log.i("DevApps", "Firebase initialized: " + app.getName());
        data.put("usage",usage);
        data.put("refId",refId);

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(getScreenAnalytics(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            HttpsCallableResult result = Tasks.await(FirebaseFunctions.getInstance()
                    .getHttpsCallable(CLOUD_FUNCTION_URL_LOG_ANALYTICS)
                    .call(data));

            Log.i("Devapps","loganalytics success");
            editor.putBoolean("lastupdated",true).apply();
            editor.putBoolean("dirty",false).apply();
        }catch (ExecutionException | InterruptedException e)
        {
            Log.e("Devapps:",e.toString());
            editor.putBoolean("lastupdated", false);
            editor.apply();
        }



    }
}
