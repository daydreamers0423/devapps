#include <jni.h>

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getDeeplink(JNIEnv *env, jobject thiz) {
    // Return the service URL
    return env->NewStringUTF("https://devapps.co.in/");
}
extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getServiceUrl(JNIEnv *env, jobject thiz) {
// Return the service URL
return env->NewStringUTF("https://us-central1-devapps-446507.cloudfunctions.net");
    //return env->NewStringUTF("http://192.168.123.59:50001/devapps-446507/us-central1");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getServiceUrl(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("https://us-central1-devapps-446507.cloudfunctions.net");
    //return env->NewStringUTF("http://192.168.123.59:50001/devapps-446507/us-central1");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getScreenAnalytics(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("SCREEN_ANALYTICS_DEVAPPS");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getPrefsName(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("app_prefs");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getKeyTaskScheduled(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("app_prefs");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getapik(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("AIzaSyDoFw-1sYKZ1KIH2ccg8o9VszYBVNJvtkU");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getappid(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("1:773635156409:web:04e8b13df7f1e4048dee68");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getprid(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("devapps-446507");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getScreenAnalytics(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("SCREEN_ANALYTICS_DEVAPPS");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getPrefsName(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("app_prefs");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getKeyTaskScheduled(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("app_prefs");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getapik(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("AIzaSyDoFw-1sYKZ1KIH2ccg8o9VszYBVNJvtkU");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getappid(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("1:773635156409:web:04e8b13df7f1e4048dee68");
}

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getprid(JNIEnv *env, jobject thiz) {
// Return the service URL
    return env->NewStringUTF("devapps-446507");
}