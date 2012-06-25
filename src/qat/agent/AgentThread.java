/**
 * Ths is the AgentThread class, a thread responsible for each request of the harness.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
package qat.agent;

// @(#)AgentThread.java 1.26 00/07/20 

// java import
import java.lang.*;
import java.io.*;
import java.util.*;
import java.net.*;
//

// thse are the qa_tester imports
import qat.common.Common;
import qat.common.ProtocolConstants;
import qat.common.TestObject;
import qat.common.Utils;
import qat.common.Utils;
import qat.common.ConsoleServer;
import qat.agent.ExecProcess;
//

public class AgentThread extends Thread {
	
    private Vector processPool; // this contains a link to all tests which have been started
    private Vector daemonPool; // this contains a link to all daemons which have been started
    private DataInputStream  inStream;
    private DataOutputStream outStream;
    private boolean showOutput; // whether the output is written to stdout or not
    private int portNo; // the port number to start the agent on
    private Socket socket;	

    public AgentThread (Vector processPool, Vector daemonPool, boolean showOutput, int portNo, Socket socket) throws IOException {
	this.processPool = processPool;
	this.daemonPool = daemonPool;
	this.showOutput = showOutput;
	this.portNo = portNo;
	this.socket = socket;
	inStream = new DataInputStream(socket.getInputStream());
	outStream = new DataOutputStream(socket.getOutputStream());
    }
	
    public void run() {
	processRequest(inStream,outStream);
    }
	
    /**
     * This method decides what action to take based on the value read from the socket connected
     * to the harness.
     */
    private void processRequest(DataInputStream in, DataOutputStream out) {
	int requestCode = 0;
	try {
	    requestCode = in.readInt();
	    switch (requestCode) {
	    case ProtocolConstants.GETINFO_OS :
		processGETINFO_OS(in, out);
		break;
	    case ProtocolConstants.GETINFO_ARCH : 
		processGETINFO_ARCH(in, out);
		break;
	    case ProtocolConstants.ZIP_SEND_REQUEST :
		processZIP_SEND_REQUEST(in, out);
		break;
	    case ProtocolConstants.ZIP_CLEAN_REQUEST : 
		processZIP_CLEAN_REQUEST(in, out);
		break;
	    case ProtocolConstants.DAEMONSTART_REQUEST : 
		processDAEMONSTART_REQUEST(in, out);
		break;
	    case ProtocolConstants.DAEMONCLEAN_REQUEST : 
		processDAEMONCLEAN_REQUEST(in, out);
		break;
	    case ProtocolConstants.CMDSTART_REQUEST : 
		processCMDSTART_REQUEST(in, out);
		break;
	    case ProtocolConstants.CMDSTOP_REQUEST : 
		processCMDSTOP_REQUEST(in, out);
		break;
	    case ProtocolConstants.CMDSTATUS_REQUEST : 
		processCMDSTATUS_REQUEST(in, out);
		break;
	    case ProtocolConstants.GETTRACEPATHS_REQUEST : 
		processGETTRACEPATHS_REQUEST(in, out);
		break;
	    case ProtocolConstants.CMDCLEAN_REQUEST : 
		processCMDCLEAN_REQUEST(in, out);
		break;
	    case ProtocolConstants.CMDGETTRACE_REQUEST : 
		processCMDGETTRACE_REQUEST(in, out);
		break;
	    case ProtocolConstants.GETFILE : 
		processGETFILE(in, out);
		break;
	    case ProtocolConstants.SENDFILE : 
		processSENDFILE(in, out);
		break;
	    case ProtocolConstants.CHECKFILE : 
		processCHECKFILE(in, out);
		break;
	    case ProtocolConstants.DELFILE :
		processDELFILE(in, out);
		break;
	    case ProtocolConstants.MKDIR : 
		processMKDIR(in, out);
		break;
	    case ProtocolConstants.KILLALL : 
		processKILLALL(in, out);
		break;
	    case ProtocolConstants.GETCONSOLE : 
		processGETCONSOLE(in, out);
		break;
	    case ProtocolConstants.CHECKAGENT : 
		processCHECKAGENT(in, out);
		break;
	    case ProtocolConstants.KILLAGENT : 
		processKILLAGENT(in, out);
		break;
	    default : 
		ConsoleServer.debugMsg("Recieved unknown action code:"+requestCode,0);
	    }
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error reading request code:"+e.getMessage(),0);
	}
	finally {
	    closeRequest(requestCode);
	    ConsoleServer.debugMsg("======================================================================",1);
	}
    }
  
    private void closeRequest(int requestCode) {
	if (requestCode != ProtocolConstants.GETCONSOLE) {
	    ConsoleServer.debugMsg("Closing input and output streams",1);
	    try {
		inStream.close();
		outStream.flush();
		outStream.close();
		socket.close();
	    }
	    catch (Exception e) {
		ConsoleServer.debugMsg("Problem closing sockets and streams :"+e.toString(),1);				
	    }
	}
    }
  
    /**
     * Sends a single int code on the connected socket.
     */
    private void sendSignal(DataOutputStream out, int code) {
	try {
	    out.writeInt(code);
	    out.flush();
	}
	catch (IOException e) {
	    ConsoleServer.debugMsg("Error trying to send signal :"+code+" ("+e.toString()+")",1);
	}
    }
	
    /**
     * This method is responsible for recieiving a zip file from the harness.
     * This method may not be used if running in a fileless system, although
     * no errors will be thrown intentionally due to it's useage.
     */
    private void processZIP_SEND_REQUEST(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing ZIP_SEND_REQUEST",1);
	    // read the work directory to use
	    String workDir = in.readUTF();
	    // now read the associated zip file, if it exists
	    String zipFileName = in.readUTF();
	    String fullZipFilePath = workDir+File.separator+zipFileName;
	    // create the zip file, test directory and it's parents if they don't exist
	    Utils.touch(fullZipFilePath);			
	    long counter = 0;
	    long size = in.readLong();
 			
	    BufferedOutputStream zipFileStream = new BufferedOutputStream(new FileOutputStream(fullZipFilePath));
	    BufferedInputStream inBuff = new BufferedInputStream(in);
	    byte buff[] = new byte[1024];
	    int bytesRead = 0;
	    ConsoleServer.debugMsg("Attempting to read :"+size+" bytes blocks from the socket connection into "+fullZipFilePath,5);
	    while (counter < size) {
		bytesRead = inBuff.read(buff,0,((size<buff.length) ? (int)size : buff.length));
		if (bytesRead>0)
		    zipFileStream.write(buff,0,bytesRead);
		counter+= bytesRead;
	    }
	    ConsoleServer.debugMsg("Read :"+counter+" bytes",5);
	    zipFileStream.flush();
	    zipFileStream.close();
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // unzip the zip file now
	    ConsoleServer.debugMsg("Unzipping  the file :"+fullZipFilePath,5);
	    Utils.unzipFiles(fullZipFilePath,workDir);
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing ZIP_SEND_REQUEST :"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * This method is responsible for cleaning a zip file previously sent to the agent.
     */
    private void processZIP_CLEAN_REQUEST(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing ZIP_CLEAN_REQUEST",1);
	    // read the work directory to use
	    String workDir = in.readUTF();
	    // now read the associated zip file name
	    String zipFileName = in.readUTF();
	    String fullZipFilePath = workDir+File.separator+zipFileName;
			
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // delete the unzipped files now
	    ConsoleServer.debugMsg("Cleaning up the file :"+fullZipFilePath,5);
	    try {
		Utils.cleanUnzippedFiles(fullZipFilePath,workDir);
	    }
	    catch (FileNotFoundException ex) {
		ConsoleServer.debugMsg("Could not delete contents of "+fullZipFilePath+" :"+ex.getMessage(),1);
	    }
	    // delete the zip file itself
	    try {
		Utils.delete(fullZipFilePath);
	    }
	    catch (IOException ex) {
		ConsoleServer.debugMsg("Could not delete "+fullZipFilePath+" :"+ex.getMessage(),1);
	    }			
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing ZIP_CLEAN_REQUEST :"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * This method is responsible for executing a daemon on the agent.
     */
    private void processDAEMONSTART_REQUEST(DataInputStream in, DataOutputStream out) {
	TestObject test=new TestObject();
	String eol = System.getProperty("line.separator");
	try {
	    ConsoleServer.debugMsg("Processing DAEMONSTART_REQUEST",1);
	    // read the serialized TestObject which we will execute
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // create details about our agent to use in the trace file created by the ExecProcess object
	    String details = "Local Address : "+socket.getLocalAddress().toString()+eol+
		"Port Number : "+portNo+eol+
		"Architecture : "+System.getProperty("os.arch")+eol+
		"Operating system : "+System.getProperty("os.name")+" "+System.getProperty("os.version");
	    // execute the test
	    ExecProcess process = new ExecProcess(test,
						  ConsoleServer.getConsoleOutput(), 
						  ConsoleServer.getDebugLevel(),
						  showOutput,details);
	    process.setDaemon(true);
	    process.start();
	    // add to our list of daemons
	    daemonPool.add(process);
			
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing daemon start request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	    test = null;
	}
    }
	
    /**
     * This method is resonsible for cleaning all files created by running a daemon.
     */
    private void processDAEMONCLEAN_REQUEST(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing DAEMONCLEAN_REQUEST",1);
			
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    ExecProcess daemon;
	    while (daemonPool.size()>0) {
		ConsoleServer.debugMsg("Cleaning 1 of "+daemonPool.size()+" daemons",5);
		try {
		    daemon = (ExecProcess)daemonPool.get(0);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    daemon = (ExecProcess)daemonPool.elementAt(0);
		}
				
		try {
		    ConsoleServer.debugMsg("Interrupting daemon",5);
		    daemon.interrupt();
		}
		catch (Throwable ex) {
		}
		// delete it from our list
		try {
		    daemonPool.remove(0);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    daemonPool.removeElementAt(0);
		}
	    }
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing DAEMONCLEAN_REQUEST request:"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	}
    }
	
    /**
     * This method is responsible for executing a command on the agent.
     */
    private void processCMDSTART_REQUEST(DataInputStream in, DataOutputStream out) {
	TestObject test=new TestObject();
	String eol = System.getProperty("line.separator");
	try {
	    ConsoleServer.debugMsg("Processing CMDSTART_REQUEST",1);
	    // read the serialized TestObject which we will execute
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // create details about our agent to use in the trace file created by the ExecProcess object
	    String details = "Local Address : "+socket.getLocalAddress().toString()+eol+
		"Port Number : "+portNo+eol+
		"Architecture : "+System.getProperty("os.arch")+eol+
		"Operating system : "+System.getProperty("os.name")+" "+System.getProperty("os.version");
	    // execute the test
	    ExecProcess process = new ExecProcess(test,
						  ConsoleServer.getConsoleOutput(), 
						  ConsoleServer.getDebugLevel(),
						  showOutput,details);
	    try {
		processPool.add(process);
	    }
	    catch (java.lang.NoSuchMethodError ex) {
		// doesn't exist in jdk1.1.x
		processPool.addElement(process);
	    }
	    process.start();
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing TestObject execute request:"+e.getMessage(),1);
	    //	    e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	    test = null;
	}
    }
	
    /**
     * This method is resonsible for stopping a previously started command on the agent.
     * If the process has finished, it will return the exit code, else it will return 
     * a negative value if the process had to be killed.
     */
    private int processCMDSTOP_REQUEST(DataInputStream in, DataOutputStream out) {
	TestObject test=new TestObject();
	int status=0;
	try {
	    ConsoleServer.debugMsg("Processing CMDSTOP_REQUEST",1);
	    // read the serialized TestObject which we will stop
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // find the exit code, blocking if neccesary
	    ExecProcess proc;
	    boolean wasRunning = false;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		// check for any instance of test running with the same id
		if (proc.getTestObject().getTestID().equals(test.getTestID())) {
		    ConsoleServer.debugMsg("Stopping process ",5);
		    wasRunning = true;
					
		    // flush and close it's output streams
		    proc.interrupt();
					
		    // get it's status if it has finished
		    status = proc.checkExitValue();
					
		    sendSignal(out,status);
		    break;
		}
	    }
	    if (!wasRunning) {
		// the process was never started, so assume it failed
		ConsoleServer.debugMsg("Process was not running :"+test.getTestID(),1);			
	    }
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CMDSTOP_REQUEST request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	    test = null;
	    return status;
	}		
    }
	
    /**
     * This method is resonsible for retrieving the status of a previously executed processes on the agent.
     * It will block until the process exits, or the timeout value specified in the CMDSTART method has been
     * reached.
     */
    private void processCMDSTATUS_REQUEST(DataInputStream in, DataOutputStream out) {
	TestObject test=new TestObject();
	try {
	    ConsoleServer.debugMsg("Processing CMDSTATUS_REQUEST",1);
	    // read the serialized TestObject that we want the exit code of
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // find the exit code, blocking if neccesary
	    ExecProcess proc;
	    boolean wasRunning = false;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		// check for any instance of test running with the same name
		if (proc.getTestObject().getTestID().equals(test.getTestID())) {
		    ConsoleServer.debugMsg("Returning CMDSTATUS value :"+proc.getExitValue(),5);
		    out.writeInt(proc.getExitValue());
		    wasRunning = true;
		    // flush and close it's output streams
		    proc.interrupt();
		    // don't remove until it's cleaned :processPool.removeElementAt(i);
		    break;
		}
	    }
	    if (!wasRunning) {
		// the process was never started, so assume it failed
		ConsoleServer.debugMsg("Process was never started :"+test.getTestID(),1);			
		out.writeInt(ProtocolConstants.FAILED);
	    }
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CMDSTATUS_REQUEST request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	    test = null;
	}
    }
	
    /**
     * This method is resonsible for cleaning all files created by running a process.
     */
    private void processCMDCLEAN_REQUEST(DataInputStream in, DataOutputStream out) {
	TestObject test=new TestObject();
	try {
	    ConsoleServer.debugMsg("Processing CMDCLEAN_REQUEST",1);
	    // read the serialized TestObject which we want the exit code of
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // find the exit code, blocking if neccesary
	    ExecProcess proc;
	    boolean exists = false;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		// check for any instance of test running with the same name
		if (proc.getTestObject().getTestID().equals(test.getTestID())) {
		    try {
			ConsoleServer.debugMsg("Deleting :"+proc.getTestObject().getEnvFileName(),5);	
			Utils.delete(proc.getTestObject().getEnvFileName());
		    }
		    catch (IOException e) {
			ConsoleServer.debugMsg("No environment file was found :"+e,1);			
		    }
		    try {
			ConsoleServer.debugMsg("Deleting :"+proc.getTestObject().getStdOutFileName(),5);	
			Utils.delete(proc.getTestObject().getStdOutFileName());
		    }
		    catch (IOException e) {
			ConsoleServer.debugMsg("No stdout file was found :"+e,1);			
		    }
		    try {
			ConsoleServer.debugMsg("Deleting :"+proc.getTestObject().getStdErrFileName(),5);	
			Utils.delete(proc.getTestObject().getStdErrFileName());
		    }
		    catch (IOException e) {
			ConsoleServer.debugMsg("No stderr file was found :"+e,1);			
		    }						
		    exists = true;
		    // flush and close it's output streams by doing a STOP command
		    proc.interrupt();
		    try {
			processPool.remove(i);
		    }
		    catch (java.lang.NoSuchMethodError ex) {
			// does not exist in jdk1.1.x
			processPool.removeElementAt(i);
		    }
		    break;
		}
	    }
	    if (!exists) {
		// the process was never started, so assume it failed
		ConsoleServer.debugMsg("Process was not running :"+test.getTestID(),1);			
		out.writeInt(ProtocolConstants.FAILED);
	    }
	    else {
		// now send a signal indicating we have finished
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	    }
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CMDCLEAN_REQUEST request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	finally {
	    test = null;
	}
    }
	
    /**
     * This method is resonsible for retrieving all trace files associated with an executed process.
     */
    private void processCMDGETTRACE_REQUEST(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing CMDGETTRACE_REQUEST",1);
	    // read the serialized TestObject which we want the exit code of
	    TestObject test = new TestObject();
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // find the exit code, blocking if neccesary
	    ExecProcess proc;
	    boolean wasRun = false;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		// check for any instance of test running with the same name
		if (proc.getTestObject().getTestID().equals(test.getTestID())) {
		    wasRun = true;
		    // ---- send the env, stdout and stderr files --------------------
		    sendFile(proc.getTestObject().getEnvFileName(), out);
		    sendFile(proc.getTestObject().getStdOutFileName(), out);
		    sendFile(proc.getTestObject().getStdErrFileName(), out);
		    // ----------------------------------
		    break;
		}
	    }
	    if (!wasRun) {
		// the process was never started, so assume it failed
		ConsoleServer.debugMsg("Process was never started :"+test.getTestID(),1);			
		out.writeInt(ProtocolConstants.FAILED);
	    }
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CMDGETTRACE_REQUEST request:"+e.getMessage(),1);
	    //	    e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * This method is resonsible for retrieving all trace files associated with an executed process.
     */
    private void processGETTRACEPATHS_REQUEST(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing GETTRACEPATHS_REQUEST",1);
	    // read the serialized TestObject which we want the exit code of
	    TestObject test = new TestObject();
	    test.readObject(in);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	  
	    // find the exit code, blocking if neccesary
	    ExecProcess proc = null;
	    boolean wasRun = false;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		// check for any instance of test running with the same name
		if (proc.getTestObject().getTestID().equals(test.getTestID())) {
		    wasRun = true;		  
		    ConsoleServer.debugMsg("Process Id was found :"+test.getTestID(),9);
		    break;
		}
	    }
	    if (!wasRun) {
		// the process was never started, so assume it failed
		ConsoleServer.debugMsg("Process was not found :"+test.getTestID(),4);			
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	    }
	    else {
		ConsoleServer.debugMsg("Sending trace file paths",9);
		ConsoleServer.debugMsg("Env file :"+proc.getTestObject().getEnvFileName().toString(),9);
		ConsoleServer.debugMsg("StdOut file :"+proc.getTestObject().getStdOutFileName().toString(),9);
		ConsoleServer.debugMsg("StdErr file :"+proc.getTestObject().getStdErrFileName().toString(),9);
		// now send a signal indicating we have finished
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
		// ---- send the env, stdout and stderr file names --------------------
		out.writeUTF(proc.getTestObject().getEnvFileName().toString());
		out.flush();
		out.writeUTF(proc.getTestObject().getStdOutFileName().toString());
		out.flush();
		out.writeUTF(proc.getTestObject().getStdErrFileName().toString());	
		out.flush();
		// ----------------------------------
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	    }
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing GETTRACEPATHS_REQUEST request:"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    private void sendFile(String fileName, DataOutputStream out) throws Exception {
	sendFile(new File(fileName),out);
    }
	
    private void sendFile(File file, DataOutputStream out) throws Exception {
	int c=1;
	byte buff[] = new byte[1024];
	ConsoleServer.debugMsg("Sending file "+file.toString()+" ("+file.length()+" bytes)",1);		
	out.writeLong(file.length());
	if (file.length()>0) {
	    InputStream inStream = new BufferedInputStream(new FileInputStream(file));
	    while (c > 0) {
		c = inStream.read(buff,0,buff.length);
		if (c>0)
		    out.write(buff,0,c);
	    }
	    inStream.close();
	}
	//ConsoleServer.debugMsg("Sending RESPONSE_FINISHED_OK for file :"+fileName,1);
	sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
    }
	
    /**
     * This method is resonsible for retrieiving information about the OS running on the agent.
     */
    private void processGETINFO_OS(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing GETINFO_OS request",2);
	    out.writeUTF(System.getProperty("os.name"));
	    out.flush();
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error getting OS information:"+e.getMessage(),1);
	}
    }
	
    /**
     * This method is resonsible for retrieving information about the architecture the agent is running on.
     */
    private void processGETINFO_ARCH(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing GETINFO_ARCH request",2);
	    out.writeUTF(System.getProperty("os.arch"));
	    out.flush();
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error getting architecture information:"+e.getMessage(),1);
	}
    }
	
    /**
     * This method is resonsible for retrieving a file from the agent and sending it to the harness.
     * This method should not be used in a file-less agent.
     */
    private void processGETFILE(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing GETFILE request",2);
	    // read the name of the file wanted
	    String fileName = in.readUTF();
	    // send the file
	    ConsoleServer.debugMsg("Sending file "+fileName,9);
	    File file = new File(fileName);
	    BufferedInputStream inStream;
	    if (file.exists()) {
		inStream = new BufferedInputStream(new FileInputStream(file));
		int c=0;
		byte buff[] = new byte[1024];
				
		out.writeLong(file.length());
		BufferedOutputStream buffOut = new BufferedOutputStream(out);
		while (c != -1) {
		    c = inStream.read(buff,0,buff.length);
		    if (c>0)
			buffOut.write(buff,0,c);
		}
		buffOut.flush();
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
		inStream.close();
	    }
	    else {
		// indicate file did not exist
		ConsoleServer.debugMsg("File not found! :"+fileName,9);
		// indicate fake file length
		out.writeLong(-1);
		// indicate an error occurred
		sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	    }
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing GETFILE request :"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * This method is resonsible for sending a file from the harness to the agent.
     * If the destination already exists, it will be deleted and replaced by the one coming from the harness.
     * This method should not be used in a file-less agent.
     */	
    private void processSENDFILE(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing SENDFILE request",1);
	    // read the name of the file to place the bytes into
	    String fileName = in.readUTF();
	    // now read the associatedbytes and write them to file
	    long counter = 0;
	    long size = in.readLong();
	    if ((new File(fileName)).exists()) {
		ConsoleServer.debugMsg("File already exists - deleting :"+fileName,1);
		Utils.delete(fileName);
	    }
	    BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(fileName));
	    BufferedInputStream inStream = new BufferedInputStream(in);
	    byte buff[] = new byte[1024];
	    int bytesRead = 0;
	    ConsoleServer.debugMsg("Attempting to read :"+size+" bytes from the socket connection",5);
	    while (counter < size) {
		bytesRead = inStream.read(buff,0,buff.length);
		if (bytesRead>0)
		    outStream.write(buff,0,bytesRead);
		counter+=bytesRead;
	    }
	    ConsoleServer.debugMsg("Read :"+counter+" bytes",5);
	    outStream.flush();
	    outStream.close();
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing SENDFILE request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * This method is resonsible for checking if a file exists on the agent.
     */
    private void processCHECKFILE(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing CHECKFILE request",1);
	    // read the name of the file to check
	    String fileName = in.readUTF();
	    // now check if it exists or not
	    ConsoleServer.debugMsg("Checking file:"+fileName,3);
	    if (new File(fileName).exists()) {
		ConsoleServer.debugMsg("File exists :"+fileName,1);
		out.writeInt(0);
	    }
	    else {
		ConsoleServer.debugMsg("File does not exist :"+fileName,1);
		out.writeInt(1);
	    }
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CHECKFILE request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    private void processDELFILE(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing DELFILE request",1);
	    // read the name of the file to delete
	    String fileName = in.readUTF();
	    ConsoleServer.debugMsg("Attempting to delete :"+fileName,8);
	    // now delete the file or directory
	    Utils.delete(fileName);
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing DELFILE request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    private void processMKDIR(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing MKDIR request",1);
	    // read the name of the file to create
	    String fileName = in.readUTF();
	    ConsoleServer.debugMsg("Attempting to create :"+fileName,8);
	    // now create the directory
	    Utils.touchDir(fileName);
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing MKDIR request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }	
	
    /**
     * This method is resonsible for killing any processes running on this agent.
     */
    private void processKILLALL(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing KILLALL request",1);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    // kill the test
	    ExecProcess proc;
	    for (int i = 0; i < processPool.size(); i++) {
		try {
		    proc = (ExecProcess)processPool.get(i);
		}
		catch (java.lang.NoSuchMethodError ex) {
		    // does not exist in jdk1.1.x
		    proc = (ExecProcess)processPool.elementAt(i);
		}
		proc.interrupt();
	    }
	    processPool.clear();
	    // now send a signal indicating we have finished
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing KILLALL request:"+e.getMessage(),1);
	    //e.printStackTrace();
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
  
    /**
     * A client is registering to recieve console traces live from
     * this agent.
     */
    private void processGETCONSOLE(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing GETCONSOLE request",1);
	    // read the desired debug level
	    int debugLevel = in.readInt();
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    ConsoleServer.addClient(in, out, socket, debugLevel);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing GETCONSOLE request:"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * A client is registering to recieve console traces live from
     * this agent.
     */
    private void processCHECKAGENT(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing CHECKAGENT request",1);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    Properties properties = System.getProperties();
	    // indicate number of properties we will send
	    out.writeInt(properties.size());
	    String key;
	    for (Enumeration e = System.getProperties().propertyNames() ; e.hasMoreElements() ;) {
		key = e.nextElement().toString();
		out.writeUTF(key);
		out.writeUTF(properties.getProperty(key));
	    }
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing CHECKAGENT request:"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
	
    /**
     * A client is registering to recieve console traces live from
     * this agent.
     */
    private void processKILLAGENT(DataInputStream in, DataOutputStream out) {
	try {
	    ConsoleServer.debugMsg("Processing KILLAGENT request",1);
	    // now send a signal indicating we are processing the request
	    sendSignal(out,ProtocolConstants.RESPONSE_PROCESSING);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_OK);
	    System.exit(0);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error processing KILLAGENT request:"+e.getMessage(),1);
	    sendSignal(out,ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
    }
    }
