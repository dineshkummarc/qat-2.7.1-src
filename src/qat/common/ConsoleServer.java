package qat.common;

import java.net.*;
import java.io.*;
import java.util.*;

import qat.common.ProtocolConstants;

public class ConsoleServer extends Thread {
  
	private DataOutputStream clientOutput;
	private DataInputStream clientInput;
	private Socket clientSocket;
	private int clientDebugLevel;
  
	private static Vector consoleClientList;
	private static int debugLevel; // this is the level of detail we will use for debug message. 0 = none, 10 = max
  
	static {
		consoleClientList = new Vector();
	}
  
	private ConsoleServer(DataInputStream in, DataOutputStream out, Socket socket, int debugLevel) {
		clientInput = in;
		clientOutput = out;
		clientSocket = socket;
		clientDebugLevel = debugLevel;
		// if the client doesn't respond in 5 seconds, we assume it's disconnected
		// and terminate it's connection
		try {
			socket.setSoTimeout(5000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
  
	private synchronized void clientDebugMsg(String msg, int level) throws Exception {
		if (level<=clientDebugLevel) {
			// write the output to this client
			clientOutput.writeUTF(msg+"\n");
			clientOutput.flush();
	  
			// read a byte to verify client is still active
			int code = clientInput.readInt();
			if (code != ProtocolConstants.RESPONSE_FINISHED_OK) {
				throw new Exception("Client not responding");
			}	
		}
	}
  
	public synchronized static void debugMsg(String msg) {
		debugMsg(msg,getDebugLevel());
	}
	
	public synchronized static void debugMsg(String msg,int level) {
		if (level<=getDebugLevel()) {
			System.out.println(msg);
		}
		ConsoleServer consoleClient;
		for (int i = 0; i < consoleClientList.size(); i++) {
			try {
				consoleClient = (ConsoleServer)consoleClientList.get(i);
				consoleClient.clientDebugMsg(msg,level);
			}
			catch (Throwable e) {
				if (getDebugLevel()>3) {
					System.out.println("Console client seems to have died :"+e.toString());
				}
				removeClient(i);
			}
		}
	}
	
	public synchronized static void debugStackTrace(Throwable ex) {
		StringWriter stringWriter = new StringWriter();
		ex.printStackTrace(new PrintWriter(stringWriter));
		debugMsg(stringWriter.toString());
		try {
			stringWriter.close();
		}
		catch (IOException err) {
		}
	}
  
	public synchronized static void setDebugLevel(int l) {
		debugLevel = l;
	}
  
	public synchronized static int getDebugLevel() {
		return debugLevel;
	}
  
	public synchronized static void removeClient(int i) {
		ConsoleServer consoleServer = (ConsoleServer)consoleClientList.get(i);	
		try {	  
			consoleServer.clientOutput.close();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		try {	  
			consoleServer.clientInput.close();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		try {	  
			consoleServer.clientSocket.close();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		consoleClientList.remove(i);
	}
  
	public synchronized static void addClient(DataInputStream in, DataOutputStream out, Socket socket, int clientDebugLevel) {
		try {
			socket.setSoTimeout(1000);
		}
		catch (java.net.SocketException e) {
			e.printStackTrace();
		}
		consoleClientList.add(new ConsoleServer(in,out,socket,clientDebugLevel));
		debugMsg("There are now "+consoleClientList.size()+" client(s) registered on the console",3);
	}
  
	public synchronized static PrintStream getConsoleOutput() {
		return System.out;
	}
}
