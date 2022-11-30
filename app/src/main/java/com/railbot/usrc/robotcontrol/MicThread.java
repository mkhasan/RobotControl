package com.railbot.usrc.robotcontrol;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.usrc.railbot.voicechat.Message;
import com.usrc.railbot.voicechat.SoundPacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import static com.railbot.usrc.robotcontrol.Def.SAMPLE_RATE;

/**
 * Created by usrc on 17. 12. 4.
 */

public class MicThread extends Thread {

    private static final String TAG = "MicThread";


    public static double amplification = 1.0;
    private ObjectOutputStream toServer;


    public boolean mic = true;

    public MicThread(ObjectOutputStream toServer) {
        this.toServer = toServer;

        Log.e(TAG, "I am in ");
        //open microphone line, an exception is thrown in case of error

    }
    static int x=100;
    @Override
    public void run() {

        boolean flag;
        Log.i(TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());
        AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);

        try {
            // Create a socket and start recording

            ByteBuffer buffer = ByteBuffer.allocate(SoundPacket.defaultDataLenght);
            byte[] temp = new byte[buffer.capacity()];
            byte[] packet = new byte[buffer.capacity()];
            audioRecorder.startRecording();
            int bytesRead=0;
            while(mic) {

                //Log.e(TAG, "Waiting " + BUF_SIZE);
                for (int i = 0; i < temp.length; i++)
                    temp[i] = 0;

                bytesRead = audioRecorder.read(temp, 0, buffer.remaining());

                flag = false;

                for (int i = 0; i < temp.length; i++) {
                    if (temp[i] > 0) {
                        flag = true;
                        break;
                    }
                }



                if(bytesRead < buffer.capacity())
                    Log.e(TAG, "Got it");


                if(bytesRead < buffer.remaining()) {
                    buffer.put(temp, 0, bytesRead);
                    sleep(10);
                    //Log.e(TAG, "ReadData nonzero " + flag + " bytes read " + bytesRead);
                    continue;
                }


                buffer.put(temp, 0, bytesRead);


                for (int i=0; i<packet.length; i++)
                    packet[i] = 0;

                //while(buffer.remaining() == 0) {    // at first buffer.rem must be 0...read repeatedly as unatil less than full buffer is found
                buffer.flip();                  // we are flushing old data to avoid echo
                buffer.get(packet, 0, packet.length);
                buffer.clear();

                //  Log.e(TAG, "Now : "+buffer.remaining());
                //  bytesRead = audioRecorder.read(temp, 0, buffer.capacity());

                //  Log.e(TAG, "After : "+buffer.capacity());
                //  buffer.put(temp, 0, bytesRead);
                //}

                //Log.e(TAG, "packet len " + packet.length);

                long tot = 0;
                flag=false;
                for (int i = 0; i < packet.length; i++) {
                    packet[i] *= amplification;
                    tot += Math.abs(packet[i]);
                    if(packet[i] > 0)
                        flag = true;

                }

                //Log.e(TAG, "data nonzero " + flag);
                tot *= 2.5;
                tot /= packet.length;
                //create and send packet
                Message m = null;
                if (tot == 0) {//send empty packet
                    m = new Message(-1, -1, new SoundPacket(null));
                    Log.e(TAG, "Sending null");

                } else { //send data
                    //compress the sound packet with GZIP
                    //Log.e(TAG, "Sending");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    GZIPOutputStream go = new GZIPOutputStream(baos);
                    go.write(packet);
                    go.flush();
                    go.close();
                    baos.flush();
                    baos.close();
                    m = new Message(-1, -1, new SoundPacket(baos.toByteArray()));  //create message for server, will generate chId and timestamp from this computer's IP and this socket's port
                }



                toServer.writeObject(m);
                Thread.sleep(100);




            }
            // Stop recording and release resources
            audioRecorder.stop();
            audioRecorder.release();
            mic = false;
            return;
        }
        catch(InterruptedException e) {

            Log.e(TAG, "InterruptedException: " + e.toString());
            mic = false;
        }
        catch(SocketException e) {

            Log.e(TAG, "SocketException: " + e.toString());
            mic = false;
        }
        catch(UnknownHostException e) {

            Log.e(TAG, "UnknownHostException: " + e.toString());
            mic = false;
        }
        catch(IOException e) {

            Log.e(TAG, "IOException: " + e.toString());
            mic = false;
        }

    }

}
