#include "com_zengge_nbmanager_Features.h"
#include <cstring>

JNIEXPORT jlong JNICALL Java_com_zengge_nbmanager_Features_ELFHash
(JNIEnv *env, jclass cls, jstring jstr) {
    char *strUri = const_cast<char*>(env->GetStringUTFChars(jstr, NULL));
	long hash = 0;
	long x = 0;
	for (int i = 0; i < strlen(strUri); i++) {
		hash = (hash << 4) + strUri[i];
		if ((x = hash & 0xF0000000L) != 0) {
			hash ^= (x >> 24);
			hash &= ~x;
		}
	}
	return (jlong)(hash & 0x7FFFFFFF);
}
