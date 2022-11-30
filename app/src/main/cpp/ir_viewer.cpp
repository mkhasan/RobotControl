//
// Created by usrc on 17. 1. 16.
//

//
// Created by usrc on 17. 1. 10.
//

//
// Created by usrc on 17. 1. 10.
//

//
// Created by usrc on 17. 1. 10.
//

//
// Created by usrc on 17. 1. 10.
//





#include <chrono>
#include <stdio.h>


#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <new>



#include "ir_viewer.h"
#include "Socket.h"

#ifndef JNI
extern struct Viewer * pIR_Viewer;
#endif

using namespace std;


int NotifyPorts(struct Viewer * pViewer, char *pIP, unsigned short Port);
struct Viewer * getViewer(JNIEnv *env, jobject thiz);

void * DecodeAndDisp(void *);
int DecodeFrame(struct Viewer * pViewer);
void Render(struct Viewer * pViewer);




#define BUFSIZE 1024

/*
 * error - wrapper for perror
 */
void error(char *msg) {
    LOGE(1, "%s", msg);

}

void HandleError(int errCode) {


    LOGE(1, "Error to be handlerd %d", errCode);
}

static void DeleteAll(JNIEnv *env, struct Viewer *pViewer) {

#ifdef JNI

    if(pViewer->thiz != NULL) {

        jclass thisClass = env->GetObjectClass(pViewer->thiz);

        jfieldID fieldNumber = env->GetFieldID(thisClass, native_viewer[0], native_viewer[1]);

        if(fieldNumber != NULL) {


            env->SetLongField(pViewer->thiz, fieldNumber, (jlong) 0);



        }

        env->DeleteGlobalRef(pViewer->thiz);
    }

#endif


    if(pViewer->pData) {
        delete[] pViewer->pData;
        pViewer->pData = NULL;
    }
    if(pViewer->pOutFrame) {
        delete[] pViewer->pOutFrame;
        pViewer->pOutFrame = NULL;
    }
    if(pViewer->pTempBuffer) {
        delete[] pViewer->pTempBuffer;
        pViewer->pTempBuffer = NULL;
    }
    if(pViewer->pFrameData) {
        delete[] pViewer->pFrameData;
        pViewer->pFrameData = NULL;
    }
    if(pViewer->pCtrlSocket) {
        delete pViewer->pCtrlSocket;
        pViewer->pCtrlSocket = NULL;
    }
    if (pViewer->pStreamSocket) {
        delete pViewer->pStreamSocket;
        pViewer->pStreamSocket = NULL;
    }
    if (pViewer->pCtrlBuffer) {
        delete pViewer->pCtrlBuffer;
        pViewer->pCtrlBuffer = NULL;
    }
    if (pViewer->pStreamBuffer) {
        delete pViewer->pStreamBuffer;
        pViewer->pStreamBuffer = NULL;
    }
    delete pViewer;
    pViewer = NULL;


#ifndef JNI
    pIR_Viewer = NULL;
#endif

}




int jni_ir_viewer_init(JNIEnv *env, jobject thiz, const char* _hostname) {


    int ret;
    struct Viewer * pViewer = new (std::nothrow) Viewer;

    if (pViewer == NULL)
        return ERROR_MEM_ALLOCATION;

    LOGE(1, "R= %d G= %d B= %d", iron[9].r, iron[9].g, iron[9].b);

    LOGE(1, "pViewer is %lx", pViewer);
    memset(pViewer, 0, sizeof(*pViewer));

    strcpy(pViewer->hostname, _hostname);

#ifdef JNI
    ret = env->GetJavaVM(&pViewer->get_javavm);
    if (ret) {
        delete pViewer;
        return ERROR_JAVA_VM_NOT_FOUND;
    }

    pViewer->thiz = env->NewGlobalRef(thiz);
    if (pViewer->thiz == NULL) {
        delete pViewer;
        return ERROR_GLOBAL_REF_CREATE_ERR;
    }

    jclass thisClass = env->GetObjectClass(thiz);

    jfieldID fieldNumber = env->GetFieldID(thisClass, native_viewer[0], native_viewer[1]);

    if(fieldNumber == NULL) {
        DeleteAll(env, pViewer);

        return ERROR_M_NATIVE_VIEWER_FIELD_NOT_FOUND;
    }

    env->SetLongField(thiz, fieldNumber, (jlong) pViewer);


#endif

    pViewer->connected = false;
    pViewer->liveID = 0;
    pViewer->live = 1;


    pViewer->pStreamBuffer = new (std::nothrow) CCircularBuffer();
    if (pViewer->pStreamBuffer == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;

    }

    pViewer->pCtrlBuffer = new (std::nothrow) CCircularBuffer();
    if (pViewer->pCtrlBuffer == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;
    }

    pViewer->pStreamBuffer->SetBufferSize(2*1024*1024);
    pViewer->pStreamBuffer->Reset();

    pViewer->pCtrlBuffer->SetBufferSize(128*1024);
    pViewer->pCtrlBuffer->SetReadHeader(true);

    pViewer->pStreamSocket = new (std::nothrow) Socket(SOCK_DATA, pViewer, pViewer->pStreamBuffer);
    if (pViewer->pStreamSocket == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;
    }

    pViewer->pCtrlSocket = new (std::nothrow) Socket(SOCK_CTRL, pViewer, pViewer->pCtrlBuffer);
    if (pViewer->pCtrlBuffer == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;

    }

    pViewer->pFrameData = new (std::nothrow) BYTE [2*1024*1024];
    if(pViewer->pFrameData == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;

    }

    pViewer->pTempBuffer = new (std::nothrow) BYTE[2*1024*1024];
    if (pViewer->pTempBuffer == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;
    }

    pViewer->pOutFrame = new (std::nothrow) BYTE[1920*1088*4];
    if(pViewer->pOutFrame == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;

    }
    pViewer->pData = new (std::nothrow) WORD[WIDTH*HEIGHT];
    if (pViewer->pData == NULL) {
        DeleteAll(env, pViewer);
        return ERROR_MEM_ALLOCATION;
    }


    pViewer->bPlay = false;
    pViewer->decThreadID = NULL;
    pViewer->periodUs = PERIOD_US;
    pViewer->stop = false;

    ret = pthread_create (&pViewer->decThreadID, NULL,&DecodeAndDisp, (void *)pViewer);
    if (ret) {
        DeleteAll(env, pViewer);
        return ERROR_THREAD_CREATE;
    }

    pthread_mutex_init(&pViewer->m_CodecMutex, NULL);




#ifndef JNI
    /*
    ret = Connect(pViewer);
    if (ret < 0) {

        DeleteAll(env, pViewer);
        return ret;
    }



    usleep(100000);




    ret = Play(pViewer);

    if (ret != 0) {
        DeleteAll(env, pViewer);
        return ret;

    }
    */

#endif

#ifndef JNI
    pIR_Viewer = pViewer;
#endif

    LOGE(1, "Init done");
    return ERROR_NO_ERROR;

}

using Clock = std::chrono::high_resolution_clock;
using TimePoint = std::chrono::time_point<Clock>;

void * DecodeAndDisp(void * ptr) {

    struct Viewer * pViewer = (struct Viewer *) ptr;

#ifdef JNI

    JNIEnv * env;
    char thread_title[256];
    sprintf(thread_title, "DecodeAndDisp");
    JavaVMAttachArgs thread_spec = { JNI_VERSION_1_4, thread_title, NULL };

    jint ret = pViewer->get_javavm->AttachCurrentThread( &env, &thread_spec);






    if (ret || env == NULL) {
        HandleError(ERROR_THREAD_ATTACH_ERROR);
        return NULL;
    }

#endif

    TimePoint twakeup = Clock::now();
    TimePoint t1,t2;
    int tsleep;

    const Clock::duration periodUs = std::chrono::microseconds(pViewer->periodUs);

    while (pViewer->stop == false) {

        twakeup += periodUs;

        if (pViewer->bPlay) {
            pthread_mutex_lock(&pViewer->m_CodecMutex);


            if (DecodeFrame(pViewer)) {

                Render(pViewer);


                //LOGE(1, "playing ...");
            }

            pthread_mutex_unlock(&pViewer->m_CodecMutex);
        }



        t1 = Clock::now();
        tsleep = std::chrono::duration_cast<std::chrono::microseconds>(twakeup-t1).count();
        if (tsleep > 0)
            usleep(tsleep);
    }


#ifdef JNI
    ret = pViewer->get_javavm->DetachCurrentThread();

    if (ret)
        HandleError(ERROR_THREAD_DETATCH_ERROR);

#endif

    return NULL;
}

int Connect(struct Viewer * pViewer) {




    int ret, seed;
    TCP_DATA Packet;
    char message[256];


    if (pViewer->connected) {
        pViewer->pStreamSocket->Close();
        pViewer->pCtrlSocket->Close();
    }

    LOGE(1, "Open Begins %s", pViewer->hostname);

    ret = pViewer->pCtrlSocket->Open(pViewer->hostname, CTRL_PORT);
    LOGE(1, "Open Done");
    if ( ret < 0) {
        return ret;
    }


    ret = pViewer->pCtrlSocket->ReceiveAnswer(-1, READY, &Packet);

    if(ret <= 0)
        return ERROR_CONNECTION_ERR;
    if (Packet.Code == NETFULL)
        return ERROR_INVALID_DATA;

    sprintf(message, "login %s %s", USER, PASS);
    seed = pViewer->pCtrlSocket->SendMsg(message);

    ret = pViewer->pCtrlSocket->ReceiveAnswer(seed, -1, &Packet);
    if (ret <= 0 || Packet.Code != LOGIN_SUCCESS)
        return ERROR_CONNECTION_ERR;


    LOGI(5, "LOGIN success");


    ret = NotifyPorts(pViewer, pViewer->hostname, DATA_PORT);
    if (ret == 0)
        return ERROR_INVALID_DATA;

    LOGI(5, "All OK");
    pViewer->connected = true;


    return ERROR_NO_ERROR;




}


int NotifyPorts(struct Viewer * pViewer, char *pIP, unsigned short Port)
{
    TCP_DATA Packet;
    unsigned short     seed;
    DWORD    i, gotit;
    int      ret, key;

    // send "port open"
    strcpy((char *)Packet.Data, "port open");
    Packet.Size     = strlen((char *)Packet.Data);
    Packet.Code     = COMMAND;
    Packet.Protocol = TCP_CTRL_PACKET;
    Packet.Reserved = 0;
    Packet.Pos      = 0;

    seed = pViewer->pCtrlSocket->SendData(&Packet);
    // receive key
    ret = pViewer->pCtrlSocket->ReceiveAnswer(seed, PORT_NOTICE_SUCCESS, &Packet);
    if(ret > 0)
    {
        if(sscanf((char *)Packet.Data, "port key %d\r\n", &key) != 1)
            return 0;
    }
    else
        return 0;

    // receive acception message (TCP)
    //pViewer->p->Open()->Connect((LPCTSTR)pIP, Port);

    ret = pViewer->pStreamSocket->Open(pIP, Port);
    if (ret < 0 )
        return 0;

    memset(Packet.Data,0,1444);
    sprintf((char *)Packet.Data, "port key %d %d", 0, key);

    Packet.Size     = strlen((char *)Packet.Data);
    Packet.Code     = PORT_NOTICE;
    Packet.Protocol = TCP_CTRL_PACKET;
    Packet.Reserved = 0;
    Packet.Pos      = 0;

    for(gotit=0,i=0; i<5; i++)
    {
        seed = pViewer->pStreamSocket->SendData(&Packet);
        ret = pViewer->pCtrlSocket->ReceiveAnswer(seed, PORT_NOTICE_SUCCESS, &Packet);
        if(ret>0)
        {
            gotit = 1;
            break;
        }
        else
            break;
    }

    if(!gotit) return 0;


    LOGI(5, "Data Socket Connected");
    return 1;
}


int Play(struct Viewer * pViewer) {



    char message[128];

    if(pViewer->connected == false)
        return ERROR_NOT_CONNECTED;

    sprintf(message, ("XLIVE STREAM %d 0 0"), pViewer->liveID);
    pViewer->live = 1;


    pViewer->pCtrlSocket->SendMsg(message);


    pViewer->frameNum = -1;

    pViewer->cx = 0;
    pViewer->cy = 0;
    pViewer->bPlay = true;


    LOGI(5, "Play started");
    return ERROR_NO_ERROR;

}
void jni_ir_viewer_dealloc(JNIEnv *env, jobject thiz) {




    struct Viewer * pViewer = getViewer(env, thiz);


    if(pViewer == NULL)
        return;


    pViewer->stop = true;
    if (pViewer->decThreadID != NULL) {
        pthread_join(pViewer->decThreadID, NULL);
        pViewer->decThreadID = NULL;
    }


    DeleteAll(env, pViewer);


    LOGI(5, "Deallocation successfull");
}




int jni_ir_viewer_init1(JNIEnv *env, jobject thiz) {


    int sockfd, portno, n;
    struct sockaddr_in serveraddr;
    struct hostent *server;
    char *hostname;
    char buf[BUFSIZE];
    char msg[100];

    hostname = "192.168.0.100";
    portno = 2004;


    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        error("ERROR opening socket");
        return -1;
    }


    server = gethostbyname(hostname);
    if (server == NULL) {
        sprintf(msg,"ERROR, no such host as %s\n", hostname);
        error(msg);
        return -1;
    }

    // build the server's Internet address
    bzero((char *) &serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    bcopy((char *)server->h_addr,
          (char *)&serveraddr.sin_addr.s_addr, server->h_length);
    serveraddr.sin_port = htons(portno);

    // connect: create a connection with the server
    if (connect(sockfd, (const struct sockaddr *)&serveraddr, sizeof(serveraddr)) < 0) {
        error("ERROR connecting");
        return -1;
    }

    // get message line from the user
    printf("Please enter msg: ");
    bzero(buf, BUFSIZE);
    strcpy(buf, "login admin admin");
    //fgets(buf, BUFSIZE, stdin);

    // send the message line to the server
    n = write(sockfd, buf, strlen(buf));
    if (n < 0) {
        error("ERROR writing to socket");
        return -1;
    }

    // print the server's reply
    bzero(buf, BUFSIZE);
    n = read(sockfd, buf, BUFSIZE);
    if (n < 0) {
        error("ERROR reading from socket");
        return -1;
    }
    sprintf(msg, "Echo from server: %s length of reply: %d \n", buf, n);

    LOGE(1, "%s", msg);
    close(sockfd);


    return 0;
}


struct Viewer * getViewer(JNIEnv *env, jobject thiz) {


#ifdef JNI
    jclass thisClass = env->GetObjectClass(thiz);

    jfieldID fieldNumber = env->GetFieldID(thisClass, native_viewer[0], native_viewer[1]);

    if (fieldNumber == NULL) {
        LOGE(1, "ERROR_M_NATIVE_VIEWER_FIELD_NOT_FOUND");
        exit(1);
    }

    jlong number = env->GetLongField(thiz, fieldNumber);

    if (number == NULL) {
        HandleError(ERROR_VIEWER_NOT_FOUND);
    }

    return (struct Viewer *) number;



#else
    return pIR_Viewer;
#endif
}

#ifdef JNI
void jni_ir_viewer_render(JNIEnv *env, jobject thiz, jobject surface) {

    struct Viewer * pViewer = getViewer(env, thiz);
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);

    LOGI(4, "jni_ir_viewer_render")
    pthread_mutex_lock(&pViewer->m_CodecMutex);
    if (pViewer->window != NULL) {
        LOGE(1,
             "jni_ir_viewer_render Window have to be null before "
                     "calling render function");
        exit(1);
    }
    ANativeWindow_acquire(window);
    pViewer->window = window;

    pthread_mutex_unlock(&pViewer->m_CodecMutex);
}

void jni_ir_viewer_render_frame_stop(JNIEnv *env, jobject thiz) {
    struct Viewer * pViewer = getViewer(env, thiz);





    pViewer->bPlay = false;


    pthread_mutex_lock(&pViewer->m_CodecMutex);
    if (pViewer->window == NULL) {
        LOGE(1,
             "jni_ir_viewer_render_frame_stop Window is null this "
                     "mean that you did not call render function");
        exit(1);
    }
    LOGI(5, "jni_irviewer_render_frame_stop releasing window");
    ANativeWindow_release(pViewer->window);
    pViewer->window = NULL;

    pthread_mutex_unlock(&pViewer->m_CodecMutex);
}

#endif