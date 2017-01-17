//
// Created by usrc on 17. 1. 16.
//

#include <cmath>

#include "Protocol.h"
#include "ir_viewer.h"
#include "CircularBuffer.h"
#include "Def.h"

#define LIVE_MPEG    0
#define LIVE_JPEG    1
#define LIVE_H264    2

#define FRAME_PREFIX_SIZE 12
#define WORD unsigned short

int DecodeFrame(struct Viewer * pViewer)
{
    int     display=0, retry = 5;
    int     size, realsize;
    int     frameType = -1;
    BYTE   *pFrameData;
    TCP_LIVE LiveHeader;
    static unsigned short frame_t[WIDTH*HEIGHT]={0,};
    CCircularBuffer * m_pStreamBuffer = pViewer->pStreamBuffer;
    BYTE * m_pFrameData = pViewer->pFrameData;
    BYTE * m_pOutFrame = pViewer->pOutFrame;


    // Check how many frames can be read from buffer.
#ifdef JNI

#endif

    int siZe;


    while(1)
    {
        // First, peek 20 Bytes.. and check whether it is valid or not.



        if((siZe=m_pStreamBuffer->Peek((BYTE*)&LiveHeader,sizeof(TCP_LIVE))!=sizeof(TCP_LIVE))) {

            LOGE(1, "size is %d", siZe);
            break;
        }


        switch(LiveHeader.Codec)
        {
            case 'M' : frameType = LIVE_MPEG; break;
            case 'J' : frameType = LIVE_JPEG; break;
            case 'H' : frameType = LIVE_H264; break;
            default:
            {
                int pos = 0, type;
                retry--;
                if(retry<=0) return 0;
                LOGI(5, "Broken header.\nSearching...");
                // Search Correct Position
                pos = m_pStreamBuffer->Search2(&type);

                // Move to correct position
                if(pos>=0)
                {
                    int bufsize = m_pStreamBuffer->GetBufferSize();
                    int movpos;
                    if(type==0) // AUDIO
                        movpos = (pos-(sizeof(TCP_LIVE)) + bufsize) % bufsize;
                    else // VIDEO
                        movpos = (pos-(sizeof(TCP_LIVE)+4) + bufsize) % bufsize;

                    m_pStreamBuffer->MovePos(movpos);
                    LOGI(5, "Got it. Move to correct position..");
                    continue;
                }
                else
                {
                    LOGI(2, "No header found");
                    break;
                }
            }
        }
        size = LiveHeader.Size;

        if((size+sizeof(TCP_LIVE)) > m_pStreamBuffer->UsedSpace())
            break;

        switch(frameType)
        {
            ///////////// ADD MOTION DETECTION /////////////////
            case LIVE_H264:
            case LIVE_MPEG:
            case LIVE_JPEG:
            {
                pViewer->cx = LiveHeader.Info2;
                pViewer->cy = LiveHeader.Info3;

                m_pStreamBuffer->MovePos(sizeof(TCP_LIVE));
                m_pStreamBuffer->Read(m_pFrameData, size);

                realsize = size - FRAME_PREFIX_SIZE;
                pFrameData = m_pFrameData;
                //memcpy(m_pOutFrame,pFrameData+FRAME_PREFIX_SIZE, realsize);
                char *ptr = (char*)(pFrameData+FRAME_PREFIX_SIZE);
                WORD t=0,max,min;
                max=0;
                min=9999;
                char low,high,temp[2];

                if(pViewer->cx > WIDTH || pViewer->cy > HEIGHT) {
                    LOGE(1, "Too large data")
                    return 0;
                }
                for(int y=0;y<pViewer->cy;y++) {
                    for(int x=0;x<pViewer->cx;x++) {
                        //m_pOutFrame[(pViewer->cx*3)*y+3*x+0] = (ptr[pViewer->cx*2*y + 2*x + 1]>>6) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x1F) << 2) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x40)<<1);
                        //m_pOutFrame[(pViewer->cx*3)*y+3*x+1] = (ptr[pViewer->cx*2*y + 2*x + 1]>>6) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x1F) << 2) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x40)<<1);
                        //m_pOutFrame[(pViewer->cx*3)*y+3*x+2] = (ptr[pViewer->cx*2*y + 2*x + 1]>>6) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x1F) << 2) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x40)<<1);

                        high=(ptr[pViewer->cx*2*y + 2*x + 0]&0x1F) | ((ptr[pViewer->cx*2*y + 2*x + 0]&0x40)>>1);
                        low=ptr[pViewer->cx*2*y + 2*x + 1];

                        temp[1]=high;
                        temp[0]=low;

                        memcpy(&t,temp,sizeof(temp));
                        frame_t[pViewer->cx*y+x]=t;


                        if(t>=max) max=t;
                        if(t<min) min=t;



                    }
                }

                WORD nsample = 0;
                WORD sample;

                int i=0;
                float span = (float)(max - min + 1);

                int x=0;
                uint8_t* pDst=m_pOutFrame;
                //uint8_t * p = (uint8_t *) &pSrc[0];
                for(int y=0;y<pViewer->cy;y++) {
                    for(int x=0;x<pViewer->cx;x++) {
                        sample = frame_t[pViewer->cx*y+x];

                        nsample = (BYTE) (((sample - min) / span) * 0xFF);
                        nsample = nsample/2;

                        pDst[i++] = iron[nsample].r;
                        pDst[i++] = iron[nsample].g;
                        pDst[i++] = iron[nsample].b;
                        //i++;
                    }
                }
                /*


                DWORD v1=min;
                DWORD v2=max;

                float T1;
                float T2;

                double objSig1,objSig2;
                double m_K1=1.0;
                double m_K2=30.0;
                int    m_R=414904;
                double m_B=1428.0;
                double m_F=1.0;
                double m_O=-493.7;

                objSig1 = m_K1 * (double)v1 - m_K2;
                objSig2 = m_K1 * (double)v2 - m_K2;
                T1 = (float)(m_B / log(m_R /(objSig1 - m_O) + m_F))-273.15;
                T2 = (float)(m_B / log(m_R /(objSig2 - m_O) + m_F))-273.15;

                */
                pViewer->frameNum++;
                LOGI(5, "frame received %d (%d x %d) \n", pViewer->frameNum, pViewer->cx, pViewer->cy);
                /*
                if(m_StartTime==(clock_t)-1)
                    m_StartTime = clock();

                ZeroMemory(&m_BMPInfo, sizeof(BITMAPINFO));
                m_BMPInfo.bmiHeader.biSize        = sizeof(BITMAPINFOHEADER);
                m_BMPInfo.bmiHeader.biWidth       =  m_szImage.cx;
                m_BMPInfo.bmiHeader.biHeight      = -m_szImage.cy;
                m_BMPInfo.bmiHeader.biPlanes      = 1;
                m_BMPInfo.bmiHeader.biBitCount    = 3 * 8;
                m_BMPInfo.bmiHeader.biCompression = BI_RGB;

                 */
                display = 1;

                break;
            }
                // Wrong Codec
            default:
                LOGE(1, "Wrong Codec Type.\n");
                m_pStreamBuffer->Dump(sizeof(TCP_LIVE) + size);
                return 0;
        }
    }
    return display;
}

#ifdef JNI
void Render(struct Viewer * pViewer) {


    BYTE *data;
    unsigned int pixel;
    int x, y;
    ANativeWindow_Buffer buffer;
    ANativeWindow *window = pViewer->window;
    BYTE * m_pOutFrame = pViewer->pOutFrame;
    if (window == NULL) {

        HandleError(ERROR_RENDERING_FAILED);
        return;
    }



    ANativeWindow_setBuffersGeometry(window, pViewer->cx, pViewer->cy,
                                     WINDOW_FORMAT_RGBA_8888);
    if (ANativeWindow_lock(window, &buffer, NULL) != 0) {
        return;
    }


    int format = buffer.format;
    if (format < 0) {
        HandleError(ERROR_COULD_NOT_GET_WINDOW_FORMAT);
        return;
    }

    data = (BYTE *)buffer.bits;

    /*
     *
                SDL_LockMutex(screen_mutex);
                unsigned int pixel = 0x0;
                int x, y;

                for (y=0; y<pViewer->cy; y++) {
                    for (x=0; x<pViewer->cx; x++) {
                        pixel = m_pOutFrame[(pViewer->cx*3)*y+3*x+2] | (m_pOutFrame[(pViewer->cx*3)*y+3*x+1] << 8) | (m_pOutFrame[(pViewer->cx*3)*y+3*x+0] << 16);
                        //pixel = 0xff;
                        set_pixel(screen, x, y, pixel);
                    }
                }
                SDL_Flip(screen);//pIR_Viewer->display);

                SDL_UnlockMutex(screen_mutex);
     */

    int pixelSize = 4;
    int factor = 1;

    for(y=0; y<pViewer->cy; y++) {
        for (x=0; x<pViewer->cx; x++) {
            pixel = m_pOutFrame[(pViewer->cx*3)*y+3*x+2] | (m_pOutFrame[(pViewer->cx*3)*y+3*x+1] << 8) | (m_pOutFrame[(pViewer->cx*3)*y+3*x+0] << 16);
            pixel = 0x00ff;
            //memcpy(&data[buffer.stride*y+x*sizeof(pixel)],&pixel, sizeof(pixel));
            data[y*buffer.stride*pixelSize+pixelSize*x+0] = m_pOutFrame[(pViewer->cx*3)*y+3*x+0]*factor;
            data[y*buffer.stride*pixelSize+pixelSize*x+1] = m_pOutFrame[(pViewer->cx*3)*y+3*x+1]*factor;
            data[y*buffer.stride*pixelSize+pixelSize*x+2] = m_pOutFrame[(pViewer->cx*3)*y+3*x+2]*factor;
            //data[y*buffer.stride*pixelSize+pixelSize*x+3] = 0x00;
            //LOGE(1, "val1 is %d val2 is %d val3 is %d", m_pOutFrame[(pViewer->cx*3)*y+3*x+0], m_pOutFrame[(pViewer->cx*3)*y+3*x+1], m_pOutFrame[(pViewer->cx*3)*y+3*x+2]);
        }

    }


    ANativeWindow_unlockAndPost(window);

    //LOGE(1, "width is %d height is %d stride is %d", buffer.width, buffer.height, buffer.stride);


}

#else

void Render(struct Viewer * pViewer) {
    BYTE * m_pOutFrame = pViewer->pOutFrame;
    SDL_LockMutex(screen_mutex);
    unsigned int pixel = 0x0;
    int x, y;

    for (y=0; y<pViewer->cy; y++) {
        for (x=0; x<pViewer->cx; x++) {
            pixel = m_pOutFrame[(pViewer->cx*3)*y+3*x+2] | (m_pOutFrame[(pViewer->cx*3)*y+3*x+1] << 8) | (m_pOutFrame[(pViewer->cx*3)*y+3*x+0] << 16);
            //pixel = 0xff;
            set_pixel(screen, x, y, pixel);
        }
    }
    SDL_Flip(screen);//pIR_Viewer->display);

    SDL_UnlockMutex(screen_mutex);
}
#endif
