package com.varunmishra.ghome;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements SensorEventListener{
    public static int WINDOW_SIZE = 10;
    public static int WINDOW_COUNT = 30;
    public float lastTimeStamp = 0;
    public float heading = 0;
    public Float[] mWindow = new Float[WINDOW_SIZE];
    public ArrayList<Float[]> mBuffer = new ArrayList<Float[]>();
    public Float[] mWindowZ = new Float[WINDOW_SIZE];
    public ArrayList<Float[]> mBufferZ = new ArrayList<Float[]>();
    public Float[] mWindowY = new Float[WINDOW_SIZE];
    public ArrayList<Float[]> mBufferY = new ArrayList<Float[]>();

    public static String SERVER_ADDR = "https://noted-function-121118.appspot.com/";

    public int counter =0;
    private SensorManager mSensorManager;
    private Sensor mGravity;
    private Sensor mOrientation;
    private TextView mTextView;
    public boolean isExecuting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

            }
        });
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor currentSensor = event.sensor;

        if (currentSensor.getType() == Sensor.TYPE_GRAVITY) {
            if (!isExecuting) {
                mWindow[counter] = event.values[0];
                mWindowY[counter] = event.values[1];
                mWindowZ[counter] = event.values[2];

                //Log.d("TAGG", counter + "--" + mWindow.length);
                counter++;
                if (counter == WINDOW_SIZE) {
                    counter = 0;
                    mBuffer.add(mWindow);
                    mBufferZ.add(mWindowZ);
                    mWindow = new Float[WINDOW_SIZE];
                    mWindowZ = new Float[WINDOW_SIZE];
                    mBufferY.add(mWindowY);
                    mWindowY = new Float[WINDOW_SIZE];

                    if (mBuffer.size() == WINDOW_COUNT) {
                        isExecuting = true;
                        new LongOperation().execute();
//                    if (classifyValues()){
//                        mBuffer.clear();
//                        mWindow = new Float[WINDOW_SIZE];
//                    } else {
//                        mBuffer.remove(0);
//                    }
                    }

                }
            }

        }
        if (currentSensor.getType() == Sensor.TYPE_ORIENTATION) {
            heading = Math.round(event.values[0]);

            mTextView.setText("Heading: " + Float.toString(heading) + " degrees");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class LongOperation extends AsyncTask<Void, Void, Double[]> {

        @Override
        protected Double[] doInBackground(Void... params) {
            float[] floatArray = new float[WINDOW_COUNT*WINDOW_SIZE];
            int i = 0;

            for (Float[] f : mBuffer) {
                for (int j = 0; j<f.length; j++){
                    floatArray[i++] = f[j]; // Or whatever default you want.

                }
            }
            float[] floatArrayZ = new float[WINDOW_COUNT*WINDOW_SIZE];
            int k = 0;

            for (Float[] f : mBufferZ) {
                for (int j = 0; j<f.length; j++){
                    floatArrayZ[k++] = f[j]; // Or whatever default you want.

                }
            }
            float[] floatArrayY = new float[WINDOW_COUNT*WINDOW_SIZE];
            int l = 0;

            for (Float[] f : mBufferY) {
                for (int j = 0; j<f.length; j++){
                    floatArrayY[l++] = f[j]; // Or whatever default you want.

                }
            }
            DTW classifierX = new DTW(floatArray, GestureTemplate.TEMPLATE_BUFFER_X);
            DTW classifierZ = new DTW(floatArrayZ, GestureTemplate.TEMPLATE_BUFFER_Z);

            return new Double[]{classifierX.getDistance(), classifierZ.getDistance(), Double.valueOf(getMean(floatArray)), Double.valueOf(getMean(floatArrayY)), Double.valueOf(getMean(floatArrayZ))};
        }

        @Override
        protected void onPostExecute(Double[] distance) {
            Log.d("MEAN", distance[2] + "---" + distance[3] + "---" + distance[4]);
            Log.d("TAGG", ""+distance[0] + "---" + distance[1]);
            if (distance[0] < 200 && distance[1]<120) {
                if(distance[2]<0 && distance[3] >0.5 && distance[3]<6){
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(100);
                    Toast.makeText(getApplicationContext(), ""+heading,Toast.LENGTH_LONG).show();

                    Log.d("Direction", "up");
                    postData();
                }
//                if(distance[2]>0 && distance[3]<6){
//                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//                    // Vibrate for 500 milliseconds
//                    v.vibrate(500);
//                    Log.d("Direction","down");
//                }
                mBuffer.clear();
                mBufferZ.clear();
                mWindow = new Float[WINDOW_SIZE];
                mWindowZ = new Float[WINDOW_SIZE];
                mBufferY.clear();
                mWindowY = new Float[WINDOW_SIZE];

                lastTimeStamp = System.currentTimeMillis();
            } else {
                mBuffer.clear();
                mBufferZ.clear();
                mWindow = new Float[WINDOW_SIZE];
                mWindowZ = new Float[WINDOW_SIZE];
                mBufferY.clear();
                mWindowY = new Float[WINDOW_SIZE];

//  mBuffer.remove(0);
//                mBufferZ.remove(0);
            }
            isExecuting = false;

        }

    }

    public Boolean classifyValues() {
        float[] floatArray = new float[WINDOW_COUNT*WINDOW_SIZE];
        int i = 0;

        for (Float[] f : mBuffer) {
            for (int j = 0; j<f.length; j++){
                floatArray[i++] = f[j]; // Or whatever default you want.

            }
        }
        DTW classifier = new DTW(floatArray, GestureTemplate.TEMPLATE_BUFFER_X);
        double distance = classifier.getDistance();
        if (distance<150) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
        } else {
            return false;
        }
        return true;
    }
    public float getMean(float [] arr){
        float sum = 0;
        for (int i=0;i<arr.length;i++){
            sum = sum+arr[i];
        }
        return sum/arr.length;
    }
    public void postData(){
        new AsyncTask<Void, Void, String>() {

            @Override
            // Get history and upload it to the server.
            protected String doInBackground(Void... arg0) {


                // Upload the history of all entries using upload().
                String device = "";
                if (heading>0 && heading<180){
                     device = "Device1";
                }
                if (heading>180 && heading<360){
                    device = "Device2";
                }
                //TODO Check orientation for device
                String uploadState="";
                try {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("device", device);

                    ServerUtilities.post(SERVER_ADDR+"/add.do", params);
                } catch (IOException e1) {
                    uploadState = "Sync failed: " + e1.getCause();
                    Log.e("TAGG", "data posting error " + e1);
                }

                return uploadState;
            }

            @Override
            protected void onPostExecute(String errString) {
                String resultString;
                if(errString.equals("")) {
                    resultString =  " entry uploaded.";
                } else {
                    resultString = errString;
                }

                Toast.makeText(getApplicationContext(), resultString,
                        Toast.LENGTH_SHORT).show();

            }

        }.execute();
    }
}
