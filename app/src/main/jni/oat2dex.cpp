#include "com_zengge_nbmanager_Features.h"
#include <cstdlib>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <string>
//#include <fstream>

using namespace std;

bool write_to_file (char *path, unsigned char *data, int size, int file_count) {
    char filename[10240];
    sprintf(filename, "%s%02d.dex", path, file_count);
//    ofstream fout("/sdcard/debug1.txt");
//    if(fout.is_open()) {
//    	fout << "Writing " << size << " bytes to " << filename << endl;
//    	fout.close();
//	}
    FILE *fp = fopen(filename, "wb");
    if(fp == NULL) return false;
    fwrite(data, 1, size, fp);
    fclose(fp);
    return true;
}

char *MyGetPrefix(const char *f) {
	string path=string(f);
	int n=path.find_last_of('.');
	string shao=path.substr(0,n);
	return const_cast<char*>(shao.c_str());
}

bool procdex(const char *infile) {
    FILE *infp = fopen(infile, "rb");
    if (infp == NULL) {
        return false;
    }
    fseek(infp, 0, SEEK_END);
    int insize = ftell(infp);
    fseek(infp, 0, SEEK_SET);
    unsigned char *indata = (unsigned char *)malloc(insize);
    fread(indata, 1, insize, infp);
    fclose(infp);

    int file_count = 0;
    int ptr;
    for (ptr = 0; ptr < insize; ptr ++) {
        if (memcmp(indata + ptr, "dex\n035", 8) != 0)
            continue;
        unsigned int dexsize = *(unsigned int *)(indata + ptr + 32); // the 'file_size_' field in the header
        if (ptr + dexsize > insize)
            continue;
        if(!write_to_file(MyGetPrefix(infile), indata + ptr, dexsize, ++file_count)) return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL Java_com_zengge_nbmanager_Features_Oat2Dex
(JNIEnv *env, jclass cls, jstring from) {
    const char *f = env->GetStringUTFChars(from, NULL);
    if(procdex(f)) return JNI_TRUE;
    else return JNI_FALSE;
}
