//
// Created by usrc on 17. 1. 16.
//

#ifndef ROBOTCONTROL_PROTOCOL_H
#define ROBOTCONTROL_PROTOCOL_H


#include "Def.h"

#define TCP_CTRL_PACKET  0
#define UDP_DATA_PACKET  1
#define BUFSIZE          1440
#define HEADER_SIZE		 12

#define MHPACKETHEADERLEN    12
#define MHPACKETSIZE         (MHPACKETHEADERLEN + BUFSIZE)

#define MAX_COMMAND_ARG      10
#define MAX_COMMAND_LEN      40

typedef struct
{
    DWORD Protocol :  3;   // protocol of the packet (PROTOCOL_CTRL)
    DWORD Size     : 13;   // size of included Data
    DWORD Seed     : 16;   // seed for responding packet
    DWORD Code     : 13;   // command code
    DWORD Last     :  1;   // last packet of some answers
    DWORD From     :  5;   // source object (for interprocess communition only)
    DWORD To       :  5;   // destination object (for interprocess communition only)
    DWORD Reserved :  8;   // reserved
    DWORD Pos;             // position of included data
} __attribute__((packed)) TCP_HEADER ;


typedef struct _TCP_DATA
{
    DWORD Protocol :  3;   // protocol of the packet (PROTOCOL_CTRL)
    DWORD Size     : 13;   // size of included Data
    DWORD Seed     : 16;   // seed for responding packet
    DWORD Code     : 13;   // command code
    DWORD Last     :  1;   // last packet of some answers
    DWORD From     :  5;   // source object (for interprocess communition only)
    DWORD To       :  5;   // destination object (for interprocess communition only)
    DWORD Reserved :  8;   // reserved
    DWORD Pos;             // position of included data
    BYTE  Data[BUFSIZE+4]; // data
} __attribute__((packed)) TCP_DATA;

typedef struct _TCP_LIVE
{
    DWORD protocol :  3;  // type of the packet (LIVE_PACKET)
    DWORD StreamID :  8;  // stream id
    DWORD Type     :  5;  // MHTYPE_VIDEO, MHTYPE_AUDIO, MHTYPE_EVENT, ...
    DWORD Key      :  1;  // 1: key frame, 0: not
    DWORD ExtStamp : 12;  // year-1900(8bit)|month(4bit)
    DWORD CRC      :  3;  // (size+streamid+stamp+extstamp+type+codec+key)%8
    DWORD Stamp;          // day+hour+minute+second+millisecond
    DWORD Size     : 24;  // size of data next to this header
    DWORD Codec    :  8;  // codec id
    DWORD Info1;          // fps*1000 for video, bps for audio, type for event
    DWORD Info2    : 16;  // width for video, channels for audio
    DWORD Info3    : 16;  // height for video, sampling frequency for audio
} __attribute__((packed)) TCP_LIVE;

////////////////////////////////////////////////////////////////////////////

#define COMMAND                   2  // ��� (client <--> server)

// ��Ÿ �Ϲ�, ����, ���� �޽���
#define READY                    30 // DVR ���� ����
#define READY_MSG              "NexReal DVR ready.\r\n"

#define LOGIN_SUCCESS            34 + 1000
#define LOGIN_SUCCESS_MSG      "user logged in"
#define LOGIN_FAILURE            34 + 2000
#define LOGIN_FAILURE_MSG      "login failed. goobye!\r\n"

#define NETFULL                  35 + 2000
#define NETFULL_MSG            "too many users.\r\n"

#define PORT_NOTICE              42
#define PORT_NOTICE_SUCCESS      42 + 1000
#define PORT_NOTICE_SUCCESS_MSG "port command succeeded.\r\n"
#define PORT_NOTICE_FAILURE      42 + 2000
#define PORT_NOTICE_FAILURE_MSG "invalid port number.\r\n"

#define OPEN_FILE                61
#define OPEN_FILE_MSG          "open"
#define OPEN_FILE_SUCCESS        61 + 1000
#define OPEN_FILE_SUCCESS_MSG  "file opened.\r\n"
#define OPEN_FILE_FAIL           61 + 2000
#define OPEN_FILE_FAIL_MSG     "file open failed.\r\n"
#define OPEN_FILE_ALREADY        61 + 3000
#define OPEN_FILE_ALREADY_MSG  "file already opened as ID %d.\r\n"

#define CLOSE_FILE               62
#define CLOSE_FILE_MSG         "close"
#define CLOSE_FILE_SUCCESS       62 + 1000
#define CLOSE_FILE_SUCCESS_MSG "file closed.\r\n"
#define CLOSE_FILE_FAIL          62 + 2000
#define CLOSE_FILE_FAIL_MSG    "file close failed.\r\n"

#define LIVE_STARTED             100 + 1000
#define LIVE_STARTED_MSG        "live id %d started.\r\n"
#define LIVE_STOPPED             100 + 2000
#define LIVE_STOPPED_MSG        "live id %d stopped.\r\n"
#define LIVE_ENABLED             101 + 1000
#define LIVE_ENABLED_MSG        "live enabled.\r\n"
#define LIVE_DISABLED            101 + 2000
#define LIVE_DISABLED_MSG       "live disabled.\r\n"
#define LIVE_RESET               102
#define LIVE_RESET_MSG          "live channel 0x%08X reset.\r\n"
#define LIVE_RESET_SUCCESS       102 + 1000
#define LIVE_RESET_SUCCESS_MSG  "live reset succeeded.\r\n"
#define LIVE_RESET_FAILURE       102 + 2000
#define LIVE_RESET_FAILURE_MSG  "live reset failed.\r\n"

#define QUERYMIC                238
#define QUERYMIC_SUCCESS        238 + 1000
#define QUERYMIC_SUCCESS_MSG   "You can use mic %dbps %dhz %dch\r\n"
#define QUERYMIC_FAIL           238 + 2000
#define QUERYMIC_FAIL_MSG      "You can't use mic\r\n"

#endif //ROBOTCONTROL_PROTOCOL_H
