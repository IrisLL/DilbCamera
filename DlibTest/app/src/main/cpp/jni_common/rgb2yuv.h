//
// Created by Iris on 2019/4/3.
//

#ifndef DLIBTEST_RGB2YUV_H
#define DLIBTEST_RGB2YUV_H


class rgb2yuv {

};
#include <stdint.h>
#include "types.h"
namespace jnicommon {


#ifdef __cplusplus
    extern "C" {
#endif

    void ConvertARGB8888ToYUV420SP(const uint32_t *const input,
                                   uint8_t *const output, int width, int height);

    void ConvertRGB565ToYUV420SP(const uint16_t *const input, uint8_t *const output,
                                 const int width, const int height);

#ifdef __cplusplus
    }
}
#endif

#endif //DLIBTEST_RGB2YUV_H
