package edu.dartmouth.cs.gcmdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import cs.dartmouth.edu.gcmdemo.backend.registration.Registration;
import edu.dartmouth.cs.gcmdemo.data.Contact;

public class MainActivity extends Activity {
    public static String SERVER_ADDR = "https://noted-function-121118.appspot.com/";
    private static final int MSG_SHOW_TOAST = 1;
    static Activity thisActivity = null;

    private Button postButton;
    private UsbManager usbManager;
    private static UsbSerialDriver device;
    public TextView txtStatus;
    int commandSaid= 0;
    public final static String TAG = "Arduino";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;

        new GcmRegistrationAsyncTask(this).execute();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        txtStatus=(TextView)findViewById(R.id.txtStatus);
        getActionBar().hide();

    }
    public static void sendToArduino(String data){
        byte[] dataToSend = data.getBytes();
//send the color to the serial device
        if (device != null){
            try{
                device.write(dataToSend, 500);
                Message msg = new Message();
                msg.what = MSG_SHOW_TOAST;
                msg.obj = "Sent";
                messageHandler.sendMessage(msg);            }
            catch (IOException e){
                Log.e(TAG, "couldn't write bytes to serial device");
            }
        }
    }
    private static Handler messageHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MSG_SHOW_TOAST) {
                String message = (String)msg.obj;
                Toast.makeText(thisActivity, message , Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void postData(){
        new AsyncTask<Void, Void, String>() {

            @Override
            // Get history and upload it to the server.
            protected String doInBackground(Void... arg0) {


                // Upload the history of all entries using upload().
                String uploadState="";
                try {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("name", "varun");
                    params.put("addr", "address");
                    params.put("phone", "123123");

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    class GcmRegistrationAsyncTask extends AsyncTask<Void, Void, String> {
        private  Registration regService = null;
        private GoogleCloudMessaging gcm;
        private Context context;

        // TODO: change to your own sender ID to Google Developers Console project number, as per instructions above
        private static final String SENDER_ID = "838019724510";

        public GcmRegistrationAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (regService == null) {
                Registration.Builder builder = new Registration.Builder(AndroidHttp.newCompatibleTransport(),
                        new AndroidJsonFactory(), null)
                        // Need setRootUrl and setGoogleClientRequestInitializer only for local testing,
                        // otherwise they can be skipped
                        .setRootUrl(SERVER_ADDR+"/_ah/api/")
                        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                            @Override
                            public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                                    throws IOException {
                                abstractGoogleClientRequest.setDisableGZipContent(true);
                            }
                        });
                // end of optional local run code

                regService = builder.build();
            }

            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                String regId = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regId;

                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                regService.register(regId).execute();

            } catch (IOException ex) {
                ex.printStackTrace();
                msg = "Error: " + ex.getMessage();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            Logger.getLogger("REGISTRATION").log(Level.INFO, msg);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
//check if the device is already closed
        if (device != null) {
            try {
                device.close();
            } catch (IOException e) {
//we couldn't close the device, but there's nothing we can do about it!
            }
//remove the reference to the device
            device = null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

//get a USB to Serial device object
        device = UsbSerialProber.acquire(usbManager);
        if (device == null) {
//there is no device connected!
            txtStatus.setTextColor(Color.RED);
            txtStatus.setText("Disconnected");
            Log.d(TAG, "No USB serial device connected.");
        } else {
            try {
//open the device
                device.open();
//set the communication speed
                txtStatus.setTextColor(Color.GREEN);
                txtStatus.setText("Connected");

                device.setBaudRate(115200); //make sure this matches your device's setting!
                if(commandSaid==1)
                    sendToArduino("a");
                if(commandSaid==2)
                    sendToArduino("b");
                commandSaid=0;
            } catch (IOException err) {
                Log.e(TAG, "Error setting up USB device: " + err.getMessage(), err);
                try {
//something failed, so try closing the device
                    device.close();
                } catch (IOException err2) {
//couldn't close, but there's nothing more to do!
                }
                device = null;
                return;
            }
        }
    }
}
