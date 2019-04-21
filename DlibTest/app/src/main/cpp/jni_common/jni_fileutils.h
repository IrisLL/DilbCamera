//
// Created by Iris on 2019/4/4.
//

#ifndef DLIBTEST_JNI_FILEUTILS_H
#define DLIBTEST_JNI_FILEUTILS_H

#include <fstream>
#include <iostream>
#include <string>
#include <sys/stat.h>
#include <unistd.h>

class jni_fileutils {

};
namespace jniutils {

    bool fileExists(const char* name);

    bool dirExists(const char* name);

    bool fileExists(const std::string& name);

    bool dirExists(const std::string& name);

}


#endif //DLIBTEST_JNI_FILEUTILS_H
