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
#include <bitmap2mat.h>
#include <primitives.h>
#include <face_detector.h>

using namespace cv;

using namespace std;
extern "C" {
// 注意这里的函数名格式：Java_各级包名_类名_函数名(参数...),需严格按照这种格式，否则会出错
JNIEXPORT jintArray JNICALL Java_com_example_dlibtest_PicResultActivity_gray(
        JNIEnv *env,
        jobject instance,
        jintArray buf,
        jint w,
        jint h) {
    jint *cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL) { return 0; }
    Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
    uchar *ptr = imgData.ptr(0);
    for (int i = 0; i < w * h; i++) {
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        // 对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587 +
                               ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 1] = grayScale;
        ptr[4 * i + 2] = grayScale;
        ptr[4 * i + 0] = grayScale;
    }
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, cbuf);
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    return result;
}

}
//////////////////////////////////////////////////////


JNI_VisionDetRet *g_pJNI_VisionDetRet; //定义了JNI_VisionDetRet类型一个指针 pJNI_VisionDetRet
JavaVM *g_javaVM = NULL;  //定义了虚拟机JavaVM类型 空指针 g_javaVM

/**
 *
 * @param vm
 * @param reserved
 * @return
 * 动态加载到本地
 */
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_javaVM = vm;
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    g_pJNI_VisionDetRet = new JNI_VisionDetRet(env);

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {

    g_javaVM = NULL;

    delete g_pJNI_VisionDetRet;
}


namespace
{
#define JAVA_NULL 0
    using DetPtr = FaceDetector *;
    class JNI_FaceDet {
    public:
        JNI_FaceDet(JNIEnv *env) {
            jclass clazz = env->FindClass(CLASSNAME_FACE_DET);
            //FaceDet java类中声明的变量  private long mNativeFaceDetContext;
            mNativeContext = env->GetFieldID(clazz, "mNativeFaceDetContext", "J");
            env->DeleteLocalRef(clazz);
        }

        DetPtr getDetectorPtrFromJava(JNIEnv *env, jobject thiz) {
            //返回Java中的 mNativeFaceDetContext 值
            DetPtr const p = (DetPtr) env->GetLongField(thiz, mNativeContext);
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

    DetPtr const getDetPtr(JNIEnv *env, jobject thiz) {
        std::lock_guard<std::mutex> lock(gLock);
        return getJNI_FaceDet(env)->getDetectorPtrFromJava(env, thiz);
    }

    // The function to set a pointer to java and delete it if newPtr is empty
    void setDetPtr(JNIEnv *env, jobject thiz, DetPtr newPtr) {
        std::lock_guard<std::mutex> lock(gLock);
        DetPtr oldPtr = getJNI_FaceDet(env)->getDetectorPtrFromJava(env, thiz);
        if (oldPtr != JAVA_NULL) {
            delete oldPtr;
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
jobjectArray getRecResult(JNIEnv *env, DetPtr faceDetector, const int &size) {
    jobjectArray jDetRetArray = JNI_VisionDetRet::createJObjectArray(env, size);
    for (int i = 0; i < size; i++) {
        //把VisionDetRet得到的点传进去
        jobject jDetRet = JNI_VisionDetRet::createJObject(env);
        env->SetObjectArrayElement(jDetRetArray, i, jDetRet);
        dlib::rectangle rect = faceDetector->getDetResultRects()[i];
        //调用dlib画图
        g_pJNI_VisionDetRet->setRect(env, jDetRet, rect.left(), rect.top(),
                                     rect.right(), rect.bottom());
    }
    //返回这个对象数组（画出的图）
    return jDetRetArray;
}

//FaceDet类中声明的 VisionDetRet[] jniBitmapDet(Bitmap bitmap);
JNIEXPORT jobjectArray JNICALL
DLIB_FACE_JNI_METHOD(jniBitmapDet)(JNIEnv *env, jobject thiz, jobject bitmap) {
    cv::Mat rgbaMat;
    cv::Mat bgrMat;
    //把Bitmap图片转成RGB矩阵
    jniutils::ConvertBitmapToRGBAMat(env, bitmap, rgbaMat, true);
    cv::cvtColor(rgbaMat, bgrMat, cv::COLOR_RGBA2BGR);
    DetPtr mDetPtr = getDetPtr(env, thiz);
    jint size = mDetPtr->Detect(bgrMat);
    return getRecResult(env, mDetPtr, size);
}


//FaceDet类声明的    private synchronized native int jniInit();
//初始化
jint JNIEXPORT JNICALL
DLIB_FACE_JNI_METHOD(jniInit)(JNIEnv *env, jobject thiz) {
    DetPtr mDetPtr = new FaceDetector();
    setDetPtr(env, thiz, mDetPtr);
    return JNI_OK;
}

//FaceDet类声明的  private synchronized native int jniDeInit();
jint JNIEXPORT JNICALL
DLIB_FACE_JNI_METHOD(jniDeInit)(JNIEnv *env, jobject thiz) {
    setDetPtr(env, thiz, JAVA_NULL);
    return JNI_OK;
}

#ifdef __cplusplus
}
#endif



