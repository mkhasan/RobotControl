package com.railbot.usrc.robotcontrol;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ReceiveCallActivity extends Activity {

    private static final String TAG = "ReceiveCall";
    private static final int BROADCAST_PORT = 50002;
    private static final int BUF_SIZE = 1024;
    private String contactIp;
    private int port;
    private String contactName;
    private boolean LISTEN = true;
    private boolean IN_CALL = false;


    private Client client = null;

    private Ringtone ring = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_call);

        Log.e(TAG, "Started");

        Intent intent = getIntent();

        contactIp = intent.getStringExtra(MainActivity.EXTRA_IP);
        port = Integer.parseInt(intent.getStringExtra(MainActivity.EXTRA_PORT));

        final Button endButton = (Button) findViewById(R.id.buttonEndCall);
        endButton.setVisibility(View.INVISIBLE);


        startListener();

        // ACCEPT BUTTON
        Button acceptButton = (Button) findViewById(R.id.buttonAccept);
        acceptButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    // Accepting call. Send a notification and start the call
                    sendMessage("ACC:");
                    InetAddress address = InetAddress.getByName(contactIp);
                    Log.i(TAG, "Calling " + address.toString());
                    IN_CALL = true;
                    //call = new AudioCall(address);
                    //call.startCall();
                    // Hide the buttons as they're not longer required
                    new ConnTask().execute();
                    Button accept = (Button) findViewById(R.id.buttonAccept);
                    accept.setEnabled(false);

                    Button reject = (Button) findViewById(R.id.buttonReject);
                    reject.setEnabled(false);

                    endButton.setVisibility(View.VISIBLE);
                }
                catch(UnknownHostException e) {

                    Log.e(TAG, "UnknownHostException in acceptButton: " + e);
                }
                catch(Exception e) {

                    Log.e(TAG, "Exception in acceptButton: " + e);
                }
                StopRing();
            }
        });

        // REJECT BUTTON
        Button rejectButton = (Button) findViewById(R.id.buttonReject);
        rejectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Send a reject notification and end the call
                sendMessage("REJ:");
                endCall();

                StopRing();
            }
        });

        // END BUTTON
        endButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                endCall();
            }
        });

        StartRing();
    }

    private void endCall() {
        // End the call and send a notification
        stopListener();
        if(IN_CALL) {

            //call.endCall();
            Log.i(TAG, "Ending call");


            if(client != null) {
                client.finished = true;
                client = null;
            }
        }
        sendMessage("END:");
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopListener();
        if(IN_CALL) {

            Log.i(TAG, "Ending call");
            if(client != null) {
                client.finished = true;
                client = null;

            }
        }
        sendMessage("END:");
        Log.e(TAG, "onPuase");
        StopRing();
    }

    private void startListener() {
        // Creates the listener thread
        LISTEN = true;
        Thread listenThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    Log.i(TAG, "Listener started!");
                    DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                    socket.setSoTimeout(1500);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {

                        try {

                            Log.i(TAG, "Listening for packets");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.i(TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("END:")) {
                                // End call notification received. End call
                                endCall();
                            }
                            else {
                                // Invalid notification received.
                                Log.w(TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(IOException e) {

                            Log.e(TAG, "IOException in Listener " + e);
                        }
                    }
                    Log.i(TAG, "Listener ending");
                    socket.disconnect();
                    socket.close();
                    return;
                }
                catch(SocketException e) {

                    Log.e(TAG, "SocketException in Listener " + e);
                    endCall();
                }
            }
        });
        listenThread.start();
    }

    private void stopListener() {
        // Ends the listener thread
        LISTEN = false;
    }

    private void sendMessage(final String message) {
        // Creates a thread for sending notifications
        Thread replyThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    InetAddress address = InetAddress.getByName(contactIp);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, BROADCAST_PORT);
                    socket.send(packet);
                    Log.e(TAG, "Sent message( " + message + " ) to " + contactIp);
                    socket.disconnect();
                    socket.close();
                }
                catch(UnknownHostException e) {

                    Log.e(TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
                }
                catch(SocketException e) {

                    Log.e(TAG, "Failure. SocketException in sendMessage: " + e);
                }
                catch(IOException e) {

                    Log.e(TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }


    private class ConnTask extends AsyncTask<Void, Integer, Long> {
        protected Long doInBackground(Void... voids) {

            long totalSize = 0;

            try {


                Log.e(TAG, "Connecting");
                //Socket socket = new Socket(ip, port);

                client = new Client(contactIp, port);
                client.start();

                //audioCall = new AudioCall();
                //audioCall.startCall();
                //client = new Client(ip.toString(), port);
                //s = new Socket("58.224.86.126", 80);

                Log.e(TAG, "client created with ip " + contactIp + " port " + port);



            }

            catch (Exception e) {

                Log.e(TAG, "caught");
            }


            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {
            Log.e(TAG, "Asyc finished");
        }
    }

    private void StartRing() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ring = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ring.play();

        Log.e(TAG, "rington started");
    }

    private void StopRing() {

        if(ring != null)
            ring.stop();
        Log.e(TAG, "rington stop");
    }

}
