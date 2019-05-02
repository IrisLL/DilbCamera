package com.example.dlibtest.ARcamera;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.dlibtest.AutoFitTextureView;
import com.example.dlibtest.Dlib.VisionDetRet;
import com.example.dlibtest.FileUtils;
import com.example.dlibtest.OnGetImageListener;
import com.example.dlibtest.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.AnimationGroup;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.ISurfaceRenderer;
import org.rajawali3d.view.SurfaceView;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import static android.content.Context.MODE_PRIVATE;
import static org.litepal.LitePalApplication.getContext;

public class ARFaceFragment extends AExampleFragment implements ARFaceContract.View{
    private static final String TAG = "ARFaceFragment";

    //private TrasparentTitleView mScoreView;
    private AutoFitTextureView textureView;
    private ImageView ivDraw;
    private TextView mTvCameraHint;
    private LinearLayout mLayoutBottomBtn;
    private RecyclerView mRvOrnament;
    private RecyclerView mRvFilter;
    private CustomBottomSheet mOrnamentSheet;
    private CustomBottomSheet mFilterSheet;
    private OrnamentAdapter mOrnamentAdapter;
   // private FilterAdapter mFilterAdapter;
    private Dialog mProgressDialog;

    private Context mContext;
    private ARFacePresenter mPresenter;
    private Paint mFaceLandmarkPaint;
    private MyCameraRenderer mCameraRenderer;
    private OnGetImageListener mOnGetPreviewListener = null;

    private List<Ornament> mOrnaments = new ArrayList<>();
    private List<Integer> mFilters = new ArrayList<>();
    private MediaLoaderCallback mediaLoaderCallback = null;
    private Subscription mSubscription = null;

    private ImageView mGuideView;

    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;
    private boolean isDrawLandMark = true;
    private boolean isBuildMask = false;
    private int mOrnamentId = -1;

    private Handler mUIHandler;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;

    private int PointX;
    private int PointY;


    //拍照相关
    private ImageView mTakePhotoIv;

    //与ar贴纸相关
    private static boolean screenshot = false;

    //与相机相关
    private ProgressBar mProgressBar;
    private TextView mProgressNumTv;
    private  int mProgress = 0;

    public static ARFaceFragment newInstance() {
        return new ARFaceFragment();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mContext = getContext();
        mPresenter = new ARFacePresenter(mContext, this);
        return mLayout;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_ar_face;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        initView(view);
        initOrnamentSheet();
        initOrnamentData();
        //initFilterSheet();
        initCamera();
    }

    /*
    初始化UI界面因素
     */
    private void initView(View view) {

        mTakePhotoIv = (ImageView) view.findViewById(R.id.iv_take_picture);

        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        ivDraw = (ImageView) view.findViewById(R.id.iv_draw);
        mTvCameraHint = (TextView) view.findViewById(R.id.tv_hint);
        mLayoutBottomBtn = (LinearLayout) view.findViewById(R.id.layout_bottom_btn);

        CheckBox checkLandMark = (CheckBox) view.findViewById(R.id.check_land_mark);
        ImageView btnOrnament = (ImageView) view.findViewById(R.id.iv_ar_sticker);
        ImageView btnFilterSheet = (ImageView) view.findViewById(R.id.iv_camera_filter);

        //引导界面
     /*   mGuideView = (ImageView) view.findViewById(R.id.iv_guide);
        mGuideView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGuideView.setVisibility(View.GONE);
            }
        });*/

        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int id = buttonView.getId();
                switch (id) {
                    case R.id.check_land_mark:
                        isDrawLandMark = isChecked;
                        break;
                }
            }
        };

        checkLandMark.setOnCheckedChangeListener(onCheckedChangeListener);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.iv_ar_sticker:
                        mOrnamentSheet.show();
                        break;
                    case R.id.iv_take_picture:
                        mOnGetPreviewListener.setSavePreviewBitmap(true);
                        takePhoto();

                        break;
                }
            }
        };

        btnOrnament.setOnClickListener(onClickListener);
        btnFilterSheet.setOnClickListener(onClickListener);
        mTakePhotoIv.setOnClickListener(onClickListener);
    }


    /**
     * 拍照相关
     */
    private void takePhoto() {
        CameraUtils.takePicture();
        if (CameraUtils.mPicture != null){
            final Canvas canvas = new Canvas(CameraUtils.mPicture);
            //饰品renderer
            ((ARFaceFragment.AccelerometerRenderer) mRenderer).setListnener(new DrawStickerListene() {
                @Override
                public void drawSticker(Bitmap bitmap) {
                    canvas.drawBitmap(bitmap, new Matrix(), new Paint());
                }
            });

            new SavePictureTask(FileUtils.getOutputMediaFile(), null).execute(CameraUtils.mPicture);
        }

        screenshot = true;
    }

    /*
    初始化饰品界面
     */
    private void initOrnamentSheet() {
        mOrnamentAdapter = new OrnamentAdapter(mContext, mOrnaments);
        mOrnamentAdapter.setOnItemClickListener(new OrnamentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mOrnamentSheet.dismiss();
                mOrnamentId = position;
                isBuildMask = true;

            }
        });

        View sheetView = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_bottom_sheet, null);
        mRvOrnament = (RecyclerView) sheetView.findViewById(R.id.rv_gallery);
        mRvOrnament.setAdapter(mOrnamentAdapter);
        mRvOrnament.setLayoutManager(new GridLayoutManager(mContext, 4));
        mOrnamentSheet = new CustomBottomSheet(mContext);
        mOrnamentSheet.setContentView(sheetView);
        mOrnamentSheet.getWindow().findViewById(R.id.design_bottom_sheet)
                .setBackgroundResource(android.R.color.transparent);
    }

    /*
    初始化饰品数据
     */

    private void initOrnamentData() {
        mOrnaments.addAll(mPresenter.getPresetOrnament());
        mOrnamentAdapter.notifyDataSetChanged();
    }

    /*
    初始化相机
     */
    private void initCamera() {
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        int orientation = getResources().getConfiguration().orientation;
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        //开始相机预览
        CameraUtils.init(textureView, cameraManager, orientation, rotation);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUIHandler = new Handler(Looper.getMainLooper());

        mFaceLandmarkPaint = new Paint();
        mFaceLandmarkPaint.setColor(Color.YELLOW);
        mFaceLandmarkPaint.setStrokeWidth(2);
        mFaceLandmarkPaint.setStyle(Paint.Style.STROKE);
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
            CameraUtils.openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            if (mOnGetPreviewListener == null) {
                initGetPreviewListener();
            }
            if (mCameraRenderer == null) {
                mCameraRenderer = new MyCameraRenderer(mContext);
            }
            textureView.setSurfaceTextureListener(mCameraRenderer);
        }

        if (mediaLoaderCallback == null) {
            //loadLocalImage();
        }
    }

    @Override
    public void onPause() {
        CameraUtils.closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSubscription != null) {
            mSubscription.cancel();
        }
        mRvOrnament.setAdapter(null);
        CameraUtils.releaseReferences();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());

        CameraUtils.setBackgroundHandler(backgroundHandler);
    }


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
            Log.e(TAG, "error" ,e );
        }
    }

    //初始化预览监听器
    private void initGetPreviewListener() {
        //开始图片监听，进入inferenceHandler
        mOnGetPreviewListener = new OnGetImageListener();

        Thread mThread = new Thread() {
            @Override
            public void run() {
                mOnGetPreviewListener.initialize(
                        getActivity().getApplicationContext(), getActivity().getAssets(), inferenceHandler);
            }
        };
        mThread.start();

    //特征点监听器
        mOnGetPreviewListener.setLandMarkListener(new OnGetImageListener.LandMarkListener() {
            @Override
            public void onLandmarkChange(final List<VisionDetRet> results) {
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                        handleMouthOpen(results);
                        if (!isDrawLandMark) {
                            ivDraw.setImageResource(0);
                        }
                    }
                });

                if (isDrawLandMark) {
                    inferenceHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (results != null && results.size() > 0) {

                                drawLandMark(results.get(0));
                            }
                        }
                    });
                }
            }
            @Override
            public void onRotateChange(double x, double y, double z) {
                rotateModel(x, y, z);
            }

            @Override
            public void onTransChange(double x, double y, double z) {
                ARFaceFragment.AccelerometerRenderer renderer = ((ARFaceFragment.AccelerometerRenderer) mRenderer);
               // renderer.mContainer.setPosition(x, -y, z);
             //   renderer.getCurrentCamera().setPosition(-x/10, y/10, z/10);
             //   renderer.mContainer.setPosition(x, y, z);
                renderer.getCurrentCamera().setPosition(x/400, -y/400, -z/200);
                Log.i(TAG,"CURRENT相机的位置！ "+String.valueOf(renderer.getCurrentCamera().getPosition().x)+" "+String.valueOf(renderer.getCurrentCamera().getPosition().y)+" "+String.valueOf(renderer.getCurrentCamera().getPosition().z));
            }

            @Override
            public void onMatrixChange(ArrayList<Double> elementList) {
            }
        });

        //模型监听器
        mOnGetPreviewListener.setBuildMaskListener(new OnGetImageListener.BuildMaskListener() {
            @Override
            public void onGetSuitableFace(final Bitmap bitmap, final ArrayList<Point> landmarks) {
                Log.e("rotateList", "onGetSuitableFace");
                isBuildMask = true;


                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                       // OBJUtils.buildFaceModel(mContext, bitmap, landmarks);
                        //isBuildMask = true;
                    }
                });
            }
        });

        CameraUtils.setOnGetPreviewListener(mOnGetPreviewListener);

}

    /*
    画特征点
     */
    private void drawLandMark(VisionDetRet ret) {
        float resizeRatio = 1.0f;
        //float resizeRatio = 2.5f;    // 预览尺寸 480x320  /  截取尺寸 192x128  (另外悬浮窗尺寸是 810x540)
        Rect bounds = new Rect();
        bounds.left = (int) (ret.getLeft() * resizeRatio);
        bounds.top = (int) (ret.getTop() * resizeRatio);
        bounds.right = (int) (ret.getRight() * resizeRatio);
        bounds.bottom = (int) (ret.getBottom() * resizeRatio);

        Size previewSize = CameraUtils.getPreviewSize();
        if (previewSize != null) {
            final Bitmap mBitmap = Bitmap.createBitmap(previewSize.getHeight(), previewSize.getWidth(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            canvas.drawRect(bounds, mFaceLandmarkPaint);

            final ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawCircle(pointX, pointY, 2, mFaceLandmarkPaint);
            }

            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    ivDraw.setImageBitmap(mBitmap);
                }
            });
        }
    }

    private void rotateModel(double x, double y, double z) {
        if (mRenderer != null) {
            boolean isJumpX = false;
            boolean isJumpY = false;
            boolean isJumpZ = false;
            double rotateX = x;
            double rotateY = y;
            double rotateZ = z;


            if (Math.abs(lastX-x) > 90) {
                Log.e("rotateException", "X 跳变");
                isJumpX = true;
                rotateX = lastX;
            }
            if (Math.abs(lastY-y) > 90) {
                Log.e("rotateException", "Y 跳变");
                isJumpY = true;
                rotateY = lastY;
            }
            if (Math.abs(lastZ-z) > 90) {
                Log.e("rotateException", "Z 跳变");
                isJumpZ = true;
                rotateZ = lastZ;
            }

            ((ARFaceFragment.AccelerometerRenderer) mRenderer).setAccelerometerValues(rotateZ, rotateY, -rotateX);

            if (!isJumpX) lastX = x;
            if (!isJumpY) lastY = y;
            if (!isJumpZ) lastZ = z;
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        mSubscription = subscription;
        mSubscription.request(Long.MAX_VALUE);
    }

    @Override
    public ISurfaceRenderer createRenderer() {
        return new ARFaceFragment.AccelerometerRenderer(getActivity(), this);
    }

    @Override
    protected void onBeforeApplyRenderer() {
        ((SurfaceView) mRenderSurface).setTransparent(true);
        //mRenderSurface.setTransparent(true);
        //((TextureView) mRenderSurface).setAlpha(0.5f);
        super.onBeforeApplyRenderer();
    }


    //贴纸Renderer
    private final class AccelerometerRenderer extends AExampleRenderer{
        private DirectionalLight mLight;
        private Object3D mContainer;
        private Object3D mMonkey;
        private Object3D mOrnament;
        private Vector3 mAccValues;
        private int mViewportWidth;
        private int mViewportHeight;

        //绘制贴纸的监听
        private DrawStickerListene mListener;

        AccelerometerRenderer(Context context, @Nullable AExampleFragment fragment) {
            super(context, fragment);
            mAccValues = new Vector3();
        }

        public void setListnener(DrawStickerListene listnener){
            mListener = listnener;
        }

        @Override
        protected void initScene() {
            try {
                mLight = new DirectionalLight(0.1f, -1.0f, -1.0f);
                mLight.setColor(1.0f, 1.0f, 1.0f);
                mLight.setPower(1);
                getCurrentScene().addLight(mLight);

                mContainer = new Object3D();
                showMaskModel();
                getCurrentScene().addChild(mContainer);


                Log.i(TAG,"相机初始位置"+String.valueOf(getCurrentCamera().getPosition().x)+" "+String.valueOf(getCurrentCamera().getPosition().y)+" "+String.valueOf(getCurrentCamera().getPosition().z));

            } catch (Exception e) {
                e.printStackTrace();
            }

            getCurrentScene().setBackgroundColor(0);
        }

        @Override
        public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
            super.onRenderSurfaceSizeChanged(gl, width, height);
            mViewportWidth = width;
            mViewportHeight = height;
        }

        @Override
        public void onRenderFrame(GL10 gl) {
            super.onRenderFrame(gl);
            if(screenshot){
                Log.d(TAG, "screenshot");
                int screenshotSize = mViewportWidth * mViewportHeight;
                ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
                bb.order(ByteOrder.nativeOrder());
                gl.glReadPixels(0, 0, mViewportWidth, mViewportHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
                int pixelsBuffer[] = new int[screenshotSize];
                bb.asIntBuffer().get(pixelsBuffer);
                bb = null;

                Bitmap mArSticker = Bitmap.createBitmap(mViewportWidth, mViewportHeight, Bitmap.Config.ARGB_8888);
                mArSticker.setPixels(pixelsBuffer, screenshotSize-mViewportWidth, -mViewportWidth, 0, 0, mViewportWidth, mViewportHeight);
                pixelsBuffer = null;

                int sBuffer[] = new int[screenshotSize];
                int result[] = new int[screenshotSize];
                IntBuffer sb = IntBuffer.wrap(sBuffer);
                mArSticker.copyPixelsToBuffer(sb);

                //Making created bitmap (from OpenGL points) compatible with Android bitmap
                for (int i = 0; i < mViewportHeight; i++) {
                    for (int j = 0; j < mViewportWidth; j++){
                        int pix=sBuffer[i*mViewportWidth+j];
                        int pb=(pix>>16)&0xff;
                        int pr=(pix<<16)&0x00ff0000;
                        int pix1=(pix&0xff00ff00) | pr | pb;
                        sBuffer[i*mViewportWidth+j]=pix1;
                    }

                }

                sb.rewind();
                mArSticker.copyPixelsFromBuffer(sb);
                //new SavePictureTask(FileUtils.getOutputMediaFile(), null).execute(bitmap);
                screenshot = false;
                if (null != mListener){
                    mListener.drawSticker(mArSticker);
                }
            }
        }

        @Override
        protected void onRender(long ellapsedRealtime, double deltaTime) {
            super.onRender(ellapsedRealtime, deltaTime);
            if (isBuildMask) {
                showMaskModel();
                isBuildMask = false;
            }
           // showMaskModel();
            mContainer.setPosition(0,0,0);
            mContainer.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);

        }

        void setAccelerometerValues(double x, double y, double z) {
            mAccValues.setAll(x, y, z);
        }


        void showMaskModel() {
            try {
                boolean isFaceVisible = true;
                boolean isOrnamentVisible = true;

                if (mOrnament != null) {
                    isOrnamentVisible = mOrnament.isVisible();
                    mOrnament.setScale(1.0f);
                    mOrnament.setPosition(0, 0, 0);
                    mContainer.removeChild(mOrnament);
                }


                if (mOrnamentId >= 0 && mOrnaments.size() > mOrnamentId) {
                    Ornament ornament = mOrnaments.get(mOrnamentId);
                    LoaderOBJ objParser1 = new LoaderOBJ(mContext.getResources(), mTextureManager, ornament.getModelResId());
                    objParser1.parse();
                    mOrnament = objParser1.getParsedObject();
                    mOrnament.setScale(ornament.getScale());
                    mOrnament.setPosition(ornament.getOffsetX(), ornament.getOffsetY(), ornament.getOffsetZ());
                    mOrnament.setRotation(ornament.getRotateX(), ornament.getRotateY(), ornament.getRotateZ());
                    int color = ornament.getColor();
                    if (color != ARFacePresenter.NO_COLOR) {
                        mOrnament.getMaterial().setColor(color);
                    }
                    mOrnament.setVisible(isOrnamentVisible);
                    mContainer.addChild(mOrnament);


                    getCurrentScene().clearAnimations();
                    List<Animation3D> animation3Ds = ornament.getAnimation3Ds();
                    if (animation3Ds != null && animation3Ds.size() > 0) {
                        final AnimationGroup animGroup = new AnimationGroup();
                        animGroup.setRepeatMode(Animation.RepeatMode.REVERSE_INFINITE);

                        for (Animation3D animation3D : animation3Ds) {
                            animation3D.setTransformable3D(mOrnament);
                            animGroup.addAnimation(animation3D);
                        }

                        getCurrentScene().registerAnimation(animGroup);
                        animGroup.play();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    interface DrawStickerListene{
        void drawSticker(Bitmap bitmap);
    }

    private void showDialog(final String title, final String content) {
        mProgressDialog = new Dialog(mContext);
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.dialog_progress, null);
        mProgressDialog.setContentView(rootView);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.pb_progress);
        mProgressNumTv = (TextView) rootView.findViewById(R.id.tv_progress_num);

        mProgressBar.setProgress(mProgress);
        mProgressNumTv.setVisibility(View.VISIBLE);
        mProgressNumTv.setText(mPresenter + "%");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

    }

    private void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgress = 0;
        }
    }
}
