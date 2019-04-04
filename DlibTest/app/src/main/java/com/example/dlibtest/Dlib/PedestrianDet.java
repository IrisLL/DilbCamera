package com.example.dlibtest.Dlib;

import android.graphics.Bitmap;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class PedestrianDet {

    // accessed by native methods

    private long mNativeDetContext;
    private static final String TAG = "dlib";

    static {
        try {
            System.loadLibrary("native-lib");
            Log.d(TAG, "jniNativeClassInit success");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "library not found!");
        }
    }

    public PedestrianDet() {
        jniInit();
    }

    @Nullable
    @WorkerThread
    public List<VisionDetRet> detect(@NonNull Bitmap bitmap) {
        VisionDetRet[] detRets = jniBitmapDetect(bitmap);
        return Arrays.asList(detRets);
    }

    @Nullable
    @WorkerThread
    public List<VisionDetRet> detect(@NonNull final String path) {
        VisionDetRet[] detRets = jniDetect(path);
        return Arrays.asList(detRets);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    public void release() {
        jniDeInit();
    }

    @Keep
    private native int jniInit();

    @Keep
    private synchronized native int jniDeInit();

    @Keep
    private synchronized native VisionDetRet[] jniDetect(String path);

    @Keep
    private synchronized native VisionDetRet[] jniBitmapDetect(Bitmap bitmap);
}
