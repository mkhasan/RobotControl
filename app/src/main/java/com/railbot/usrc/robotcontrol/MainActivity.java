package com.railbot.usrc.robotcontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // Used to load the 'native-lib' library on application startup.

    /*
        libyuv.so has to be created by checkig the target SDK otherwise it will not work
        I loaded library yuv here to test if it works well with our app.
        Actually we do not need to use yuv in the activity I just load it to check at the
        beginning of the app
     */


    static {
        System.loadLibrary("yuv");
        //System.loadLibrary("ffmpeg");
    }





    public static final String PREF_TITLE = "ServerInfo";

    public static final String SERVER_ADDR = "serverAddr";
    public static final String IMAGE_IP = "imageIP";
    public static final String THERMAL_IP = "thermalIP";
    public static final String RAIL_ROBOT_IP = "railRobotIP";
    public static final String VOIP_PORT = "voipPort";
    public static final String RAIL_ROBOT_PORT = "railRobotPort";

    public static final int SERVER_SETTING_RESULT = 1;

    public static final String EXTRA_IP = "com.railbot.usrc.robotcontrol.IP";
    public static final String EXTRA_PORT = "com.railbot.usrc.robotcontrol.Port";
    public static final String EXTRA_IMAGE_IP = "com.railbol.usrc.robotcontrol.image_ip";
    public static final String EXTRA_THERMAL_IP = "com.railbol.usrc.robotcontrol.thermal_ip";
    public static final String EXTRA_RAIL_ROBOT_IP = "com.railbol.usrc.robotcontrol.rail_robot_ip";
    public static final String EXTRA_RAIL_ROBOT_PORT = "com.railbol.usrc.robotcontrol.rail_robot_port";



    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;
    private boolean STARTED = false;
    private boolean IN_CALL = false;
    private boolean LISTEN = false;
    private static final String HELO_MSG = "HELO:";


    public static final int REQUEST_MICROPHONE = 1;
    public static final int CALL_RECEIVED = 2;
    public static final String CALL_RECEIVED_NOTFICATION="com.usrc.railbot.voicechat.NOTIFICATION";


    private static String serverIP;
    public static String imageIP;
    public static String thermalIP;
    public static String railRobotIP;
    private static int voipPort;
    public static int railRobotPort;

    private ConnTask connTask = null;

    String TAG = MainActivity.class.getCanonicalName();

    TextView tv;

    //static public String serverAddr = "1"

    static public String url = "rtsp://admin:admin@192.168.0.101:554/stream1";      // choose stream0 or stream1 depending to the required stream resolution.
    //static public String url = "rtsp://admin:admin@192.168.1.100:554/12";

    private ItemsAdapter adapter;
    TextView textView = null;

    SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREF_TITLE, MODE_PRIVATE);

        serverIP = preferences.getString(SERVER_ADDR, "210.107.139.87");
        imageIP = preferences.getString(IMAGE_IP, "192.168.0.101");
        thermalIP = preferences.getString(THERMAL_IP, "192.168.0.100");
        railRobotIP = preferences.getString(RAIL_ROBOT_IP, "192.168.0.254");

        voipPort = preferences.getInt(VOIP_PORT, 1049);
        railRobotPort = preferences.getInt(RAIL_ROBOT_PORT, 8081);


        //railRobotPort = preferences.getInt(RAIL_ROBOT_PORT, 8899);




        // Example of a call to a native method
        final ListView listView = (ListView) findViewById(R.id.main_list);
        textView = (TextView) findViewById(R.id.select);


        adapter = new ItemsAdapter(LayoutInflater.from(this));
        adapter.swapItems(getItems());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);



        final int permission = PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if(permission == PermissionChecker.PERMISSION_GRANTED) {
            Log.e(TAG, "got permission");
        } else {
            Log.e(TAG, "did not get  permission");

            final String message = "Please turn on RECORD_AUDIO permission";
            new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Notification")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        startCallListener();

        /*
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
            Log.e(TAG, "looking for permission");

        }
        else
            Log.e(TAG, "did not get  permission");

            */



        connTask = new ConnTask();
        connTask.execute();
    }

    @NonNull
    private List<ListItem> getItems() {
        final List<ListItem> items = new ArrayList<ListItem>();
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.motion_only).toString(),
                null,
                true));

        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.image_camera).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.thermal_camera).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.voip_call).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.wifi_settings).toString(),
                null,
                true));

        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.settings).toString(),
                null,
                true));

        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.exit).toString(),
                null,
                true));
        return items;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
        final ListItem item = adapter.getItem(position);
    /*
    final Intent intent = new Intent(AppConstants.VIDEO_PLAY_ACTION)
            .putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL, videoItem.video())
            .putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_ENCRYPTION_KEY, videoItem.video());
    startActivity(intent);
    */


        String str = item.text();

        String exitStr = getResources().getText(R.string.exit).toString();
        Log.e(TAG, "Str is "+str);
        if (str.equals(exitStr)) {

            Log.e(TAG, "Str is X"+str);
            finish();
        }
        // com.usrc.railbot.voicechat
        else if (str.equals(getResources().getText(R.string.wifi_settings))) {

            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        else if (str.equals(getResources().getText(R.string.settings))) {
            Intent intent = new Intent(this, ServerSettingsActivity.class);
            intent.putExtra(EXTRA_IP, serverIP);
            intent.putExtra(EXTRA_PORT, voipPort);
            intent.putExtra(EXTRA_IMAGE_IP, imageIP);
            intent.putExtra(EXTRA_THERMAL_IP, thermalIP);
            intent.putExtra(EXTRA_RAIL_ROBOT_IP, railRobotIP);
            intent.putExtra(EXTRA_RAIL_ROBOT_PORT, railRobotPort);

            startActivityForResult(intent, SERVER_SETTING_RESULT);
        }
        else if(str.equals(getResources().getText(R.string.voip_call))) {

            IN_CALL = true;
            Intent intent = new Intent(MainActivity.this, MakeCallActivity.class);
            intent.putExtra(EXTRA_IP, serverIP);
            intent.putExtra(EXTRA_PORT, Integer.toString(voipPort));
            startActivity(intent);


        }
        else {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra("choice", str);
            this.startActivityForResult(intent, 0);
        }



        // text_view.setText(str);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {



        Log.e(TAG, "Req code is " + requestCode);
        if (requestCode == 0) {
            if (resultCode == 0) {
                // A contact was picked.  Here we will just display it
                // to the user.
                //Log.e(TAG, "10");
            }
            //else
              //  Log.e(TAG, "100");
        }

        else if(requestCode == SERVER_SETTING_RESULT) {

            if(resultCode == RESULT_OK) {

                serverIP = data.getStringExtra(SERVER_ADDR);
                imageIP = data.getStringExtra(IMAGE_IP);
                thermalIP = data.getStringExtra(THERMAL_IP);
                railRobotIP = data.getStringExtra(RAIL_ROBOT_IP);
                voipPort = Integer.parseInt(data.getStringExtra(VOIP_PORT));
                railRobotPort = Integer.parseInt(data.getStringExtra(RAIL_ROBOT_PORT));


                Log.e(TAG, "Server ip " + serverIP + " port " + voipPort);

                preferences = getSharedPreferences(PREF_TITLE, MODE_PRIVATE);
                SharedPreferences.Editor edit= preferences.edit();
                edit.putString(SERVER_ADDR, serverIP);
                edit.putString(IMAGE_IP, imageIP);
                edit.putString(THERMAL_IP, thermalIP);
                edit.putString(RAIL_ROBOT_IP, railRobotIP);
                edit.putInt(VOIP_PORT, voipPort);
                edit.putInt(RAIL_ROBOT_PORT, railRobotPort);
                edit.commit();


            }

        }


    }

    private void startCallListener() {
        // Creates the listener thread
        LISTEN = true;
        Thread listener = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // Set up the socket and packet to receive
                    Log.i(TAG, "Incoming call listener started");
                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
                    socket.setSoTimeout(1000);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {
                        // Listen for incoming call requests
                        //Log.e(TAG, "Listening");
                        try {
                            Log.i(TAG, "Listening for incoming calls");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.e(TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("CAL:")) {
                                // Received a call request. Start the ReceiveCallActivity
                                String address = packet.getAddress().toString();
                                String name = data.substring(4, packet.getLength());

                                Intent intent = new Intent(MainActivity.this, ReceiveCallActivity.class);
                                intent.putExtra(EXTRA_IP, serverIP);
                                intent.putExtra(EXTRA_PORT, Integer.toString(voipPort));
                                IN_CALL = true;
                                //LISTEN = false;
                                //stopCallListener();
                                startActivity(intent);
                            }
                            else {
                                // Received an invalid request
                                Log.w(TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(Exception e) {

                        }
                    }
                    Log.i(TAG, "Call Listener ending");
                    socket.disconnect();
                    socket.close();
                }
                catch(SocketException e) {

                    Log.e(TAG, "SocketException in listener " + e);
                }
            }
        });
        listener.start();
    }

    private void stopCallListener() {
        // Ends the listener thread
        LISTEN = false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_MICROPHONE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.e(TAG, "permission granted");
                }
                else {
                    Log.e(TAG, "permission not granted");
                    finish();
                }
            }
        }


    }

    @Override
    public void onPause() {

        super.onPause();
        if(STARTED) {

            //STARTED = false;
        }
        stopCallListener();
        Log.i(TAG, "App paused!");

        if(connTask != null) {
            connTask.finished = true;
            connTask = null;
        }

    }

    @Override
    public void onStop() {

        super.onStop();
        Log.e(TAG, "App stopped!");
        stopCallListener();

    }

    @Override
    public void onRestart() {

        super.onRestart();
        Log.e(TAG, "App restarted!");
        IN_CALL = false;
        STARTED = true;

        startCallListener();
        connTask = new ConnTask();
        connTask.execute();
    }

    private class ConnTask extends AsyncTask<Void, Integer, Long> {
        final String message = MainActivity.HELO_MSG;
        public boolean finished = false;

        protected Long doInBackground(Void... voids) {

            long totalSize = 0;

            try {


                while(finished == false) {

                    InetAddress address = InetAddress.getByName(serverIP);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, LISTENER_PORT);
                    socket.send(packet);
                    Log.e(TAG, "Sent message( " + message + " ) to " + serverIP + ":" + LISTENER_PORT);
                    socket.disconnect();
                    socket.close();

                    try {
                        sleep(2000);
                    }
                    catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }



            }

            catch(UnknownHostException e) {

                Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + serverIP);
            }
            catch(SocketException e) {

                Log.e(TAG, "Failure. SocketException in sendMessage: " + e);
            }
            catch(IOException e) {

                Log.e(TAG, "Failure. IOException in sendMessage: " + e);
            }

            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {
            Log.e(TAG, "Conn finished");
        }
    }
}
