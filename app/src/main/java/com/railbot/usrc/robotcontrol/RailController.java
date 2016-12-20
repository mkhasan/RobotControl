package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 16. 12. 19.
 */

public class RailController extends DeviceController {

    float minSpeed;
    float maxSpeed;
    float currSpeed;
    RailController(MsgSender _msgSender, float _minSpeed, float _maxSpeed) {
        super(_msgSender);
        minSpeed = _minSpeed;
        maxSpeed = _maxSpeed;
        currSpeed = _minSpeed;
    }

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

}
