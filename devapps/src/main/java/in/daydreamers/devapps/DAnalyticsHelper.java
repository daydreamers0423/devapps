package in.daydreamers.devapps;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
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
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Pair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
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
                        Log.i("DevApps::","nonce="+nonce);
                        Gson gson = new Gson();
                        try {
                            callCloudFunction(gson.fromJson(prefs.getString("timeline",""), HashMap.class), Objects.requireNonNull(prefs.getLong("usage", 0L)), getServiceUrl() + CLOUD_FUNCTION_URL_LOG_ANALYTICS,prefs.getString("referer",""),response.token(),nonce);
                        } catch (IOException e) {
                            Log.e("Error2222",e.toString());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {


                    }
                });
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
        Calendar calendar = getCurrentDate();
        Log.i("DevApps:::",calendar.toString());
        DecimalFormat mFormat= new DecimalFormat("00");
        Map<String,Object> screentime = (Map<String, Object>) Objects.requireNonNullElse(data.get("analytics"),new HashMap<String,Object>());
        Map<String,Object> existingMap = (Map<String, Object>) Objects.requireNonNullElse(screentime.get(calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format((calendar.get(Calendar.MONTH)+1))+"-"+ calendar.get(Calendar.YEAR)),new HashMap<String,Object>());
        Log.i("DevApps","existingMap="+existingMap);
        if(!screentime.isEmpty() &&  !existingMap.isEmpty())
        {

            elapsed = Objects.requireNonNullElse(Long.parseLong(Objects.requireNonNullElse(existingMap.get(screenName),0.0).toString().split("\\.")[0]) + elapsed,elapsed);
            //screentime.put(screenName,elapsed);
            //Map<String,Object> time = (Map<String, Object>) Objects.requireNonNullElse(existingMap.get(screenName),new HashMap<String,Object>());
            existingMap.put(screenName,elapsed);
            //HashMap<String,Object> dateMap = new HashMap<>();
            screentime.put(calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format(calendar.get(Calendar.MONTH)+1)+"-"+ calendar.get(Calendar.YEAR),existingMap);
            data.put("analytics", screentime);
            Log.i("DevApps","analyticsex="+existingMap);
            Log.i("DevApps","analyticsdata="+data);
        }
        else {

            existingMap.put(screenName, elapsed);

            screentime.put(calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format(calendar.get(Calendar.MONTH)+1)+"-"+ calendar.get(Calendar.YEAR),existingMap);
            data.put("analytics", screentime);
            data.put("userid", userId);
            data.put("appid", appId);
            data.put("identity", getSHA256Fingerprint(application.getApplicationContext()));
            Log.i("DevApps","analytics="+screentime);
            Log.i("DevApps","analytics="+data);
        }


        editor.putString("timeline", gson.toJson( data));
        editor.putBoolean("dirty",true);
        // Set the flag
        editor.apply();


    }

    private Calendar getCurrentDate() {
        Long utcTimeMillis = Long.valueOf(0);
        Callable<Long> task = new Callable<Long>() {
            @Override
            public Long call() {
                return DevAppsTime.getCurrentTimeFromNTP();
            }
        };
        try {
            if(executorService == null )
            {
                executorService = Executors.newSingleThreadExecutor();
            }
            utcTimeMillis = executorService.submit(task).get();

        } catch (Exception e) {
            Log.e("DevApps:::1", e.toString());
        }


        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if(utcTimeMillis != 0) {
            calendar.setTimeInMillis(utcTimeMillis);
        }
        return calendar;
    }

    @Override
    public void onCreate(){

        super.onCreate();
        application = this;


        SharedPreferences prefs = application.getSharedPreferences(getScreenAnalytics(),MODE_PRIVATE);
        if(!prefs.getBoolean("ref_processed",false)) {
            getInstallReferer(application);

        }
        Log.i("DevApps","application="+ String.valueOf(application));
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

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
                            if(sharedPreferences.getString("referer","").isEmpty()) {
                                editor.putString("referer", itemId);
                                editor.putBoolean("dirty", true);

                                editor.apply();
                            }
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
                Log.i("DevApps","after chk ref="+prefs.getString("referer",""));
               if(!prefs.getBoolean("lastupdated",true) && !prefs.getString("referer","").isEmpty())//
               {

                    try {
                        executorService.execute(()-> {
                            try {
                                Log.i("DevApps","requestPlayIntegrityToken");
                                requestPlayIntegrityToken(activity.getApplicationContext(),prefs);
                                prefs.edit().putBoolean("lastupdated",true).apply();
                                prefs.edit().putBoolean("dirty",false).apply();
                            } catch (Exception e) {
                                Log.e("Error",e.toString());
                            }
                        });



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
                    Log.i("DevApps","endtime");
                    long usageTime = Math.round((endTime - startTime) / 1000L); // Time in milliseconds
                    //long savedUsage = sharedPreferences.getLong("usage", 0);
                    Gson gson = new Gson();
                    Calendar calendar = getCurrentDate();
                    Map<String,Object> usage = Objects.requireNonNullElse(gson.fromJson(sharedPreferences.getString("usage",""),HashMap.class),new HashMap<String,Long>());
                    DecimalFormat mFormat= new DecimalFormat("00");
                    Long dayUsage = (Long) Objects.requireNonNullElse(usage.get(calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format((calendar.get(Calendar.MONTH)+1))+"-"+ calendar.get(Calendar.YEAR)),0L);
                    Log.i("DevApps","dayUsage");
                        usageTime = Math.round((dayUsage / 1000L) + usageTime);
                    Log.i("DevApps","usageTime");
                        usage.put(calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format(calendar.get(Calendar.MONTH)+1)+"-"+ calendar.get(Calendar.YEAR),usageTime);
                        editor.putString("usage",gson.toJson(usage));
                    Log.i("DevApps","after putstring");

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

    private void getInstallReferer(Application application) {
        InstallReferrerClient referrerClient;

        referrerClient = InstallReferrerClient.newBuilder(this).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {

                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        ReferrerDetails response;
                        try {
                            response = referrerClient.getInstallReferrer();
                            Log.i("DevApps->", response.getInstallReferrer());
                            SharedPreferences.Editor editor = application.getSharedPreferences(getScreenAnalytics(),Context.MODE_PRIVATE).edit();
                            String referrerUrl = response.getInstallReferrer();
                            if (referrerUrl != null && referrerUrl.startsWith(getDeeplink())) {
                                Uri uri = Uri.parse(referrerUrl);
                                Log.i("DevApps->",referrerUrl);
                                Log.i("DevApps->", Objects.requireNonNull(uri.getQueryParameter("refId")));
                                editor.putString("referer", uri.getQueryParameter("refId"));
                                editor.putBoolean("dirty", true);
                                editor.apply();
                                isDeepLinkHandled = Boolean.TRUE;
                            }


                            editor.putBoolean("ref_processed", true);
                            editor.apply();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;

                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        // API not available on the current Play Store app.
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        // Connection couldn't be established.
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {

            }
        });
    }


}
