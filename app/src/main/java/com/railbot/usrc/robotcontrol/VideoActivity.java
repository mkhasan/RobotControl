package com.railbot.usrc.robotcontrol;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class VideoActivity extends Activity {

    private static final String TAG 	 = "VideoActiveity";
    boolean portraitOrientation;
    private View mVideoView;


    ////////////////////////// newly added ///////////////////////////////
    private SeekBar speedBar;
    public RailController railController;
    private MsgSender msgSender;
    TextView speedView;

    ImageButton moveBackwardBtn;
    ImageButton moveForwardBtn;


    ////////////////////////// newly added ///////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mVideoView = findViewById(R.id.video_view);

        /////////////////////////////  newly added ///////////////////////
        speedBar = (SeekBar) findViewById(R.id.speed_bar);
        moveBackwardBtn = (ImageButton) findViewById(R.id.button_backward);
        moveForwardBtn = (ImageButton) findViewById(R.id.button_forward);

        portraitOrientation = true;


        //msgSe = new DeviceController("172.24.1.1", 8000, 8081);
        msgSender = new MsgSender("172.24.1.1", 8000, MsgSender.protocoletype.udp);
        //msgSender = new MsgSender("192.168.0.254", 8899, MsgSender.protocoletype.udp);

        railController = new RailController(msgSender, (float) 0.0, (float) 3.0);

        TextView tv = (TextView) findViewById(R.id.min_val);
        tv.setText(Float.toString(railController.GetMinSpeed()));
        tv = (TextView) findViewById(R.id.max_val);
        tv.setText(Float.toString(railController.GetMaxSpeed()));

        speedView = (TextView) findViewById(R.id.speed_text);
        speedView.setText(Float.toString(railController.GetCurSpeed())+" m/s");


        speedBar.setOnSeekBarChangeListener(

                new SeekBar.OnSeekBarChangeListener() {
                    int progress = 0;
                    float maxSpeed = railController.GetMaxSpeed();
                    float minSpeed = railController.GetMinSpeed();
                    float curSpeed;

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                        progress = progresValue;
                        //speedView.setText();
                        curSpeed = (float) ((double) minSpeed +  ((maxSpeed-minSpeed)*(float) progress/(100.0)));
                        speedView.setText(Float.toString(curSpeed)+" m/s");
                        railController.SetCurSpeed(curSpeed);
                        Log.e(TAG, Float.toString(curSpeed));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do something here,

                        //if you want to do anything at the start of
                        // touching the seekbar
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Display the value in textview
                        //textView.setText(progress + "/" + seekBar.getMax());
                    }
                });


        moveBackwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e(TAG, "action down");
                    moveBack();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.e(TAG, "action up");
                    moveBackRelease();
                }

                return true;
            }
        });

        moveForwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e(TAG, "action down");
                    moveFront();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.e(TAG, "action up");
                    moveFrontRelease();
                }

                return true;
            }
        });
        /*

        final ImageButton ib = (ImageButton) findViewById(R.id.button_back);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ib.getLayoutParams();

        // Set the height of this ImageButton
        params.height = 400;

        // Set the width of that ImageButton
        params.width = 450;

        // Apply the updated layout parameters to last ImageButton
        ib.setLayoutParams(params);
        */

        /////////////////////////////  newly added ///////////////////////


    }


    public void sendMessage(View view) {

        Log.e(TAG, "fullscreen");

        /*


        if (portraitOrientation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        }
        */
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.e(TAG, "onChagne");
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            android.view.ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoView.setLayoutParams(params);
            portraitOrientation = false;
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            android.view.ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.player_width);
            params.height = (int) getResources().getDimension(R.dimen.player_height);
            mVideoView.setLayoutParams(params);
            portraitOrientation = true;

        }
    }

    public void moveStop(View view) {

        railController.StopMoving();
        Log.e(TAG, "move stop");
    }

    public void moveBack() {

        railController.MoveBackward();
        Log.e(TAG, "move back");
    }


    public void moveBackRelease() {
        railController.StopMoving();
        Log.e(TAG, "move back release");
    }

    public void moveFront() {
        railController.MoveForward();
        Log.e(TAG, "move front");
    }


    public void moveFrontRelease() {
        railController.StopMoving();
        Log.e(TAG, "move front release");
    }


    public void camDown(View view) {
        Log.e(TAG, "camera down");
    }

    public void camUp(View view) {
        Log.e(TAG, "camera up");
    }

    public void camLeft(View view) {
        Log.e(TAG, "camera left");
    }

    public void camRight(View view) {
        Log.e(TAG, "camera right");
    }

    public void zoomMinus(View view) {
        Log.e(TAG, "zoom minus");
    }

    public void zoomPlus(View view) {
        Log.e(TAG, "zoom plus");
    }


}
