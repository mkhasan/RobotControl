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
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatProperty;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
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

import static android.content.RestrictionsManager.RESULT_ERROR;

public class VideoActivity extends Activity implements FFListener {

    private static final String TAG 	 = "VideoActiveity";
    private VideoPlayer mMpegPlayer;
    boolean portraitOrientation;
    protected boolean mPlay = false;
    private boolean stopOnRelease;

    Boolean connected;

    private enum CameraType {
        image,
        thermal,
        both,
        none
    }


    ////////////////////////// newly added ///////////////////////////////
    private SeekBar speedBar;
    public RailController railController;
    private MsgSender msgSender;
    TextView speedView;
    private TextView state;

    ImageButton moveBackwardBtn;
    ImageButton moveForwardBtn;

    VideoView videoView;
    private View surfaceView;

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
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        surfaceView = findViewById(R.id.surface_view);
        mLoadingView = this.findViewById(R.id.loading_view);

        mMpegPlayer = new VideoPlayer((VideoDisplay) surfaceView, this);

        /////////////////////////////  newly added ///////////////////////
        speedBar = (SeekBar) findViewById(R.id.speed_bar);
        moveBackwardBtn = (ImageButton) findViewById(R.id.button_backward);
        moveForwardBtn = (ImageButton) findViewById(R.id.button_forward);

        portraitOrientation = true;
        connected = false;
        stopOnRelease = true;

        String choice = getIntent().getStringExtra("choice");

        Log.e(TAG, "Choice is "+choice);

        if (choice.equals(getString(R.string.motion_only)))
            cameraType = CameraType.none;
        else if (choice.equals(getString(R.string.image_camera)))
            cameraType = CameraType.image;
        else if (choice.equals(getString(R.string.thermal_camera)))
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

        //timerHandler.postDelayed(timerRunnable, 0);

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
        /*
        videoView.setVideoURI(Uri.parse("rtsp://admin:admin@192.168.0.101:554/stream1"));


        //videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();


        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
                mLoadingView.setVisibility(View.GONE);

                Toast.makeText(getApplicationContext(), getString(R.string.connected),
                        Toast.LENGTH_LONG).show();

                connected = true;
            }
        });


*/

        HashMap<String, String> params = new HashMap<String, String>();
        mPlay = false;

        mMpegPlayer.setListener(this);

        if (cameraType == CameraType.none) {

            mLoadingView.setVisibility(View.GONE);
            connected = true;
        }
        else if (cameraType == CameraType.image || cameraType == CameraType.thermal) {
            surfaceView.setVisibility(View.VISIBLE);
            mMpegPlayer.setDataSource(url, params, VideoPlayer.UNKNOWN_STREAM, VideoPlayer.NO_STREAM,
                    VideoPlayer.NO_STREAM);
            String str = Integer.toHexString(mMpegPlayer.NativePlayer()) + " ";
            Log.e(TAG, str);



        }
        else
            connected = false;



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
    }

    public void moveStop(View view) {

        ShowAllert();
        //setResult(-1);
        //finish();
        Log.e(TAG, cameraType == CameraType.image ? "Image" : (cameraType == CameraType.none ? "none" : "error"));
        if (!connected) {

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

        if (!connected) {

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

        if (!connected) {

            Toast.makeText(this, getString(R.string.wait_for_connection), Toast.LENGTH_LONG).show();
            return;
        }

        railController.MoveForward();
        state.setText(getString(R.string.state)+getString(R.string.forward));
        Log.e(TAG, "move front");
    }


    public void moveFrontRelease() {


        if (stopOnRelease) {
            railController.StopMoving();
            state.setText(getString(R.string.state)+getString(R.string.stop));
        }
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "going to stop");
        this.mMpegPlayer.stop();

        setResult(RESULT_OK);

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
    }

    @Override
    public void onFFDataSourceLoaded(FFError err, StreamInfo[] streams) {
        if (err == null) {
            String format = getResources().getString(
                    R.string.main_could_not_open_stream);
            String message = "test";//String.format(format, err.getMessage());

            AlertDialog.Builder builder = new AlertDialog.Builder(VideoActivity.this);
            builder.setTitle(R.string.app_name)
                    .setMessage(message)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    VideoActivity.this.finish();
                                }
                            }).show();
            return;
        }
        //mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
        //mPlayPauseButton.setEnabled(true);
        //this.mControlsView.setVisibility(View.VISIBLE);

        this.mLoadingView.setVisibility(View.GONE);
        connected = true;

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

        timerHandler.removeCallbacks(timerRunnable);

        Log.e(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onStop");
        finish();
    }

    @Override
    public void onFFError() {


        callback();


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


}
