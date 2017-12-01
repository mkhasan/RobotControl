package com.railbot.usrc.robotcontrol;

import android.util.Log;

/**
 * Created by usrc on 16. 12. 19.
 */

public class RailController extends DeviceController {


    final int ANGLE_STEP = 20;

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



    private static final String TAG = RailController.class.getName();

    float minSpeed;
    float maxSpeed;
    float currSpeed;

    private final char frameHead, frameTail;
    private final char checkSum;

    private CurMove curMove;

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
    // stx(0x02) | type (1byte) | cmd (1 byte) | value (4 byte) | checksum (1 byte) | ext(0x03)

    public void SetCurSpeed(float _currSpeed) {
        currSpeed = _currSpeed;
    }

    public float GetCurSpeed() {
        return currSpeed;
    }

    public float GetMaxSpeed() {
        return maxSpeed;
    }

    public float GetMinSpeed() {
        return minSpeed;
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
        msg += '2';  // cmd for up/down movement
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
        msg += '2';  // cmd for up/down movement
        msg += angleStr; // continous move
        msg += checkSum;
        msg += frameTail;

        msgSender.SendMsg(msg, true);

        curMove = CurMove.down;

        Log.e(TAG, "curUpangle: " + curSideAngle + " msg: " + msg);
    }




    private void SetRailSpeed() {
        int value = Math.round(currSpeed*100);    // in 0.1m/sec



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
}
