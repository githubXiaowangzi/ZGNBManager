#ifndef _ZLIBMGR
#define _ZLIBMGR
#define  MAXBUFFERSIZE 200000
#include <iostream>
#include <string.h>
#include <stdlib.h>
#include "zlib.h"

using namespace std;

class CZlibMgr {
public:
    CZlibMgr();
    ~CZlibMgr();

    bool Compress(const char *pcContentBuf, char *pcCompBuf, unsigned long &ulCompLen);  // 压缩，pcContentBuf 要压缩的内容 pcCompBuf 压缩后的内容 ulCompLen 压缩后的长度
    bool UnCompress(const char *pcCompBuf, char *pcUnCompBuf, unsigned long ulCompLen); // 解压,pcCompBuf 压缩的内容, pcUnCompBuf 解压后的内容  ulCompLen 压缩内容的长度

private:
    Byte compr[MAXBUFFERSIZE];
    Byte uncompr[MAXBUFFERSIZE];

};

CZlibMgr::CZlibMgr() {
}

CZlibMgr::~CZlibMgr() {
}

bool CZlibMgr::Compress(const char *pcContentBuf, char *pcCompBuf, unsigned long &ulCompLen) {
    if (pcContentBuf == NULL) {
        return false;
    }

    if (strlen(pcContentBuf) == 0) {
        return false;
    }

    memset(compr, 0, MAXBUFFERSIZE);

    uLong comprLen;
    int err;

    uLong len = strlen(pcContentBuf);
    comprLen = sizeof(compr) / sizeof(compr[0]);

    err = compress(compr, &comprLen, (const Bytef *)pcContentBuf, len);
    if (err != Z_OK) {
        cout << "compess error: " << err << endl;
        return false;
    }
    cout << "orignal size: " << len << " , compressed size : " << comprLen << endl;
    cout << "Ratio:" << (double)((double)comprLen / len) * 100 << "%" << endl;
    memcpy(pcCompBuf, compr, comprLen);
    ulCompLen = comprLen;

    return true;
}

bool CZlibMgr::UnCompress(const char *pcCompBuf, char *pcUnCompBuf, unsigned long ulCompLen) {
    if (pcCompBuf == NULL) {
        cout << __FUNCTION__ << "================> pcCompBuf is null please to check " << endl;
        return false;
    }

    if (strlen(pcCompBuf) == 0) {
        cout << __FUNCTION__ << "strlen(pcCompBuf) == 0  ========================> " << endl;
        return false;
    }

    memset(uncompr, 0, MAXBUFFERSIZE);
    uLong uncomprLen = MAXBUFFERSIZE;
    int err;

    err = uncompress(uncompr, &uncomprLen, (const Bytef *)pcCompBuf, ulCompLen);
    if (err != Z_OK) {
        cout << "uncompess error: " << err << endl;
        return false;
    }

    cout << "compressed size: " << ulCompLen << "  uncompressed size : " << uncomprLen << endl;
    memcpy(pcUnCompBuf, uncompr, uncomprLen);

    return true;
}

CZlibMgr g_kZlibMgr;

#endif