package com.example.andrew.thermal_cam;


import android.content.res.Resources;
import android.graphics.Canvas;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Andrew on 26.05.18.
 */
public class TFaceDraw{
    private static final String TAG = "FACE_DRWBL";

    private int             Wdt,Hgt             ;
    private float           CellSize            ;
    private boolean         flStrk = true       ;

    private TAmgRegs        AmgRegs = null      ;
    private int             Orient = 0          ;
    private Point           PntTrg = new Point(-1,-1) ;
    //------------------------------------------------------------------------
    public TFaceDraw(Resources resources){
    }
    //------------------------------------------------------------------------
    public void SetSize(int w,int h){/* Wdt = w ; Hgt = h   ; */}
    //------------------------------------------------------------------------
    public void Work(TAmgRegs amgRegs,int orient){
        if(amgRegs != null){
            Orient = orient                     ;
            AmgRegs = amgRegs                   ;
            flStrk = true                       ;
        }
    }
    //------------------------------------------------------------------------
    protected synchronized void onDraw(Canvas canvas,int bk){
        if(AmgRegs == null || !flStrk) return               ;
        int cols = AmgRegs.INTERPOLATED_COLS , ix           ;
        int rows = AmgRegs.INTERPOLATED_ROWS                ;

        Wdt = canvas.getWidth() ;
        Hgt = canvas.getHeight();

        float CellWdt = (float)Wdt / cols                   ;
        float CellHgt = (float)Hgt / rows                   ;
        CellSize = CellWdt < CellHgt ? CellWdt : CellHgt    ;
        RectF   rectCell                                    ;
        Paint   pt = new Paint()                            ;

//        flStrk = false  ;

        for(int ir=0;ir<rows;ir++){
            for(int ic=0;ic<cols;ic++){
                rectCell = new RectF(ic*CellSize,ir*CellSize,(ic+1)*CellSize,(ir+1)*CellSize)   ;
                ix = Flat(ic,ir,cols,rows)                  ;
                pt.setColor(AmgRegs.ImgPxl[ix] | 0xFF000000);
                canvas.drawRect(rectCell,pt)                ;
            }
        }

        if(PntTrg.x >= 0 && PntTrg.y >= 0){
            pt.setColor(Color.BLACK)                    ;
            pt.setStyle(Paint.Style.STROKE)             ;
            pt.setStrokeWidth(3)                        ;
            float cx = CellSize * (PntTrg.x + 0.5f)     ;
            float cy = CellSize * (PntTrg.y + 0.5f)     ;
            canvas.drawCircle(cx,cy,CellSize/2,pt)      ;
        }
    }
    //------------------------------------------------------------------------
    public int SingleTapUp(MotionEvent e,Rect localRect){
        int posX = (int)e.getX() - localRect.left       ;
        int posY = (int)e.getY() - localRect.top        ;
        float col = posX / CellSize                     ;
        float row = posY / CellSize                     ;
        int cols = AmgRegs != null ? AmgRegs.INTERPOLATED_COLS : 24 ;
        int rows = AmgRegs != null ? AmgRegs.INTERPOLATED_ROWS : 24 ;
        PntTrg = new Point((int)col,(int)row)           ;
        int ixTrg = Flat(PntTrg.x,PntTrg.y,cols,rows)   ;
        l("Tap X="+PntTrg.x+" Y="+PntTrg.y + " IX="+ixTrg)        ;

        return ixTrg  ;
    }
    //------------------------------------------------------------------------
    private int Flat(int ic,int ir,int cols,int rows){
        int ix = -1  ;
        switch(Orient){
        case 0: ix = ir*cols+ic             ; break ;
        case 1: ix = ir*cols+(cols-ic-1)    ; break ;
        case 2: ix = ic*rows+ir             ; break ;
        case 3: ix = ic*rows+(rows-ir-1)    ; break ;
        case 4: ix = (cols-ic-1)*rows+ir    ; break ;
        case 5: ix = (rows-ir-1)*cols+ic    ; break ;
        default:ix = ir*cols+ic ;
        }
        if(ix < 0 || ix >= cols*rows) ix = -1       ;
        return ix   ;
    }
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------
    private void e(String msg){Log.e(TAG,">=< " + msg + " >=<"); }
    //----------------------------------------------------------------------
    private void l(String msg){Log.d(TAG, ">==< "+msg+" >==<"); }
    //----------------------------------------------------------------------
    private void i(String msg){Log.i(TAG, ">===< "+msg+" >===<"); }
    //----------------------------------------------------------------------

}
