package in.daydreamers.devapps;

import android.app.Activity;
import android.app.Application;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.functions.FirebaseFunctions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DAnalyticsHelper extends Application {
    private static volatile DAnalyticsHelper instance;
    private ExecutorService executorService;

    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "http://192.168.130.226:5001/devapps-446507/us-central1/loganalytics";
    private static final String CLOUD_FUNCTION_URL_LOG_USGAE = "http://192.168.130.226:5001/devapps-446507/us-central1/logusage";


    // Private constructor to prevent direct instantiation
    private DAnalyticsHelper() {


    }

    // Thread-safe method to get the singleton instance
    public static DAnalyticsHelper getInstance() {
        if (instance == null) {
            synchronized (DAnalyticsHelper.class) {
                if (instance == null) {
                    instance = new DAnalyticsHelper();
                }
            }
        }
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
        if (response.isSuccessStatusCode()) {
            String responseBody = response.parseAsString();
            System.out.println("Response: " + responseBody);
        } else {
            System.err.println();
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
            return null;
        }
    }


    // Method to log an event with API key validation
    public void logScreenView(Application application,@NonNull String screenName, @NonNull String userId,@NonNull String appId) {
        if (appId.isEmpty()) {
            Log.e("DevApps","App ID is required to log events.");
            return;
        }
        executorService =  executorService == null ? Executors.newSingleThreadExecutor():executorService;
        Map<String, Object> data = new HashMap<>();
        data.put("screenname", screenName);
        data.put("userid", userId);
        data.put("appid", appId);
        data.put("identity", getSHA1Fingerprint(application.getApplicationContext()));



            executorService.execute(()-> {
                try {
                callCloudFunction(data,CLOUD_FUNCTION_URL_LOG_ANALYTICS);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });



    }

    // Method to monitor app usage with API key validation
    public void monitorAppUsage(Application application,@NonNull String userId,@NonNull String appId ) {

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int activityReferences = 0;
            private boolean isActivityChangingConfigurations = false;
            private long startTime;

            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters foreground
                    startTime = SystemClock.elapsedRealtime();
                }
                activityReferences = activityReferences+1;
                Log.i("activityReferences=","onActivityStarted="+activityReferences);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                activityReferences = activityReferences+1;
                startTime = SystemClock.elapsedRealtime();
                Log.i("onActivityResumed=","onActivityResumed="+activityReferences);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {


                activityReferences = activityReferences > 0 ?activityReferences-1:activityReferences;
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                Log.i("activityReferences=","onActivityPaused="+activityReferences);
                if (/*activityReferences == 0 &&*/ !isActivityChangingConfigurations) {
                    // App goes to background
                    long endTime = SystemClock.elapsedRealtime();
                    long usageTime = endTime - startTime; // Time in milliseconds
                    Log.i("onActivityPaused","onActivityPaused###Time="+usageTime);
                    logAppUsageTime(userId, usageTime,appId,getSHA1Fingerprint(activity.getApplicationContext()));
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
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

                callCloudFunction(data, CLOUD_FUNCTION_URL_LOG_USGAE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
