package in.daydreamers.devapps;

import android.app.Activity;
import android.app.Application;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class DAnalyticsHelper extends Application {
    private static volatile DAnalyticsHelper instance;
    private FirebaseFunctions firebaseFunctions;
    private String apiKey;


    // Private constructor to prevent direct instantiation
    private DAnalyticsHelper(String apiKey) {
        this.apiKey = apiKey;
        FirebaseApp.initializeApp(this);
        this.firebaseFunctions = FirebaseFunctions.getInstance();
    }

    // Thread-safe method to get the singleton instance
    public static DAnalyticsHelper getInstance(String apiKey) {
        if (instance == null) {
            synchronized (DAnalyticsHelper.class) {
                if (instance == null) {
                    instance = new DAnalyticsHelper(apiKey);
                }
            }
        }
        return instance;
    }



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
    public void logScreenView(String screenName, String userId) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("API key is required to log events.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("screenName", screenName);
        data.put("userId", userId);
        data.put("apiKey", apiKey);
        data.put("key", getSHA1Fingerprint(this));
        firebaseFunctions
                .getHttpsCallable("logScreenView")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        System.out.println("Screen view logged successfully.");
                    } else {
                        System.err.println("Error logging screen view: " + task.getException().getMessage());
                    }
                });
    }

    // Method to monitor app usage with API key validation
    public void monitorAppUsage(Application application, String userId) {
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
                activityReferences++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityReferences--;
                isActivityChangingConfigurations = activity.isChangingConfigurations();

                if (activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App goes to background
                    long endTime = SystemClock.elapsedRealtime();
                    long usageTime = endTime - startTime; // Time in milliseconds
                    logAppUsageTime(userId, usageTime);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void logAppUsageTime(String userId, long usageTime) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("API key is required to log events.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("usageTime", usageTime);
        data.put("apiKey", apiKey);
        data.put("key", getSHA1Fingerprint(this));
        firebaseFunctions
                .getHttpsCallable("logAppUsageTime")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        System.out.println("App usage time logged successfully.");
                    } else {
                        System.err.println("Error logging app usage time: " + task.getException().getMessage());
                    }
                });
    }

}
