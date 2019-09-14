#include <cstdio>
#include <cstdlib>
#include "com_zengge_nbmanager_Features.h"
#include <fstream>
#include <string>
#include <android/log.h>

using namespace std;

void mydebug(const char *msg) {
    __android_log_print(ANDROID_LOG_INFO, "ZGNB jni Main Function", "%s" , msg);
}

JNIEXPORT void JNICALL Java_com_zengge_nbmanager_Features_printLog
(JNIEnv *env, jclass cls, jstring file, jstring content, jboolean append) {
    const char *s1 = env->GetStringUTFChars(file, NULL);
    const char *s2 = env->GetStringUTFChars(content, NULL);
    ofstream os;
    mydebug(s1);
    if(append == JNI_TRUE) {
        os.open(string(s1), ios::app);
    } else {
        os.open(string(s1));
    }
    if(os.is_open()) {
        os << string(s2) << endl;
        os.close();
    }
}
