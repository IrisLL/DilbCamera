//
// Created by Iris on 2019/3/31.
//

#ifndef DLIBTEST_PRIMITIVES_H
#define DLIBTEST_PRIMITIVES_H


#include <jni.h>

#define CLASSNAME_VISION_DET_RET "com/example/dlibtest/Dlib/VisionDetRet"
#define CONSTSIG_VISION_DET_RET "()V"
#define CLASSNAME_FACE_DET "com/example/dlibtest/Dlib/FaceDet"

//定义一个 VisinDetRet 对应的 JNI类 把C++数据传到Java
class JNI_VisionDetRet {
public:
    JNI_VisionDetRet(JNIEnv *env) {
        //获取VisionDetRet类
        jclass detRetClass = env->FindClass(CLASSNAME_VISION_DET_RET);
        //根据fieldID 找到下列这些变量
        jID_left = env->GetFieldID(detRetClass, "mLeft", "I");
        jID_top = env->GetFieldID(detRetClass, "mTop", "I");
        jID_right = env->GetFieldID(detRetClass, "mRight", "I");
        jID_bottom = env->GetFieldID(detRetClass, "mBottom", "I");
    }

    //设置VisionDetRet 里定义的下列变量的值
    void setRect(JNIEnv *env, jobject &jDetRet, const int &left, const int &top,
                 const int &right, const int &bottom) {
        env->SetIntField(jDetRet, jID_left, left);
        env->SetIntField(jDetRet, jID_top, top);
        env->SetIntField(jDetRet, jID_right, right);
        env->SetIntField(jDetRet, jID_bottom, bottom);
    }

    static jobject createJObject(JNIEnv *env) {
        jclass detRetClass = env->FindClass(CLASSNAME_VISION_DET_RET);
        //VisionDetRet init方法
        jmethodID mid =
                env->GetMethodID(detRetClass, "<init>", CONSTSIG_VISION_DET_RET);
        return env->NewObject(detRetClass, mid);
    }

    //VisionDetRet 创建一个变量对象 （用于储存这些点，画图）
    static jobjectArray createJObjectArray(JNIEnv *env, const int &size) {
        jclass detRetClass = env->FindClass(CLASSNAME_VISION_DET_RET);
        return (jobjectArray) env->NewObjectArray(size, detRetClass, NULL);
    }

private:
    jfieldID jID_left;
    jfieldID jID_top;
    jfieldID jID_right;
    jfieldID jID_bottom;
};


#endif //DLIBTEST_PRIMITIVES_H
