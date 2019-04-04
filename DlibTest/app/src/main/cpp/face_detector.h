//
// Created by Iris on 2019/3/31.
//

#ifndef DLIBTEST_FACE_DETECTOR_H
#define DLIBTEST_FACE_DETECTOR_H

#include <vector>
#include "opencv2/opencv.hpp"
#include "opencv2/core/core.hpp"
#include "dlib/opencv/cv_image.h"
#include "dlib/image_processing/shape_predictor.h"
#include "dlib/image_processing/frontal_face_detector.h"

class FaceDetector {
public:
    string modePath;
    int32_t targetWidth;
   dlib::frontal_face_detector face_detector;
   std::vector<dlib::rectangle> det_rects;
   std::vector<dlib::full_object_detection> g_Shapes;
   dlib::shape_predictor* g_pSp;
   dlib::frontal_face_detector* g_pDetector;

public:

    FaceDetector();

    void initFaceDetection(std::string spFnStr);

    int Detect(const cv::Mat &image);

    std::vector<dlib::rectangle> getDetResultRects();

    std::vector<dlib::full_object_detection> getDetectShapes();

private:
    dlib::full_object_detection getScaleShape(dlib::full_object_detection shape, const double scale);
    std::vector<dlib::full_object_detection> detectFaceLandmark(cv::Mat &pSrcImage);


};


#endif //DLIBTEST_FACE_DETECTOR_H
