package com.railbot.usrc.robotcontrol;

import android.util.Log;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

/**
 * Created by usrc on 17. 12. 1.
 */

public class ControlStickListener implements JoystickListener {

    public enum ControlType {
        MOTION,
        PAN,
        TILT
    }

    private class Angle{
        int startAngle;
        int requiredAngle;
        int setAngle;

    }

    Angle pan, tilt;

    private static final String TAG = ControlStickListener.class.getSimpleName();

    private static final float FR_BK_THRESH = (float) 90.0;

    private static final int THRESH = 10;

    private static final float PAN_RANGE = (float) 360.0;
    private static final float TILT_RANGE = (float) 180.0;

    private static final int MAX_PAN = 180;
    private static final int MIN_PAN = -180;

    private static final int MAX_TILT = 10;
    private static final int MIN_TILT = -90;




    private ControlType controlType;

    private RailController railController = null;
    ControlStickListener(ControlType _controlType ) {
        controlType = _controlType;

        if(controlType == ControlType.PAN) {
            pan = new Angle();

        }
        else if(controlType == ControlType.TILT) {
            tilt = new Angle();
        }




    }


    @Override
    public void onDown() {


        if(controlType == ControlType.PAN) {
            pan.setAngle = pan.startAngle = VideoActivity.panAngle;
            Log.e(TAG, "Start angle: " + pan.setAngle);
        }
        else if(controlType == ControlType.TILT) {
            tilt.setAngle = tilt.startAngle = VideoActivity.tiltAngle;
        }


        Log.e(TAG, "onDown()");
    }

    @Override
    public void onDrag(float degrees, float offset) {


        final float LOWER_VALUE = (float) 0.08;
        if(railController != null && offset > LOWER_VALUE) {

            float value = offset * (Math.abs(degrees) > FR_BK_THRESH ? -1 : 1);

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
        }



    }

    @Override
    public void onUp() {
        Log.e(TAG, "onUp()");

    }

    void SetRailController (RailController _railController) {
        railController = _railController;
    }


}


