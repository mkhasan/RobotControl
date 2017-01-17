//
// Created by usrc on 17. 1. 16.
//

#ifndef ROBOTCONTROL_CIRCULARBUFFER_H
#define ROBOTCONTROL_CIRCULARBUFFER_H




#include <pthread.h>

#include "Def.h"


class CCircularBuffer
{
public:
    CCircularBuffer(void);
    CCircularBuffer(int size);
    ~CCircularBuffer();

private:
    BYTE  *m_pBuffer;
    int    m_Size, m_In, m_Out;
    BOOL   m_bReadHeader;

    pthread_mutex_t m_HanNomManCBuffer;


public:
    int Read(BYTE *dest, int length);
    int Write(BYTE *src, int length);
    int Peek(BYTE *dest, int length);
    int Dump(int length);
    int MoveData(CCircularBuffer *src, int length);
    // JSPARK added to receive data from socket directly 08.11.19


    int Receive(int hSocket);





    int   UsedSpace(void);
    int   FreeSpace(void);
    float FreeSpaceRatio(void);

    int  MovePos(int add);
    int  Search(const char* str);
    // Hard Coded Searcher
    int  Search2(int *pos);

    void SetBufferSize(int size);
    int  GetBufferSize(void) { return m_Size; }
    void Reset(void);

    BOOL IsReadHeader(void)     {return m_bReadHeader;}
    void SetReadHeader(BOOL rh) {m_bReadHeader = rh;}
};



#endif //ROBOTCONTROL_CIRCULARBUFFER_H
