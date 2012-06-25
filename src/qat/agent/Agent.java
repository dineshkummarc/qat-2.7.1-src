/**
 * Ths is the Agent class, responsible for launching/managing all the started processes on a machine.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
package qat.agent;

// thse are the qa_tester imports
import qat.common.Common;
import qat.common.ConsoleServer;
import qat.common.ProtocolConstants;
import qat.agent.AgentThread;

// thse are the standard Java imports
import java.net.*;
import java.io.*;
import java.util.*;
/**
 * The agent is responsible for interfacing with the QA Test harness, and recieving, executing, cleaning etc. of
 * tests for each request sent by the harness. Multiple requests may run simultaneously.
 */
public class Agent {
	
	private Vector processPool; // this contains a link to all started tests
	private Vector daemonPool; // this contains a link to all daemons
	private int portNo; // the port number to start the agent on
	private boolean showOutput; // whether the output is written to stdout or not
	private boolean virtualFileSystem; // whether we use a virtual file system or not
	private Socket socket; 
	
	/** This constructs an agent which will listen to harness requests on the specified port.
	 * @param args the port number on which to listen for harness messages..
	 */
	public Agent(String args[]) {
		processArgs(args);
	    processPool = new Vector();
	    daemonPool = new Vector();
		try {
			ConsoleServer.debugMsg("Agent Version " + Common.VERSION,1);
			ServerSocket server = new ServerSocket(portNo);
			DataInputStream  inStream;
			DataOutputStream outStream;
			
			startDiscoveryService(portNo);
			
			while (true) {
				try {
					ConsoleServer.debugMsg("======================================================================",1);
					ConsoleServer.debugMsg("Listening for connections on port " + server.getLocalPort(),1);
					socket = server.accept();
					socket.setSoTimeout(ProtocolConstants.SOCKET_TIMEOUT);
					ConsoleServer.debugMsg("Connection established with " + socket.getInetAddress().getHostName(),1);
					new AgentThread( processPool, daemonPool, showOutput,  portNo, socket).start();					
				}
				catch (Exception e) {
					ConsoleServer.debugMsg("Error establishing socket connection:"+e.getMessage(),2);
				}
			}
		}
		catch (Exception e) {
			ConsoleServer.debugMsg("Error starting server:"+e.getMessage(),0);
		}
	}
	
	private void processArgs(String args[]) {
		if (args.length==0) {
			useage();
		}
		try {
			// get the port number
			portNo = (new Integer(args[0])).intValue();
			// get the debug level
			ConsoleServer.setDebugLevel(getDebugLevel(args));
			// determine if we are in quiet mode or not
			showOutput = (ConsoleServer.getDebugLevel()>0);
		}
		catch (Exception e) {
			System.out.println(e);
			useage();
		}
	}
	
	private void startDiscoveryService(int agentPort) {
		DiscoveryResponder r = new DiscoveryResponder(Common.MultiCastGroup,
													  Common.MultiCastPort,
													  agentPort);
		r.start();
	}
	
	private int getDebugLevel(String args[]) {
		try {
			if (args.length<=1)
				return 0;
			else
				return (new Integer(args[1])).intValue();
		}
		catch (NumberFormatException n) {
			try {
				return (new Integer(args[1])).intValue();
			}
			catch (Exception e) {
				return 0;
			}
		}
	}
	
	private boolean getVirtualFileSystem(String args[]) {
		if (args.length<=1)
			return false;
		else
			for (int i = 0; i < args.length; i++) {
				if (args[i].toLowerCase().indexOf("virtual")>=0)
					return true;
			}
		return false;
	}
	
	private void useage() {
		System.out.println("QAT Agent Version "+Common.VERSION);
		System.out.println("        Useage: java Agent port [debug_level(0..9)]");
		System.out.println("          port        : The port to listen for requests");
		System.out.println("          debug_level : The detail of debug messages written to stdoutput, default 0(none)");
		System.exit(0);
	}	
}
