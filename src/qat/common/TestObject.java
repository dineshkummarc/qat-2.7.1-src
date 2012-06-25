package qat.common;
/**
 * Used on the agent side to store info about a process which will, was or is currently running.
 *
 * @author webhiker
 * @version 2.4, 17 June 1999
 *
 */
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StreamTokenizer;
import java.lang.System;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/** The TestObject class represents all the information required to execute a single test on a single agent.
 */
public class TestObject implements Serializable {
	private String instanceId;
	private String[] executeCommand;
	private Properties executeEnvironment;
	private File stdErrFileName, stdOutFileName, envFileName;
	private String workDirectory;
	private int timeOut;
	
	/** This constructor creates a blank TestObject.
	 */
	public TestObject() {
		instanceId = "INSTANCE_ID_NOT_SET";
		executeCommand = null;
		executeEnvironment = null;
		timeOut = 0; // default to no timeout
		stdErrFileName=null;
		stdOutFileName=null;
		envFileName=null;
	}
	
	public String getTestID() {
		return instanceId;
	}
	
	public TestObject(String instanceId, 
			  String[] executeCommand,
			  Properties executeEnvironment,
			  String workDirectory, 
			  int timeOut) {
		this.instanceId=instanceId;
		this.executeCommand=executeCommand;
		this.executeEnvironment=executeEnvironment;
		this.workDirectory=workDirectory;
		this.timeOut=timeOut;
	}
	
	/**
	 * This method adds a property to the TestObject properties.
	 * @param key the name of the key to be added or modified.
	 * @param value the value of the key to be added or modified.
	 */
	public void addProperty(Object key, Object value) {
		executeEnvironment.put(key, value);
	}
	
	/**
	 * This method sets the execute command for this TestObject.
	 * @param c the command which will be executed when this TestObject is run.
	 */
	public void setExecuteCommand(String[] c) {
		executeCommand = c;
	}
	
	/**
	 * This method sets the execute enviroment to be used when the execute command is executed..
	 * @param p a Property object containing the relevant environment settings.
	 */
	public void setExecuteEnvironment(Properties p) {
		executeEnvironment = p;
	}

	/**
	 * This method sets the path of the work directory for agent to unzip any
	 * sent zip files into, and to create/delete test directories.
	 * @param w the absolute path of the work directory, on the agent.
	 */
	public void setWorkDirectory(String w) {
		workDirectory = w;
	}
	/**
	 * This method sets the length of time this TestObject is allowed to run
	 * before it will be forcibly killed.
	 * The value is calculated as a value in seconds since the TestObject was executed.
	 * A timeout of 0 will allow the test to run indefinitely. Defaults to zero if not explicitly set.
	 * @param w the timeout value in seconds.
	 */
	public void setTimeout(int t) {
		timeOut = t;
	}

	/**
	 * This method returns the execute command of this TestObject.
	 * @return a String object representing the execute command of this TestObject.
	 */	
	public String[] getExecuteCommand() {
		return executeCommand;
	}
	
	/**
	 * This method returns the execute environment of this TestObject.
	 * @return a Properties object representing the execute environment of this TestObject.
	 */	
	public Properties getExecuteEnvironment() {
		return executeEnvironment;
	}
	
	/**
	 * This method returns the name and path of the file used to store this test object's.
	 * environment.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stderr file.
	 */	
	public File getEnvFileName() {
		if (envFileName==null) {
			return (envFileName = Utils.createTempFile("env","out"));
		}
		else {
			return envFileName;
		}
	}
	
	/**
	 * This method returns the name and path of the file used to store this test object's.
	 * std error output.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stderr file.
	 */	
	public File getStdOutFileName() {
		if (stdOutFileName==null) {
			return (stdOutFileName = Utils.createTempFile("std","out"));
		}
		else {
			return stdOutFileName;
		}
	}
	
	/**
	 * This method returns the name and path of the file used to store this test object's.
	 * std output.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stdout file.
	 */	
	public File getStdErrFileName() {
		if (stdErrFileName==null) {
			return (stdErrFileName = Utils.createTempFile("err","out"));
		}
		else {
			return stdErrFileName;
		}
	}

	/**
	 * This method returns the work directory of this TestObject.
	 * @return a String object representing the work directory of this TestObject.
	 * All files unzipped or created during execution will/should be placed in the directory to
	 * allow for easy cleaning of tests.
	 */	
	public String getWorkDirectory() {
		return workDirectory;
	}	
	
	/**
	 * This method sets the length of time this TestObject is allowed to run
	 * before it will be forcibly killed.
	 * The value is calculated as a value in seconds since the TestObject was executed.
	 * @return the timeout value in seconds.
	 */
	public int getTimeout() {
		return timeOut;
	}
	
	private void writePropertiesObject(Properties p, java.io.DataOutputStream out) throws IOException {
		String key;
		out.writeInt(p.size());
		for (Enumeration e = p.keys() ; e.hasMoreElements() ;) {
			key = (String)e.nextElement();
			out.writeUTF(key);
			out.writeUTF(p.getProperty(key));
		}
	}
	
	private Properties readPropertiesObject(java.io.DataInputStream in) throws IOException {
		int length = in.readInt();
		String key, value;
		Properties res = new Properties();
		for (int i = 0; i < length; i++) {
			key = in.readUTF();
			value = in.readUTF();
			try {
				res.setProperty(key,value);
			}
			catch (java.lang.NoSuchMethodError ex) {
				// Running jdk1.1.x
				res.put(key,value);
			}
		}
		return res;
	}
	
 	public void writeObject(java.io.DataOutputStream out) throws IOException {
		out.writeUTF(instanceId);
		out.writeInt(executeCommand.length);
		for (int i = 0; i < executeCommand.length; i++) {
		    out.writeUTF(executeCommand[i]);
		}
		writePropertiesObject(executeEnvironment,out);
		out.writeUTF(workDirectory);
		out.writeInt(timeOut);
 	}
	
 	public void readObject(java.io.DataInputStream in) throws IOException, ClassNotFoundException {
		instanceId = in.readUTF();
		executeCommand = new String[in.readInt()];
		for (int i = 0; i < executeCommand.length; i++) {
		    executeCommand[i] = in.readUTF();
		}
		executeEnvironment = readPropertiesObject(in);
		workDirectory = in.readUTF();
		timeOut = in.readInt();
 	}
}
