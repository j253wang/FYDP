package org.opencv.samples.colorblobdetect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class ColorBlobDetectionActivity extends Activity implements CvCameraViewListener2,  LocationListener, SensorEventListener{ //TODO
    /* * * General * * */
	private static final String  TAG              = "--FYDPSample::Activity--";
	Long tsLong = System.currentTimeMillis()/1000;
    
    /* * * USB Connection * * */
	//The android will send over a LOCATION TO GO TO. Arduino will figure the rest out
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpointIntr;
    private UsbInterface intf;
    private PendingIntent mPermissionIntent;
    private boolean forceClaim = true;
    
    private static final int VID = 0x2341;
	private static final int PID = 0x0000;//I believe it is 0x0000 for the Arduino Megas
	private static UsbController sUsbController;
    
	private final IUsbConnectionHandler mConnectionHandler = new IUsbConnectionHandler() {
		@Override
		public void onUsbStopped() {
			Log.e(TAG, "Usb stopped!");
		}
		
		@Override
		public void onErrorLooperRunningAlready() {
			Log.e(TAG, "Looper already running!");
		}
		
		@Override
		public void onDeviceNotFound() {
			if(sUsbController != null){
				sUsbController.stop();
				sUsbController = null;
			}
		}
	};
    /* * * Sensors * * */
	private static SensorManager mySensorManager;
	private boolean sensorrunning;
	
	public double gyr_x, gyr_y, gyr_z, acc_x, acc_y, acc_z, compass_direction;
	
	private SensorEventListener mySensorEventListener = new SensorEventListener(){
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) { }
		public void onSensorChanged(SensorEvent event) {
			Sensor snsr = event.sensor;
			if (snsr.getType() == Sensor.TYPE_ACCELEROMETER){
				float X = (float)event.values[0];
				float Y = (float)event.values[1];
				float Z = (float)event.values[2];
			} else if (snsr.getType() == Sensor.TYPE_GYROSCOPE) {
				float X = (float)event.values[0];
				float Y = (float)event.values[1];
				float Z = (float)event.values[2];
			} else if (snsr.getType() == Sensor.TYPE_ORIENTATION) {
				float compass_direction = (float)event.values[0];
			}
		}
	};
	public Sensor gyroscope, accelerometer, compass; 
    
	/* * * Location Manager * * */
	private TextView latituteField;
	private TextView longitudeField;
	private LocationManager locationManager;
	private String provider;
	
	public double curr_x;
    public double curr_y;
    public double curr_heading;
	
    /* * * Vision * * */
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    /* * * SMS * * */
    public SmsListener mySMSListener;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //TODO mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        //SMS
        //IntentFilter filter2 = new IntentFilter(TELEPHONY_SERVICE);
        //registerReceiver(mySMSListener, filter2);
        
        if(sUsbController == null){
	        sUsbController = new UsbController(this, mConnectionHandler, VID, PID);
        }
        
        //Compass
	    mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	    compass = mySensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); //Compass (or getSensorList)
	    gyroscope = mySensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); //Gyroscope
	    accelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //Accel
	    
	    mySensorManager.registerListener(mySensorEventListener, compass, SensorManager.SENSOR_DELAY_NORMAL);
	    mySensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
	    mySensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	    sensorrunning = true;
        
	    // Get the location manager
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 3000, 10, this);
	    // Define the criteria how to select the location provider -> use
	    // default
	    if(sUsbController == null)
			sUsbController = new UsbController(ColorBlobDetectionActivity.this, mConnectionHandler, VID, PID);
		else{
			sUsbController.stop();
			sUsbController = new UsbController(ColorBlobDetectionActivity.this, mConnectionHandler, VID, PID);
		}
	    /*Criteria criteria = new Criteria();
	    provider = LocationManager.GPS_PROVIDER;//locationManager.getBestProvider(criteria, false);
	    Location location = locationManager.getLastKnownLocation(provider);
	    
	    // Initialize the location fields
	    if (location != null) {
	      System.out.println("Provider " + provider + " has been selected.");
	      onLocationChanged(location);
	    }*/
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        //locationManager.requestLocationUpdates(provider, 400, 1, this); TODO
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector(); //I don't know if I need this
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(118, 118, 118, 255);
        mBlobColorHsv = new Scalar(0.0, 0.0, 188);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        
        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            
            double total_x  = 0;
            double total_y = 0;
            int total_points = 0;
            
            for (int a = 0; a<contours.size(); a++){
            	Point[] points = contours.get(a).toArray();
            	for (int b = 0; b<points.length; b++){
            		total_points ++;
            		total_x += points[b].x;
            		total_y += points[b].y;
            	}
            }
            curr_x = total_x / total_points;
            curr_y = total_y / total_points;
            
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }
    
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }
	public void onSensorChanged(SensorEvent event) {
		Sensor snsr = event.sensor;
		if (snsr.getType() == Sensor.TYPE_ACCELEROMETER){
			gyr_x = (double)event.values[0];
			gyr_y = (double)event.values[1];
			gyr_z = (double)event.values[2];
		} else if (snsr.getType() == Sensor.TYPE_GYROSCOPE) {
			acc_x = (double)event.values[0];
			acc_y = (double)event.values[1];
			acc_z = (double)event.values[2];
		} else if (snsr.getType() == Sensor.TYPE_ORIENTATION) {
			compass_direction = (double)event.values[0];
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		int lat = (int) (location.getLatitude());
		int lng = (int) (location.getLongitude());
		latituteField.setText(String.valueOf(lat));
		longitudeField.setText(String.valueOf(lng));
	}
	
	//Compass
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
	}
	
	public class SmsListener extends BroadcastReceiver{ //TODO make a variable for this
	    private SharedPreferences preferences;

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.e(TAG, "Received a message");
	        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
	            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
	            SmsMessage[] msgs = null;
	            String msg_from = null;
	            if (bundle != null){
	                //---retrieve the SMS message received---
	                try{
	                    Object[] pdus = (Object[]) bundle.get("pdus");
	                    msgs = new SmsMessage[pdus.length];
	                    for(int i=0; i<msgs.length; i++){
	                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
	                        msg_from = msgs[i].getOriginatingAddress();
	                        String msgBody = msgs[i].getMessageBody();
	                    }
	                    //TODO parse this parts
	                    sendSMS(msg_from, "Received, command is: ");
	                }catch(Exception e){
	                	Log.d("Exception caught",e.getMessage());
	                }
	            }
	        }
	    }
	}
	private void sendSMS(String phoneNumber, String message)
    {        
        Log.v(TAG, "PhoneNumber" + phoneNumber);
        Log.v(TAG, "Message" + message);
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, ColorBlobDetectionActivity.class), 0);                
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, pi, null);        
    }    
	
	Vector<Double> X = new Vector<Double>(0);
	Vector<Double> Y = new Vector<Double>(0);
	double slope, intercept;
	
	void findTrend() { //Y = a + bX
		double sumX = 0, sumXX = 0, sumXY = 0, sumY = 0, sumYY = 0;
		for (int q = 0; q<X.size(); q++){
			sumX += X.get(q);
			sumXX += X.get(q)*X.get(q);
			sumXY += X.get(q)*Y.get(q);
			sumYY += Y.get(q)*Y.get(q);
			sumY += Y.get(q);
		}
		slope = (Y.size()*sumXY - sumX*sumY)/(Y.size()*sumXX - sumX*sumX);
		intercept = (sumY-slope*sumX)/Y.size();
	}
}

//http://mobiforge.com/design-development/sms-messaging-android
//http://stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library*****
//http://www.vogella.com/tutorials/AndroidLocationAPI/article.html#locationapi
//http://androidexample.com/GPS_Basic__-__Android_Example/index.php?view=article_discription&aid=68&aaid=93

//Issues: smslister not picking up the sms, cant put in locationlistener yet (might work in actual test)

//Try this instead for usb later on
//http://android.serverbox.ch/?p=549