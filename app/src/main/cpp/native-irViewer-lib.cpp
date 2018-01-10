//
// Created by usrc on 17. 1. 16.
//
/*
#include <jni.h>
#include <string>


extern "C"
jstring
Java_com_railbot_usrc_robotcontrol_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject ) {
    std::string hello = "Test from C++";

    return env->NewStringUTF(hello.c_str());
}

*/

#include "ir_viewer.h"
#include <string>
#include <unistd.h>



extern "C"
jint
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_initNative(
        JNIEnv *env,
        jobject thiz,/* this */
        jstring _hostname ) {

    LOGE(1, "Going to init native ...")

    const char *hostname = env->GetStringUTFChars(_hostname, NULL);
    if (NULL == hostname)
        return ERROR_HOSTNAME_INIT_ERROR;

    int ret = jni_ir_viewer_init(env, thiz, hostname);
    // Step 2: Perform its intended operations

    env->ReleaseStringUTFChars(_hostname, hostname);  // release resources


    return ret;
}


extern "C"
jint
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_connectNativeViewer(
        JNIEnv *env,
        jobject thiz/* this */) {

    struct Viewer * pViewer = getViewer(env, thiz);
    if (pViewer == NULL)
        return ERROR_VIEWER_NOT_FOUND;

    int ret;
    LOGE(1, "Going connnect ...")
    ret = Connect(pViewer);
    if (ret)
        return ret;

    usleep(100000);

    ret = Play(pViewer);

    return ret;

}


extern "C"
void
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_deallocNative(
        JNIEnv *env,
        jobject thisObj) {


    LOGE(1, "De alloc done");

    jni_ir_viewer_dealloc(env, thisObj);

}

extern "C"
void
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_renderViewer(
        JNIEnv *env,
        jobject thisObj, jobject surface) {

#ifdef JNI

    jni_ir_viewer_render(env, thisObj, surface);

#endif

}

extern "C"
void
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_renderFrameStop(
        JNIEnv *env,
        jobject thisObj) {

#ifdef JNI
    jni_ir_viewer_render_frame_stop(env, thisObj);
    LOGE(1, "renderFrameStopping");

#endif
    //thread_func((void *) player);

}


extern "C"
jfloat
Java_com_railbot_usrc_robotcontrol_IR_1Viewer_getTemperature(
        JNIEnv *env,
        jobject thisObj, jint x, jint y, jint maxX, jint maxY) {


    LOGE(1, "get temperature");
    struct Viewer * pViewer = getViewer(env, thisObj);

    return (jfloat) GetTemperature(pViewer, x, y, maxX, maxY);

}