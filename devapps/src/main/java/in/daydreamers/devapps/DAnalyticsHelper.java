package in.daydreamers.devapps;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.Json;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DAnalyticsHelper extends Application  {
    private static final String SCREEN_ANALYTICS = "SCREEN_ANALYTICS" ;
    private static volatile DAnalyticsHelper instance;
    private ExecutorService executorService;

    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "/loganalytics";
    private static final String CLOUD_FUNCTION_URL_LOG_USGAE = "/logusage";
    private static  Boolean ACTIVITY_EVENT_PAUSED = Boolean.FALSE;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_TASK_SCHEDULED = "task_scheduled";

    private static Boolean ACTIVITY_EVENT_RESUMED = Boolean.FALSE;

    static long startTime;
    private static String userId;
    private static String appId;
    private static Application application;

    private static int retry = 0;

    static {
        System.loadLibrary("native-lib");
    }

    public native String getServiceUrl();

    // Private constructor to prevent direct instantiation
    private DAnalyticsHelper() {


    }

    // Thread-safe method to get the singleton instance
    public static DAnalyticsHelper getInstance(@NonNull String userId,@NonNull String appId,@NonNull Application application) {
        if (instance == null) {
            synchronized (DAnalyticsHelper.class) {
                if (instance == null) {
                    instance = new DAnalyticsHelper();
                }
            }
        }
        startTime = SystemClock.elapsedRealtime();
        DAnalyticsHelper.userId = userId;
        DAnalyticsHelper.appId = appId;
        DAnalyticsHelper.application = application;

        return instance;
    }

    public static void callCloudFunction(@NonNull Map data,@NonNull String url) throws IOException {
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
        if (!response.isSuccessStatusCode()) {
            retry +=1;
            if(retry < 3) {
                callCloudFunction(data, url);
            }
        }
    }

    // Helper class to create JSON payload


    public static String getSHA1Fingerprint(Context context) {
        try {
            // Get the package manager and package info
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );

            // Get the first signature (assuming a single signing key)
            Signature signature = packageInfo.signatures[0];

            // Calculate the SHA-1 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(signature.toByteArray());

            // Convert hash bytes to a hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xFF & b).toUpperCase(Locale.US);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
                hexString.append(":");
            }

            // Remove the trailing colon
            if (hexString.length() > 0) {
                hexString.setLength(hexString.length() - 1);
            }

            return hexString.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
    private void schedulePeriodicTask() {
        // Define the work request
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(UpdateWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
    }

    private boolean isPeriodicTaskScheduled() {
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TASK_SCHEDULED, false);
    }

    // Method to mark that the task has been scheduled
    private void markPeriodicTaskScheduled() {
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_TASK_SCHEDULED, true); // Set the flag
        editor.apply();
    }


    /**
     *
     * @param screenName - Name of the screen or activity
     * @param elapsed - Time spent on each screen or activity of an app
     */
    public void logScreenView(@NonNull String screenName,int elapsed) {
        if (appId.isEmpty()) {
            Log.e("DevApps","App ID is required to log events.");
            return;
        }
        if(!isPeriodicTaskScheduled())
        {
            schedulePeriodicTask();
            markPeriodicTaskScheduled();
        }


        //executorService =  executorService == null ? Executors.newSingleThreadExecutor():executorService;
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(SCREEN_ANALYTICS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        HashMap<String,Object> data = Objects.requireNonNullElse(gson.fromJson(prefs.getString("timeline",""),HashMap.class),new HashMap<String,Object>());

        HashMap<String,Object> screentime = (HashMap<String, Object>) Objects.requireNonNullElse(data.get("analytics"),new HashMap<String,Object>());
        if(!screentime.isEmpty())
        {
            elapsed = Objects.requireNonNullElse(Integer.parseInt(Objects.requireNonNullElse(screentime.get(screenName),0.0).toString().split("\\.")[0]) + elapsed,elapsed);
            screentime.put(screenName,elapsed);
            Log.i("elapsed--",""+elapsed);
            data.put("analytics", screentime);
        }
        else {

            screentime.put(screenName, elapsed);
            data.put("analytics", screentime);
            data.put("userid", userId);
            data.put("appid", appId);
            data.put("identity", getSHA1Fingerprint(application.getApplicationContext()));
        }



        editor.putString("timeline", gson.toJson( data));
        // Set the flag
        editor.apply();


//            executorService.execute(()-> {
//                try {
//                callCloudFunction(data,getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });



    }


    // Method to monitor app usage with API key validation
   public void monitorAppUsage(@NotNull Application application ) {

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {



                                               @Override
                                               public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {


                                               }

                                               @Override
                                               public void onActivityStarted(@NonNull Activity activity) {

                                                  /* startTime = SystemClock.elapsedRealtime();
                                                   Log.i("val==", "startTime=" + startTime);
                                                   SharedPreferences sharedPreferences = activity.getSharedPreferences("devapps", MODE_PRIVATE);
                                                   int saves = sharedPreferences.getInt("saves", 1);*/





                                               }

                                               @Override
                                               public void onActivityResumed(@NonNull Activity activity) {
                                                   if (!ACTIVITY_EVENT_RESUMED) {
                                                       ACTIVITY_EVENT_RESUMED = Boolean.TRUE;
                                                       ACTIVITY_EVENT_PAUSED = Boolean.FALSE;
                                                       startTime = SystemClock.elapsedRealtime();
                                                       Log.i("val==", "startTimeR=" + startTime);
                                                   }
                                               }

                                               @Override
                                               public void onActivityPaused(@NonNull Activity activity) {
                                                   if (!ACTIVITY_EVENT_PAUSED) {
                                                       ACTIVITY_EVENT_PAUSED = Boolean.TRUE;
                                                       ACTIVITY_EVENT_RESUMED = Boolean.FALSE;

                                                       SharedPreferences sharedPreferences = activity.getSharedPreferences("devapps", MODE_PRIVATE);
                                                       SharedPreferences.Editor editor = sharedPreferences.edit();


                                                       // App goes to background
                                                       long endTime = SystemClock.elapsedRealtime();

                                                       long usageTime = endTime - startTime; // Time in milliseconds
                                                       long savedUsage = sharedPreferences.getLong("usage", 0);
                                                       Log.i("val==", "endTime=" + endTime);
                                                       Log.i("val==", "startTime=" + startTime);
                                                       Log.i("val==", "usageTime=" + usageTime);
                                                       Log.i("val==", "savedUsage=" + savedUsage);

                                                       int saves = sharedPreferences.getInt("saves", 1);
                                                       saves = saves + 1;
                                                       editor.putLong("usage", savedUsage + usageTime);//
                                                       Log.i("val==", "usage=" + sharedPreferences.getLong("usage", -1));
                                                       editor.putInt("saves", saves);
                                                       editor.apply();
                                                       if (saves % 10 == 0) {
                                                           logAppUsageTime(userId, sharedPreferences.getLong("usage", 0), appId, getSHA1Fingerprint(activity.getApplicationContext()));
                                                       }


                                                   }
                                               }

                                               @Override
                                               public void onActivityStopped(@NonNull Activity activity) {

                                               }

                                               @Override
                                               public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                                               }

                                               @Override
                                               public void onActivityDestroyed(@NonNull Activity activity) {
                                               }

        });
    }

    private void logAppUsageTime(@NonNull String userId,@NonNull long usageTime,@NonNull String appId,String... identity) {
        if ( appId.isEmpty()) {
            Log.e("devapps","App Id is required to log events.");
            return;
        }
        executorService = executorService == null ? Executors.newSingleThreadExecutor():executorService;
        Map<String, Object> data = new HashMap<>();
        data.put("userid", userId);
        data.put("usagetime", usageTime);
        data.put("appid", appId);
        data.put("identity", identity != null ? identity[0] : getSHA1Fingerprint(this));
        executorService.execute(()-> {
            try {

                callCloudFunction(data,getServiceUrl() + CLOUD_FUNCTION_URL_LOG_USGAE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
