package com.railbot.usrc.robotcontrol;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * Created by usrc on 16. 12. 19.
 */

public class VideoSurfaceView extends SurfaceView implements //VideoDisplay,
        SurfaceHolder.Callback {

    private static final String TAG 	 = "VideoSurfaceView";

    public static enum ScaleType {
        CENTER_CROP, CENTER_INSIDE, FIT_XY
    }

    //private VideoPlayer player = null;
    private boolean mCreated = false;


    public VideoSurfaceView(Context context) {
        this(context, null, 0);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.RGBA_8888);
        holder.addCallback(this);
    }


    /*
    @Override
    public void setMpegPlayer(VideoPlayer _player) {
        if (player != null)
            throw new RuntimeException(
                    "setMpegPlayer could not be called twice");

        this.player = _player;
    }
    */

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCreated  == true) {
            surfaceDestroyed(holder);
        }

        Surface surface = holder.getSurface();
       // player.render(surface);
        mCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //this.player.renderFrameStop();
        mCreated = false;
    }




}
