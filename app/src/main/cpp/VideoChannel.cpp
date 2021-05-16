//
// Created by renzhenfei on 2021/4/4.
//

#include "VideoChannel.h"
VideoChannel::VideoChannel(){ NOLINT
    pthread_mutex_init(&mutex, nullptr);
}

VideoChannel::~VideoChannel(){
    pthread_mutex_destroy(&mutex);
}
//创建x264编码器
void VideoChannel::SetVideoEnvInfo(int width, int height, int bitrate, int fps) {
    pthread_mutex_lock(&mutex);
    if (videoCodec){
        //释放之前的编码器
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }
    if (in){
        x264_picture_clean(in);
        delete in;
        in = nullptr;
    }
    mWidth = width;
    mHeight = height;
    mBitrate = bitrate;
    mFps = fps;
    //编码器参数
    x264_param_t param;
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    param.i_level_idc = 32;
    param.i_csp = X264_CSP_I420;
    param.i_width = mWidth;
    param.i_height = mHeight;
    param.i_bframe = 0;//无b帧
    param.rc.i_rc_method = X264_RC_ABR; //cqp 恒定质量 crf恒定码率 abr 平均码率
    param.rc.i_bitrate = mBitrate / 1000;
    //瞬时最大码率
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;
    param.rc.i_vbv_buffer_size = bitrate / 1000; //码率控制区大小
    param.b_repeat_headers = 1;
    //帧率
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;

    param.b_vfr_input = 0;
    param.i_keyint_max = fps * 2;
    param.i_threads = 1;
    x264_param_apply_profile(&param,"baseline");
    videoCodec = x264_encoder_open(&param);
    in = new x264_picture_t;
    x264_picture_alloc(in,X264_CSP_I420,mWidth,mHeight);
    pthread_mutex_unlock(&mutex);
}
/**
 * @param buf NV21
 */
void VideoChannel::EncodeData(int8_t *buf) {
    LOGE("encode data -----------------1111------------");
    pthread_mutex_lock(&mutex);
    int count = mWidth * mHeight * 3 / 2;
    memcpy(in->img.plane[0],buf,mWidth * mHeight);//y
    for (int i = mWidth * mHeight,j = 0; i < count; i+=2,j++) {
        //u
        in->img.plane[1][j] = buf[i + 1];
        //v
        in->img.plane[2][j] = buf[i];
    }
    in->i_pts = index++;
    //编码出的数据
    x264_nal_t *pp_nal;
    //编码出的数据个数
    int pi_nal;
    x264_picture_t pic_out;
    x264_encoder_encode(videoCodec,&pp_nal,&pi_nal,in,&pic_out);
    int sps_len,pps_len;
    uint8_t sps[100],pps[100];
    for (int i = 0; i < pi_nal; ++i) {
        if (pp_nal[i].i_type == NAL_PPS){
            pps_len = pp_nal[i].i_payload - 4;
            memcpy(pps,pp_nal[i].p_payload + 4,pps_len);
            //pps紧跟着sps
            SendSpsPPs(sps,pps,sps_len,pps_len);
            LOGE("encode data -----------------222222------------");
        } else if (pp_nal[i].i_type == NAL_SPS){
            sps_len = pp_nal[i].i_payload - 4;
            memcpy(sps,pp_nal[i].p_payload + 4,sps_len);
            LOGE("encode data -----------------33333------------");
        } else{
            sendFrame(pp_nal[i].i_type,pp_nal[i].p_payload,pp_nal[i].i_payload);
            LOGE("encode data -----------------44444------------");
        }
    }
    pthread_mutex_unlock(&mutex);
}

void VideoChannel::SendSpsPPs(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    auto *packet = new RTMPPacket;
    int bodySize = 13 + sps_len + 3 + pps_len;
    // todo
    RTMPPacket_Alloc(packet,bodySize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);

    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    //随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    if (videoCallback){
        videoCallback(packet);
    }
}

void VideoChannel::SetVideoCallback(VideoChannel::VideoCallback videoCallback) {
    this->videoCallback = videoCallback;
}

void VideoChannel::sendFrame(int type, uint8_t *payload, int i_payload) {
    if (payload[2] == 0x00){
        i_payload -= 4;
        payload += 4;
    } else{
        i_payload -= 3;
        payload += 3;
    }
    int bodySize = 9 + i_payload;
    auto *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet,bodySize);
    int index = 0;
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        LOGE("关键帧");
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (i_payload >> 24) & 0xff;
    packet->m_body[6] = (i_payload >> 16) & 0xff;
    packet->m_body[7] = (i_payload >> 8) & 0xff;
    packet->m_body[8] = (i_payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9],payload, i_payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    videoCallback(packet);

}
