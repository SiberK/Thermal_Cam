package com.example.andrew.thermal_cam;



import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import android.view.SurfaceView;
import android.view.TextureView;

import java.util.Arrays;

/**
 * Created by Andrew on 02.06.18.
 */
public class CameraHelper {
    private static final String     LOG_TAG        = "CAMERA"   ;
    private CameraManager           mCameraManager = null       ;
    public  String                  mCameraID      = null       ;
    private CameraDevice            mCameraDevice  = null       ;
    private TextureView             mTextureView   = null       ;
    private SurfaceView             mSurfaceView   = null       ;
    private CameraCaptureSession    mSession                    ;
    //==========================================================================================
    public void setTextureView(TextureView textureView){     mTextureView = textureView; }
    public void setSurfaceView(SurfaceView surfaceView){     mSurfaceView = surfaceView; }
    //==========================================================================================
    public CameraHelper(CameraManager cameraManager,String cameraID) {
        mCameraManager  = cameraManager;
        mCameraID       = cameraID;     }
    //==========================================================================================
    public boolean isOpen() {
        if (mCameraDevice == null) {
            return false;     }
        else {
            return true;
        }
    }
    //==========================================================================================
    private void createCameraPreviewSession() {
        Surface surface = null  ;
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(1920,1080);
        surface = new Surface(texture);
        try {
            final CaptureRequest.Builder builder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mSession = session;
                            try {
                                mSession.setRepeatingRequest(builder.build(),null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                        }
                    },                 null         );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //==========================================================================================
    public void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    //==========================================================================================
    public CameraDevice.StateCallback   GetCameraCallback(){return mCameraCallback  ;}
    //==========================================================================================
    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());
            createCameraPreviewSession();
        }
        @Override     public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
            Log.i(LOG_TAG, "disconnect camera with id:"+mCameraDevice.getId());
            mCameraDevice = null;
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
        }
    };
    //==========================================================================================
    public void viewFormatSize(int formatSize) {                                        // Получения характеристик камеры
        CameraCharacteristics cc = null;
        try {
            cc = mCameraManager.getCameraCharacteristics(mCameraID);                    // Получения списка выходного формата, который поддерживает камера
            StreamConfigurationMap configurationMap =
                    cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);     // Получения списка разрешений которые поддерживаются для формата jpeg
            Size[] sizesJPEG = configurationMap.getOutputSizes(ImageFormat.JPEG);
            if (sizesJPEG != null) {
                for (Size item:sizesJPEG) {
                    Log.i(LOG_TAG, "w:" + item.getWidth() + " h:" + item.getHeight());
                }
            }
            else {
                Log.e(LOG_TAG, "camera with id: "+mCameraID+" don`t support format: "+formatSize);
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG,e.getMessage());
            //e.printStackTrace();
        }
    }
    //==========================================================================================
}
