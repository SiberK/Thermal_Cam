package com.example.andrew.thermal_cam;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by Andrew on 25.05.18.
 */

public class TAmgRegs{
    private byte    PCTL        ;
    private byte    FPSC        ;
    private byte    INTC        ;
    private byte    STAT        ;
    private byte    AVE         ;
    private short   TTH         ;
    private float   TempPxl[]   ;// значения температуры полученные с сенсора (8х8)
    public  float   IntPxl[]    ;// интерполированные значения температуры (24х24)
    public  int     ImgPxl[]    ;// цветовая интерпретация температур (24х24)
    private int     IxTrg = -1  ;

    public final int AMG_COLS = 8  ;
    public final int AMG_ROWS = 8  ;
    public final int AMG_COUNT_CELLS = AMG_COLS * AMG_ROWS  ;

    public final int INTERPOLATED_COLS = 24    ;
    public final int INTERPOLATED_ROWS = 24    ;
    public final int INT_COUNT_CELLS = INTERPOLATED_COLS * INTERPOLATED_ROWS    ;

    private final int CamColors[] =
           {       0x480078,0x400078,0x400078,0x400078,0x400080,0x380080,0x380080,0x380080,
                   0x380080,0x300080,0x300080,0x300080,0x280080,0x280080,0x280080,0x280080,
                   0x200080,0x200080,0x200080,0x180080,0x180080,0x180088,0x180088,0x100088,
                   0x100088,0x100088,0x080088,0x080088,0x080088,0x000088,0x000088,0x000088,
                   0x000088,0x000088,0x000488,0x000488,0x000888,0x000C90,0x000C90,0x001090,
                   0x001490,0x001490,0x001890,0x001C90,0x001C90,0x002090,0x002490,0x002890,
                   0x002890,0x002C90,0x003090,0x003090,0x003490,0x003890,0x003C98,0x003C98,
                   0x004098,0x004498,0x004898,0x004898,0x004C98,0x005098,0x005498,0x005898,
                   0x005898,0x005C98,0x006098,0x006498,0x006498,0x006898,0x006C98,0x0070A0,
                   0x0074A0,0x0078A0,0x0078A0,0x007CA0,0x0080A0,0x0084A0,0x0088A0,0x008CA0,
                   0x008CA0,0x0090A0,0x0094A0,0x0098A0,0x009CA0,0x00A0A0,0x00A4A0,0x00A4A0,
                   0x00A8A0,0x00A8A0,0x00ACA0,0x00ACA0,0x00AC98,0x00AC98,0x00AC98,0x00AC90,
                   0x00AC90,0x00AC90,0x00AC88,0x00B088,0x00B088,0x00B080,0x00B080,0x00B078,
                   0x00B078,0x00B078,0x00B070,0x00B470,0x00B470,0x00B468,0x00B468,0x00B468,
                   0x00B460,0x00B460,0x00B458,0x00B858,0x00B858,0x00B850,0x00B850,0x00B850,
                   0x00B848,0x00B848,0x00B840,0x00BC40,0x00BC40,0x00BC38,0x00BC38,0x00BC30,
                   0x00BC30,0x00BC30,0x00BC28,0x00BC28,0x00C020,0x00C020,0x00C020,0x00C018,
                   0x00C018,0x00C010,0x00C010,0x00C008,0x00C408,0x00C408,0x00C400,0x00C400,
                   0x00C400,0x00C400,0x08C400,0x08C400,0x08C800,0x10C800,0x10C800,0x18C800,
                   0x18C800,0x20C800,0x20C800,0x28C800,0x28CC00,0x30CC00,0x30CC00,0x38CC00,
                   0x38CC00,0x38CC00,0x40CC00,0x40CC00,0x48CC00,0x48D000,0x50D000,0x50D000,
                   0x58D000,0x58D000,0x60D000,0x60D000,0x68D000,0x68D400,0x70D400,0x70D400,
                   0x78D400,0x78D400,0x80D400,0x80D400,0x88D400,0x88D800,0x90D800,0x90D800,
                   0x98D800,0x98D800,0xA0D800,0xA8D800,0xA8D800,0xB0DC00,0xB0DC00,0xB8DC00,
                   0xB8DC00,0xC0DC00,0xC0DC00,0xC8DC00,0xC8DC00,0xD0DC00,0xD0E000,0xD8E000,
                   0xD8DC00,0xD8D800,0xD8D400,0xD8D000,0xD8D000,0xE0CC00,0xE0C800,0xE0C400,
                   0xE0C000,0xE0BC00,0xE0B800,0xE0B400,0xE0B000,0xE0AC00,0xE0A800,0xE0A400,
                   0xE0A000,0xE09C00,0xE09800,0xE09400,0xE09000,0xE08C00,0xE88800,0xE88400,
                   0xE88000,0xE87C00,0xE87800,0xE87400,0xE87000,0xE86C00,0xE86800,0xE86400,
                   0xE86000,0xE85C00,0xE85800,0xE85400,0xE85000,0xE84C00,0xE84800,0xF04400,
                   0xF04000,0xF03C00,0xF03800,0xF03400,0xF03000,0xF02C00,0xF02800,0xF02000,
                   0xF01C00,0xF01800,0xF01400,0xF01000,0xF00C00,0xF00800,0xF00400,0xF80000
           };

    //=======================================================================================
    TAmgRegs(){
        PCTL   =  FPSC = INTC = STAT = AVE  = 0 ; TTH  = 0      ;
        TempPxl    = new float[AMG_COUNT_CELLS]                 ;
        IntPxl  = new float[INT_COUNT_CELLS]                    ;
        ImgPxl = new int[INTERPOLATED_ROWS * INTERPOLATED_COLS] ;
    }
    //=======================================================================================
    public void Set(byte[] buf,float TempMin,float TempMax){
        if(buf != null && TempPxl != null && IntPxl != null && ImgPxl != null){
            ByteBuffer bbPack   = ByteBuffer.wrap(buf)	; bbPack.order(ByteOrder.LITTLE_ENDIAN)	;
            ShortBuffer sbPack  = bbPack.asShortBuffer();
            PCTL = bbPack.get(0)        ;
            FPSC = bbPack.get(1)        ;
            INTC = bbPack.get(2)        ;
            STAT = bbPack.get(3)        ;
            AVE  = bbPack.get(4)        ;
            TTH  = sbPack.get(3)        ;

            for(int ix=0;ix<AMG_COUNT_CELLS;ix++){
                short val = sbPack.get(ix + 4);
                if((val & 0x800) != 0) val = (short)(val | 0xF000);
                TempPxl[ix] = val * 0.25f   ;// переводим в градусы!
            }

            interpolate_image(TempPxl, AMG_ROWS, AMG_COLS, IntPxl, INTERPOLATED_ROWS, INTERPOLATED_COLS);

            for(int ix=0;ix<INTERPOLATED_ROWS * INTERPOLATED_COLS;ix++){
                int val = map(IntPxl[ix],TempMin,TempMax,0,255)  ;
                ImgPxl[ix] = CamColors[val] ;
            }
        }
    }
    //=======================================================================================
    TAmgRegs(byte[] buf,float TempMin,float TempMax){
        TempPxl = new float[AMG_COUNT_CELLS]                    ;
        IntPxl  = new float[INT_COUNT_CELLS]                    ;
        ImgPxl  = new int[INTERPOLATED_ROWS * INTERPOLATED_COLS];
        Set(buf,TempMin,TempMax)                                ;
    }
    //=======================================================================================
    public float GetTempTrg(int ixTrg){
        return (ixTrg >= 0 && ixTrg < INT_COUNT_CELLS) ? IntPxl[ixTrg] : 0f  ;}
    //=======================================================================================
    //=======================================================================================
    //=======================================================================================
    private int map(float val,float valMin,float valMax,int outMin,int outMax){
        val = (val<valMin) ? valMin :(val>valMax) ? valMax : val   ;
        int result = (int) (outMin + (val-valMin)/(valMax-valMin)*(outMax-outMin));
        result = (result<outMin) ? outMin : (result > outMax) ? outMax : result     ;
        return result   ;}
    //=======================================================================================
    // src is a grid src_rows * src_cols
// dest is a pre-allocated grid, dest_rows*dest_cols
    private void interpolate_image(float src[], int src_rows, int src_cols,
                          float dest[], int dest_rows, int dest_cols) {
        float mu_x = (src_cols - 1.0f) / (dest_cols - 1.0f);
        float mu_y = (src_rows - 1.0f) / (dest_rows - 1.0f);

        float adj_2d[] = new float[16]; // matrix for storing adjacents

        for (int y_idx=0; y_idx < dest_rows; y_idx++) {
            for (int x_idx=0; x_idx < dest_cols; x_idx++) {
                float x = x_idx * mu_x;
                float y = y_idx * mu_y;
                get_adjacents_2d(src, adj_2d, src_rows, src_cols, (int)x, (int)y);
                float frac_x = x - (int)x; // we only need the ~delta~ between the points
                float frac_y = y - (int)y; // we only need the ~delta~ between the points
                float out = bicubicInterpolate(adj_2d, frac_x, frac_y);
                if ((x_idx >= 0) && (x_idx < dest_cols) && (y_idx >= 0) && (y < dest_rows))
                    dest[y_idx * dest_cols + x_idx] = out   ;
            }
        }
    }
    //=======================================================================================
    private float get_point(float p[], int rows, int cols, int x, int y) {
        if (x < 0)        x = 0;
        if (y < 0)        y = 0;
        if (x >= cols)    x = cols - 1;
        if (y >= rows)    y = rows - 1;
        return p[y * cols + x];
    }
    //=======================================================================================
    // p is a list of 4 points, 2 to the left, 2 to the right
    private float cubicInterpolate(float p[],int offset , float x) {
        float r = p[1+offset] + (0.5f * x * (p[2+offset] - p[0+offset] + x*(2.0f*p[0+offset] - 5.0f*p[1+offset] + 4.0f*p[2+offset] - p[3+offset] + x*(3.0f*(p[1+offset] - p[2+offset]) + p[3+offset] - p[0+offset]))));
        return r;
    }
    //=======================================================================================
    // p is a 16-point 4x4 array of the 2 rows & columns left/right/above/below
    private float bicubicInterpolate(float p[], float x, float y) {
        float arr[] = {0,0,0,0};
        arr[0] = cubicInterpolate(p, 0, x);
        arr[1] = cubicInterpolate(p, 4, x);
        arr[2] = cubicInterpolate(p, 8, x);
        arr[3] = cubicInterpolate(p,12, x);
        return   cubicInterpolate(arr, 0, y);
    }
    //=======================================================================================
    // src is rows*cols and dest is a 16-point array passed in already allocated!
    private void get_adjacents_2d(float src[], float dest[], int rows, int cols, int x, int y) {
        for (int delta_y = -1; delta_y < 3; delta_y++) { // -1, 0, 1, 2
            int row_offset = 4 * (delta_y+1); // index into each chunk of 4
            for (int delta_x = -1; delta_x < 3; delta_x++) { // -1, 0, 1, 2
                dest[delta_x+1+row_offset] = get_point(src, rows, cols, x+delta_x, y+delta_y);
            }
        }
    }
    //=======================================================================================
    //=======================================================================================
}

