//
// Created by usrc on 17. 1. 16.
//

#ifndef ROBOTCONTROL_IR_VIEWER_H
#define ROBOTCONTROL_IR_VIEWER_H

//
// Created by usrc on 17. 1. 10.
//




#ifdef JNI
#include <jni.h>
#include <android/log.h>

#endif

#include <pthread.h>

#include "Def.h"



#define LOG_LEVEL 24
#define LOG_TAG "ir-native"
#define MAX_STREAMS 3
#define MAX_STRLEN 50
#define PERIOD_US 30000



enum ViewerErrors {
    ERROR_NO_ERROR = 0,
    ERROR_NOT_CONNECTED = -1,
    ERROR_DATAGARAM_SEND = -2,
    ERROR_SERVER_NOT_FOUND = -3,
    ERROR_SOCK_CREATE = -4,
    ERROR_DATA_FORMAT = -5,
    ERROR_THREAD_CREATE = -6,
    ERROR_MEM_ALLOCATION = -7,
    ERROR_CONNECTION_ERR = -8,
    ERROR_INVALID_DATA = -9,
    ERROR_JAVA_VM_NOT_FOUND = 10,
    ERROR_GLOBAL_REF_CREATE_ERR = -11,
    ERROR_M_NATIVE_VIEWER_FIELD_NOT_FOUND = -12,
    ERROR_THREAD_ATTACH_ERROR = -13,
    ERROR_THREAD_DETATCH_ERROR = -14,
    ERROR_VIEWER_NOT_FOUND = -15,
    ERROR_RENDERING_FAILED = -16,
    ERROR_COULD_NOT_GET_WINDOW_FORMAT = -17,
    ERROR_OUT_OF_RANGE = -18,
    ERROR_HOSTNAME_INIT_ERROR = -19

};

//#define SERVER_ADDR "192.168.0.100"
#define CTRL_PORT 2004
#define DATA_PORT 2005
#define USER "admin"
#define PASS "admin"
#define MAX_STRLEN 50



#ifdef JNI

#include <android/native_window_jni.h>

#define LOG(...) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}

#define LOGI(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(level, ...) if (level <= LOG_LEVEL + 10) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}
#define LOGW(level, ...) if (level <= LOG_LEVEL + 5) {__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);}

static const char native_viewer[2][MAX_STRLEN] = {"mNativeViewer", "J"};
#else
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define LOGI(level, ...) if (level <= LOG_LEVEL) {printf(__VA_ARGS__);printf("\n");}
#define LOGE(level, ...) if (level <= LOG_LEVEL + 10) {printf(__VA_ARGS__);printf("\n");}
#define LOGW(level, ...) if (level <= LOG_LEVEL + 5) {printf(__VA_ARGS__);printf("\n");}



#define JNIEnv void
#define jobject void *


#include <SDL.h>
#include <SDL_thread.h>

extern SDL_Surface* screen;
extern SDL_mutex       *screen_mutex;

void set_pixel(SDL_Surface *surface, int x, int y, Uint32 pixel);

#endif




int jni_ir_viewer_init(JNIEnv *env, jobject thiz, const char* hostname);
struct Viewer * getViewer(JNIEnv *env, jobject thiz);
void jni_ir_viewer_dealloc(JNIEnv *env, jobject thiz);
void jni_ir_viewer_render(JNIEnv *env, jobject thiz, jobject surface);
void HandleError(int errCode);
void jni_ir_viewer_render_frame_stop(JNIEnv *env, jobject thiz);

int Connect(struct Viewer * pViewer);
int Play(struct Viewer * pViewer);
float GetTemperature(struct Viewer *pViewer, int x, int y, int maxX, int maxY);

class Socket;
class CCircularBuffer;

struct Viewer {

#ifdef JNI
    JavaVM *get_javavm;
    jobject thiz;
    ANativeWindow * window;
#endif

    char hostname[MAX_STRLEN];
    Socket * pCtrlSocket;
    Socket * pStreamSocket;
    CCircularBuffer * pCtrlBuffer;
    CCircularBuffer * pStreamBuffer;
    WORD * pData;

    bool connected;

    BYTE * pFrameData;
    BYTE * pOutFrame;
    BYTE * pTempBuffer;

    int liveID;
    int live;

    int cx;
    int cy;
    int frameNum;
    bool stop;
    unsigned int periodUs;
    bool bPlay;
    pthread_t decThreadID;
    pthread_mutex_t m_CodecMutex;


};

#endif //ROBOTCONTROL_IR_VIEWER_H



