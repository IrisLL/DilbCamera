package com.example.dlibtest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.nfc.Tag;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class CustomCamera extends Activity implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceView mPreview;
    private SurfaceHolder mHolder;
    private ImageView back, position;//返回和切换前后置摄像头
    private int cameraPosition = 1;


    private  Camera.PictureCallback mPictureCallback=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            File tempFile=new File("/sdcard/temp.png");
            try {
                FileOutputStream fos=new FileOutputStream(tempFile);
                fos.write(bytes);
                fos.close();
                Intent intent=new Intent(CustomCamera.this,PicResultActivity.class);
                intent.putExtra("picPath",tempFile.getAbsolutePath());
                intent.putExtra("cameraPosition",cameraPosition);
                startActivity(intent);
                CustomCamera.this.finish();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);
        mPreview=(SurfaceView) findViewById(R.id.preview);
        mHolder=mPreview.getHolder();
        mHolder.addCallback(this);

    }

    public void capture(View view) {
        Log.i(TAG,"clicked 拍照");
        Camera.Parameters parameters=mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setPreviewSize(800,400);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success)
                {
                    Log.i(TAG,"聚焦成功");
                    mCamera.takePicture(null,null,mPictureCallback);
                }
                else
                {
                    Log.i(TAG,"聚焦失败");
                    if(cameraPosition==0)
                        mCamera.takePicture(null,null,mPictureCallback);
                }
            }
        });
    }

    /**
     * camera生命周期与activity生命周期进行绑定
     */
  @Override
    protected  void onResume() {

        super.onResume();
        if(mCamera==null)
        {
            mCamera=getCamera();
            if(mHolder!=null)
            {
                setStartPreview(mCamera,mHolder);
            }
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        releaseCamera();

    }

    //获取camera对象
    private Camera getCamera()
    {
        Camera camera;
        try {
                camera=Camera.open();

        } catch (Exception e) {
        camera=null;
        e.printStackTrace();
    }
        return camera;
    }

    /**
     * 开始预览相机内容
     * camera 与 surfaceview 绑定
     */
    private  void setStartPreview(Camera camera,SurfaceHolder holder)  {
        try {
            camera.setPreviewDisplay(holder);
            //将系统camera预览角度进行调整
            camera.setDisplayOrientation(90);
            camera.startPreview();;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 释放系统相机所占有的资源
     */
    private void  releaseCamera()
    {
        if(mCamera!=null)
        {
            //系统相机回调置空，取消surfaceview和mcamera关联
            mCamera.setPreviewCallback(null);
            //取消相机取景功能
            mCamera.stopPreview();
            //释放资源
            mCamera.release();
            mCamera=null;

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        setStartPreview(mCamera,mHolder);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCamera.stopPreview();
        //重启
        setStartPreview(mCamera,mHolder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();

    }


    public void ChangeCamera(View view) {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for(int i = 0; i < cameraCount; i++   ) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if(cameraPosition == 1) {
                //现在是后置，变更为前置
                if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();//开始预览
                    Log.i(TAG,"前置摄像头");
                    cameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();//开始预览
                    Log.i(TAG,"后置摄像头");
                    cameraPosition = 1;
                    break;
                }
            }
        }
    }


}
