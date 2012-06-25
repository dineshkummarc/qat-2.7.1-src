package qat.agent;
/**
 * This class handles the execution of a single process, and manages it's output stream collection etc.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
// standard Java imports
import java.lang.Thread;
import java.lang.System;
import java.lang.Math;
import java.io.*;
import java.lang.Runtime;
import java.lang.Process;
import java.lang.IllegalThreadStateException;
import java.lang.Thread;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Date;

// qa tester imports
import qat.agent.ReaderThread;
import qat.common.ConsoleServer;
import qat.common.TestObject;
import qat.common.Utils;

/**
 * This class is responsible for executing a single TestObject, and starting new threads to read it's standard output
 * and error output streams.
 * It allows for killing of the started TestObject at anytime by calling the cancel() method.
 */
public class ExecProcess extends Object {
    private static final int NOT_YET_SET          = -9999;
    public  static final int TIMEDOUT_STATE       = -9998;
    private static final int ILLEGAL_THREAD_STATE = -5;
    private static final int NULL_POINTER         = -6;
    private static final int OTHER_EXCEPTION      = -7;
    private TestObject test;
    private Process process;
    private Date startTime;
    private ReaderThread stdin;
    private ReaderThread stderr;
    private int exitValue;
    private int debugLevel; // this is the level of detail we will use for debug message. 0 = none, 10 = max
    private PrintStream stdout;
    private boolean showOutput;
    private boolean interrupted;
    private String parentDetails;
    private boolean isDaemon;	
    /** 
     * This constructs a new ExecProcess with the specified TestObject
     * @param t - the TestObject that will be executed when the start() method is called.
     * @param so - the PrintWriter to use for output.
     * @param level - the detail of debug message we want.
     * @param mode - if true, the output is displayed to stdout
     * @param parentDetails - some details about the parent so we can print it
     * to the trace files.
     */
    public ExecProcess(TestObject t, PrintStream so, int level, boolean mode, String parentDetails) {
	setDaemon(false);
	setInterrupted(false);
	test = t;
	setProcess(null);
	stdout = so;
	debugLevel = level;
	showOutput = mode;
	this.parentDetails = parentDetails;
    }
    /** 
     * This method returns the TestObject associated with this Object.
     * @return the TestObject associated with this object.
     */
    public TestObject getTestObject() {
	return test;
    }
    /** 
     * This method should not be called directly, but rather via ObjectName.start() to
     * ensure the thread is started correctly.
     */
    public void start() {
	exitValue = NOT_YET_SET;
	// make sure we allow only one instance of this process to run
	if (getProcess()!=null) {
	    System.out.println("Internal error in ExecProcess - already have a process running!");
	    return;
	}
	// take a snapshot of the current time, so we can calculate the timeout value
	startTime = new Date();
	// build the environment string list from our TestObject properties
	String commandArgs[] = test.getExecuteCommand();		

	// build the environment string list from our TestObject properties
	String environment[] = new String[test.getExecuteEnvironment().size()];
	Enumeration e = test.getExecuteEnvironment().propertyNames();
	for (int i = 0; i < environment.length; i++) {
	    environment[i] = (String)e.nextElement();
	    environment[i] = environment[i]+"="+ test.getExecuteEnvironment().getProperty(environment[i]);	
	}
	PrintWriter out=null;
	try {
	    // create and write the environment file
	    out = new PrintWriter(new FileOutputStream(test.getEnvFileName()));
		    
	    debugMsg("Command arguments :"+commandArgs.length,out,1);
	    debugMsg("-------------------",out,1);
	    for (int i = 0; i < commandArgs.length; i++) {
		debugMsg(" Argument "+i+" :"+commandArgs[i],out,1);
	    }
	    debugMsg("",out,1);
		    
	    debugMsg("Agent details :",out,1);
	    debugMsg("---------------",out,1);
	    debugMsg(parentDetails,out,1);
	    debugMsg("",out,1);
		    
	    debugMsg("Process environment :",out,1);
	    debugMsg("---------------------",out,1);
	    for (int i = 0; i < environment.length; i++)
		debugMsg(environment[i],out,1);
	    debugMsg("",out,1);

	    // patch to inherit env if we don't specify one
	    if (environment.length==0)
		environment = null;

	    setProcess(Runtime.getRuntime().exec(commandArgs, environment));
		    
	    // only store traces if it's not a daemon process
	    if (!isDaemon()) {
		// now process the output and erroutput streams of this process by starting two threads
		// which monitor these streams
		stderr = new ReaderThread("StdErr",getProcess().getErrorStream(),showOutput, test.getStdErrFileName());
		debugMsg("Starting errout thread",out,5);
		stderr.start();
		stdin  = new ReaderThread("StdOutput",getProcess().getInputStream(),showOutput, test.getStdOutFileName());
		debugMsg("Starting stdout thread",out,5);
		stdin.start();
	    }
	    debugMsg("Process started at :"+(new Date()).toString(),out,1);
	}
	catch (IOException ex) {
	    ConsoleServer.debugStackTrace(ex);
	    debugMsg("IOException starting process :"+ex.getMessage(),out,5);
	    interrupt();
	}
	catch (Throwable ex) {
	    ConsoleServer.debugStackTrace(ex);
	    debugMsg("Fatal error starting process :"+ex.getMessage(),out,5);
	    interrupt();
	    // delete this temporary files if they were created, since
	    // it's an abnormal behaviour to get an error here
	    if (test.getStdOutFileName().exists())
		test.getStdOutFileName().delete();
	    if (test.getStdErrFileName().exists())
		test.getStdErrFileName().delete();
	    if (test.getEnvFileName().exists())
		test.getEnvFileName().delete();
	}
		
	finally {
	    try {
		out.flush();
		out.close();
	    }
	    catch (Exception ex) {
		ConsoleServer.debugMsg("Error closing trace file :"+ex.getMessage(),2);
	    }
	}
    }
    
    private Process getProcess() {
	return this.process;
    }
    
    private void setProcess(Process p) {
	this.process = p;
    }
	
    /**
     * This function returns the exit value of the running process if it has finished 
     * else it returns a negative value.
     * This method will not block if the process is still running.
     * @return the absolute value of the exit value of the process, or a negative int to indicate other
     * thread states (ILLEGAL_THREAD_STATE, NULL_POINTER, OTHER_EXCEPTION)
     */
    public synchronized int checkExitValue() {
	try {
	    return Math.abs(getProcess().exitValue());
	}
	catch (IllegalThreadStateException  e) {
	    return ILLEGAL_THREAD_STATE;
	}
	catch (NullPointerException  e) {
	    return NULL_POINTER;
	}
	catch (Exception e) {
	    return OTHER_EXCEPTION;
	}
    }
	
    /**
     * This function returns the exit value of the running process. If the process is not finished, this method 
     * will block until it finishes, or the TestObject timeout value is reached.
     * If the test times-out, it is explicitly killed, as if a ACTION_TESTOBJECT_KILL message was sent by the harness.
     * @return the exit value of the process.
     */
    public synchronized int getExitValue() {
	if (exitValue>=0) {
	    ConsoleServer.debugMsg("Process already finished ("+exitValue+") - returning previous exit code",9);
	    return exitValue;
	}
	else {
	    while ((!wasInterrupted())&&
		   ((exitValue = checkExitValue())<0)&&
		   (!testTimedOut())&&
		   (exitValue>=OTHER_EXCEPTION)) {
		ConsoleServer.debugMsg("Checking process exit value ("+exitValueString(exitValue)+") - will timeout in "+getTimeLeft()+" seconds",9);
		Utils.safeSleep(1);
	    }
	    // check if it timed out
	    if (testTimedOut())
		exitValue = TIMEDOUT_STATE;
	    return exitValue;
	}
    }
	
    /**
     * If the val is an internal QAT code, return a string version of the code,
     * else just return the integer value as a String.
     */
    private String exitValueString(int val) {
	if (val < 0) {
	    switch (val) {
	    case NOT_YET_SET         :return "NOT_YET_SET";
	    case ILLEGAL_THREAD_STATE:return "NOT_FINISHED";
	    case TIMEDOUT_STATE      :return "TIMEDOUT_STATE";
	    case NULL_POINTER        :return "NULL_POINTER";
	    case OTHER_EXCEPTION     :return "OTHER_EXCEPTION";
	    }
	}
	return Integer.toString(val);
    }
	
    /**
     * Returns true if we need to stop this process.
     */
    private boolean wasInterrupted() {
	return interrupted;
    }
	
    private void setInterrupted(boolean i) {
	interrupted = i;
    }
	
    private long getTimeLeft() {
	return ((startTime.getTime()+1000*test.getTimeout())-(new Date()).getTime())/1000;
    }
	
    /**
     * Will return false until the test has timed out, and then it will return true.
     * A test is considered as timed out if it runs longer than the value of TestObject.geTimeout() which
     * is measured in seconds.
     */
    private boolean testTimedOut() {
	Date now = new Date();
	if ((test.getTimeout()!=0)&&(now.after(new Date(startTime.getTime()+1000*test.getTimeout())))) {
	    return true;
	}
	else {
	    return false;
	}
    }
    /** 
     * This will kill the TestObject execution and free all the resource associated with this
     * TestObject execution.
     */
    public void interrupt() {
	setInterrupted(true);
		
	if (exitValue==NOT_YET_SET)
	    exitValue = checkExitValue();
		
	destroy();
		
	if (stdin != null) {
	    stdin.interrupt();
	    stdin = null;
	}
	if (stderr != null) {
	    stderr.interrupt();
	    stderr = null;
	}
    }
	
    /**
     * Outputs a debug message if the debug level corresponds to the level
     * of debug the agent is set to.
     */
    private void debugMsg(String msg, PrintWriter out, int level) {
	ConsoleServer.debugMsg(msg,level);
	try {
	    out.println(msg);
	}
	catch (Exception e) {
	    ConsoleServer.debugMsg("Error recording trace file :"+e,2);
	}
    }
	
    public boolean isDaemon() {
	return isDaemon;
    }
	
    public void setDaemon(boolean isDaemon) {
	this.isDaemon = isDaemon;
    }
	
    public void destroy() {
	Process proc = getProcess();
	if (proc != null)
	    proc.destroy();
    }

    }
