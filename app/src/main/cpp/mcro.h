//
// Created by renzhenfei on 2021/4/4.
//

#ifndef RTMP_DEMO_LOG_H
#define RTMP_DEMO_LOG_H

#include <android/log.h>

#define TAG "RtmpDemo"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#define DELETE(obj) if(obj) {delete obj; obj = 0;}

#define NOLINT /* NOLINT */

#endif //RTMP_DEMO_LOG_H
