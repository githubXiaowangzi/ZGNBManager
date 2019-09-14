#include "com_zengge_nbmanager_Features.h"
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <android/log.h>

#define _UNIX
#include "unrar/dll.hpp"

#define EXCEPTION -1601

JNIEXPORT jint JNICALL Java_com_zengge_nbmanager_Features_ExtractAllRAR
(JNIEnv *env, jclass cls, jstring archive, jstring target) {
    try {
        const char *targetDir = env->GetStringUTFChars(target, NULL);
        const char *rarName = env->GetStringUTFChars(archive, NULL);
        RAROpenArchiveDataEx archiveInfo;
        memset(&archiveInfo, 0, sizeof(archiveInfo));
        archiveInfo.CmtBuf = NULL;
        archiveInfo.OpenMode = RAR_OM_EXTRACT;
        archiveInfo.ArcName = (char *)rarName;
        HANDLE rarFile = RAROpenArchiveEx(&archiveInfo);
        RARHeaderDataEx fileInfo;
        while(true) {
            int RHCode = RARReadHeaderEx(rarFile, &fileInfo);
            if(RHCode != 0) break;
            __android_log_print(ANDROID_LOG_VERBOSE, "UnRar", "Extracting %s (%d => %d)", fileInfo.FileName, fileInfo.PackSize, fileInfo.UnpSize);
            int PFCode = RARProcessFile(rarFile, RAR_EXTRACT, (char *)targetDir, NULL);
        }
        env->ReleaseStringUTFChars(archive, rarName);
        env->ReleaseStringUTFChars(target, targetDir);
        return 0;
    } catch (...) {
        return EXCEPTION;
    }
}
