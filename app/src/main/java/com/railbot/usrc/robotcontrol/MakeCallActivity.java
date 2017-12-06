package com.railbot.usrc.robotcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;



public class MakeCallActivity extends Activity {

    private static final String LOG_TAG = "MakeCall";
    private static final int BROADCAST_PORT = 50002;
    private static final int BUF_SIZE = 1024;
    private String displayName;
    private String contactName;
    private String contactIp;
    private int port;
    private boolean LISTEN = true;
    private boolean IN_CALL = false;


    private Client client = null;

    private DatagramSocket socket = null;


    private Runnable updateTextRunnable = new Runnable() {

        @Override
        public void run() {

            final TextView textView = (TextView) findViewById(R.id.textViewCalling);
            textView.setText("Server Received Call");

        }

    };

    private Runnable serverNoReplyDlg = new Runnable() {
        @Override
        public void run() {
            String message = "No reply fron the Server ";
            Log.i(LOG_TAG, "No reply from contact. Ending call");
            new AlertDialog.Builder(MakeCallActivity.this, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Notification")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            endCall();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    endCall();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    };


    private class ListenThread extends Thread {
        Activity activity;
        ListenThread(Activity _activity) {
            activity = _activity;
        }

        @Override
        public void run() {

            try {

                Log.i(LOG_TAG, "Listener started!");
                socket = new DatagramSocket(BROADCAST_PORT);
                socket.setSoTimeout(15000);
                byte[] buffer = new byte[BUF_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                while(LISTEN) {

                    try {

                        Log.i(LOG_TAG, "Listening for packets");
                        socket.receive(packet);
                        String data = new String(buffer, 0, packet.getLength());
                        Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                        String action = data.substring(0, 4);
                        if(action.equals("ACC:")) {
                            // Accept notification received. Start call
                            //call = new AudioCall(packet.getAddress());
                            //call.startCall();
                            new ConnTask().execute();
                            IN_CALL = true;

                            activity.runOnUiThread(updateTextRunnable);
                            //final TextView textViewCalling = (TextView) findViewById(R.id.textViewCalling);
                            //textViewCalling.setText("Server Received Call");

                        }
                        else if(action.equals("REJ:")) {
                            // Reject notification received. End call
                            endCall();
                        }
                        else if(action.equals("END:")) {
                            // End call notification received. End call
                            endCall();
                        }
                        else {
                            // Invalid notification received
                            Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
                        }
                    }
                    catch(SocketTimeoutException e) {
                        if(!IN_CALL) {


                            activity.runOnUiThread(serverNoReplyDlg);
                            //endCall();
                            return;
                        }

                    }
                    catch(IOException e) {

                        Log.i(LOG_TAG, "No response");
                    }
                }
                Log.i(LOG_TAG, "Listener ending");
                socket.disconnect();
                socket.close();
                return;
            }
            catch(SocketException e) {

                Log.e(LOG_TAG, "SocketException in Listener");
                endCall();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_call);

        Log.i(LOG_TAG, "MakeCallActivity started!");


        Intent intent = getIntent();
        contactIp = intent.getStringExtra(MainActivity.EXTRA_IP);
        port = Integer.parseInt(intent.getStringExtra(MainActivity.EXTRA_PORT));



        TextView textView = (TextView) findViewById(R.id.textViewCalling);
        textView.setText("Calling: " + "Server");


        startListener();

        makeCall();

        Button endButton = (Button) findViewById(R.id.buttonEndCall);
        endButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Button to end the call has been pressed

                endCall();
            }
        });



    }

    private void makeCall() {
        // Send a request to start a call
        sendMessage("CAL:", 50003);
    }

    private void endCall() {
        // Ends the chat sessions
        stopListener();
        if(IN_CALL) {

            Log.i(LOG_TAG, "Ending call");
            if(client != null) {
                client.finished = true;
                client = null;

            }
        }
        sendMessage("END:", BROADCAST_PORT);
        Log.e(LOG_TAG, "endCall");
        finish();
    }

    private void startListener() {
        // Create listener thread
        LISTEN = true;
        new ListenThread(MakeCallActivity.this).start();
        //listenThread.start();
    }

    private void stopListener() {
        // Ends the listener thread
        LISTEN = false;
    }

    private void sendMessage(final String message, final int port) {
        // Creates a thread used for sending notifications
        Thread replyThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    InetAddress address = InetAddress.getByName(contactIp);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    socket.send(packet);
                    Log.e(LOG_TAG, "Sent message( " + message + " ) to " + contactIp + ":" + port);
                    socket.disconnect();
                    socket.close();
                }
                catch(UnknownHostException e) {

                    Log.e(LOG_TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "Failure. SocketException in sendMessage: " + e);
                }
                catch(IOException e) {

                    Log.e(LOG_TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }

    @Override
    public void onPause() {

        super.onPause();


        stopListener();
        if(IN_CALL) {

            Log.i(LOG_TAG, "Ending call");
            if(client != null) {
                client.finished = true;
                client = null;

            }
        }
        sendMessage("END:", BROADCAST_PORT);
        Log.e(LOG_TAG, "onPuase");

        if(socket != null) {
            socket.disconnect();
            socket.close();
        }


    }

    private class ConnTask extends AsyncTask<Void, Integer, Long> {
        protected Long doInBackground(Void... voids) {

            long totalSize = 0;

            try {


                Log.e(LOG_TAG, "Connecting");
                //Socket socket = new Socket(ip, port);

                client = new Client(contactIp, port);
                client.start();

                //audioCall = new AudioCall();
                //audioCall.startCall();
                //client = new Client(ip.toString(), port);
                //s = new Socket("58.224.86.126", 80);



            }

            catch (Exception e) {

                Log.e(LOG_TAG, "caught");
            }


            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {
            Log.e(LOG_TAG, "Asyc finished");
        }
    }



}
