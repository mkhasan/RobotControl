package com.railbot.usrc.robotcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

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
    }



    public static final String PREF_TITLE = "ServerInfo";

    public static final String SERVER_ADDR = "serverAddr";
    public static final String VOID_PORT = "voipPort";
    public static final int SERVER_SETTING_RESULT = 1;

    public static final String EXTRA_IP = "com.railbot.usrc.robotcontrol.IP";
    public static final String EXTRA_PORT = "com.railbot.usrc.robotcontrol.Port";


    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;
    private boolean STARTED = false;
    private boolean IN_CALL = false;
    private boolean LISTEN = false;
    private static final String HELO_MSG = "HELO:";


    public static final int REQUEST_MICROPHONE = 1;
    public static final int CALL_RECEIVED = 2;
    public static final String CALL_RECEIVED_NOTFICATION="com.usrc.railbot.voicechat.NOTIFICATION";


    private String serverIP;
    private int voipPort;

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

        serverIP = preferences.getString(SERVER_ADDR, "143.248.204.35");
        voipPort = preferences.getInt(VOID_PORT, 1049);



        // Example of a call to a native method
        final ListView listView = (ListView) findViewById(R.id.main_list);
        textView = (TextView) findViewById(R.id.select);


        adapter = new ItemsAdapter(LayoutInflater.from(this));
        adapter.swapItems(getItems());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);




        //VideoActivity.ShowAllertTest(this);

        //String str = stringFromJNI();

        //Log.e(TAG, stringFromJNI());


        /*
        filters[0] = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        text.setFilters(filters);
        */





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
                getResources().getText(R.string.wifi_settings).toString(),
                null,
                true));

        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.server_settings).toString(),
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
        Log.e(TAG, "Str is "+str.length());
        if (str.equals(exitStr)) {

            Log.e(TAG, "Str is "+str);
            finish();
        }
        // com.usrc.railbot.voicechat
        else if (str.equals(getResources().getText(R.string.wifi_settings))) {

            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        else if (str.equals(getResources().getText(R.string.server_settings))) {
            Intent intent = new Intent(this, ServerSettingsActivity.class);
            intent.putExtra(EXTRA_IP, serverIP);
            intent.putExtra(EXTRA_PORT, voipPort);
            startActivityForResult(intent, SERVER_SETTING_RESULT);
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

                serverIP = data.getStringExtra("server_ip");
                voipPort = Integer.parseInt(data.getStringExtra("voip_port"));

                Log.e(TAG, "Server ip " + serverIP + " port " + voipPort);

                preferences = getSharedPreferences(PREF_TITLE, MODE_PRIVATE);
                SharedPreferences.Editor edit= preferences.edit();
                edit.putString(SERVER_ADDR, serverIP);
                edit.putInt(VOID_PORT, voipPort);
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
                                intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
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



}
