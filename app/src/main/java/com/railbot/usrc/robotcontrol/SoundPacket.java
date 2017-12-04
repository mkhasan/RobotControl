package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 12. 4.
 */

import java.io.Serializable;

public class SoundPacket implements Serializable {
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
