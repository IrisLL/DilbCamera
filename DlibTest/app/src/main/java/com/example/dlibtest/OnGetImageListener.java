package com.example.dlibtest;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.dlibtest.Dlib.Constants;
import com.example.dlibtest.Dlib.FaceDet;
import com.example.dlibtest.Dlib.VisionDetRet;

import junit.framework.Assert;

import org.opencv.android.OpenCVLoader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
public class OnGetImageListener implements ImageReader.OnImageAvailableListener {


    private static final boolean SAVE_PREVIEW_BITMAP = false;


    private static final int INPUT_SIZE = 224;

    private static final String TAG = "OnGetImageListener";

    private int mScreenRotation = 90;

    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;

    private Context mContext;
    private FaceDet mFaceDet;
    // private TransparentTitleView mTransparentTitleView;
    private FloatingCameraWindow mWindow;
    private Paint mFaceLandmardkPaint;

    //添加姿态检测
    private PoseDetection poseDetection = new PoseDetection();

    static {
        if (OpenCVLoader.initDebug()) {
        }
        else {
        }
    }


    public void initialize(
            final Context context,
            final AssetManager assetManager,
            // final TransparentTitleView scoreView,
            final Handler handler) {
        this.mContext = context;
        //this.mTransparentTitleView = scoreView;
        this.mInferenceHandler = handler;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mWindow = new FloatingCameraWindow(mContext);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(1);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }


    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }

            if (mWindow != null) {
                mWindow.release();
            }
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = 270;      //////////方向
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

      //  Assert.assertEquals(dst.getWidth(), dst.getHeight());
      //  final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
       // final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
       // final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        final float translateX = -Math.max(0, src.getWidth()/ 2);
         final float translateY = -Math.max(0, src.getHeight() / 2);
        matrix.preTranslate(translateX, translateY);

       // final float scaleFactor = dst.getHeight() / minDim;
       // matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                Log.i(TAG, "没有图片");
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;
            Log.i(TAG, "ImageAvailabl有图片(＾－＾)V");
            Trace.beginSection("imageAvailable");
            //获取该图片的像素矩阵
            final Image.Plane[] planes = image.getPlanes();

            // Log.i(TAG,"width"+String.valueOf(image.getWidth())+"height"+String.valueOf(image.getHeight()));
            // Log.i(TAG,"mPreviewWidth"+String.valueOf(mPreviewWdith)+"  "+"mPreviewHeight"+String.valueOf(mPreviewHeight));



            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Log.i(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Bitmap.Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(mPreviewHeight, mPreviewWdith, Bitmap.Config.ARGB_8888);


                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mRGBBytes,
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false);

            image.close();
        } catch( final Exception e){
            if (image != null) {
                image.close();
            }
            Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }


        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        //drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);
        Bitmap alterBitmap=mRGBframeBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mCroppedBitmap=rotateBitmap(alterBitmap,270);


        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(mCroppedBitmap);
            Log.i(TAG,"mCroppedBitmap保存成功");
        }
        else Log.i(TAG,"mCroppedBitmap保存失败");

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG,"我执行到Runnable啦！");
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            // mTransparentTitleView.setText("Copying landmark model to " + Constants.getFaceShapeModelPath());
                            Log.i(TAG,"copyFaceShape68ModelFile "+ Constants.getFaceShapeModelPath()+"正常");
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                        }
                        else
                            Log.i(TAG,"copyFaceShape68ModelFile "+ Constants.getFaceShapeModelPath()+" 有异常");


                        long startTime = System.currentTimeMillis();
                        List<VisionDetRet> results;
                        synchronized (OnGetImageListener.this) {
                            results = mFaceDet.detect(mCroppedBitmap);
                        }
                        long endTime = System.currentTimeMillis();
                        // mTransparentTitleView.setText("Time cost: " + String.valueOf((endTime - startTime) / 1000f) + " sec");





                        //add headpose
                        Paint featuresPaint = new Paint();
                        featuresPaint.setColor(Color.CYAN);
                        featuresPaint.setStrokeWidth(1);
                        featuresPaint.setStyle(Paint.Style.STROKE);
                        // Loop result list
                        final Set<Integer> key_points = new HashSet<Integer>(Arrays.asList(
                                new Integer[] {
                                        //11, //chin
                                        17, //jaw
                                        22, //right brow
                                        27, //left_brow
                                        31, //nose bridge
                                        36, //nose bottom
                                        42, //right eye
                                        48, //left eye
                                        60, // mouth outer
                                        68 //mouth inner

                                }
                        ));


                        // Draw on bitmap
                        if (results != null) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 1.0f;
                                Rect bounds = new Rect();
                                bounds.left = (int) (ret.getLeft() * resizeRatio);
                                bounds.top = (int) (ret.getTop() * resizeRatio);
                                bounds.right = (int) (ret.getRight() * resizeRatio);
                                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                                Canvas canvas = new Canvas(mCroppedBitmap);
                                Log.i(TAG,"画布大小：w"+String.valueOf(canvas.getWidth())+"  h"+String.valueOf(canvas.getHeight()));
                                canvas.drawRect(bounds, mFaceLandmardkPaint);

                                mWindow.getPreviewHeight(mPreviewHeight);
                                mWindow.getPreviewWidth(mPreviewWdith);
                                mWindow.translateOrb(bounds);

                                // Draw landmark
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                /*for (Point point : landmarks) {
                                    int pointX = (int) (point.x * resizeRatio);
                                    int pointY = (int) (point.y * resizeRatio);
                                    canvas.drawCircle(pointX, pointY, 1, mFaceLandmardkPaint);
                                }*/


                                for (int p = 0 ;  p < 68 -1 ; p++) {
                                    if ( ! key_points.contains(p + 1) ) {
                                        int pointXS = (int) (landmarks.get(p).x * resizeRatio);
                                        int pointYS = (int) (landmarks.get(p).y * resizeRatio);
                                        int pointXF = (int) (landmarks.get(p+1).x * resizeRatio);
                                        int pointYF = (int) (landmarks.get(p+1).y * resizeRatio);
                                        canvas.drawLine(pointXS, pointYS, pointXF, pointYF, featuresPaint);
                                    }

                                }
                                canvas.drawCircle(landmarks.get(33).x, landmarks.get(33).y, 3, mFaceLandmardkPaint);

                            }

                            //add
                            if(results.size() > 0) {

                                long beforePose = System.currentTimeMillis();

                                double[] rotationRads = poseDetection.estimatePose(results.get(0), mCroppedBitmap);
                                long aftrePOse = System.currentTimeMillis();

                                mWindow.rotateOrb(rotationRads[0], rotationRads[1], rotationRads[2]);

                                //window width =
                                float widthWindow = mCroppedBitmap.getWidth();
                                //scale = 1 is half the width
                                //left checkbone ..

                                Point ear_r = results.get(0).getFaceLandmarks().get(0);
                                Point ear_l = results.get(0).getFaceLandmarks().get(16);

                                double faceWidth =
                                        Math.sqrt(
                                                Math.pow((ear_l.x - ear_r.x), 2.0f)
                                                        + Math.pow((ear_l.y - ear_r.y), 2) );

                                //At 1 facewidth is 1 / 4 of the screen
                                double faceWidthScale =  faceWidth / (widthWindow / 2.0f ) ;

                                Point noseTip = results.get(0).getFaceLandmarks().get(33);
                                Point chin = results.get(0).getFaceLandmarks().get(8);
                                double faceHeight =
                                        Math.sqrt(
                                                Math.pow((noseTip.x - chin.x), 2.0f)
                                                        + Math.pow((noseTip.y - chin.y), 2) );


                                faceHeight = faceHeight * 2;


                                mWindow.scaleOrb(faceWidthScale * 0.80f, faceHeight * 0.80f, 1.0f );
                            }

                        }

                        mWindow.setRGBBitmap(mCroppedBitmap);
                        mIsComputing = false;
                    }
                });

        Trace.endSection();
    }
    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

}