package com.example.andrew.thermal_cam;



import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String                 TAG = "THRMCAM_MAIN"     ;
    private static final boolean                D = true                ;
    private static MainActivity	                Instance = null			;

    private TAmgRegs                            AmgRegs = null          ;
    private int                                 IxTrg = -1              ;
    private float                               TempTrg = 0             ;

//    private UsbManager mUsbManager             ;
//    private static  UsbSerialDriver             sDriver = null          ;
//    private         SerialInputOutputManager    mSerialIoManager        ;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SharedPreferences                   sPref					;
    private int                                 CntBadCRC = 0, Orient=0 ;
    private float                               TempMin=20,TempMax=30,Alpha=50   ;
    private SeekBar                             sbMax,sbMin,sbAlpha     ;
    private TextView                            lblMax,lblMin,lblTempTrg,lblAlpha  ;
//    private ThermalView                         mThermalView            ;
    private ThermalView2                         mThermalView2            ;
    private GestureDetector                     gestureDetector         ; // listens for double taps
    private int                                 Rotation = 0            ;

//    private Camera mCamera                 ;
    private CameraManager                       mCameraManager = null   ;
    private CameraHelper[]                      myCameras = null        ;
    private TextureView                         mImageView = null       ;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent   mPermissionIntent    ;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                break;
            case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                break;
            case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                break;
            case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                break;
            case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    //----------------------------------------------------------------
//    private Handler mHandler = new Handler() {
//        public void handleMessage(android.os.Message msg) { HandleMessage(msg)	;}
//    };
    //----------------------------------------------------------------------
    public static MainActivity	GetInstance() { return Instance	; }
    //------------------------------------------------------------------------
    //===================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState){
        e("++ ON CREATE ++");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Instance = this;
        mHandler = new MyHandler(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Rotation = getWindowManager().getDefaultDisplay().getRotation();
        Display display = getWindowManager().getDefaultDisplay();

        sPref   = getPreferences(MODE_PRIVATE);
        TempMax = sPref.getFloat("TEMP_MAX", 30);
        TempMin = sPref.getFloat("TEMP_MIN", 20);
        Alpha   = sPref.getFloat("ALPHA", 50);
        Orient  = sPref.getInt("ORIENT", 0);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        gestureDetector = new GestureDetector(this, gestureListener);// initialize the GestureDetector
        sbMax = (SeekBar) findViewById(R.id.sbTempMax);
        sbMin = (SeekBar) findViewById(R.id.sbTempMin);
        sbAlpha = (SeekBar) findViewById(R.id.sbAlpha)  ;
        lblMax = (TextView) findViewById(R.id.lblTempMax);
        lblMin = (TextView) findViewById(R.id.lblTempMin);
        lblAlpha = (TextView) findViewById(R.id.lblAlpha);
        lblTempTrg = (TextView) findViewById(R.id.lblTempTrg);
        mImageView = (TextureView) findViewById(R.id.image_view);
        mThermalView2 = (ThermalView2) findViewById(R.id.ThermalView2);

        InitSeekbars();
        InitSelectOrient();
        ShowLabels();

        i("CAMERA");
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            String[] cameraList = mCameraManager.getCameraIdList()          ;// Получение списка камер с устройства
            myCameras = new CameraHelper[cameraList.length]                 ;//создаем место для наших камер

            for(String cameraID : cameraList){  // создаем обработчики для нашых камер и выводим информацию по камере
                i("cameraID: " + cameraID);
                int id = Integer.parseInt(cameraID);
                myCameras[id] = new CameraHelper(mCameraManager, cameraID)  ;// создаем обработчик для камеры
                myCameras[id].viewFormatSize(ImageFormat.JPEG)              ;// выводим инормацию по камере
                myCameras[id].setTextureView(mImageView)                    ;// устанавливаем текстуру для отображения
            }
        } catch(CameraAccessException e){
            e.printStackTrace();
        }
    }
        //===================================================================
    // listens for touch events sent to the GestureDetector
    GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener()
    {
        // called when the user double taps the screen
        @Override
        public boolean onSingleTapUp(MotionEvent e){
            IxTrg = (mThermalView2 != null) ? mThermalView2.SingleTapUp(e) : -1;
            if(IxTrg >= 0 && AmgRegs != null && lblTempTrg != null){
                TempTrg = AmgRegs.GetTempTrg(IxTrg);
                lblTempTrg.setText(""+TempTrg);
            }

            return true ;}
    };
    //----------------------------------------------------------------------
    // called when the user touches the screen in this Activity
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // get int representing the type of action which caused this event
        int action = event.getAction();

        // the user user touched the screen or dragged along the screen
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_MOVE)
        {
//            mGameView.OnMove(event); // align the cannon
        } // end if

        // call the GestureDetector's onTouchEvent method
        return gestureDetector.onTouchEvent(event);
    } // end method onTouchEvent
    //===================================================================
    private void InitSeekbars(){
        if(sbMax   != null){ sbMax.setMax(180)   ; sbMax.setProgress((int) (TempMax + 55))   ;}
        if(sbMin   != null){ sbMin.setMax(180)   ; sbMin.setProgress((int) (TempMin + 55))   ;}
        if(sbAlpha != null){ sbAlpha.setMax(100) ; sbAlpha.setProgress((int) (Alpha))        ;}
        if(sbMax != null){
            sbMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b){
                    if(sbMin != null && sbMin.getProgress() > sbMax.getProgress()-1)
                        sbMin.setProgress(sbMax.getProgress()-1)              ;
                    ShowLabels()  ;
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar){}
                @Override public void onStopTrackingTouch(SeekBar seekBar){}
            });
        }
        if(sbMin != null){
            sbMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b){
                    if(sbMax != null && sbMin.getProgress() > sbMax.getProgress()-1)
                        sbMax.setProgress(sbMin.getProgress()+1)            ;
                    ShowLabels()  ;
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar){}
                @Override public void onStopTrackingTouch(SeekBar seekBar){}
            });
        }
        if(sbAlpha != null){
            sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b){
                    if(mThermalView2 != null)
                        mThermalView2.setAlpha(sbAlpha.getProgress()/100f)  ;
                    ShowLabels()  ;
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar){}
                @Override public void onStopTrackingTouch(SeekBar seekBar){}
            });
        }
    }
    //===================================================================
    private void ShowLabels(){
        if(lblMax != null && sbMax != null){
            TempMax = sbMax.getProgress() - 55   ; lblMax.setText(""+TempMax)           ;}
        if(lblMin != null && sbMin != null){
            TempMin = sbMin.getProgress() - 55   ; lblMin.setText(""+TempMin)           ;}
        if(lblAlpha != null && sbAlpha != null){
            Alpha = sbAlpha.getProgress()        ; lblAlpha.setText(""+Alpha)           ;}
    }
    //===================================================================
    public void onBtnClick(View v){
        switch(v.getId()){
        case R.id.btnCam:
            if(myCameras == null || myCameras.length<1) break   ;

            if (myCameras[0] != null && !myCameras[0].isOpen()){
                if(myCameras[1] != null && myCameras[1].isOpen())
                    myCameras[1].closeCamera();
                OpenCamera(0);
            }
            else{
                if (myCameras[0] != null && myCameras[0].isOpen())
                    myCameras[0].closeCamera();
            }
            break   ;
        }
    }
    //===================================================================
    String[] data = {"0", "1", "2", "3", "4", "5"};
    private void InitSelectOrient(){
        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        spinner.setPrompt("Ориентация");// заголовок
        spinner.setSelection(2);// выделяем элемент
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,int position, long id) {
                // показываем позиция нажатого элемента
                Toast.makeText(getBaseContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }
    //===================================================================
    private void OpenCamera(int idCam){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        try {
            mCameraManager.openCamera(myCameras[idCam].mCameraID,myCameras[idCam].GetCameraCallback(),null);
        } catch (CameraAccessException e) {
            e(e.getMessage());
        }
    }
    //===================================================================
    private void requestCameraPermission() {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }
    //===================================================================
    @Override
    public void onClick(View view){
    }
    //===================================================================
    protected void ParsePack(byte[] BuffPack,int LenPack) {
        if(LenPack < 1000){
            TPacket Packet = new TPacket(BuffPack, LenPack);
            if(Packet.isValid){
                if(AmgRegs == null) AmgRegs = new TAmgRegs()                    ;
                if(AmgRegs != null) AmgRegs.Set(Packet.bData,TempMin,TempMax)   ;
//                AmgRegs = new TAmgRegs(Packet.bData,TempMin,TempMax)          ;
                if(mThermalView2 != null) mThermalView2.Work(AmgRegs,Orient)      ;
                if(IxTrg >= 0 && AmgRegs != null && lblTempTrg != null){
                    TempTrg = AmgRegs.GetTempTrg(IxTrg)                         ;
                    lblTempTrg.setText(String.format("%3.1f°C",TempTrg))        ;
                }
            }
        }
    }
    //===================================================================
    private static final int	LenBFpack = 1000			;
    private static byte[]			BuffPack  = new byte[LenBFpack]	;
    private static byte[]			BuffPack2 = new byte[LenBFpack]	;
    private static int				IxPack=0,LenPack=0,SignPack=0,FrameIx,PrevFrameIx=0	;
    //===================================================================
    protected static void ParseRawData(byte readBuf[]){
        int		ix,Count = readBuf.length			;

        for(ix=0;ix<Count;ix++){
            if(IxPack==0){
                BuffPack[0] = BuffPack[1] ; BuffPack[1] = BuffPack[2] ; BuffPack[2] = readBuf[ix] ;
                // Ищем сигнатуру пакета "OSC"
                if((BuffPack[0] == 'O') && (BuffPack[1] == 'S') && (BuffPack[2] == 'C')){
                    IxPack = 3; LenPack = 0   ;
                }
                // Ищем сигнатуру пакета "TIR"
                else if((BuffPack[0] == 'T') && (BuffPack[1] == 'I') && (BuffPack[2] == 'R')){
                    IxPack = 3; LenPack = 0   ;
                }
            }
            else if(IxPack < 6){
                BuffPack[IxPack++] = readBuf[ix]	;
                if(IxPack == 6){
                    ByteBuffer bbBuff = ByteBuffer.wrap(BuffPack)      ;
                    bbBuff.order(ByteOrder.LITTLE_ENDIAN)                 ;
//                      shBuff  = bbBuff.asShortBuffer()		                ;
//                      LenPack = shBuff.get(2);
                    LenPack = bbBuff.getShort(4)  ;
                    if(LenPack >= LenBFpack || LenPack <= 0){
                        SignPack = 0	; IxPack = 0	; LenPack = 0	;
                    }
                }
            }
            else if(IxPack < LenPack){
                BuffPack[IxPack++] = readBuf[ix]	;
                if(LenPack > 0 && IxPack >= LenPack){
                    BuffPack2 = BuffPack.clone()	;
                    if(MainActivity.GetInstance() != null) GetInstance().ParsePack(BuffPack2,LenPack) ;
//                    mHandler.obtainMessage(MESSAGE_READ_PACK,LenPack, -1,BuffPack2).sendToTarget();
                    SignPack = 0	; IxPack = 0	; LenPack = 0	;
                    BuffPack[0] = BuffPack[1] = BuffPack[2] = 0   ;
                }
            }
        }

    }
    //===================================================================

//TODO  USB
    //===================================================================
    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:   // !!!!!!!!!!!!!!!!!!!!!!!!!!!TODO TODO
//                String data = (String) msg.obj;
//                mActivity.get().display.append(data);
                break;
            case UsbService.RAWDATA_FROM_SERIAL_PORT:
                ParseRawData((byte[])msg.obj)   ;
                break   ;
            case UsbService.CTS_CHANGE:
                Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                break;
            case UsbService.DSR_CHANGE:
                Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
    //===================================================================
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    //===================================================================
    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }
    //===================================================================
    //===================================================================
    //===================================================================
    //===================================================================
//TODO  onStart, onStop etc
    //===================================================================
    @Override
    public void onStart() {
        super.onStart();
        e("++ ON START ++");
//        OnStartDriverBT();
    }
    //===================================================================
    @Override
    public synchronized void onResume() {
        e("+ ON RESUME +")          ;
        super.onResume()            ;

//        mCamera = Camera.open()     ;

        mThermalView2.onResume()     ;
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }
    //===================================================================
    @Override
    public synchronized void onPause() {
        super.onPause();
        mThermalView2.onPause()  ;
        e("-* ON PAUSE *-");

        sPref = getPreferences(MODE_PRIVATE)		    ;
        SharedPreferences.Editor ed = sPref.edit()	    ;
        ed.putFloat("TEMP_MAX",TempMax)                 ; ed.apply()   ;
        ed.putFloat("TEMP_MIN",TempMin)                 ; ed.apply()   ;
        ed.putFloat("ALPHA"   ,Alpha  )                 ; ed.apply()   ;
        ed.putInt  ("ORIENT"  ,Orient )                 ; ed.apply()   ;
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }
    //===================================================================
    @Override
    public void onStop() {
        super.onStop();
        e("-- ON STOP --");
    }
    //===================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        e("-- ON DESTROY --");
    }
    //===================================================================
    //===================================================================
    //===================================================================
    public static String IntToHex(long val,int cnt){
        String result = ""  ;
        return result    ;}
    //===================================================================
    public static int StrToInt(String str) {
        int val 	;
        try{val = Integer.parseInt(str)	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    public static float StrToFloat(Editable str) {
        float val 	;
        try{val = Float.parseFloat(String.valueOf(str))	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    public int StrToInt(CharSequence text){ return StrToInt("" + text);}
    //===================================================================
    public int StrToHex(String str) {
        int val = 0	;
        try{val = Integer.parseInt(str,16)	;
        } catch (NumberFormatException e) { val = 0	;}
        return val;}
    //===================================================================
    private void e(String msg){Log.e(TAG,">=< " + msg + " >=<"); }
    //----------------------------------------------------------------------
    private void l(String msg){Log.d(TAG, ">==< "+msg+" >==<"); }
    //----------------------------------------------------------------------
    private void i(String msg){Log.i(TAG, ">===< "+msg+" >===<"); }
    //----------------------------------------------------------------------
}
