#include <jni.h>
#include <string>
#include <rtmp.h>
#include <x264.h>
#include "mcro.h"
#include "SafeQueue.h"
#include "VideoChannel.h"
#include "AudioChannel.h"
static SafeQueue<RTMPPacket*> packets; NOLINT
static VideoChannel *videoChannel;
static AudioChannel *audioChannel;
static int isStart = 0;
static int readyPushing = 0;
static uint32_t startTime;

void ReleaseCallback(RTMPPacket*& packet){
    DELETE(packet);
}

void VideoCallback(RTMPPacket *packet){
    if (packet){
        packet->m_nTimeStamp = RTMP_GetTime() - startTime;
        packets.Put(packet);
        LOGE("put RTMPPacket ................................");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    videoChannel = new VideoChannel;
    videoChannel->SetVideoCallback(VideoCallback);
    ::packets.SetReleaseCallback(ReleaseCallback);
}

void ReleaseRtmpPacket(RTMPPacket*& rtmpPacket){
    if (rtmpPacket){
        RTMPPacket_Free(rtmpPacket);
        delete rtmpPacket;
        rtmpPacket = 0;
    }
}


//rtmp连接任务
void * rtmpTask(void * arg){
    char* livePath = static_cast<char *>(arg);
    RTMP *rtmp;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp){
            LOGE("创建rtmp失败");
            break;
        }
        RTMP_Init(rtmp);
        //设置超时时间
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp,livePath);
        if (!ret){
            LOGE("rtmp设置地址失败%s",livePath);
            break;
        }
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp,nullptr);
        if (!ret){
            LOGE("连接服务器失败");
            break;
        }
        readyPushing = 1;
        startTime = RTMP_GetTime();
        RTMPPacket *rtmpPacket = nullptr;
        while (isStart){
            packets.Get(rtmpPacket);
            if (!rtmpPacket){
                continue;
            }
            LOGE("================22=============");
            rtmpPacket->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp,rtmpPacket,1);
            ReleaseRtmpPacket(rtmpPacket);
            if (!ret){
                LOGE("RTMP_SendPacket失败");
                break;
            }
        }
        ReleaseRtmpPacket(rtmpPacket);
    }while (false);
    //释放
    packets.Clear();
    if (rtmp){
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = nullptr;
    }
    delete[] livePath;
    return nullptr;
}

static pthread_t t;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path) {

    if (isStart){
        return;
    }
    const char *temp = env->GetStringUTFChars(path,nullptr);
    isStart = 1;
    char *livePath = new char[strlen(temp) + 1];
    strcpy(livePath,temp);
    pthread_create(&t, nullptr, rtmpTask, livePath);
    env->ReleaseStringUTFChars(path,temp);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1SetVideoEnvInfo(JNIEnv *env, jobject thiz,
                                                               jint width, jint height,
                                                               jint bitrate, jint fps) {
    if (videoChannel){
        videoChannel->SetVideoEnvInfo(width,height,bitrate,fps);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1PushVideo(JNIEnv *env, jobject thiz,
                                                         jbyteArray bits) {
    if (!videoChannel){
        return;
    }
    LOGE("start video encodec ..............................................");
    jbyte *buf = env->GetByteArrayElements(bits, 0);
    videoChannel->EncodeData(buf);
    env->ReleaseByteArrayElements(bits,buf,0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1Stop(JNIEnv *env, jobject thiz) {

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_rtmp_1demo_LivePusher_native_1Release(JNIEnv *env, jobject thiz) {

}