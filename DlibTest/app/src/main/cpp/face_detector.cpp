//
// Created by Iris on 2019/3/31.
//

#include "face_detector.h"
FaceDetector::FaceDetector() {
    face_detector = dlib::get_frontal_face_detector();
}

int FaceDetector::Detect(const cv::Mat &image) {

    if (image.empty())
        return 0;

    if (image.channels() == 1) {
        cv::cvtColor(image, image, CV_GRAY2BGR);
    }

    dlib::cv_image<dlib::bgr_pixel> dlib_image(image);

    det_rects.clear();

    det_rects = face_detector(dlib_image);

    cout<<"Number of faces detected"<<det_rects.size()<<endl;

    return det_rects.size();
}

std::vector<dlib::rectangle> FaceDetector::getDetResultRects() {
    return det_rects;
}

void FaceDetector::initFaceDetection(std::string modePath) {
    this->targetWidth=300;
    this->modePath=modePath;
    g_pDetector=new dlib::frontal_face_detector;
    *g_pDetector=dlib::get_frontal_face_detector();
    g_pSp=new dlib::shape_predictor;
    dlib::deserialize(modePath)>> *g_pSp;
    return;
}

std::vector<dlib::full_object_detection> FaceDetector::getDetectShapes(){
    return g_Shapes;
}

std::vector<dlib::full_object_detection> FaceDetector::detectFaceLandmark(cv::Mat &pSrcImage)
{
  cv::Mat tmpImage;
  int channels=pSrcImage.channels();
  if(channels==4)
  {
      cvtColor(pSrcImage, tmpImage, CV_BGRA2GRAY);
  }
  else if(channels==3)
  {
      cvtColor(pSrcImage, tmpImage, CV_BGR2GRAY);
  }
  else if (channels == 1) {
      tmpImage = pSrcImage.clone();
  }else{
      cout<<"Error: Unsupported image format for face detection.";
  }

    double scale = (double)targetWidth / pSrcImage.rows;
    cv::resize(tmpImage, tmpImage, cv::Size(), scale, scale, cv::INTER_LINEAR);
    dlib::cv_image<unsigned char> dlib_img(tmpImage);

    std::vector<dlib::rectangle> dets = g_pDetector->operator()(dlib_img);
    std::vector<dlib::full_object_detection> shapes;

    for (size_t i = 0; i < dets.size(); i++)
    {
        dlib::full_object_detection shape;
        shape = g_pSp->operator()(dlib_img, dets[i]);
        shape = getScaleShape(shape, 1 / scale);
        cv::Rect rect(shape.get_rect().left(),
                      shape.get_rect().top(),
                      shape.get_rect().right() - shape.get_rect().left(),
                      shape.get_rect().bottom() - shape.get_rect().top());
        shapes.push_back(shape);
    }
    return shapes;
}
dlib::full_object_detection FaceDetector::getScaleShape(dlib::full_object_detection shape, const double scale)
{
    dlib::full_object_detection reShape = shape;
    dlib::rectangle &mRect = reShape.get_rect();
    mRect.set_left(mRect.left()*scale);
    mRect.set_right(mRect.right() *scale);
    mRect.set_top(mRect.top()*scale);
    mRect.set_bottom(mRect.bottom() *scale);
    for (size_t i = 0; i < shape.num_parts(); i++)
    {
        reShape.part(i).x() = cvRound(shape.part(i).x()*scale);
        reShape.part(i).y() = cvRound(shape.part(i).y()*scale);
    }
    return reShape;
}