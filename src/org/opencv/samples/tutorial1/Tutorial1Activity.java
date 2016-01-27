package org.opencv.samples.tutorial1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import org.opencv.samples.tutorial1.ConnectedThread;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

@SuppressLint("HandlerLeak") public class Tutorial1Activity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private static final String TAGB = "OPCBluetooth";
    //addictive
    long lastTime;//=System.currentTimeMillis();// время в ms
    long RelaxTime=1300; // in ms
    String transmitCommand="4";
    
    //ПОВОРОТНЫЙ КОД
    SensorManager sensorManager;
    Sensor sensorOrientation;
    boolean finishedLastRotation=true;
    
    int lastFig;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
        
    // из tutorial2
    private Mat                  mRgba;
    private Mat                  mIntermediateMat;
    private Mat					 mGray;
    private Mat 				 mHsvMat;
    private Mat 				 mMask;
    private Mat 				 mDilatedMask;
    
    long prevTotal = 0;
    long prevPTotal = 0;
    long prevPPTotal = 0;
    long prevPPPTotal = 0;
    
    boolean flagFind0 = true; // 3
    boolean flagFind1 = true; // 4
    boolean flagFind2 = true; // 6 
    boolean flagFind3 = true; // 10 угол
    
        
    private Scalar               CONTOUR_COLOR;
    Mat hierarchy;
    List<MatOfPoint> 			 contours;
    
    Handler h;
    
    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;		// Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    
    private ConnectedThread mConnectedThread;
     
    // SPP UUID сервиса 
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
   
    // MAC-адрес Bluetooth модуля
    private static String address = "00:11:11:28:06:02";
    //private static String address = "00:12:04:06:91:15";
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);
// Код для работы с Open CV
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);        

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
//Повороты инициализация
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(listenerOrientation, sensorOrientation, sensorManager.SENSOR_DELAY_FASTEST);
        lastTime=System.currentTimeMillis();
        
// Код Bluetooth
        h = new Handler() {
        	public void handleMessage(android.os.Message msg) {
        		switch (msg.what) {
                case RECIEVE_MESSAGE:													// если приняли сообщение в Handler
                	byte[] readBuf = (byte[]) msg.obj;
                	String strIncom = new String(readBuf, 0, msg.arg1);
                	sb.append(strIncom);												// формируем строку
                	int endOfLineIndex = sb.indexOf("\r\n");							// определяем символы конца строки
                	if (endOfLineIndex > 0) { 											// если встречаем конец строки,
                		String sbprint = sb.substring(0, endOfLineIndex);				// то извлекаем строку
                        sb.delete(0, sb.length());										// и очищаем sb
                    	// Ответ от Ардуино
                        Core.putText(mRgba, sbprint, new Point(200, 300), 3, 1, new Scalar(0,0,0));
                    	Log.i("Reports", "Report from Arduino" + sbprint);   
                    	//Toast toast = Toast.makeText(getApplicationContext(), "Ответ -" + sbprint , Toast.LENGTH_LONG);
                        //toast.show();
                    }
                	//Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                	break;
        		}
            };
    	};
         
        btAdapter = BluetoothAdapter.getDefaultAdapter();		// получаем локальный Bluetooth адаптер
        //checkBTState();     
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
     // Bluetooth
        Log.d(TAGB, "...In onPause()...");
        
        try     {
          btSocket.close();
        } catch (IOException e2) {
          errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }        
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    // Bluetooth
        Log.d(TAGB, "...onResume - попытка соединения...");
        
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
       
        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
          btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
          errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
       
        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();
       
        // Establish the connection.  This will block until it connects.
        Log.d(TAGB, "...Соединяемся...");
        try {
          btSocket.connect();
          Log.d(TAGB, "...Соединение установлено и готово к передачи данных...");
        } catch (IOException e) {
          try {
            btSocket.close();
          } catch (IOException e2) {
            errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
          }
        }
         
        // Create a data stream so we can talk to server.
        Log.d(TAGB, "...Создание Socket...");
       
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;
                        
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    	
    	mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        hierarchy = new Mat();
        mHsvMat = new Mat();
        mMask = new Mat();
        mDilatedMask = new Mat();
        //CONTOUR_COLOR = new Scalar(255,0,0,255);
               
    }

    public void onCameraViewStopped() {
    	
    	mRgba.release();        
        mIntermediateMat.release();
        mGray.release();
        hierarchy.release();
        mHsvMat.release();
        mMask.release();
        mDilatedMask.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	 	
    	//mConnectedThread.write("3");
    	//Toast toast;
    	double tgAlpha = 0;
    	double alpha = 0;
    	    	
    	MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
    	
    	mRgba = inputFrame.rgba();
    	// Получаем черно-белое изображение
    	mGray = inputFrame.gray();
    	contours = new ArrayList<MatOfPoint>();
    	
    	hierarchy = new Mat();
    	// Поиск контуров на изображении    	
        //Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
        // Применение фильтра гауса, для сглаживания и избавление от шумов
        //Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new org.opencv.core.Size(5, 5), 5);
        // Поиск связанных контуров
    	Imgproc.cvtColor(mRgba, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
    	
    	Core.inRange(mHsvMat, new Scalar(128,88,189,0), new Scalar(178,188,289,255), mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());
    	
        Imgproc.findContours(mDilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
        
        hierarchy.release();        
        
        double maxArea = -1;
        int maxAreaIdx = -1;
        Log.d("size",Integer.toString(contours.size()));
        
        if (contours.size() != 0) {
        	MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
	        MatOfPoint2f approxCurve = new MatOfPoint2f();
	        MatOfPoint largest_contour = contours.get(0);
	        //
	        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();	        
	
	        for (int idx = 0; idx < contours.size(); idx++) {
	            temp_contour = contours.get(idx);
	            double contourarea = Imgproc.contourArea(temp_contour);
	            //
	            if ((contourarea > maxArea) && (contourarea > 2000)) {
	                //
	                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
	                int contourSize = (int)temp_contour.total();
	                
	                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize*0.05, true);
	                
	                if ((approxCurve_temp.total() == 3)) {
	                	
	                    maxArea = contourarea;
	                    maxAreaIdx = idx;
	                    approxCurve=approxCurve_temp;
	                    largest_contour = temp_contour;
	                    largest_contours.add(largest_contour);
	                    Imgproc.drawContours(mRgba, largest_contours, -1, new Scalar(255, 0, 0), -1);	                                 
               
	                 // bluetooth message
	                    transmitCommand="2";
	                    /*if (timeGone()) {//flagFind0) {
	                    	//mConnectedThread.write("4"); // stop
	                    	
	                    	//flagFind0 = false;   	                    	
							   
	                    }*/
	                    	                    
	                    Log.d("typeFig", "Ответ - Триугольник");
	                    prevTotal = approxCurve_temp.total();
	                    
	                }
	                else if ((approxCurve_temp.total() == 4)) {
	                	maxArea = contourarea;
	                    maxAreaIdx = idx;
	                    approxCurve=approxCurve_temp;
	                    largest_contour = temp_contour;
	                    largest_contours.add(largest_contour);	                    
	                    //Imgproc.drawContours(mRgba, largest_contours, -1, new Scalar(255, 255, 0));
	                    Imgproc.drawContours(mRgba, largest_contours, -1, new Scalar(0, 255, 255), -1);
	                    // bluetooth message;
	                    
	                    //turnLeft(90);
	                    transmitCommand="4"; // forward
	                    //if (timeGone()){
	                    	
	                    	//flagFind1 = false;
	                   // } 
	                    
	                    Log.d("typeFig", "Ответ - Прямоуольник");
	                    prevTotal = approxCurve_temp.total();
	                }
	                else if ((approxCurve_temp.total() == 5)) {
	                	maxArea = contourarea;
	                    maxAreaIdx = idx;
	                    approxCurve=approxCurve_temp;
	                    largest_contour = temp_contour;
	                    largest_contours.add(largest_contour);	                    
	                    //Imgproc.drawContours(mRgba, largest_contours, -1, new Scalar(255, 255, 0));
	                    Imgproc.drawContours(mRgba, largest_contours, -1, new Scalar(0, 255, 0), -1);
	                    // bluetooth message;
	                    
	                    turnLeft(90);
	                    //transmitCommand="4"; // forward
	                    //if (timeGone()){	                    	
	                    	//flagFind1 = false;
	                    //} 
	                    
	                    Log.d("typeFig", "Ответ - Пятиуольник");
	                    prevTotal = approxCurve_temp.total();
	                }
	            }
	        }
        } 
        prevTotal = approxCurve_temp.total(); 
        prevPTotal= prevTotal;
        prevPPTotal = prevPTotal;
        prevPPPTotal = prevPPTotal;
        timeGone();
        return mRgba;
        //return mDilatedMask;
    }

    public void flagToZero()
    {
        flagFind0 = true; // 3
        flagFind1 = true; // 4
        flagFind2 = true; // 6 
        flagFind3 = true; // 10
    	
    }
    
    public void timeGone() //boolean timeGone()
    {
    	//Core.putText(mRgba, transmitCommand, new Point(200, 300), 3, 1, new Scalar(0,0,0));
    	long followTime=System.currentTimeMillis();
    	if((followTime-lastTime)>RelaxTime)
    	{
    		lastTime=followTime;
    		mConnectedThread.write(transmitCommand);
    		Core.putText(mRgba, transmitCommand, new Point(200, 300), 3, 1, new Scalar(0,0,255));
    		//return true;
    	}
    	/*else
    	{
    		//return false;
    	}*/
    }
// ------------------------- bluetooth -----------------------------------
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) { 
          errorExit("Fatal Error", "Bluetooth не поддерживается");
        } else {
          if (btAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth включен...");
          } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
          }
        }
      }
     
      private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
      }
      
           
      
      
      
      
      
      
    //Операции по повороту
  	float initAngle;
  	boolean firstMeasure=false;
  	boolean turnPermission=false;
  	float targetAngle=0;
  	float finishAngle=0;
  	int direction=0; //0 - left, 1 - right;
     
  	//ПОВОРОТЫ
      SensorEventListener listenerOrientation = new SensorEventListener() {
		  @Override
		  public void onAccuracyChanged(Sensor sensor, int accuracy) {	  
		  }
		  @Override
		  public void onSensorChanged(SensorEvent event)
		  {
			  
			  float followAngle=event.values[0];
			  //Core.putText(mRgba, Float.toString(followAngle), new Point(100, 300), 3, 1, new Scalar(0,0,0));
			  if(firstMeasure)
			  {
				  finishedLastRotation=false;
				  initAngle = followAngle;
				  if(direction==0) //left
				  {
					  finishAngle=initAngle-targetAngle;
					  if(finishAngle<0)
					  {						  
						  finishAngle=360+finishAngle;
					  }
				  }
				  else if(direction==1) //right
				  {
					  finishAngle=initAngle+targetAngle;
				  }
				  finishAngle=finishAngle%360;
				  firstMeasure=false;
			  }
			  			  
			  if(turnPermission)
			  { 
				  transmitCommand = "5";
				  
				  //тестовая не трогать bluetoothSend("turnLeft");				  
				  if(angleEqual(followAngle,finishAngle))	//that's All. Now stop.
				  {
					  turnPermission=false;
					  transmitCommand="4";
					  finishedLastRotation=true;
					  
				  }
				  /*else
				  {
					  if(direction==0)//left
					  {
						  transmitCommand="5";//bluetoothSend("turnLeft");
					  }
					  else//right
					  {
						  transmitCommand="6";//bluetoothSend("turnRight");
					  }
				  }	*/			 
			  }
//!!!!!ДОБАВИЛ ДЛЯ БОЛЬШЕЙ ЧАСТОТЫ ВЫЗОВА
			  //timeGone();
		  }		  
	  };
  
	//РЕАЛИЗАЦИЯ ПОВОРОТОВ
	  public void turnLeft(float angle) 
	  {			  
		  
		  if(finishedLastRotation)
		  {
			  targetAngle = angle;
			  direction=0;			//left
			  firstMeasure=true;
			  turnPermission=true;
		  }
	  }
	  
	  public void turnRight(float angle)
	  {		  
		  if(finishedLastRotation)
		  {
			  targetAngle = angle;
			  direction=1;			//right
			  firstMeasure=true;
			  turnPermission=true;
		  }
	  }
	  
	  public boolean angleEqual(float Angle1, float Angle2) {
		  boolean result=false;
		  if(((Angle1+5)>Angle2)&&((Angle1-5)<Angle2))
			  result=true;
		  return result;
	  }
}





