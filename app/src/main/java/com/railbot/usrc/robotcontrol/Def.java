package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 12. 4.
 */

public class Def {

    public static final int SAMPLE_RATE = 11025; // Hertz
    public static final int SAMPLE_INTERVAL = 20; // Milliseconds
    public static final int SAMPLE_SIZE = 2; // Bytes
    public static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    public static final int VOL = 3;
}
