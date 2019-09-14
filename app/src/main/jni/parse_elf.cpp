#include "com_zengge_nbmanager_Features.h"

#include <cstdlib>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <fcntl.h>
#include <android/log.h>
#include <elf.h>
#include <sys/stat.h>
#include <unistd.h>

char *j2c(JNIEnv *env, jstring jstr) {
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

int elf_identification(int fd) {
#if defined(__x86_64__)
    Elf64_Ehdr header;
#else
    Elf32_Ehdr header;
#endif

    if(read(fd, &header, sizeof(header)) == -1) {
        return 0;
    }

    return memcmp(&header.e_ident[EI_MAG0], ELFMAG, SELFMAG) == 0;
}

bool iself(const char *f) {
    int elffd;
    if((elffd = open(f, O_RDONLY)) == -1) {
        return false;
    }
    if(!elf_identification(elffd)) {
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL Java_com_zengge_nbmanager_Features_isValidElf
(JNIEnv *env, jclass cls, jstring js) {
    const char *st = env->GetStringUTFChars(js, NULL);
    if(!iself(st)) return JNI_FALSE;
    else return JNI_TRUE;
}
