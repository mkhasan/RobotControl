//
// Created by usrc on 17. 1. 16.
//

//
// Created by usrc on 17. 1. 16.
//



#include <cstddef>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>

#include "CircularBuffer.h"
#include "ir_viewer.h"
#include "unistd.h"

#define DEFAULT_CBUFFER_SIZE  10240

CCircularBuffer::CCircularBuffer()
{
    m_Size    = 0;
    m_In      = 0;
    m_Out     = 0;
    m_pBuffer = NULL;
    m_bReadHeader = false;

    pthread_mutex_init(&m_HanNomManCBuffer, NULL);
}

CCircularBuffer::CCircularBuffer(int length)
{
    if(length <= 0) length = DEFAULT_CBUFFER_SIZE;

    m_Size    = length;
    m_In      = 0;
    m_Out     = 0;
    m_pBuffer = NULL;

    m_bReadHeader = false;

    pthread_mutex_init(&m_HanNomManCBuffer, NULL);

    SetBufferSize(m_Size);
}

CCircularBuffer::~CCircularBuffer()
{
    if(m_pBuffer)
    {
        delete m_pBuffer;
        m_pBuffer = NULL;
    }


}


int CCircularBuffer::Read(BYTE *dest, int length)
{
    if(m_Size <= 0 || length <= 0) return 0;

    int left, right, written;
    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    written = (m_In-m_Out+m_Size) % m_Size;
    length  = (length <= written ? length : written);


    right = (length <= m_Size-m_Out ? length : m_Size-m_Out);
    left  = length - right;


    if(right)
    {
        memcpy(dest, m_pBuffer+m_Out, right);
        m_Out = (m_Out + right) % m_Size;
    }
    if(left)
    {
        memcpy(dest+right, m_pBuffer+m_Out, left);
        m_Out += left;
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return length;
}



int CCircularBuffer::Write(BYTE *src, int length)
{
    if(m_Size <= 0 || length <= 0) return 0;

    int left, right, free;
    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    free   = ((m_Out - m_In + m_Size - 1) % m_Size); // FreeSpace()
    length = (length <= free ? length : free);


    right = (length <= m_Size-m_In ? length : m_Size-m_In);
    left  = length - right;


    if(right)
    {
        memcpy(m_pBuffer+m_In, src, right);
        m_In = (m_In + right) % m_Size;
    }

    if(left)
    {
        memcpy(m_pBuffer+m_In, src+right, left);
        m_In += left;
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return length;
}


int CCircularBuffer::Receive(int hSocket)
{
    int left, right, free, length;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    free   = ((m_Out - m_In + m_Size - 1) % m_Size);

    right = m_Size-m_In;


    LOGI(50, "reading started")

    /*
    int flags = fcntl(hSocket, F_GETFL, 0);
    fcntl(hSocket, F_SETFL, flags | O_NONBLOCK);
     */

    length = read(hSocket, (char*)(m_pBuffer+m_In), right);

    LOGI(50, "reading finished")
    if(length < 0)
    {
        pthread_mutex_unlock(&m_HanNomManCBuffer);
        //ReleaseMutex(m_HanNomManCBuffer);

        LOGE(1, "Error no is : %d\n", errno);
        return 0;
    }


    if(right==length)
    {
        left = m_Out - 1;
        length += read(hSocket, (char*)m_pBuffer, left);
    }

    m_In = (m_In + length)%m_Size;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);

    if(length == free)
    {
        LOGI(5, "Full");
        return -1; // FULL
    }
    else
        return length;
}


int CCircularBuffer::Peek(BYTE *dest, int length)
{
    if(m_Size <= 0 || length <= 0) return 0;

    int left, right, written, tempout;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    written = (m_In-m_Out+m_Size) % m_Size; // UsedData()�� ����
    length  = (length <= written ? length : written);


    right = (length <= m_Size-m_Out ? length : m_Size-m_Out);
    left  = length - right;


    tempout = m_Out;
    if(right)
    {
        memcpy(dest, m_pBuffer+tempout, right);
        tempout = (tempout + right) % m_Size;
    }
    if(left)
    {
        memcpy(dest+right, m_pBuffer+tempout, left);
        tempout += left;
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);


    return length;
}

// CircularBuffer --> CircularBuffer
int CCircularBuffer::MoveData(CCircularBuffer *src, int length)
{
    if(m_Size <= 0 || length <= 0) return 0;

    int left, right, used, free;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    used   = src->UsedSpace();
    length = (length <= used ? length : used);
    free   = ((m_Out - m_In + m_Size - 1) % m_Size); // FreeSpace()�� ����
    length = (length <= free ? length : free);


    right = (length <= m_Size-m_In ? length : m_Size-m_In);
    left  = length - right;


    if(right)
    {
        src->Read(m_pBuffer+m_In, right);
        m_In = (m_In + right) % m_Size;
    }
    if(left)
    {
        src->Read(m_pBuffer+m_In, left);
        m_In += left;
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return length;
}


int CCircularBuffer::UsedSpace()
{
    if(m_Size <= 0) return 0;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    int usedsize = (m_In - m_Out + m_Size) % m_Size;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return usedsize;
}

int CCircularBuffer::FreeSpace()
{
    if(m_Size <= 0) return 0;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    int freesize = (m_Out - m_In + m_Size - 1) % m_Size;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return freesize;
}


float CCircularBuffer::FreeSpaceRatio()
{
    if(m_Size <= 0) return 0;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);
    float freeratio = (FreeSpace() / ((float)m_Size));

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return freeratio;
}


void CCircularBuffer::SetBufferSize(int buffersize)
{
    if(buffersize <= 0) buffersize = DEFAULT_CBUFFER_SIZE;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    m_Size = buffersize;

    if(m_pBuffer)
    {
        delete m_pBuffer;
        m_pBuffer = NULL;
    }

    m_pBuffer = (BYTE *)malloc(sizeof(BYTE)*m_Size);

    // reset
    m_In  = 0;
    m_Out = 0;
    m_bReadHeader = FALSE;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
}

// data reset
void CCircularBuffer::Reset()
{
    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);
    LOGI(6, "Buffer Reset");
    m_In  = 0;
    m_Out = 0;

    m_bReadHeader = FALSE;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
}

// move m_Out postion
int CCircularBuffer::Dump(int length)
{
    int used, dumped;

    if(m_Size <= 0 || length <= 0) return 0;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    used   = UsedSpace();
    dumped = length <= used ? length : used;

    m_Out = (m_Out + dumped) % m_Size;

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);

    return dumped;
}


int CCircularBuffer::MovePos(int add)
{
    if(add<0)
        return 0;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);


    if((m_In - m_Out + m_Size) % m_Size >= add)
    {
        m_Out += add;
        m_Out = m_Out % m_Size;
        pthread_mutex_unlock(&m_HanNomManCBuffer);
        //ReleaseMutex(m_HanNomManCBuffer);
        return 1;
    }
    else
    {
        pthread_mutex_unlock(&m_HanNomManCBuffer);
        //ReleaseMutex(m_HanNomManCBuffer);
        return 0;
    }
}

int CCircularBuffer::Search(const char *str)
{
    int i;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    for(i=0; (i+3) < (m_In - m_Out + m_Size) % m_Size; i++)
    {
        if(*(char*)(m_pBuffer+(m_Out+i)%m_Size)==str[0])
        {
            if(*(char*)(m_pBuffer+(m_Out+i+1)%m_Size)==str[1])
            {
                if(*(char*)(m_pBuffer+(m_Out+i+2)%m_Size)==str[2])
                {
                    if(*(char*)(m_pBuffer+(m_Out+i+3)%m_Size)==str[3])
                    {
                        pthread_mutex_unlock(&m_HanNomManCBuffer);
                        //ReleaseMutex(m_HanNomManCBuffer);
                        return i;
                    }
                }
            }
        }
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return i;
}

int CCircularBuffer::Search2(int *type)
{
    int i, val;

    pthread_mutex_lock(&m_HanNomManCBuffer);
    //WaitForSingleObject(m_HanNomManCBuffer, INFINITE);

    for(i=0; (i+3) < (m_In - m_Out + m_Size) % m_Size; i++)
    {
        val = *(unsigned int*)(m_pBuffer+(m_Out+i)%m_Size);
        if(val==0x63643030)
        {
            pthread_mutex_unlock(&m_HanNomManCBuffer);
            //ReleaseMutex(m_HanNomManCBuffer);
            LOGI(5, "0x%08x\n", val);
            *type = 1; // VIDEO
            return i;
        }
        else if(val==0x62773130)
        {
            pthread_mutex_unlock(&m_HanNomManCBuffer);
            //ReleaseMutex(m_HanNomManCBuffer);
            LOGI(5, "0x%08x\n", val);
            *type = 0; // AUDIO
            return i;
        }
    }

    pthread_mutex_unlock(&m_HanNomManCBuffer);
    //ReleaseMutex(m_HanNomManCBuffer);
    return -1;
}