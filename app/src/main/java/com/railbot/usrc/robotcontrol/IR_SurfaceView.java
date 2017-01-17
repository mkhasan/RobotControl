package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 1. 16.
 */


import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by usrc on 16. 12. 12.
 */

public class IR_SurfaceView extends SurfaceView implements View.OnTouchListener, SurfaceHolder.Callback {

    private static final String TAG 	 = "irSurfaceView";


    private int width;
    private int height;
    private IR_Viewer viewer;
    private boolean mCreated = false;

    public IR_SurfaceView(Context context) {
        this(context, null, 0);
    }


    public IR_SurfaceView(Context context, AttributeSet attrs) {





        this(context, attrs, 0);


        setOnTouchListener(this);


    }

    public IR_SurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.RGBA_8888);
        holder.addCallback(this);
    }


    public void SetViewer(IR_Viewer _viewer) {
        viewer = _viewer;
    }
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
        viewer.renderViewer(surface);
        mCreated = true;
        width = getWidth();
        height = getHeight();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //this.player.renderFrameStop();
        viewer.Finalize();
        mCreated = false;
    }

    @Override
    public boolean onTouch (View view, MotionEvent event) {
        //Log.e(TAG, "Testing inside ...");

        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("TAG", "donw: (" + x + ", " + y + ")" + "width is " + width + " height is " + height);
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.e("TAG", "moving: (" + x + ", " + y + ")");
                break;
            case MotionEvent.ACTION_UP:
                //Log.e("TAG", "touched up");
                break;
        }

        return true;
    }





}
