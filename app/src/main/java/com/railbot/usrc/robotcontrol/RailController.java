package com.railbot.usrc.robotcontrol;

import android.util.Log;

/**
 * Created by usrc on 16. 12. 19.
 */

public class RailController extends DeviceController {


    final int ANGLE_STEP = 20;

    final static int MIN_SPEED_IN_CM = 10;
    final static int MAX_SPEED_IN_CM = 30;

    public static final int MAX_DISTANCE = 10;


    public enum CurMove {
        stop,
        forward,
        backward,
        up,
        down,
        left,
        right

    }

    int curSideAngle = 0;
    int curUpAngle = -30;

    private int distance = 1;



    private static final String TAG = RailController.class.getName();

    float minSpeed;
    float maxSpeed;
    float currSpeed;

    int maxSpeedInCm;
    int minSpeedInCm;
    int currSpeedInCm;

    private final char frameHead, frameTail;
    private final char checkSum;

    private CurMove curMove;

    /*
    RailController(MsgSender _msgSender, float _minSpeed, float _maxSpeed) {
        super(_msgSender);
        minSpeed = _minSpeed;
        maxSpeed = _maxSpeed;
        currSpeed = _minSpeed;
        curMove = CurMove.stop;

        frameHead = 0x02;
        frameTail = 0x03;
        checkSum = '0';     // arbitrary

    }



    */

    RailController(MsgSender _msgSender, int _minSpeedInCm, int _maxSpeedInCm) {
        super(_msgSender);
        minSpeedInCm = _minSpeedInCm;
        maxSpeedInCm = _maxSpeedInCm;
        SetCurSpeed(_minSpeedInCm);
        curMove = CurMove.stop;

        frameHead = 0x02;
        frameTail = 0x03;
        checkSum = '0';     // arbitrary

    }

    // stx(0x02) | type (1byte) | cmd (1 byte) | value (4 byte) | checksum (1 byte) | ext(0x03)

    public void SetCurSpeed(int _currSpeedInCm) {

        if(currSpeedInCm > MAX_SPEED_IN_CM)
            return;

        currSpeedInCm = _currSpeedInCm;
        Log.e(TAG, "current speeed is " + currSpeedInCm);
    }

    public int GetCurSpeed() {
        return currSpeedInCm;
    }

    public int GetMaxSpeed() {
        return maxSpeedInCm;
    }

    public int GetMinSpeed() {
        return minSpeedInCm;
    }

    void MoveForward() {
        SetRailSpeed();

        String msg;
        msg = "" + frameHead;
        msg += '1';  // type
        msg += '3';  // cmd for continuous move forward
        msg += "0000"; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.forward;

        Log.e(TAG, msg);
    }

    void MoveForward(int distInCm) {
        SetRailSpeed();

        String dist = String.format("%04d", distInCm/10);

        String msg;
        msg = "" + frameHead;
        msg += '1';  // type
        msg += '1';  // cmd for continuous move forward
        msg += dist; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.forward;

        Log.e(TAG, msg);
    }

    void MoveBackward() {

        SetRailSpeed();

        String msg;
        msg = "" + frameHead;
        msg += '1';  // type
        msg += '4';  // cmd for continuous move forward
        msg += "0000"; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.backward;

        Log.e(TAG, msg);

    }

    void MoveBackward(int distInCm) {

        SetRailSpeed();

        String dist = String.format("%04d", distInCm/10);

        String msg;
        msg = "" + frameHead;
        msg += '1';  // type
        msg += '2';  // cmd for continuous move forward
        msg += dist; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.backward;

        Log.e(TAG, msg);

    }




    void StopMoving(int nCout) {

        curMove = CurMove.stop;
        String msg;
        msg = "" + frameHead;
        msg += '1';     // type
        msg += '0';     //
        msg += "0000";  // value
        msg += checkSum;    // checksum
        msg += frameTail;

        Log.e(TAG, msg);

        int i;
        for (i=0; i<nCout; i++) {
            if (i==0)
                msgSender.SendMsg(msg);
            else
                msgSender.SendMsg(msg, true);
        }

    }

    void StopMoving() {

        StopMoving(1);
    }


    void CameraUp() {

        Log.e(TAG, "Sending msg");

        curUpAngle += ANGLE_STEP;
        String angleStr = String.format("%04d", curUpAngle);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '1';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.up;

        Log.e(TAG, "curUpangle: " + curUpAngle + " msg: " + msg);
    }

    void CameraDown() {

        Log.e(TAG, "Sending msg");

        curUpAngle -= ANGLE_STEP;
        String angleStr = String.format("%04d", curUpAngle);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '1';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.down;

        Log.e(TAG, "curUpangle: " + curUpAngle + " msg: " + msg);
    }


    void CameraLeft() {

        Log.e(TAG, "Sending msg");

        curSideAngle += ANGLE_STEP;
        String angleStr = String.format("%04d", curSideAngle);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '2';  // cmd for left/right movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.left;

        Log.e(TAG, "curUpangle: " + curSideAngle + " msg: " + msg);
    }


     void CameraRight() {

        Log.e(TAG, "Sending msg");

        curSideAngle -= ANGLE_STEP;
        String angleStr = String.format("%04d", curSideAngle);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '2';  // cmd for left/right movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.down;

        Log.e(TAG, "curUpangle: " + curSideAngle + " msg: " + msg);
    }


    void Calibrate() {

        Log.e(TAG, "Sending msg");


        String angleStr = "0000";

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '1';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        angleStr = "0000";
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '2';  // cmd for left/right movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        VideoActivity.panAngle = 0;
        VideoActivity.tiltAngle = 0;

    }

    void CameraPan(int angle) {

        Log.e(TAG, "Sending msg");


        String angleStr = String.format("%04d", angle*4);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '2';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        VideoActivity.panAngle = angle;


        Log.e(TAG, "angle str: " + angleStr + " msg: " + msg);
    }


    void CameraTilt(int angle) {

        Log.e(TAG, "Sending msg");


        String angleStr = String.format("%04d", angle*4);

        String msg;
        msg = "" + frameHead;
        msg += '3';  // type for camera mode
        msg += '1';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);



        VideoActivity.tiltAngle = angle;

        Log.e(TAG, "angle str: " + angleStr + " msg: " + msg);


    }



    private void SetRailSpeed() {
        int value = currSpeedInCm;



        String valueStr = String.format("%04d", value);

        String msg;
        msg = "" + frameHead;
        msg += '2';  // type
        msg += '0';  // cmd
        msg += valueStr;
        msg += checkSum;
        msg += frameTail;

        Log.e(TAG, "speed:" + valueStr);

        msgSender.SendMsg(msg);
    }



    CurMove GetCurMove() {
        return curMove;
    }

    int getDistance() {
        return distance;
    }

    void setDistance(int _distance) {
        distance = _distance;
    }
}
