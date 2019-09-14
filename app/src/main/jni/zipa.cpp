#include "com_zengge_nbmanager_Features.h"
#include <ZipAlign.h>
#include <cstdlib>

JNIEXPORT jboolean JNICALL Java_com_zengge_nbmanager_Features_ZipAlign
(JNIEnv *env, jclass cls, jstring str1, jstring str2) {
    const char *s1 = env->GetStringUTFChars(str1, NULL);
    const char *s2 = env->GetStringUTFChars(str2, NULL);
    if(zipalign(s1, s2, 9, 0) == 1) return JNI_TRUE;
    else return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_zengge_nbmanager_Features_isZipAligned
(JNIEnv *env, jclass cls, jstring str) {
    const char *s = env->GetStringUTFChars(str, NULL);
    if(zipalign_is_aligned(s, 9) == 1) return JNI_TRUE;
    else return JNI_FALSE;
}
