package qat.common;

/**
 * This file contains the constant values used in the simple communication between the harness and the agent.
 * Some of the id's are left over from the initial prototyping of this application, and are no longer used.
 *
 * @author webhiker
 * @version 2.4, 17 June 1999
 */
public abstract class ProtocolConstants {
	
	// test result constants
	public static final int PENDING    =  4;
	public static final int RUNNING    =  3;
	public static final int NOTRUN     =  2;
	public static final int PASSED     =  0;
	public static final int FAILED     =  1;
	public static final int UNRESOLVED = -9998;
	
	// agent-harness protocol constants
	public final static int ZIP_SEND_REQUEST     = 1000;
	public final static int ZIP_CLEAN_REQUEST    = 1001;
	public final static int CMDSTART_REQUEST     = 1002;
	public final static int CLASSSTART_REQUEST   = 1014;
	public final static int CMDSTATUS_REQUEST    = 1003;
	public final static int CMDCLEAN_REQUEST     = 1004;
	public final static int CMDGETTRACE_REQUEST  = 1005;
	public final static int CMDSTOP_REQUEST      = 1006;
	public final static int GETINFO_OS           = 1007;
	public final static int GETINFO_ARCH         = 1008;
	public final static int GETFILE              = 1009;
	public final static int SENDFILE             = 1010;
	public final static int DELFILE              = 1011;
	public final static int KILLALL              = 1012;
	public final static int CHECKFILE            = 1013;
	public final static int MKDIR                = 1015;
	public final static int GETCONSOLE           = 1016;
	public final static int CHECKAGENT           = 1017;
	public final static int KILLAGENT            = 1018;
	public final static int GETTRACEPATHS_REQUEST= 1019;	
	public final static int DAEMONSTART_REQUEST	 = 1020;
	public final static int DAEMONCLEAN_REQUEST	 = 1021;
	
	/**
	 * This indicates the agent is busy processing a request.
	 */	
	public final static int RESPONSE_PROCESSING    = 100;
	/**
	 * This indicates an action request has been completed.
	 */
	public final static int RESPONSE_FINISHED_OK   = 101;
	/**
	 * This indicates an action request has been completed, but an error occured.
	 */
	public final static int RESPONSE_FINISHED_ERROR = 102;
	/**  
	 *the length (in ms) to wait before a socket times out. Zero indicates no timeout is set.
	 */
	public static final int SOCKET_TIMEOUT = 0;
	
}
