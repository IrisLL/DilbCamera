//
// Created by Iris on 2019/3/31.
//

#ifndef DLIBTEST_FACE_DETECTOR_H
#define DLIBTEST_FACE_DETECTOR_H

#include <vector>
#include <opencv2/opencv.hpp>
#include <dlib/opencv/cv_image.h>
#include "dlib/image_processing/frontal_face_detector.h"

class FaceDetector {
private:

    dlib::frontal_face_detector face_detector;
    std::vector<dlib::rectangle> det_rects;

public:

    FaceDetector();

    int Detect(const cv::Mat &image);

    std::vector<dlib::rectangle> getDetResultRects();
};


#endif //DLIBTEST_FACE_DETECTOR_H
