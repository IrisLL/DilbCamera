//
// Created by Iris on 2019/4/4.
//

#ifndef DLIBTEST_JNI_UTILS_H
#define DLIBTEST_JNI_UTILS_H


class jni_utils {

};
#include <jni.h>
#include <string>

namespace jniutils {

    char* convertJStrToCStr(JNIEnv* env, jstring lString);

    std::string convertJStrToString(JNIEnv* env, jstring lString);

    JNIEnv* vm2env(JavaVM* vm);

}

#endif //DLIBTEST_JNI_UTILS_H
