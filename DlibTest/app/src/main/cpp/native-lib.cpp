/*
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_dlibtest_MainActivity_stringFromJNI(
        JNIEnv *env,   //C++中env是一个一级指针
        jobject ) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
*/

#include <jni.h>
#include <android/bitmap.h>
#include "jni_common/bitmap2mat.h"
#include "jni_common/primitives.h"
#include "face_detector.h"
#include "jni_common/yuv2rgb.h"
#include "jni_common/rgb2yuv.h"
#include "jni_common/jni_utils.h"
#include "jni_common/jni_fileutils.h"
#include "detector.h"


using namespace cv;
using namespace std;
using namespace dlib;

//////////////////////////////////////////////////////

extern JNI_VisionDetRet *g_pJNI_VisionDetRet; //定义了JNI_VisionDetRet类型一个指针 pJNI_VisionDetRet




namespace
{
#define JAVA_NULL 0
    using DetectorPtr = DLibHOGFaceDetector*;
    class JNI_FaceDet {
    public:
        JNI_FaceDet(JNIEnv *env) {
            jclass clazz = env->FindClass(CLASSNAME_FACE_DET);
            //FaceDet java类中声明的变量  private long mNativeFaceDetContext;
            mNativeContext = env->GetFieldID(clazz, "mNativeFaceDetContext", "J");
            env->DeleteLocalRef(clazz);
        }

        DetectorPtr getDetectorPtrFromJava(JNIEnv *env, jobject thiz) {
            //返回Java中的 mNativeFaceDetContext 值
            DetectorPtr const p = (DetectorPtr) env->GetLongField(thiz, mNativeContext);
            return p;
        }

        void setDetectorPtrToJava(JNIEnv *env, jobject thiz, jlong ptr) {
            //设置Java中的 mNativeFaceDetContext 值
            env->SetLongField(thiz, mNativeContext, ptr);
        }

        jfieldID mNativeContext;
    };

    // Protect getting/setting and creating/deleting pointer between java/native
    std::mutex gLock;

    std::shared_ptr<JNI_FaceDet> getJNI_FaceDet(JNIEnv *env) {
        static std::once_flag sOnceInitflag;
        static std::shared_ptr<JNI_FaceDet> sJNI_FaceDet;
        std::call_once(sOnceInitflag, [env]() {
            //动态内存分配对象并初始化它
            sJNI_FaceDet = std::make_shared<JNI_FaceDet>(env);
        });
        return sJNI_FaceDet;
    }

    DetectorPtr const getDetectorPtr(JNIEnv *env, jobject thiz) {
        std::lock_guard<std::mutex> lock(gLock);
        return getJNI_FaceDet(env)->getDetectorPtrFromJava(env, thiz);
    }

    // The function to set a pointer to java and delete it if newPtr is empty
    void setDetectorPtr(JNIEnv *env, jobject thiz, DetectorPtr newPtr) {
        std::lock_guard<std::mutex> lock(gLock);
        DetectorPtr oldPtr = getJNI_FaceDet(env)->getDetectorPtrFromJava(env, thiz);
        if (oldPtr != JAVA_NULL) {
            delete oldPtr;
        }

        if (newPtr != JAVA_NULL) {
            DLOG(INFO) << "setMapManager set new ptr : " << newPtr;
        }

        getJNI_FaceDet(env)->setDetectorPtrToJava(env, thiz, (jlong) newPtr);
    }
}

//C++宏定义，表示如果是一段C++代码，执行下面
#ifdef __cplusplus
extern "C" {
#endif

    //调用FaceDet java类的方法
#define DLIB_FACE_JNI_METHOD(METHOD_NAME) Java_com_example_dlibtest_Dlib_FaceDet_##METHOD_NAME


void JNIEXPORT
DLIB_FACE_JNI_METHOD(jniNativeClassInit)(JNIEnv *env, jclass _this) {}

//jobjectArray 为 Object[]任何对象的数组
//获取矩形框的结果
jobjectArray getDetectResult(JNIEnv *env, DetectorPtr faceDetector, const int &size) {

        LOG(INFO) << "getFaceRet";
        jobjectArray jDetRetArray = JNI_VisionDetRet::createJObjectArray(env, size);
        for (int i = 0; i < size; i++) {
            jobject jDetRet = JNI_VisionDetRet::createJObject(env);
            env->SetObjectArrayElement(jDetRetArray, i, jDetRet);
            dlib::rectangle rect = faceDetector->getResult()[i];
            g_pJNI_VisionDetRet->setRect(env, jDetRet, rect.left(), rect.top(),
                                         rect.right(), rect.bottom());
            g_pJNI_VisionDetRet->setLabel(env, jDetRet, "face");
            std::unordered_map<int, dlib::full_object_detection>& faceShapeMap =
                    faceDetector->getFaceShapeMap();
            if (faceShapeMap.find(i) != faceShapeMap.end()) {
                dlib::full_object_detection shape = faceShapeMap[i];
                for (unsigned long j = 0; j < shape.num_parts(); j++) {
                    int x = shape.part(j).x();
                    int y = shape.part(j).y();
                    // Call addLandmark
                    g_pJNI_VisionDetRet->addLandmark(env, jDetRet, x, y);
                }
            }
        }
        return jDetRetArray;
}


JNIEXPORT jobjectArray JNICALL
DLIB_FACE_JNI_METHOD(jniDetect)(JNIEnv* env, jobject thiz,
        jstring imgPath) {
    LOG(INFO) << "jniFaceDet";
    const char* img_path = env->GetStringUTFChars(imgPath, 0);
    DetectorPtr detPtr = getDetectorPtr(env, thiz);
    int size = detPtr->det(std::string(img_path));
    env->ReleaseStringUTFChars(imgPath, img_path);
    LOG(INFO) << "det face size: " << size;
    return getDetectResult(env, detPtr, size);
}


//FaceDet类中声明的 VisionDetRet[] jniBitmapDet(Bitmap bitmap);
JNIEXPORT jobjectArray JNICALL
DLIB_FACE_JNI_METHOD(jniBitmapDetect)(JNIEnv *env, jobject thiz, jobject bitmap) {
    LOG(INFO) << "jniBitmapFaceDet";
    cv::Mat rgbaMat;
    cv::Mat bgrMat;
    //把Bitmap图片转成RGB矩阵
    jniutils::ConvertBitmapToRGBAMat(env, bitmap, rgbaMat, true);
    cv::cvtColor(rgbaMat, bgrMat, cv::COLOR_RGBA2BGR);
    DetectorPtr mDetPtr = getDetectorPtr(env, thiz);
    jint size = mDetPtr->det(bgrMat);
#if 0
    cv::Mat rgbMat;
  cv::cvtColor(bgrMat, rgbMat, cv::COLOR_BGR2RGB);
  cv::imwrite("/sdcard/ret.jpg", rgbaMat);
#endif
    LOG(INFO) << "det face size: " << size;
    return getDetectResult(env, mDetPtr, size);
}



//FaceDet类声明的    private synchronized native int jniInit();
//初始化 ！！！！
jint JNIEXPORT JNICALL DLIB_FACE_JNI_METHOD(jniInit)(JNIEnv* env, jobject thiz,
                                                     jstring jLandmarkPath) {
    LOG(INFO) << "jniInit";
    std::string landmarkPath = jniutils::convertJStrToString(env, jLandmarkPath);
    DetectorPtr detPtr = new DLibHOGFaceDetector(landmarkPath);
    setDetectorPtr(env, thiz, detPtr);
    ;
    return JNI_OK;
}

//FaceDet类声明的  private synchronized native int jniDeInit();
jint JNIEXPORT JNICALL
DLIB_FACE_JNI_METHOD(jniDeInit)(JNIEnv *env, jobject thiz) {
    LOG(INFO) << "jniDeInit";
    setDetectorPtr(env, thiz, JAVA_NULL);
    return JNI_OK;
}

#ifdef __cplusplus
}
#endif

///////////////////////////////



extern "C"
JNIEXPORT void JNICALL
Java_com_example_dlibtest_ImageUtils_convertYUV420ToARGB8888(JNIEnv *env, jclass clazz,
                                               jbyteArray y, jbyteArray u,
                                               jbyteArray v, jintArray output,
                                               jint width, jint height,
                                               jint y_row_stride, jint uv_row_stride,
                                               jint uv_pixel_stride,
                                               jboolean halfSize) {
    jboolean inputCopy = JNI_FALSE;
    jbyte *const y_buff = env->GetByteArrayElements(y, &inputCopy);
    jboolean outputCopy = JNI_FALSE;
    jint *const o = env->GetIntArrayElements(output, &outputCopy);

    if (halfSize) {
        ConvertYUV420SPToARGB8888HalfSize(reinterpret_cast<uint8_t *>(y_buff),
                                          reinterpret_cast<uint32_t *>(o), width,
                                          height);
    } else {
        jbyte *const u_buff = env->GetByteArrayElements(u, &inputCopy);
        jbyte *const v_buff = env->GetByteArrayElements(v, &inputCopy);

        ConvertYUV420ToARGB8888(
                reinterpret_cast<uint8_t *>(y_buff), reinterpret_cast<uint8_t *>(u_buff),
                reinterpret_cast<uint8_t *>(v_buff), reinterpret_cast<uint32_t *>(o),
                width, height, y_row_stride, uv_row_stride, uv_pixel_stride);

        env->ReleaseByteArrayElements(u, u_buff, JNI_ABORT);
        env->ReleaseByteArrayElements(v, v_buff, JNI_ABORT);
    }

    env->ReleaseByteArrayElements(y, y_buff, JNI_ABORT);
    env->ReleaseIntArrayElements(output, o, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dlibtest_ImageUtils_convertYUV420SPToARGB8888(JNIEnv *env, jclass clazz,
                                                 jbyteArray input, jintArray output,
                                                 jint width, jint height,
                                                 jboolean halfSize) {
    jboolean inputCopy = JNI_FALSE;
    jbyte *const i = env->GetByteArrayElements(input, &inputCopy);

    jboolean outputCopy = JNI_FALSE;
    jint *const o = env->GetIntArrayElements(output, &outputCopy);

    if (halfSize) {
        ConvertYUV420SPToARGB8888HalfSize(reinterpret_cast<uint8_t *>(i),
                                          reinterpret_cast<uint32_t *>(o), width,
                                          height);
    } else {
        ConvertYUV420SPToARGB8888(reinterpret_cast<uint8_t *>(i),
                                  reinterpret_cast<uint8_t *>(i) + width * height,
                                  reinterpret_cast<uint32_t *>(o), width, height);
    }

    env->ReleaseByteArrayElements(input, i, JNI_ABORT);
    env->ReleaseIntArrayElements(output, o, 0);
}



////////////////////////

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dlibtest_ImageUtils_convertARGB8888ToYUV420SP(JNIEnv* env, jclass clazz,
                                                               jintArray input,
                                                               jbyteArray output, jint width,
                                                               jint height) {
jboolean inputCopy = JNI_FALSE;
jint* const i = env->GetIntArrayElements(input, &inputCopy);

jboolean outputCopy = JNI_FALSE;
jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

jnicommon::ConvertARGB8888ToYUV420SP(reinterpret_cast<uint32*>(i),
reinterpret_cast<uint8*>(o), width, height);

env->ReleaseIntArrayElements(input, i, JNI_ABORT);
env->ReleaseByteArrayElements(output, o, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_dlibtest_ImageUtils_convertYUV420SPToRGB565(JNIEnv* env, jclass clazz,
                                                             jbyteArray input,
                                                             jbyteArray output, jint width,
                                                             jint height) {
    jboolean inputCopy = JNI_FALSE;
    jbyte* const i = env->GetByteArrayElements(input, &inputCopy);

    jboolean outputCopy = JNI_FALSE;
    jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

    ConvertYUV420SPToRGB565(reinterpret_cast<uint8*>(i),
                            reinterpret_cast<uint16*>(o), width, height);

    env->ReleaseByteArrayElements(input, i, JNI_ABORT);
    env->ReleaseByteArrayElements(output, o, 0);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_dlibtest_ImageUtils_convertRGB565ToYUV420SP(JNIEnv* env, jclass clazz,
                                                             jbyteArray input,
                                                             jbyteArray output, jint width,
                                                             jint height) {
    jboolean inputCopy = JNI_FALSE;
    jbyte* const i = env->GetByteArrayElements(input, &inputCopy);

    jboolean outputCopy = JNI_FALSE;
    jbyte* const o = env->GetByteArrayElements(output, &outputCopy);

    jnicommon::ConvertRGB565ToYUV420SP(reinterpret_cast<uint16*>(i),
                            reinterpret_cast<uint8*>(o), width, height);

    env->ReleaseByteArrayElements(input, i, JNI_ABORT);
    env->ReleaseByteArrayElements(output, o, 0);
}

