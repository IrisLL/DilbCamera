package com.example.dlibtest.Dlib;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import static android.content.ContentValues.TAG;

public final class Constants {


    public static String faceShape5ModelName = "shape_predictor_5_face_landmarks.dat";
    public static String faceShape68ModelName = "shape_predictor_68_face_landmarks.dat";

    private Constants() {
        // Constants should be prive
    }

    /**
     * getFaceShapeModelPath
     * @return default face shape model path
     */
    public static String getFaceShapeModelPath() {
        File sdcard = Environment.getExternalStorageDirectory();
        String targetPath = sdcard.getAbsolutePath() + File.separator + "shape_predictor_68_face_landmarks.dat";
        Log.i(TAG,"模型的路径"+targetPath);
        return targetPath;
    }
}
