//
// Created by Iris on 2019/3/31.
//

#ifndef DLIBTEST_BITMAP2MAT_H
#define DLIBTEST_BITMAP2MAT_H

#include <android/bitmap.h>
#include "opencv2/core/core.hpp"
#include "opencv2/opencv.hpp"
#include "opencv2/core/mat.hpp"

class bitmap2mat {

};
namespace jniutils {

    void ConvertBitmapToRGBAMat(JNIEnv *env, jobject &bitmap, cv::Mat &dst,
                                bool needUnPremultiplyAlpha);

}

#endif //DLIBTEST_BITMAP2MAT_H
