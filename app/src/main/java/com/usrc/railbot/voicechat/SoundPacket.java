package com.usrc.railbot.voicechat;

/**
 * Created by usrc on 17. 10. 25.
 */

import android.media.AudioFormat;

import java.io.Serializable;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * some sound
 * @author dosse
 */

/*
public class SoundPacket implements Serializable{

    public static int defaultDataLenght=1200; //send 1200 samples/packet by default
    private byte[] data; //actual data. if null, comfort noise will be played

    public SoundPacket(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

}

*/

public class SoundPacket implements Serializable{
    //public static AudioFormat defaultFormat=new AudioFormat(11025f, 8, 1, true, true); //11.025khz, 8bit, mono, signed, big endian (changes nothing in 8 bit) ~8kb/s
    public static int defaultDataLenght=2400; //send 1200 samples/packet by default
    private byte[] data; //actual data. if null, comfort noise will be played

    public SoundPacket(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

}
