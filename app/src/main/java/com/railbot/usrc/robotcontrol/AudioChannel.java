package com.railbot.usrc.robotcontrol;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.usrc.railbot.voicechat.Message;
import com.usrc.railbot.voicechat.SoundPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import static com.railbot.usrc.robotcontrol.Def.SAMPLE_RATE;
import static com.railbot.usrc.robotcontrol.Def.VOL;

/**
 * Created by usrc on 17. 12. 4.
 */

public class AudioChannel extends Thread {

    private static final String TAG = "AudioChannel";
    private long chId; //an id unique for each user. generated by IP and port
    private ArrayList<Message> queue = new ArrayList<Message>(); //queue of messages to be played
    private int lastSoundPacketLen = 0;//SoundPacket.defaultDataLenght;
    private long lastPacketTime = System.nanoTime();
    private final float adjustVol = (float) VOL / (float) 100.0;

    boolean speakers = true;

    public boolean canKill() { //returns true if it's been a long time since last received packet
        if (System.nanoTime() - lastPacketTime > 5000000000L) {
            return true; //5 seconds with no data
        } else {
            return false;
        }
    }

    public void closeAndKill() {

        speakers = false;
        stop();
    }

    public AudioChannel(long chId) {
        this.chId = chId;
    }

    public long getChId() {
        return chId;
    }

    public void addToQueue(Message m) { //adds a message to the play queue
        queue.add(m);
    }
    //private SourceDataLine speaker = null; //speaker

    @Override
    public void run() {
        try {
            //open channel to sound card
            /*
            AudioFormat af = SoundPacket.defaultFormat;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(af);
            speaker.start();
            */

            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, SoundPacket.defaultDataLenght, AudioTrack.MODE_STREAM);
            track.setStereoVolume(adjustVol, adjustVol);
            Log.e(TAG, "Volume is " + Float.toString(adjustVol));
            track.play();
            //sound card ready


            while (speakers) { //this infinite cycle checks for new packets to be played in the queue, and plays them. to avoid busy wait, a sleep(10) is executed at the beginning of each iteration
                if (queue.isEmpty()) { //nothing to play, wait
                    Thread.sleep(10);  // later change
                    continue;
                } else {//we got something to play
                    lastPacketTime = System.nanoTime();
                    Message in = queue.get(0);
                    queue.remove(in);


                    if (in.getData() instanceof SoundPacket) { //it's a sound packet, send it to sound card
                        SoundPacket m = (SoundPacket) (in.getData());
                        if (m.getData() == null) {//sender skipped a packet, play comfort noise
                            byte[] noise = new byte[lastSoundPacketLen];
                            for (int i = 0; i < noise.length; i++) {
                                noise[i] = (byte) ((Math.random() * 3) - 1);
                            }
                            track.write(noise, 0, noise.length);
                        } else {
                            //decompress data
                            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(m.getData()));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            for (;;) {
                                int b = gis.read();
                                if (b == -1) {
                                    break;
                                } else {
                                    baos.write((byte) b);
                                }
                            }
                            //play decompressed data
                            byte[] toPlay=baos.toByteArray();
                            track.write(toPlay, 0, toPlay.length);
                            lastSoundPacketLen = m.getData().length;
                        }
                    } else { //not a sound packet, trash
                        continue; //invalid message
                    }

                }
            }

            track.stop();
            track.flush();
            track.release();
            speakers = false;

        } catch (Exception e) { //sound card error or connection error, stop
            System.out.println("receiverThread " + chId + " error: " + e.toString());
            /*
            if (speaker != null) {
                speaker.close();
            }
            */
            stop();
        }
    }
}