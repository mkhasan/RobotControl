//
// Created by usrc on 16. 12. 12.
//

//
// Created by usrc on 16. 12. 7.
//

#include <pthread.h>
#include <unistd.h>
#include <android/log.h>
#include <android/native_window.h>

#include "native_test.h"
#include "player.h"

#ifdef __cplusplus
extern "C" {
#endif

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>


#ifdef __cplusplus
}
#endif

void  * thread_func(void * arg);
int native_func()
{


    pthread_t tid;
    char *b;
    int ret=0;//pthread_create(&tid, NULL, thread_func, NULL);

    //if (ret == 0)
    //  pthread_join(tid, (void **)&b);

    //thread_func(NULL);




    return ret;
}


void * thread_func(void  * arg) {

    enum AVPixelFormat out_format;

    /*
    AVFormatContext *pFormatCtx = NULL;
    int i, videoStream;
    AVCodecContext *pCodecCtx = NULL;
    AVCodecContext *pCodecCtxOrig = NULL;
    AVCodec *pCodec = NULL;


    int numBytes;
    uint8_t *buffer = NULL;
     */

    AVFrame *pFrame = NULL;
    AVFrame *pFrameRGB;
    AVPacket packet;
    int frameFinished;
    ANativeWindow_Buffer buf;
    ANativeWindow * window = NULL;
    int i = 1;

    int format;

    AVDictionary *optionsDict = NULL;
    struct SwsContext *sws_ctx = NULL;

    struct Player * player = (struct Player *) arg;

    if (player == NULL)
    {
        __android_log_print(ANDROID_LOG_ERROR, "msg:", "Error in finding player");
        return NULL;
    }

    window = player->window;


    if (window == NULL)
    {
        __android_log_print(ANDROID_LOG_ERROR, "msg:", "Error in finding window");
        return NULL;
    }


    static int k = 0;
    char str[100] = "rtsp://admin:admin@192.168.1.100:554/12";
    //char str[100] = "rtsp://admin:admin@192.168.1.101:554/stream1";

    AVFormatContext *pFormatCtx = player->input_format_ctx;
    /*
    player->videoStream = -1;
    for (i = 0; i < pFormatCtx->nb_streams; i++)
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            player->videoStream = i;
            break;
        }
    player->pCodecCtxOrig = pFormatCtx->streams[player->videoStream]->codec;


    // Find the decoder for the video stream
    player->pCodec = avcodec_find_decoder(player->pCodecCtxOrig->codec_id);
    if (player->pCodec == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "msg:", "%s \n", "Unsupported codec!");
       goto error;
    }

    player->pCodecCtx = avcodec_alloc_context3(player->pCodec);

    if (player->pCodecCtx == NULL || avcodec_copy_context(player->pCodecCtx, player->pCodecCtxOrig) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "msg:", "%s \n", "Codec copy error!");
        goto error;

    }



    if(avcodec_open2(player->pCodecCtx, player->pCodec, &optionsDict)<0)
        goto error;

     */

    player->pCodecCtx = player->input_format_ctx->streams[player->video_stream_no]->codec;
    __android_log_print(ANDROID_LOG_ERROR, "msg:", "%s \n", "Codec found");

    __android_log_print(ANDROID_LOG_ERROR, "msg:", "this pixfmt %d, available pixfmt : %d %d draw = %d", player->pCodecCtx->pix_fmt, AV_PIX_FMT_YUV420P, AV_PIX_FMT_NV12, 0);

    __android_log_print(ANDROID_LOG_ERROR, "msg:", "this pixfmt %d, available pixfmt : %d %d draw = %d", player->pCodecCtx->pix_fmt, AV_PIX_FMT_YUV420P, AV_PIX_FMT_NV12, 0);


    LOGE(1, "came here 2 %d !!! \n", -1);


    pFrame=av_frame_alloc();

    // Allocate an AVFrame structure
    pFrameRGB=av_frame_alloc();
    /*
    if(pFrameRGB==NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "msg:", "%s \n", "pFrameRGB: Memory allocation error");
        return NULL;
    }
     */

    // Determine required buffer size and allocate buffer

    LOGE(1, "came here 2 %d !!! \n", -2);


    ANativeWindow_setBuffersGeometry(window, player->pCodecCtx->width, player->pCodecCtx->height, WINDOW_FORMAT_RGBA_8888);

    if (ANativeWindow_lock(window, &buf, NULL) != 0)
        goto error;

    format = buf.format;
    out_format = AV_PIX_FMT_RGBA;

    LOGE(1, "came here 2 %d !!! \n", -3);
    __android_log_print(ANDROID_LOG_ERROR, "msg:", "width : %d and height %d \n", player->pCodecCtx->width, player->pCodecCtx->height);
    LOGE(1, "came here 2 %d !!! \n", -4);
    sws_ctx =
            sws_getContext
                    (
                            player->pCodecCtx->width,
                            player->pCodecCtx->height,
                            player->pCodecCtx->pix_fmt,
                            player->pCodecCtx->width,
                            player->pCodecCtx->height,
                            out_format,
                            SWS_BILINEAR,
                            NULL,
                            NULL,
                            NULL
                    );

    LOGE(1, "came here 2 %d !!! \n", -5);
    avpicture_fill((AVPicture *) pFrameRGB, (uint8_t *)buf.bits, out_format,
                   buf.width, buf.height);

    LOGE(1, "came here 2 %d !!! \n", -6);
    pFrameRGB->data[0] = (uint8_t *) buf.bits;
    pFrameRGB->linesize[0] = buf.stride*4;


    i=0;
    while(av_read_frame(player->input_format_ctx, &packet)>=0) {
        // Is this a packet from the video stream?
        if(packet.stream_index==player->videoStream) {
            // Decode video frame
            avcodec_decode_video2(player->pCodecCtx, pFrame, &frameFinished,
                                  &packet);

            // Did we get a video frame?
            if(frameFinished) {
                // Convert the image from its native format to RGB
                sws_scale
                        (
                                sws_ctx,
                                (uint8_t const * const *)pFrame->data,
                                pFrame->linesize,
                                0,
                                player->pCodecCtx->height,
                                pFrameRGB->data,
                                pFrameRGB->linesize
                        );

                // Save the frame to disk
                //if(++i<=25)
                //  SaveFrame(pFrameRGB, pCodecCtx->width, pCodecCtx->height,
                //i);

                __android_log_print(ANDROID_LOG_ERROR, "msg:", "Frame %d : width: %d height: %d \n", i, player->pCodecCtx->width, player->pCodecCtx->height);
            }
        }

        // Free the packet that was allocated by av_read_frame
        av_free_packet(&packet);

        if (++i == 20)
            break;
    }


    error:

    ANativeWindow_unlockAndPost(window);



    av_frame_free(&player->rgb_frame);
    av_frame_free(&player->tmp_frame);
    av_frame_free(&player->tmp_frame2);
    av_frame_free(&player->input_frames[0]);

    free(player->tmp_buffer);
    free(player->tmp_buffer2);
    av_frame_free(&pFrameRGB);

    // Free the YUV frame
    av_frame_free(&pFrame);

    avcodec_close(player->pCodecCtx);
    //avcodec_close(player->pCodecCtxOrig);
    avformat_close_input(&player->input_format_ctx);

    LOGE(1, "video id %d \n", player->videoStream);
    return NULL;




}