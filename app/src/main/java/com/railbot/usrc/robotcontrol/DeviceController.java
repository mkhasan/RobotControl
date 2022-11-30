package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 16. 12. 19.
 */

public class DeviceController {

    MsgSender msgSender;
    public DeviceController(MsgSender _msgSender) {
        this.msgSender = _msgSender;

    }

    boolean SendMsg(String str) {
        return true;//msgSender.SendUDP_Msg(str);
    }

}
