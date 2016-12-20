package com.railbot.usrc.robotcontrol;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    String TAG = "RobotControl";
    static {
        System.loadLibrary("native-lib");
    }

    TextView tv;

    //static public String serverAddr = "1"

    private class CheckStatusTask extends AsyncTask<Object, Object, Boolean> {
        protected Boolean doInBackground(Object... arg0) {
            Log.e(TAG, "sending ...");
            boolean flag = Send();
            return flag;
        }

        protected void onPostExecute(Boolean flag) {
            // use your flag here to check true/false.
            Log.e(TAG, "sent");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("");

        Intent intent = new Intent(this, VideoActivity.class);
        this.startActivity(intent);

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public void sendMessage(View view) {
        //tv.setText("how are you");

        new CheckStatusTask().execute();


    }

    boolean Send() {
        try {
            String host = "172.24.1.1";
            int port = 8081; //Random Port

            byte[] message = "LAWL,LAWL,LAWL".getBytes();

            // Get the internet address of the specified host
            InetAddress address = InetAddress.getByName(host);

            // Initialize a datagram packet with data and address
            DatagramPacket packet = new DatagramPacket(message, message.length,
                    address, port);

            // Create a datagram socket, send the packet through it, close it.
            DatagramSocket dsocket = new DatagramSocket();
            dsocket.send(packet);
            dsocket.close();
            System.out.println("Sent");
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }

        return  true;
    }



    public native String stringFromJNI();
}
