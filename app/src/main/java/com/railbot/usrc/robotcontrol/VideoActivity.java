package com.railbot.usrc.robotcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatProperty;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.railbot.usrc.mediaplayer.FFError;
import com.railbot.usrc.mediaplayer.FFListener;
import com.railbot.usrc.mediaplayer.NotPlayingException;
import com.railbot.usrc.mediaplayer.StreamInfo;
import com.railbot.usrc.mediaplayer.VideoDisplay;
import com.railbot.usrc.mediaplayer.VideoPlayer;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.jmedeisis.bugstick.Joystick;

import static android.content.RestrictionsManager.RESULT_ERROR;


public class VideoActivity extends Activity implements FFListener, IR_ViewerListener {

    private static final String TAG 	 = "VideoActiveity";
    public static int panAngle = 0;
    public static int tiltAngle = -40;

    private final int ANGLE_STEP = 10;

    private VideoPlayer mMpegPlayer;
    boolean portraitOrientation;
    protected boolean mPlay = false;
    private boolean stopOnRelease;



    Boolean imageCamConnected, thermalCamConnectted;

    private enum CameraType {
        image,
        thermal,
        both,
        none
    }


    Joystick panStick;
    Joystick tiltStick;


    ////////////////////////// newly added ///////////////////////////////
    private SeekBar speedBar;
    public RailController railController;
    private ControlStickListener moveStickListener = null;
    private ControlStickListener panStickListener = null;
    private ControlStickListener tiltStickListener = null;
    private MsgSender msgSender;
    TextView speedView;
    private TextView state;

    ImageButton moveBackwardBtn;
    ImageButton moveForwardBtn;

    VideoView videoView;
    private View surfaceView;
    private IR_SurfaceView irSurfaceView;

    private IR_Viewer irViewer = null;
    private View mLoadingView;

    CameraType cameraType;

    int count;

    ////////////////////////// newly added ///////////////////////////////

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {



            state.setText(String.format("%d", count++));

            Log.e(TAG, "onTimer");
            //timerHandler.postDelayed(this, 1000);
        }


    };

    private static class ConnectionCheckTask extends AsyncTask<Object, Void, Integer> {

        private final VideoActivity activity;

        public ConnectionCheckTask(VideoActivity _activity) {
            this.activity = _activity;

        }


        @Override
        protected Integer doInBackground(Object... params) {

            //player.stopNative();
            //return null;
            try {
                Thread.sleep(5000);
            }
            catch (Exception e) {
                return -1;
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.e(TAG, "Connection Task Result is " + result);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        surfaceView = findViewById(R.id.surface_view);
        irSurfaceView = (IR_SurfaceView) findViewById(R.id.ir_view);





        mLoadingView = this.findViewById(R.id.loading_view);

        mMpegPlayer = new VideoPlayer((VideoDisplay) surfaceView, this);

        /////////////////////////////  newly added ///////////////////////
        speedBar = (SeekBar) findViewById(R.id.speed_bar);
        moveBackwardBtn = (ImageButton) findViewById(R.id.button_backward);
        moveForwardBtn = (ImageButton) findViewById(R.id.button_forward);

        portraitOrientation = true;
        imageCamConnected = false;
        thermalCamConnectted = false;
        stopOnRelease = true;

        String choice = getIntent().getStringExtra("choice");

        Log.e(TAG, "Choice is "+choice);

        String motionStr = getResources().getText(R.string.motion_only).toString();
        String imageStr = getResources().getText(R.string.image_camera).toString();
        String thermalStr = getResources().getText(R.string.thermal_camera).toString();

        if (choice.equals(getString(R.string.motion_only)))
            cameraType = CameraType.none;
        else if (choice.equals(imageStr))
            cameraType = CameraType.image;
        else if (choice.equals(thermalStr))
            cameraType = CameraType.thermal;
        else
            cameraType = CameraType.none;



        //msgSe = new DeviceController("172.24.1.1", 8000, 8081);
        //msgSender = new MsgSender("172.24.1.1", 8000, MsgSender.protocoletype.udp);
        //msgSender = new MsgSender("192.168.0.254", 8899, MsgSender.protocoletype.udp);
        msgSender = new MsgSender(getString(R.string.rail_server_ip), Integer.parseInt(getString(R.string.rail_server_port)), MsgSender.protocoletype.udp);

        float minSpeed = Float.parseFloat(getString(R.string.min_speed));
        float maxSpeed = Float.parseFloat(getString(R.string.max_speed));
        railController = new RailController(msgSender, minSpeed, maxSpeed);

        String speedStr = getString(R.string.speed) + " (" + minSpeed + "~" + maxSpeed + ")";



        TextView speedLabel = (TextView) findViewById(R.id.speed_label);
        speedLabel.setText(speedStr);

        videoView = (VideoView) findViewById(R.id.video_view);

        state = (TextView) findViewById(R.id.state);

        state.setText(getString(R.string.state)+getString(R.string.stop));

        count = 0;

        //timerHandler.postDelayed(timerRunnable, 2000);

        Log.e(TAG, "onCreate");

        //String url = "rtsp://admin:admin@"+getString(R.string.rail_server_ip)+":554/stream1";

        String url = "rtsp://admin:admin@"+getString(R.string.image_camera_ip)+":554/stream1";

        if (cameraType == CameraType.thermal)
            url = "rtsp://admin:admin@"+getString(R.string.thermal_camera_ip)+":554/stream0";


        String testStr = getString(R.string.image_camera_ip);



        String[] parts = testStr.split("\\.");


        if (parts.length == 4 && parts[2].equals("1"))
            url = "rtsp://admin:admin@"+getString(R.string.image_camera_ip)+":554/12";
        //String url = "rtsp://admin:admin@192.168.1.100:554/12";

        HashMap<String, String> params = new HashMap<String, String>();
        mPlay = false;

        mMpegPlayer.setListener(this);

        if (cameraType == CameraType.none) {

            mLoadingView.setVisibility(View.GONE);


        }
        else if (cameraType == CameraType.image ) {
            surfaceView.setVisibility(View.VISIBLE);
            mMpegPlayer.setDataSource(url, params, VideoPlayer.UNKNOWN_STREAM, VideoPlayer.NO_STREAM,
                    VideoPlayer.NO_STREAM);
            String str = Long.toHexString(mMpegPlayer.NativePlayer()) + " ";
            Log.e(TAG, str);



        }
        else if (cameraType == CameraType.thermal) {
            irSurfaceView.setVisibility(View.VISIBLE);
            irViewer = new IR_Viewer(irSurfaceView);

        } else {
                    // to be handled later
        }

        if (cameraType == CameraType.thermal)

            irViewer.setListener(this);


        if (cameraType == CameraType.thermal)
            irViewer.Connect();



        //Toast.makeText(getApplicationContext(), getString(R.string.connected) + " to " + getString(R.string.rail_server_ip),
          //      Toast.LENGTH_LONG).show();

        //connected = true;

        //TextView tv = (TextView) findViewById(R.id.min_val);
        //tv.setText(Float.toString(railController.GetMinSpeed()));
        //tv = (TextView) findViewById(R.id.max_val);
        //tv.setText(Float.toString(railController.GetMaxSpeed()));

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

                        /*
                        if (railController.GetCurMove() == RailController.CurMove.forward)
                            railController.MoveForward();
                        else if (railController.GetCurMove() == RailController.CurMove.backward)
                            railController.MoveBackward();
                        */

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



        panStick = (Joystick) findViewById(R.id.pan_stick);
        panStickListener= new ControlStickListener(ControlStickListener.ControlType.PAN);
        panStickListener.SetRailController(railController);
        panStick.setJoystickListener(panStickListener);

        tiltStick = (Joystick) findViewById(R.id.tilt_stick);
        tiltStickListener = new ControlStickListener(ControlStickListener.ControlType.TILT);
        tiltStickListener.SetRailController(railController);
        tiltStick.setJoystickListener(tiltStickListener);


        final ImageButton calibrateBtn = (ImageButton) findViewById(R.id.calibrate);
        calibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(railController != null) {
                    railController.Calibrate();

                }
            }
        });

        final ImageButton panRightBtn = (ImageButton) findViewById(R.id.pan_front);
        panRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int angle = panAngle+ANGLE_STEP;
                if (angle > ControlStickListener.MAX_PAN)
                    angle = ControlStickListener.MAX_PAN;
                Log.e(TAG, "pan angle " + angle);
                railController.CameraPan(angle);

            }
        });

        final ImageButton panLeftBtn = (ImageButton) findViewById(R.id.pan_back);
        panLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int angle = panAngle-ANGLE_STEP;
                if (angle < ControlStickListener.MIN_PAN)
                    angle = ControlStickListener.MIN_PAN;
                Log.e(TAG, "pan angle " + angle);
                railController.CameraPan(angle);

            }
        });


        final ImageButton tiltUpBtn = (ImageButton) findViewById(R.id.tilt_up);
        tiltUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int angle = tiltAngle+ANGLE_STEP;
                if (angle > ControlStickListener.MAX_TILT)
                    angle = ControlStickListener.MAX_TILT;

                railController.CameraTilt(angle);
                Log.e(TAG, "ANGLE is " + angle);

            }
        });

        final ImageButton tiltDownBtn = (ImageButton) findViewById(R.id.tilt_down);
        tiltDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int angle = tiltAngle-ANGLE_STEP;
                if (angle < ControlStickListener.MIN_TILT)
                    angle = ControlStickListener.MIN_TILT;
                railController.CameraTilt(angle);
                Log.e(TAG, "ANGLE is " + angle);

            }
        });





    }


    public void sendMessage(View view) {

        Log.e(TAG, "fullscreen");

        if (cameraType == CameraType.thermal) {
            //Log.e(TAG, "onConnect");
            //irViewer.Connect();
        }


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.e(TAG, "onChagne");


        /*
        RelativeLayout contorlSet = (RelativeLayout) findViewById(R.id.control_set);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            contorlSet.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            android.view.ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            surfaceView.setLayoutParams(params);
            portraitOrientation = false;
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            contorlSet.setVisibility(View.VISIBLE);
            android.view.ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.player_width);
            params.height = (int) getResources().getDimension(R.dimen.player_height);//getResources().getDimension(R.dimen.player_height);
            surfaceView.setLayoutParams(params);
            portraitOrientation = true;

        }

        */
    }

    public void moveStop(View view) {

        ShowAllert();
        //setResult(-1);
        //finish();
        Log.e(TAG, cameraType == CameraType.image ? "Image" : (cameraType == CameraType.none ? "none" : "error"));
        if (!IsConnected()) {

            Toast.makeText(this, getString(R.string.wait_for_connection), Toast.LENGTH_LONG).show();
            return;
        }

        if (stopOnRelease == false)
            railController.StopMoving(3);
        else
            railController.StopMoving();

        //Toast.makeText(this, getString(R.string.stop), Toast.LENGTH_LONG).show();
        state.setText(getString(R.string.state)+getString(R.string.stop));
        Log.e(TAG, "move stop");
    }

    public void moveBack() {

        if (!IsConnected()) {

            Toast.makeText(this, getString(R.string.wait_for_connection), Toast.LENGTH_LONG).show();
            return;
        }


        railController.MoveBackward();
        //Toast.makeText(this, getString(R.string.backward), Toast.LENGTH_LONG).show();
        state.setText(getString(R.string.state)+getString(R.string.backward));
        Log.e(TAG, "move back");
    }


    public void moveBackRelease() {


        if (stopOnRelease) {
            railController.StopMoving();
            state.setText(getString(R.string.state)+getString(R.string.stop));
        }
        Log.e(TAG, "move back release");
    }

    public void moveFront() {

        if (!IsConnected()) {

            Toast.makeText(this, getString(R.string.wait_for_connection), Toast.LENGTH_LONG).show();
            return;
        }

        railController.MoveForward();
        state.setText(getString(R.string.state)+getString(R.string.forward));
        Log.e(TAG, "move front");
    }


    public void cameraUp() {

        if (!IsConnected()) {

            Toast.makeText(this, getString(R.string.wait_for_connection), Toast.LENGTH_LONG).show();
            return;
        }

        railController.CameraUp();
        state.setText(getString(R.string.state)+getString(R.string.forward));
        Log.e(TAG, "camera up");
    }


    public void moveFrontRelease() {


        if (stopOnRelease) {
            railController.StopMoving();
            state.setText(getString(R.string.state)+getString(R.string.stop));
        }
        Log.e(TAG, "move front release");
    }


    public void camDown(View view) {
        railController.CameraDown();
        Log.e(TAG, "camera down");
    }

    public void camUp(View view) {
        railController.CameraUp();
        Log.e(TAG, "camera up");
    }

    public void camLeft(View view) {
        railController.CameraLeft();
        Log.e(TAG, "camera left");
    }

    public void camRight(View view) {
        railController.CameraRight();
        Log.e(TAG, "camera right");
    }

    public void zoomMinus(View view) {
        Log.e(TAG, "zoom minus");
    }

    public void zoomPlus(View view) {
        Log.e(TAG, "zoom plus");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraType == CameraType.image) {

            /*
            mMpegPlayer.setListener(null);
            Log.e(TAG, "going to stop");
            this.mMpegPlayer.stop();
            */

            if(mMpegPlayer !=  null)
                mMpegPlayer.DellaocatePlayer();

            Log.e(TAG, "going to deallocate");

            setResult(RESULT_OK);




        }

        if (cameraType == CameraType.thermal)

            irViewer.setListener(null);





    }

    private void stop() {
        //this.mControlsView.setVisibility(View.GONE);
        //this.mStreamsView.setVisibility(View.GONE);
        this.mLoadingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFFUpdateTime(long currentTimeUs, long videoDurationUs, boolean isFinished) {
        /*
        mCurrentTimeUs = currentTimeUs;
        if (!mTracking) {
            int currentTimeS = (int)(currentTimeUs / 1000 / 1000);
            int videoDurationS = (int)(videoDurationUs / 1000 / 1000);
            mSeekBar.setMax(videoDurationS);
            mSeekBar.setProgress(currentTimeS);
        }

        if (isFinished) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_end_of_video_title)
                    .setMessage(R.string.dialog_end_of_video_message)
                    .setCancelable(true).show();
        }
        */

        if (isFinished) {
            mMpegPlayer.stop();

            Log.e(TAG, "Error: stream should not be finished so earyly");
            finish();

        }

        //Log.e(TAG, "current time " + currentTimeUs + " duration " + videoDurationUs );
    }

    @Override
    public void onFFError(FFError error) {


        if (error == null) {
            //Toast.makeText(this, "hello", Toast.LENGTH_LONG).show();
            this.mLoadingView.setVisibility(View.GONE);
            imageCamConnected = true;

            return;
        }

        if (error != null) {
            String format = getResources().getString(
                    R.string.main_could_not_open_image_stream);
            String message = String.format(format, error.getMessage());

            Log.e(TAG, error.getMessage());
            new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            VideoActivity.this.finish();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    VideoActivity.this.finish();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }


    }

    @Override
    public void onFFDataSourceLoaded(FFError err, StreamInfo[] streams) {
        if (err != null) {
            String format = getResources().getString(
                    R.string.main_could_not_open_ir_stream);
            String message = String.format(format, err.getMessage());

            Log.e(TAG, err.getMessage());
            new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            VideoActivity.this.finish();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    VideoActivity.this.finish();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        this.mLoadingView.setVisibility(View.GONE);
        imageCamConnected = true;
        Log.e(TAG, "Image Camera connected");
        //mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
        //mPlayPauseButton.setEnabled(true);
        //this.mControlsView.setVisibility(View.VISIBLE);


        /*

        for (FFmpegStreamInfo streamInfo : streams) {
            CodecType mediaType = streamInfo.getMediaType();
            Locale locale = streamInfo.getLanguage();
            String languageName = locale == null ? getString(
                    R.string.unknown) : locale.getDisplayLanguage();
            if (FFmpegStreamInfo.CodecType.AUDIO.equals(mediaType)) {
                audio.addRow(new Object[] {languageName, streamInfo.getStreamNumber()});
            } else if (FFmpegStreamInfo.CodecType.SUBTITLE.equals(mediaType)) {
                subtitles.addRow(new Object[] {languageName, streamInfo.getStreamNumber()});
            }
        }
        mLanguageAdapter.swapCursor(audio);
        mSubtitleAdapter.swapCursor(subtitles);

        */
    }

    @Override
    public void onFFResume(NotPlayingException result) {
        mPlay = true;
    }

    public void onFFPause(NotPlayingException err) {
        mPlay = false;
    }


    @Override
    public void onFFStop() {

        Log.e(TAG, "MpegPlayer stopped");
        //mMpegPlayer.deallocNative();
    }

    @Override
    public void onFFSeeked(NotPlayingException result) {
//		if (result != null)
//			throw new RuntimeException(result);
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_yes:
                if (checked)
                    // Pirates are the best
                    stopOnRelease = true;
                    Log.e(TAG, "stp on release is true");
                    break;
            case R.id.radio_no:
                if (checked)
                    // Ninjas rule
                    stopOnRelease = false;
                    Log.e(TAG, "stp on release is false");
                    break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //timerHandler.postDelayed(timerRunnable, 0);

        Log.e(TAG, "onResume");
    }


    @Override
    protected void onPause()
    {
        // TODO Auto-generated method stub
        super.onPause();

        //timerHandler.removeCallbacks(timerRunnable);

        Log.e(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onStop");
        finish();
    }


    void callback() {
        Log.e(TAG, "From callback");
        finish();
    }

    static public void ShowAllertTest(Context context) {

        new AlertDialog.Builder(context, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Error")
                .setMessage("Communication Error Occured")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        //finish();
                    }
                })
                /*
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                */

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void ShowAllert() {

        Context context = this;
        new AlertDialog.Builder(context, AlertDialog.THEME_HOLO_DARK)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        //finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    @Override
    public void onDataSourceLoaded(int result) {

        Log.e(TAG, "Connected is " + result);

        if (result != 0) {
            String message = getResources().getString(
                    R.string.main_could_not_open_ir_stream);
            //String message = String.format(format, error.getMessage());

            //Log.e(TAG, error.getMessage());
            new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            VideoActivity.this.finish();
                        }
                    })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    VideoActivity.this.finish();
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        else {
            thermalCamConnectted = true;
            mLoadingView.setVisibility(View.GONE);
        }

    }

    boolean IsConnected() {
        if (cameraType == CameraType.none)
            return  true;
        boolean connected = false;
        connected = connected || (cameraType == CameraType.image && imageCamConnected == true);
        connected = connected || (cameraType == CameraType.thermal && thermalCamConnectted == true);

        return connected;
    }


}

