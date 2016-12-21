package com.railbot.usrc.robotcontrol;

import android.util.Log;

/**
 * Created by usrc on 16. 12. 19.
 */

public class RailController extends DeviceController {

    private static final String TAG = "RailController";

    float minSpeed;
    float maxSpeed;
    float currSpeed;

    private final char frameHead, frameTail;
    private final char checkSum;

    RailController(MsgSender _msgSender, float _minSpeed, float _maxSpeed) {
        super(_msgSender);
        minSpeed = _minSpeed;
        maxSpeed = _maxSpeed;
        currSpeed = _minSpeed;

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

        Log.e(TAG, msg);

    }


    void StopMoving() {

        String msg;
        msg = "" + frameHead;
        msg += '1';     // type
        msg += '0';     //
        msg += "0000";  // value
        msg += checkSum;    // checksum
        msg += frameTail;

        Log.e(TAG, msg);

        msgSender.SendMsg(msg);

    }

    private void SetRailSpeed() {
        int value = Math.round(currSpeed*10);    // in 0.1m/sec



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


}
