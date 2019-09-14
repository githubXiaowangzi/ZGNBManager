#include "com_zengge_nbmanager_Features.h"
#include <cstdlib>
#include <cstdio>
#include <cstdint>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>
#include <android/log.h>
#define DEX_OPT_MAGIC   "dey\n"
#define DEX_MAGIC       "dex\n"

struct odex_header {
    uint8_t magic[8];
    uint32_t dex_off;
    uint32_t dex_len;
};

struct dex_header {
    uint8_t magic[8];
    uint8_t padding[24];
    uint32_t file_len;
};

static void logerr(char *msg) {
    __android_log_print(ANDROID_LOG_ERROR, "Odex2Dex", "%s", msg);
}

bool procodex(char *in, char *out) {
    int ret = -1;
    int odexFD = 0, dexFD = 0;
    size_t odexSize = 0, dexSize = 0;
    if((odexFD = open(in, O_RDONLY)) == -1) {
        logerr("open odex error");
        return false;
    }
    if((dexFD = open(out, O_RDWR | O_CREAT, 00660)) == -1) {
        logerr("open dex error");
        close(odexFD);
        return false;
    }
    struct stat Stat;
    if(fstat(odexFD, &Stat)) {
        logerr("fstat odex error");
        close(odexFD);
        close(dexFD);
        return false;
    }
    odexSize = Stat.st_size;
    uint8_t *odexBuf = NULL, *dexBuf = NULL;
    if((odexBuf = (uint8_t *)mmap(NULL, odexSize, PROT_READ, MAP_SHARED, odexFD, 0)) == MAP_FAILED) {
        logerr("mmap odex error");
        close(odexFD);
        close(dexFD);
        return false;
    }
    struct odex_header *odexH = (struct odex_header *)odexBuf;
    struct dex_header *dexH = (struct dex_header *)(odexBuf + odexH->dex_off);
    if(memcmp(odexH->magic, DEX_OPT_MAGIC, sizeof(uint8_t))) {
        logerr("odex bad magic error\n");
        munmap(odexBuf, odexSize);
        close(odexFD);
        close(dexFD);
        return false;
    }
    if(memcmp(dexH->magic, DEX_MAGIC, sizeof(uint8_t))) {
        logerr("dex bad magic error");
        munmap(odexBuf, odexSize);
        close(odexFD);
        close(dexFD);
        return false;
    }
    if(odexH->dex_len != dexH->file_len || (odexH->dex_len + 40) > odexSize) {
        logerr("dex bad file len error");
        munmap(odexBuf, odexSize);
        close(odexFD);
        close(dexFD);
        return false;
    }
    dexSize = odexH->dex_len;
    if(ftruncate(dexFD, dexSize)) {
        logerr("ftruncate dex error");
        munmap(odexBuf, odexSize);
        close(odexFD);
        close(dexFD);
        return false;
    }
    if((dexBuf = (uint8_t *)mmap(NULL, dexSize, PROT_READ | PROT_WRITE, MAP_SHARED, dexFD, 0)) == MAP_FAILED) {
        logerr("mmap dex error");
        munmap(odexBuf, odexSize);
        close(odexFD);
        close(dexFD);
        return false;
    }
    memcpy(dexBuf, odexBuf + odexH->dex_off, dexSize);
    munmap(dexBuf, dexSize);
    munmap(odexBuf, odexSize);
    close(odexFD);
    close(dexFD);
    return true;
}

char *j0c(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *)malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}

JNIEXPORT jboolean JNICALL Java_com_zengge_nbmanager_Features_Odex2Dex
(JNIEnv *env, jclass cls, jstring jstr1, jstring jstr2) {
    char *s1 = j0c(env, jstr1);
    char *s2 = j0c(env, jstr2);
    if(procodex(s1, s2)) return JNI_TRUE;
    else return JNI_FALSE;
}
