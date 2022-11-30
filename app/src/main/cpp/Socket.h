//
// Created by usrc on 17. 1. 16.
//

#ifndef ROBOTCONTROL_SOCKET_H
#define ROBOTCONTROL_SOCKET_H

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include "Protocol.h"
#include "ir_viewer.h"
#include "CircularBuffer.h"


#define MODE_NONBLOCK 0
#define MODE_BLOCK    1

#define SOCK_CTRL 0
#define SOCK_DATA 1
#define SOCK_CGI  2

/////////////////////////////////////////////////////////////////////////////
// CMySocket command target

struct ReceiveThreadData {
    struct Viewer *pViewer;
    CCircularBuffer *pBuffer;
    int sockfd;
    int sockType;
};

class Socket
{
// Attributes
public:

// Operations
public:
    Socket(int _m_sockType, Viewer *pViewer, CCircularBuffer *_m_pSockBuffer);
    virtual ~Socket();

    CCircularBuffer *m_pSockBuffer;

    struct sockaddr_in m_ServerAddr;

    Viewer *m_pViewer;



    //SOCKADDR_IN      m_ServerAddr;


    int  m_SockType;

    pthread_t tid;
    int sockfd;

    //int  Connect(char* pIP, unsigned short port);
    int  ReceiveAnswer(int seed, int code,TCP_DATA *pPacket);
    int  Pop(TCP_DATA *pPacket);
    int  Push(BYTE *data, int nSize);
    int  SendMsg(char *message);
    int  SendData(TCP_DATA* pPacket);
    int  SendRawData(char* data, int size);
    int Open(const char * hostname, int port);
    void Close();

// Overrides
public:

    static void* ReceiveThread(void *arg);
    static bool IsSocketReady(int sockfd);
    //}}AFX_VIRTUAL

    // Generated message map functions
    //{{AFX_MSG(CMySocket)
    // NOTE - the ClassWizard will add and remove member functions here.
    //}}AFX_MSG

// Implementation
protected:

public:

};





#endif //ROBOTCONTROL_SOCKET_H
