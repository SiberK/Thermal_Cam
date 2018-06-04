package com.example.andrew.thermal_cam;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Andrew on 26.05.18.
 */

public class ThermalView extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = "TH_VIEW" ;
    private ThThread        mThThread           ;
    public	long			FrameTime			;
    private TFaceDraw       FaceDraw            ;
    private boolean         flChange = true     ;
    SurfaceHolder           holder              ;
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
    public ThermalView(Context context, AttributeSet attrs){
        super(context,attrs);
        i("++ThermalView++")   ;
        if(!isInEditMode()){
            setZOrderOnTop(true);
            holder = getHolder()                        ;
            holder.addCallback(this);
            holder.setFormat(PixelFormat.TRANSPARENT);

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
    // вызывается при изменении размеров представления, а также
    // при первом добавлении в иерархию представлений
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        flChange = true     ;
    }
    //=====================================================================
    public void onResume() {
    }
    //=====================================================================
    public void onPause() {
    }
    //=====================================================================
    public void stop(){ // пауза в игре
        i("++stopGame++");
        if (mThThread != null)
            mThThread.setRunning(false);
//        thThreadStopped = true	;
    }
    //=====================================================================
    // освобождает ресурсы; вызывается методом onDestroy класса CannonGame
    public void releaseResources(){
        i("++releaseResources++")   ;
    }
    //=====================================================================
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder){
        i("++surfaceCreated++") ;
        mThThread = new ThThread(getHolder());
        mThThread.setRunning(true);
        mThThread.start();
    }
    //------------------------------------------------------------------------------------------
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2){
    e("++surfaceChanged++") ;
    }
    //------------------------------------------------------------------------------------------
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder){e("++surfaceDestroyed++") ;}
    //------------------------------------------------------------------------------------------
        //------------------------------------------------------------------
    class ThThread extends Thread{
        private boolean running = false;
        private SurfaceHolder surfaceHolder;
        //-------------------------------------------
        public ThThread(SurfaceHolder surfaceHolder){
            this.surfaceHolder = surfaceHolder;
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
                        canvas = surfaceHolder.lockCanvas(null);
                        if(canvas == null) continue;
                        synchronized (surfaceHolder){
                            DrawFace(canvas,1)         ;
                        }
                    } finally{
                        if(canvas != null){
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            }
        }
    }
    //=====================================================================================
    private void e(String msg){Log.e(TAG,">=< " + msg + " >=<"); }
    //----------------------------------------------------------------------
    private void l(String msg){Log.d(TAG, ">==< "+msg+" >==<"); }
    //----------------------------------------------------------------------
    private void i(String msg){Log.i(TAG, ">===< "+msg+" >===<"); }
//----------------------------------------------------------------------
}
