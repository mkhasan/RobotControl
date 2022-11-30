//
// Created by usrc on 17. 1. 16.
//


#include <stdio.h>

#include <new>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <fcntl.h>

#include "Socket.h"

#include "Protocol.h"
#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CMySocket

Socket::Socket(int _m_sockType, Viewer *_m_pViewer, CCircularBuffer *_m_pSockBuffer)
        : m_SockType(_m_sockType)
        , m_pViewer(_m_pViewer)
        , m_pSockBuffer(_m_pSockBuffer)
        , tid(NULL)
        , sockfd(-1)
{

    m_pViewer->stop = false;
}

Socket::~Socket()
{

    Close();

}

int Socket::Open(const char *hostName, int portNo) {
    struct hostent * server;


    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0)
        return ERROR_SOCK_CREATE;



    server = gethostbyname(hostName);

    if (server == NULL)
        return ERROR_SERVER_NOT_FOUND;

    bzero((char *) &m_ServerAddr, sizeof(m_ServerAddr));

    m_ServerAddr.sin_family = AF_INET;
    bcopy((char *)server->h_addr,
          (char *)&m_ServerAddr.sin_addr.s_addr, server->h_length);
    m_ServerAddr.sin_port = htons(portNo);

    if (connect(sockfd, (const struct sockaddr *)&m_ServerAddr, sizeof(m_ServerAddr)) < 0)
        return ERROR_SERVER_NOT_FOUND;



    ///////////////////

    m_pViewer->stop = false;

    ReceiveThreadData * data = new (std::nothrow) ReceiveThreadData;
    if(data == NULL)
        return ERROR_MEM_ALLOCATION;

    data->pViewer = m_pViewer;
    data->pBuffer = m_pSockBuffer;
    data->sockfd = this->sockfd;
    data->sockType = this->m_SockType;



    int ret = pthread_create (&tid, NULL,&ReceiveThread, (void *)data);


    if (ret)
        return ERROR_THREAD_CREATE;



    ////////////////////////

    LOGE(1, "Host name is %s", hostName);
    return ERROR_NO_ERROR;

}


bool Socket::IsSocketReady(int sockfd)
{

    /// Got here because iSelectReturn > 0 thus data available on at least one descriptor
    // Is our socket in the return list of readable sockets
    bool             res;
    fd_set          sready;
    struct timeval  nowait;

    FD_ZERO(&sready);
    FD_SET((unsigned int)sockfd,&sready);
    //bzero((char *)&nowait,sizeof(nowait));
    memset((char *)&nowait,0,sizeof(nowait));

    res = select(sockfd+1,&sready,NULL,NULL,&nowait);
    if( FD_ISSET(sockfd,&sready) )
        res = true;
    else
        res = false;


    return res;

}

void * Socket::ReceiveThread(void *arg) {



    ReceiveThreadData * data = (ReceiveThreadData *) arg;


    Viewer *pViewer = data->pViewer;
    CCircularBuffer *pBuffer = data->pBuffer;
    int sockfd = data->sockfd;
    int sockType = data->sockType;

    int written, i, length, flags;

    const int sleepUs = 10000;



#ifdef JNI

    JNIEnv * env;
    char thread_title[256];
    sprintf(thread_title, "ReceiveThread-%d",sockType);
    JavaVMAttachArgs thread_spec = { JNI_VERSION_1_4, thread_title, NULL };

    jint ret = pViewer->get_javavm->AttachCurrentThread( &env, &thread_spec);

    if (ret || env == NULL) {
        HandleError(ERROR_THREAD_ATTACH_ERROR);
        return NULL;
    }

#endif





    while (pViewer->stop == false ) {

        if (Socket::IsSocketReady(sockfd)) {
            //written = pBuffer->Receive(sockfd);

            flags = fcntl(sockfd, F_GETFL, 0);
            fcntl(sockfd, F_SETFL, flags | O_NONBLOCK);

            length = read(sockfd, (char*)(pViewer->pTempBuffer), 2*1024*1024);

            written = pBuffer->Write(pViewer->pTempBuffer, length);
            if(sockType == SOCK_CTRL)
            {
                TCP_DATA   Packet;
                TCP_HEADER Header;
                pBuffer->Peek((BYTE*)&Header, sizeof(TCP_HEADER));
                if(Header.Size>=0)
                {
                    pBuffer->Read((BYTE*)&Packet, sizeof(TCP_HEADER) + Header.Size);
                    LOGI(5, "Receive %d Bytes", Header.Size);
                }
            }



        }

        usleep(sleepUs);




    }



    delete data;

#ifdef JNI
    ret = pViewer->get_javavm->DetachCurrentThread();

    if (ret)
        HandleError(ERROR_THREAD_DETATCH_ERROR);
#endif

    return NULL;


}

void Socket::Close() {
    if (sockfd < 0)
        return;

    m_pViewer->stop = true;

    pthread_join(tid, NULL);

    close(sockfd);

    sockfd = -1;

}


int Socket::ReceiveAnswer(int seed, int code, TCP_DATA *pPacket)
{
    TCP_DATA Block;
    BYTE     data[MHPACKETSIZE+4];
    int    length, match;

    int waitfor = 10;

    if (sockfd < 0)
        return ERROR_NOT_CONNECTED;

    while(--waitfor)
    {

        int flags = fcntl(sockfd, F_GETFL, 0);
        fcntl(sockfd, F_SETFL, flags & ~O_NONBLOCK);

        length = read(sockfd, (char*)data, 1452);

        //LOGE(1, "READ check %d", length);
        if(length <= 0)
        {
            LOGE(1, "Error in ReceiveAnswer");
            usleep(300);


        }

        //LOGE(1, "READ done %d", length);

        if(length > 0 )
            Push(data, length);

        while(Pop(&Block))
        {

            match = 1;
            LOGI(5, "SEED:%d", Block.Seed);
            if(seed != -1 && seed != Block.Seed)
                match = 0;
            LOGI(5, "Code:%d", Block.Code);
            if(code != -1 && code != (int)Block.Code)
                match = 0;

            if(!match && (code%1000 == (int)Block.Code%1000))
                return -1;

            if(match)
            {
                memcpy((BYTE *)pPacket, (BYTE *)&Block, Block.Size + MHPACKETHEADERLEN);
                pPacket->Data[pPacket->Size] = 0;
                return pPacket->Size;
            }
        }
    }

    return 0;
}

int Socket::SendData(TCP_DATA* pPacket)
{
    int result = 0;

    if (sockfd < 0)
        return ERROR_NOT_CONNECTED;

    if(sendto(sockfd, (char *)pPacket, pPacket->Size+MHPACKETHEADERLEN, 0,
              (const struct sockaddr *)&m_ServerAddr, sizeof(m_ServerAddr)) > 0)
        return pPacket->Seed;
    else
        return ERROR_DATAGARAM_SEND;
}


int Socket::SendRawData(char* data, int size)
{

    if (size == 0)
        return ERROR_DATA_FORMAT;

    if(sendto(sockfd, (char *)data, size, 0,
              (const struct sockaddr *)&m_ServerAddr, sizeof(m_ServerAddr)) > 0)
        return 0;
    return ERROR_DATAGARAM_SEND;
}

int Socket::Push(BYTE *data, int nSize)
{
    if(m_pSockBuffer == NULL)
        return 0;

    if((int)m_pSockBuffer->FreeSpace() >= nSize)
        m_pSockBuffer->Write(data, nSize);

    return m_pSockBuffer->UsedSpace();
}

int Socket::Pop(TCP_DATA *pPacket)
{
    if(m_pSockBuffer == NULL || pPacket == NULL) return 0;

    if(m_pSockBuffer->IsReadHeader())
    {
        int read, toread, written = m_pSockBuffer->UsedSpace();

        if(written >= MHPACKETHEADERLEN)
        {
            m_pSockBuffer->Peek((BYTE *)pPacket, MHPACKETHEADERLEN);
            toread = pPacket->Size + MHPACKETHEADERLEN;
            if(written >= toread)
            {
                read = (int)m_pSockBuffer->Read((BYTE *)pPacket, toread);
                pPacket->Data[pPacket->Size] = 0;
                return read;
            }
        }
    }
    return 0;
}

int Socket::SendMsg(char *message)
{
    TCP_DATA Packet;
    int result = 0;

    Packet.Size     = strlen(message);
    Packet.Seed     = rand();
    Packet.Code     = COMMAND;
    Packet.Protocol = TCP_CTRL_PACKET;
    Packet.Reserved = 0;
    Packet.Pos      = 0;
    memcpy(Packet.Data, message, Packet.Size);

    // blocking mode send
    if(sendto(sockfd, (char *)&Packet, Packet.Size+MHPACKETHEADERLEN, 0,
              (const struct sockaddr *)&m_ServerAddr, sizeof(m_ServerAddr)) > 0)
        return Packet.Seed;
    else
        return ERROR_DATAGARAM_SEND;
}


