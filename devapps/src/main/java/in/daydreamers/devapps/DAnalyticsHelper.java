package in.daydreamers.devapps;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
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
import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DAnalyticsHelper extends Application  {

    private static final String CLOUD_FUNCTION_URL_LOG_ANALYTICS = "/loganalytics";
    private static volatile DAnalyticsHelper instance;

    private static  Boolean ACTIVITY_EVENT_PAUSED = Boolean.FALSE;
    private static Boolean ACTIVITY_EVENT_RESUMED = Boolean.FALSE;

    private boolean isDeepLinkHandled = Boolean.FALSE;;
    private ExecutorService executorService;
    public native String getServiceUrl();
    public native String getScreenAnalytics();

    public native String getPrefsName();
    public  native String getKeyTaskScheduled();

    static long startTime;
    private static String userId;
    private static String appId;
    private static Application application;


    static Pair<String,Long> screenStartTime;

    static {
        System.loadLibrary("native-lib");
    }

    public native String getDeeplink();

    // Private constructor to prevent direct instantiation
    public DAnalyticsHelper() {

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
        //instance.monitorAppUsage(DAnalyticsHelper.application);
        return instance;
    }

    public static void callCloudFunction(@NonNull Map data,@NonNull  Long usage ,@NonNull String url,String refId) throws IOException {
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
    public static String getSHA256Fingerprint(Context context) {
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Ensure network connectivity
                .setRequiresBatteryNotLow(false)
                .setTriggerContentMaxDelay(2,TimeUnit.MINUTES)

                .build();
        // Define the work request
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(UpdateWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,2,TimeUnit.HOURS).build();

        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
    }

    private boolean isPeriodicTaskScheduled() {
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE);
        return prefs.getBoolean(getKeyTaskScheduled(), false);
    }

    // Method to mark that the task has been scheduled
    private void markPeriodicTaskScheduled() {
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getKeyTaskScheduled(), true); // Set the flag
        editor.apply();
    }

    public void logScreenViewStart(@NonNull String screenName) {
        if (appId.isEmpty()) {
            Log.e("DevApps", "App ID is required to log events.");
            return;
        }

        screenStartTime = Pair.create(screenName,SystemClock.elapsedRealtime());
    }

    /**
     *
     * @param screenName - Name of the screen or activity
     */
    public void logScreenView(@NonNull String screenName,boolean... paused) {
        if (appId.isEmpty()) {
            Log.e("DevApps","App ID is required to log events.");
            return;
        }
        executorService =  executorService == null ? Executors.newSingleThreadExecutor():executorService;
        long elapsed = 0L;
        if(screenName.equals(screenStartTime.first) && paused == null) {
            elapsed = (SystemClock.elapsedRealtime() - screenStartTime.second) / 1000;
            screenStartTime = Pair.create(null,null);
        }
        else {
            elapsed = (SystemClock.elapsedRealtime() - screenStartTime.second) / 1000;
            screenStartTime = Pair.create(screenStartTime.first, null);
        }
        SharedPreferences prefs = application.getApplicationContext().getSharedPreferences(getScreenAnalytics(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        HashMap<String,Object> data = Objects.requireNonNullElse(gson.fromJson(prefs.getString("timeline",""),HashMap.class),new HashMap<String,Object>());

        Map<String,Object> screentime = (Map<String, Object>) Objects.requireNonNullElse(data.get("analytics"),new HashMap<String,Object>());
        if(!screentime.isEmpty())
        {
            elapsed = Objects.requireNonNullElse(Long.parseLong(Objects.requireNonNullElse(screentime.get(screenName),0.0).toString().split("\\.")[0]) + elapsed,elapsed);
            screentime.put(screenName,elapsed);


            data.put("analytics", screentime);
        }
        else {

            screentime.put(screenName, elapsed);
            data.put("analytics", screentime);
            data.put("userid", userId);
            data.put("appid", appId);
            data.put("identity", getSHA256Fingerprint(application.getApplicationContext()));
        }

        Log.i("DevApps","analytics="+screentime);
        editor.putString("timeline", gson.toJson( data));
        editor.putBoolean("dirty",true);
        // Set the flag
        editor.apply();


    }
    @Override
    public void onCreate(){

        super.onCreate();
        application = this;
        Log.i("DevApps","application="+ String.valueOf(application));
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                Log.i("DevApps","onActivityCreated..."+activity.toString());
                if(!isDeepLinkHandled) {
                    Intent intent = activity.getIntent();
                    Uri uri = intent.getData();
                    Log.i("DevApps", String.valueOf(uri));
                    if (uri != null && uri.toString().startsWith(getDeeplink())) {
                        Log.i("DevApps",getDeeplink());
                        Log.i("DevApps",uri.toString());
                        String itemId = uri.getQueryParameter("refId");
                        if(itemId != null) {
                            Log.i("DevApps", "ID==" + itemId);
                            SharedPreferences sharedPreferences = activity.getSharedPreferences(getScreenAnalytics(), MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("referer", itemId);
                            editor.putBoolean("dirty", true);
                            editor.apply();
                            isDeepLinkHandled = Boolean.TRUE;
                        }
                    }
                }
                SharedPreferences prefs = activity.getSharedPreferences(getScreenAnalytics(), MODE_PRIVATE);
                if(!isPeriodicTaskScheduled() && !prefs.getString("referer","").isEmpty())
                {
                    schedulePeriodicTask();
                    markPeriodicTaskScheduled();
                }
                if(!prefs.getBoolean("lastupdated",true) && !prefs.getString("referer","").isEmpty())
                {
                    Gson gson = new Gson();
                    try {
                        executorService.execute(()-> {
                            try {

                                callCloudFunction(gson.fromJson(prefs.getString("timeline",""), HashMap.class), Objects.requireNonNull(prefs.getLong("usage", 0L)), getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS,prefs.getString("referer",""));
                            } catch (IOException e) {
                                Log.e("Error",e.toString());
                            }
                        });

                        prefs.edit().putBoolean("lastupdated",true).apply();
                        prefs.edit().putBoolean("dirty",false).apply();

                    } catch (Exception e) {
                        Log.e("Error",e.toString());
                    }

                }

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                // Activity is started
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                SharedPreferences sharedPreferences = activity.getSharedPreferences(getScreenAnalytics(), MODE_PRIVATE);
                if (!ACTIVITY_EVENT_RESUMED && !sharedPreferences.getString("referer","").isEmpty()) {
                    ACTIVITY_EVENT_RESUMED = Boolean.TRUE;
                    ACTIVITY_EVENT_PAUSED = Boolean.FALSE;
                    startTime = SystemClock.elapsedRealtime();
                    if(screenStartTime != null) {
                        screenStartTime = Pair.create(screenStartTime.first, SystemClock.elapsedRealtime());
                    }
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                SharedPreferences sharedPreferences = activity.getSharedPreferences(getScreenAnalytics(), MODE_PRIVATE);
                if (!ACTIVITY_EVENT_PAUSED && !sharedPreferences.getString("referer","").isEmpty()) {
                    ACTIVITY_EVENT_PAUSED = Boolean.TRUE;
                    ACTIVITY_EVENT_RESUMED = Boolean.FALSE;


                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    // App goes to background
                    long endTime = SystemClock.elapsedRealtime();
                    long usageTime = endTime - startTime; // Time in milliseconds
                    long savedUsage = sharedPreferences.getLong("usage", 0);
                    editor.putLong("usage", savedUsage + usageTime);//
                    editor.putBoolean("dirty",true);
                    Log.i("DevApps","usage..."+ usageTime);
                    Log.i("DevApps","total usage..."+ (savedUsage + usageTime));
                    editor.apply();
                    if(screenStartTime != null && screenStartTime.second != null) {
                        logScreenView(screenStartTime.first,true);
                    }

                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // Activity is stopped
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // Activity is saving its instance state
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // Activity is destroyed
            }
        });


    }



}
