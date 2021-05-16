//
// Created by renzhenfei on 2021/4/4.
//

#ifndef RTMP_DEMO_SAFEQUEUE_H
#define RTMP_DEMO_SAFEQUEUE_H

#include <queue>
#include <pthread.h>
#include <mutex>

template<typename T>
class SafeQueue{
public:
    typedef void(*ReleaseCallback)(T&);
    SafeQueue(){
        pthread_mutex_init(&mutex, nullptr);
    }

    void Put(T data){
        pthread_mutex_lock(&mutex);
        datas.push(data);
        pthread_mutex_unlock(&mutex);
    }

    void Get(T& data){
        pthread_mutex_lock(&mutex);
        if (datas.size() <= 0){
            pthread_mutex_unlock(&mutex);
            return;
        }
        LOGE("================44=============");
        data = datas.front();
        datas.pop();
        pthread_mutex_unlock(&mutex);
    }

    void Clear(){
        pthread_mutex_lock(&mutex);
        for (int i = 0; i < datas.size(); ++i) {
            T data = datas.front();
            datas.pop();
            this->releaseCallback(data);
        }
        pthread_mutex_unlock(&mutex);
    }

    void SetReleaseCallback(ReleaseCallback releaseCallback1){
        this->releaseCallback = releaseCallback1;
    }

    ~SafeQueue(){
        pthread_mutex_destroy(&mutex);
    }

private:
    std::queue<T> datas;
    pthread_mutex_t mutex;
    ReleaseCallback releaseCallback;
};

#endif //RTMP_DEMO_SAFEQUEUE_H
