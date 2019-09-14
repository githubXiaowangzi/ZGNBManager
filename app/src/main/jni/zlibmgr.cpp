#include <cstring>
#include <cstdlib>
#include <iostream>
#include <string>
#include <zlib.h>
#include "zlibmgr.h"
#include <sstream>
#include "com_zengge_nbmanager_Features.h"
#include <android/log.h>

using namespace std;

extern CZlibMgr g_kZlibMgr;

void mydbg(const char *msg) {
    __android_log_print(ANDROID_LOG_INFO, "ZGNB String Compression", "%s" , msg);
}

string doCompress(string hello) {
    Byte compr[200000], uncompr[200000];    // big enough
    string decdata;
    memset(compr, 0, 200000);
    memset(uncompr, 0, 200000);
    unsigned long u1, u2;
    u1 = 0;
    u2 = 0;
    char sOutBuf[80960];
    g_kZlibMgr.Compress(hello.c_str(), sOutBuf, u1);
    int *strd = new int[strlen(sOutBuf)];
    for(int k = 0; k < strlen(sOutBuf); k++) {
        strd[k] = (int)sOutBuf[k];
        strd[k] = abs(strd[k]);
    }
    //cout << "Compressed Integer Data:" << endl;
    stringstream ss;
    //ss << "Compressed Integer Data:" << endl;
    for(int k = 0; k < strlen(sOutBuf); k++) {
        ss << strd[k];
    }
    decdata = ss.str();
    mydbg(decdata.c_str());
    char sUnCompressBuf[8096];
    memset(sUnCompressBuf, 0, sizeof(sUnCompressBuf));
    g_kZlibMgr.UnCompress(sOutBuf, sUnCompressBuf, u1);
    stringstream sa;
    //sa << "Uncompressed Data:" << endl;
    sa << sUnCompressBuf << endl;
    mydbg(sa.str().c_str());
    return decdata;
}

JNIEXPORT jstring JNICALL Java_com_zengge_nbmanager_Features_compressStrToInt
(JNIEnv *env, jclass cls, jstring jstr) {
    const char *s1 = env->GetStringUTFChars(jstr, NULL);
    string p = doCompress(string(s1));
    int len1 = string(s1).length(), len2 = p.length();
    stringstream as;
    as << "Compress Ratio:" << (double)((double)(len2 / len1)) * 100 << "%";
    mydbg(as.str().c_str());
    return env->NewStringUTF(p.c_str());
}
