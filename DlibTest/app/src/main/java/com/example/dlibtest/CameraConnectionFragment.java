package com.example.dlibtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraConnectionFragment extends Fragment {
    /**
     * The camera preview size will be chosen to be the smallest frame by pixel size capable of
     * containing a DESIRED_SIZE x DESIRED_SIZE square.
     */
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private static final String TAG = "CameraConnectionFragment";
    private TransparentTitleView mScoreView;
    private FloatingActionButton captureBtn;
    private FloatingCameraWindow floatBtn;
    public boolean TAKE_PHOTO=false;


    /**
     * Conversion from screen rotation to JPEG orientation.
     * 手机旋转对应的角度
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;   //摄像头ID
    private AutoFitTextureView textureView; //自定义一个Texture来显示预览取景
    private CameraCaptureSession captureSession;//定义CameraCptureSession 成员变量
    private CameraDevice cameraDevice; //代表摄像头的成员变量
    private Size previewSize;//预览尺寸


    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest; //定义用于预览照片的捕获请求

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;
    private ImageReader previewReader;




/////01

    /**
     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     * 设置surfacetexture监听事件
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    openCamera(width, height);     //①第一步
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                }
            };



/////  02
    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressLint("LongLogTag")
    private void setUpCameraOutputs(final int width, final int height) {
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取摄像头支持的配置属性
            SparseArray<Integer> cameraFaceTypeMap = new SparseArray<>();
            // Check the facing types of camera devices
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                        cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT, cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) + 1);
                    } else {
                        cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT, 1);
                    }
                }

                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                        cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_BACK) + 1);
                    } else {
                        cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, 1);
                    }
                }
            }

/////////////////////设置前后摄像头
            Integer num_facing_back_camera = cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_BACK);
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // If facing back camera or facing external camera exist, we won't use facing front camera
                if (num_facing_back_camera != null && num_facing_back_camera > 0) {
                    // We don't use a front facing camera in this sample if there are other camera device facing types
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        continue;
                    }
                }

                // 获取摄像头支持的配置属性
                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // 获取摄像头支持的最大尺寸
                final Size largest =
                        Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                                new CompareSizesByArea());


                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.


                // 获取最佳的预览尺寸
                previewSize =
                        chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                // 根据选中的预览尺寸来调整预览组件（TextureView）的长宽比
                final int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                CameraConnectionFragment.this.cameraId = cameraId;
                return;
            }
        } catch (final CameraAccessException e) {
            //Timber.tag(TAG).e("Exception!", e);
        } catch (final NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error)).
                    show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    @SuppressLint("LongLogTag")
    private static Size chooseOptimalSize(
            final Size[] choices, final int width, final int height, final Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        final List<Size> bigEnough = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.getHeight() >= MINIMUM_PREVIEW_SIZE && option.getWidth() >= MINIMUM_PREVIEW_SIZE) {
                //Timber.tag(TAG).i("Adding size: " + option.getWidth() + "x" + option.getHeight());
                bigEnough.add(option);
            } else {
                //Timber.tag(TAG).i("Not adding size: " + option.getWidth() + "x" + option.getHeight());
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            //Timber.tag(TAG).i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            //Timber.tag(TAG).e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

//////

    //使用信号量 Semaphore 进行多线程任务调度
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }



    public static CameraConnectionFragment newInstance() {
        return new CameraConnectionFragment();}


    @Override
    public View onCreateView(

            //加载fragment的布局
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_connection_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        // mScoreView = (TransparentTitleView) view.findViewById(R.id.results);
        captureBtn=(FloatingActionButton) view.findViewById(R.id.capture);

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("takePhoto");
                captureStillPicture();
            }
        });

    }


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }




////03

    /**
     * Opens the camera specified by {@link CameraConnectionFragment#cameraId}.
     */
    @SuppressLint("LongLogTag")
    private void openCamera(final int width, final int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
               // Timber.tag(TAG).w("checkSelfPermission CAMERA");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
            //Timber.tag(TAG).d("open Camera");
        } catch (final CameraAccessException e) {
            //Timber.tag(TAG).e("Exception!", e);
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    ///04
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                //摄像头被打开时，激发该方法
                @Override
                public void onOpened(final CameraDevice cd) {
                    // This method is called when the camera is opened.  We start camera preview here.
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();   //②第二步
                }

                //摄像头断开连接时，激发该方法
                @Override
                public void onDisconnected(final CameraDevice cd) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;

                    if (mOnGetPreviewListener != null) {
                        mOnGetPreviewListener.deInitialize();
                    }
                }

                //打开摄像头出错时，激发该方法
                @Override
                public void onError(final CameraDevice cd, final int error) {
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                    final Activity activity = getActivity();
                    if (null != activity) {
                        activity.finish();
                    }

                    if (mOnGetPreviewListener != null) {
                        mOnGetPreviewListener.deInitialize();
                    }
                }

            };


    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != previewReader) {
                previewReader.close();
                previewReader = null;
            }
            if (null != mOnGetPreviewListener) {
                mOnGetPreviewListener.deInitialize();
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    @SuppressLint("LongLogTag")
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        inferenceThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;

            inferenceThread.join();
            inferenceThread = null;
            inferenceThread = null;
        } catch (final InterruptedException e) {
            //Timber.tag(TAG).e("error", e);
        }
    }

    //在预览界面过程中，需要间隔刷新界面
    private final OnGetImageListener mOnGetPreviewListener = new OnGetImageListener();

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final CaptureResult partialResult) {
                }

                @Override
                public void onCaptureCompleted(
                        final CameraCaptureSession session,
                        final CaptureRequest request,
                        final TotalCaptureResult result) {
                }
            };



    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    @SuppressLint("LongLogTag")
    private void createCameraPreviewSession() {
        try {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            final Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
          //  previewRequestBuilder.addTarget(surface);

           // Timber.tag(TAG).i("Opening camera preview: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            // Create the reader for the preview frames.
            previewReader =
                    ImageReader.newInstance(
                            previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            previewReader.setOnImageAvailableListener(mOnGetPreviewListener, backgroundHandler);
            previewRequestBuilder.addTarget(previewReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, previewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        //自动对焦
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                //自动曝光
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (final CameraAccessException e) {
                                //Timber.tag(TAG).e("Exception!", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
           // Timber.tag(TAG).e("Exception!", e);
        }

        //mOnGetPreviewListener.initialize(getActivity().getApplicationContext(), getActivity().getAssets(), mScoreView, inferenceHandler);

        mOnGetPreviewListener.initialize(getActivity().getApplicationContext(), getActivity().getAssets(),  inferenceHandler);

    }




    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }


    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }



    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(final String message) {
            final ErrorDialog dialog = new ErrorDialog();
            final Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }
    }



    private void captureStillPicture()
    {
        try
        {
            if (cameraDevice == null)
            {
                return;
            }
            // 创建作为拍照的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            final Activity activity = getActivity();
          // previewReader.setOnImageAvailableListener(mOnGetPreviewListener, backgroundHandler);

/*
           ImageReader imagereader =
                    ImageReader.newInstance(
                            previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 2);*/


            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(previewReader.getSurface());
            // 设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);


            // 获取设备方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION
                    , ORIENTATIONS.get(rotation));
            // 停止连续取景
            captureSession.stopRepeating();
            previewReader.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener()
                    {
                        // 当照片数据可用时激发该方法
                        @Override
                        public void onImageAvailable(ImageReader reader)
                        {
                            // 获取捕获的照片数据
                            Image image = reader.acquireNextImage();

                            byte[][] mYUVBytes;
                            Image.Plane[] planes = image.getPlanes();
                            int[] mRGBBytes = new int[previewSize.getWidth()* previewSize.getHeight()];
                            Bitmap mRGBframeBitmap = Bitmap.createBitmap(previewSize.getWidth(), previewSize.getHeight(), Bitmap.Config.ARGB_8888);
                            mYUVBytes = new byte[planes.length][];
                            for (int i = 0; i < planes.length; ++i) {
                                mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
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
                                    previewSize.getWidth(),
                                    previewSize.getHeight(),
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    false);
                            mRGBframeBitmap.setPixels(mRGBBytes, 0,  previewSize.getWidth(), 0, 0,  previewSize.getWidth(),  previewSize.getHeight());


                            showToast("Image size: width"+mRGBframeBitmap.getWidth()+"  height"+mRGBframeBitmap.getHeight());
                            File file = new File("/sdcard/" + "dlib"+ ".jpg");
                            try (
                                    FileOutputStream output = new FileOutputStream(file))
                            {
                                mRGBframeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                                output.flush();
                                output.close();
                                showToast( "保存: " + file);
                                  //打开新的活动
                                Intent intent=new Intent(activity,PicResultActivity.class);
                                intent.putExtra("picPath",file.getAbsolutePath());
                                intent.putExtra("cameraID",cameraId);
                                startActivity(intent);
                                activity.finish();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                image.close();
                            }
                        }
                    }, backgroundHandler);


            // 捕获静态图像
          /* captureSession.capture(captureRequestBuilder.build()
                    , new CameraCaptureSession.CaptureCallback()  // ⑤
                    {
                        // 拍照完成时激发该方法
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session
                                , CaptureRequest request, TotalCaptureResult result)
                        {
                            try
                            {
                                // 重设自动对焦模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                                // 设置自动曝光模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                // 打开连续取景模式
                                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                                showToast("准备拍照片啦");
                                TAKE_PHOTO=false;
                                showToast("TakePhoto3"+TAKE_PHOTO);
                            }
                            catch (CameraAccessException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }, null);*/
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

    }




}
