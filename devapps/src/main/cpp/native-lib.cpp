#include <jni.h>

extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_DAnalyticsHelper_getServiceUrl(JNIEnv *env, jobject thiz) {
    // Return the service URL
    return env->NewStringUTF("http://192.168.130.226:5001/devapps-446507/us-central1");
}
extern "C"
JNIEXPORT jstring  JNICALL
Java_in_daydreamers_devapps_UpdateWorker_getServiceUrl(JNIEnv *env, jobject thiz) {
// Return the service URL
return env->NewStringUTF("http://192.168.130.226:5001/devapps-446507/us-central1");
}
