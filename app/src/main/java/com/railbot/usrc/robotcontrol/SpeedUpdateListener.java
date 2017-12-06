package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 12. 4.
 */

public interface SpeedUpdateListener {
    public void OnUpdateSpeed(float speed);
    public void OnDirChanged(ControlStickListener.MotionDir motionDir);
}
