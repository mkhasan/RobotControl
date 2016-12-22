package com.railbot.usrc.robotcontrol;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Created by usrc on 16. 12. 19.
 */

public class MsgSender {
    public enum protocoletype {
        udp,
        tcp
    }

    ;

    private static final String TAG = "MsgSender";

    private int port;
    private String destAddr;
    private protocoletype protocol;

    private static class SendMsgTask extends AsyncTask<Object, Object, Boolean> {
        private final MsgSender msgSender;
        private final String msg;
        private final Boolean sleepBeforeSending;


        public SendMsgTask(MsgSender _msgSender, String _msg) {
            this.msgSender = _msgSender;
            msg = _msg;
            sleepBeforeSending = false;

        }

        public SendMsgTask(MsgSender _msgSender, String _msg, boolean _sleepBeforeSending) {
            this.msgSender = _msgSender;
            msg = _msg;
            sleepBeforeSending = _sleepBeforeSending;

        }


        @Override
        protected Boolean doInBackground(Object... arg0) {
            return Send();
        }

        protected void onPostExecute(Boolean flag) {
            Log.e(TAG, "sent");
        }

        private Boolean Send() {

            if (msgSender.protocol == protocoletype.udp) {


                try {

                    if (sleepBeforeSending)
                        Thread.sleep(100);
                    String host = msgSender.destAddr;
                    int port = msgSender.port; //Random Port

                    byte[] message = msg.getBytes();

                    // Get the internet address of the specified host
                    InetAddress address = InetAddress.getByName(host);

                    // Initialize a datagram packet with data and address
                    DatagramPacket packet = new DatagramPacket(message, message.length,
                            address, port);

                    // Create a datagram socket, send the packet through it, close it.
                    DatagramSocket dsocket = new DatagramSocket();
                    dsocket.send(packet);
                    dsocket.close();


                } catch (Exception e) {
                    System.err.println(e);
                    return false;
                }

                return true;
            } else
                return false;
        }

    }


    public MsgSender(String _destAddr, int _port, protocoletype _protocol) {

        destAddr = _destAddr;
        port = _port;
        protocol = _protocol;


    }

    public void SendMsg(String msg) {



        new SendMsgTask(this, msg).execute();

        //return true;

    }

    public void SendMsg(String msg, Boolean sleepBeforeSending) {
        new SendMsgTask(this, msg, sleepBeforeSending).execute();

    }


}
