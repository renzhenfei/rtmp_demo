//
// Created by renzhenfei on 2021/4/4.
//

#ifndef RTMP_DEMO_VIDEOCHANNEL_H
#define RTMP_DEMO_VIDEOCHANNEL_H

#include <x264.h>
#include <pthread.h>
#include "mcro.h"
#include <stdlib.h>
#include <string>
#include <rtmp.h>

class VideoChannel {
    typedef void(*VideoCallback)(RTMPPacket*);
public:
    VideoChannel();

    ~VideoChannel();

    void SetVideoEnvInfo(int width, int height, int bitrate, int fps);

    void EncodeData(int8_t *buf);

    void SetVideoCallback(VideoCallback videoCallback);

private:
    pthread_mutex_t mutex;
    int mWidth;
    int mHeight;
    int mBitrate;
    int mFps;
    x264_t *videoCodec = nullptr;
    x264_picture_t *in = nullptr;
    VideoCallback videoCallback;
    int index = 0;

    void SendSpsPPs(uint8_t sps[100], uint8_t pps[100], int sps_len, int pps_len);

    void sendFrame(int type, uint8_t *payload, int i_payload);
};


#endif //RTMP_DEMO_VIDEOCHANNEL_H
