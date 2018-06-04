package com.example.andrew.thermal_cam;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Rect;

import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.TextureView;

/**
 * Created by Andrew on 02.06.18.
 */

public class ThermalView2 extends TextureView implements TextureView.SurfaceTextureListener{
    private static final String TAG = "TH_VIEW2" ;
    private ThermalView2.ThThread mThThread           ;
    public	long			FrameTime			;

    private TFaceDraw       FaceDraw            ;
    private boolean         flChange = true     ;
    SurfaceHolder holder              ;

    public ThermalView2(Context context, AttributeSet attrs){
        super(context,attrs);
        i("++ThermalView2++")   ;
        if(!isInEditMode()){
            setSurfaceTextureListener(this)             ;
            FaceDraw = new TFaceDraw(getResources())  ;
        }
    }
    //------------------------------------------------------------------------------------------
    public void DrawFace(Canvas canvas,int bk) {
        if(FaceDraw != null) FaceDraw.onDraw(canvas, bk);
    }
    //-------------------------------------------------------------------------------------
    public int SingleTapUp(MotionEvent e){
        Rect rect = new Rect()                  ;
        getGlobalVisibleRect(rect)              ;
        return (FaceDraw != null)? FaceDraw.SingleTapUp(e,rect) : 0    ;}
    //------------------------------------------------------------------------------------------
    public void Work(TAmgRegs AmgRegs,int orient){
        if(FaceDraw != null) FaceDraw.Work(AmgRegs,orient)  ;
        flChange = true  ;
    }
    //------------------------------------------------------------------------------------------
    public void onResume() {
    }
    //=====================================================================
    public void onPause() {
    }
    //=====================================================================
    public void stop(){ // пауза
        i("++stopGame++");
        if (mThThread != null)
            mThThread.setRunning(false);
//        thThreadStopped = true	;
    }
    //=====================================================================
    // освобождает ресурсы; вызывается методом onDestroy
    public void releaseResources(){
        i("++releaseResources++")   ;
    }
    //=====================================================================
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1){
        i("++surfaceTextureAvailable++") ;
        mThThread = new ThermalView2.ThThread(this);
        mThThread.setRunning(true);
        mThThread.start();
    }
//------------------------------------------------------------------
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1){
    }
//------------------------------------------------------------------
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture){
        return false;
    }
//------------------------------------------------------------------
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture){

    }

    //----------------------------------------------------------------------
    class ThThread extends Thread{
    private boolean running = false;
    private TextureView mTextureView;
    //-------------------------------------------
    public ThThread(TextureView textureView){
        this.mTextureView = textureView;
        setName("GameThread");
    }
    //-------------------------------------------
    public void setRunning(boolean running){
        this.running = running;
    }
    //-------------------------------------------
    @Override
    public void run(){
        Canvas canvas = null    ;
        long previousFrameTime = System.currentTimeMillis();
        while(running){
            long currentTime = System.currentTimeMillis()	;
            FrameTime = currentTime - previousFrameTime	    ;
            if(FrameTime >= 120 || flChange){ previousFrameTime = currentTime 		; flChange = false   ;
                try{
                    canvas = mTextureView.lockCanvas(null);
                    if(canvas == null) continue;
                    synchronized (mTextureView){
                        DrawFace(canvas,1)         ;
                    }
                } finally{
                    if(canvas != null){
                        mTextureView.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
    }
 //=====================================================================================
//=====================================================================================
    private void e(String msg){Log.e(TAG,">=< " + msg + " >=<"); }
    //----------------------------------------------------------------------
    private void l(String msg){Log.d(TAG, ">==< "+msg+" >==<"); }
    //----------------------------------------------------------------------
    private void i(String msg){Log.i(TAG, ">===< "+msg+" >===<"); }
//=====================================================================================
}

