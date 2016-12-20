package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 16. 12. 19.
 */

public class MsgSender {
    int UDP_port;
    int TCP_port;
    String destAddr;

    public MsgSender(String _destAddr, int _TCP_port, int _UDP_port) {

        UDP_port = _UDP_port;
        TCP_port = _TCP_port;
        destAddr = _destAddr;

    }

    boolean SendUDP_Msg(String smg) {
        return true;

    }
}
