package com.railbot.usrc.robotcontrol;

import android.os.AsyncTask;
import android.util.Log;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static java.lang.Thread.sleep;

/**
 * Created by usrc on 17. 12. 1.
 */

public class ControlStickListener implements JoystickListener {

    public static final float INITIAL_MAX_SPEED = (float) 0.5;
    public enum ControlType {
        MOTION,
        PAN,
        TILT
    }

    public enum MotionDir {
        NONE,
        FORWARD,
        BACKWARD
    }

    private class Angle{
        int startAngle;
        int requiredAngle;
        int setAngle;

    }

    ConnTask connTask = null;

    Angle pan, tilt;

    private static final String TAG = ControlStickListener.class.getSimpleName();

    private static final float FR_BK_THRESH = (float) 90.0;
    private static final float UP_DOWN_THRESH = (float) 0.0;

    private static final int THRESH = 10;

    private static final float PAN_RANGE = (float) 360.0;
    private static final float TILT_RANGE = (float) 200.0;

    public static final int MAX_PAN = 180;
    public static final int MIN_PAN = -180;

    public static final int MAX_TILT = 10;
    public static final int MIN_TILT = -90;

    private static final float DELTA_SPD = (float) 0.2;

    private MotionDir motionDir = MotionDir.NONE;
    private MotionDir prevDir = MotionDir.NONE;

    private float prevSpdOffset = (float) 0.0;

    private ControlType controlType;

    private RailController railController = null;

    private float maxSpeed = INITIAL_MAX_SPEED;

    ControlStickListener(ControlType _controlType ) {
        controlType = _controlType;

        if(controlType == ControlType.PAN) {
            pan = new Angle();

        }
        else if(controlType == ControlType.TILT) {
            tilt = new Angle();
        }
        else if(controlType == ControlType.MOTION) {
            //connTask = new ConnTask();
            //connTask.execute();
        }




    }

    private SpeedUpdateListener speedUpdateListener = null;


    @Override
    public void onDown() {


        if(controlType == ControlType.PAN) {
            pan.setAngle = pan.startAngle = VideoActivity.panAngle;
            Log.e(TAG, "Pan Start angle: " + pan.setAngle);
        }
        else if(controlType == ControlType.TILT) {
            tilt.setAngle = tilt.startAngle = VideoActivity.tiltAngle;
            Log.e(TAG, "Tilt Start angle: " + tilt.setAngle);
        }


        Log.e(TAG, "onDown()");
    }

    @Override
    public void onDrag(float degrees, float offset) {


        final float LOWER_VALUE = (float) 0.08;
        if(railController != null && offset > LOWER_VALUE) {

            float value;
            if(controlType == ControlType.TILT)
                value = offset * (degrees > UP_DOWN_THRESH ? 1 : -1);
            else
                value = offset * (Math.abs(degrees) > FR_BK_THRESH ? -1 : 1);


            if(controlType == ControlType.PAN) {
                int target = (int)((float) pan.startAngle+value*PAN_RANGE/2);
                if(target > MAX_PAN)
                    target = MAX_PAN;
                if(target < MIN_PAN)
                    target = MIN_PAN;

                if(Math.abs(target-pan.setAngle) > THRESH) {

                    Log.e(TAG, "Value is: " + value);
                    Log.e(TAG, "Degree " + degrees + " Offset " + offset);
                    Log.e(TAG, "setting target " + target);
                    pan.setAngle = target;
                    railController.CameraPan(target);
                }
            }

            else if(controlType == ControlType.TILT) {

                int target = (int)((float) tilt.startAngle+value*TILT_RANGE/2);
                Log.e(TAG, "in Tilt mode " + target + " deg " + degrees + " offset " + offset);
                if(target > MAX_TILT)
                    target = MAX_TILT;
                if(target < MIN_TILT)
                    target = MIN_TILT;

                if(Math.abs(target-tilt.setAngle) > THRESH) {

                    Log.e(TAG, "Value is: " + value);
                    Log.e(TAG, "Degree " + degrees + " Offset " + offset);
                    Log.e(TAG, "setting target " + target);
                    tilt.setAngle = target;
                    railController.CameraTilt(target);
                }
            }
            else if(controlType == ControlType.MOTION) {

                Log.e(TAG, "deg " + degrees);

                if(Math.abs(degrees) > FR_BK_THRESH)
                    motionDir = MotionDir.BACKWARD;
                else
                    motionDir = MotionDir.FORWARD;

                if(Math.abs(offset-prevSpdOffset) > DELTA_SPD ) {
                    float speed = offset * maxSpeed;
                    if (speedUpdateListener != null) {
                        speedUpdateListener.OnUpdateSpeed(speed);
                        if (prevDir != motionDir) {
                            speedUpdateListener.OnDirChanged(motionDir);
                            prevDir = motionDir;
                        }

                    }
                    if (railController != null) {
                        if (motionDir == MotionDir.FORWARD) {
                            railController.MoveForward();

                        } else if (motionDir == MotionDir.BACKWARD) {
                            railController.MoveBackward();
                        }
                    }

                    prevSpdOffset = offset;
                }


            }


        }



    }

    @Override
    public void onUp() {
        Log.e(TAG, "onUp()");

        if(controlType == ControlType.MOTION) {
            motionDir = MotionDir.NONE;
            if(speedUpdateListener != null){
                speedUpdateListener.OnUpdateSpeed((float) 0.0);
                speedUpdateListener.OnDirChanged(motionDir);
            }
            railController.StopMoving();
            prevSpdOffset = (float) 0.0;
        }


    }

    public void SetRailController (RailController _railController) {
        railController = _railController;
    }

    public void SetListener(SpeedUpdateListener _speedUpdateListener) {
        speedUpdateListener = _speedUpdateListener;
    }


    public void SetMaxSpeed(float _maxSpeed) {
        maxSpeed = _maxSpeed;
    }
    void TerminateConnTask() {
        if(connTask != null)
            connTask.finished = true;
    }

    private class ConnTask extends AsyncTask<Void, Integer, Long> {

        public boolean finished = false;

        protected Long doInBackground(Void... voids) {




            while(finished == false) {


                try {
                    sleep(500);

                    if(motionDir == MotionDir.NONE) {


                        Log.e(TAG, "in Conn task Stop");
                    }
                    else if(motionDir == MotionDir.FORWARD) {
                        railController.MoveForward();
                        Log.e(TAG, "in Conn task Forward");
                    }
                    else {
                        railController.MoveBackward();
                        Log.e(TAG, "in Conn task Backward");
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }






            return (long)0;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {
            Log.e(TAG, "Conn finished");
        }
    }




}


