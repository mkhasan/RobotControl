package com.railbot.usrc.mediaplayer;

/**
 * Created by usrc on 16. 12. 12.
 */

public class FFError extends Throwable {

    public enum ErrorType {
        NO_ERROR,
        PTS_ERROR
    }

    public FFError(int err) {
        super(String.format("FFmpegPlayer error %d", err));
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

}

