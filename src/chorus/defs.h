/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */
#ifndef DEFS
#define DEFS            1
#define SOCKET          int
#define INVALID_SOCKET -1
#define SOCKET_ERROR   -1
#define FALSE           0
#define TRUE            1
#define systemError     errno
#define STR_LENGTH 1024
#define STR_ARR_LENGTH 512
#define SEND_FLAG 0 // socket send flag
#define MAX_PROCESS_POOL_COUNT 100
// agent-harness protocol constants
const int ZIP_SEND_REQUEST      =   1000;
const int ZIP_CLEAN_REQUEST     =   1001;
const int CMDSTART_REQUEST      =   1002;
const int CLASSSTART_REQUEST    =   1014;
const int CMDSTATUS_REQUEST     =   1003;
const int CMDCLEAN_REQUEST      =   1004;
const int CMDGETTRACE_REQUEST   =   1005;
const int CMDSTOP_REQUEST       =   1006;
const int GETINFO_OS            =   1007;
const int GETINFO_ARCH          =   1008;
const int GETFILE               =   1009;
const int SENDFILE              =   1010;
const int DELFILE               =   1011;
const int KILLALL               =   1012;
const int CHECKFILE             =   1013;
const int MKDIR                 =   1015;
const int GETCONSOLE            =   1016;
const int CHECKAGENT            =   1017;
const int KILLAGENT             =   1018;
const int GETTRACEPATHS_REQUEST =   1019;
const int DAEMONSTART_REQUEST   =   1020;
const int DAEMONCLEAN_REQUEST   =   1021;

const int RESPONSE_PROCESSING     = 100;
const int RESPONSE_FINISHED_OK    = 101;
const int RESPONSE_FINISHED_ERROR = 102;

const int RUNNING    =  3;
const int NOTRUN     =  2;
const int PASSED     =  0;
const int FAILED     =  1;
const int UNRESOLVED = -9998;

#endif
