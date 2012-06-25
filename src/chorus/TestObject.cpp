/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#include "TestObject.hpp"

/** This constructor creates a blank TestObject.
	 */
TestObject::TestObject() { 
  executeEnvironment = NULL;
	
  timeOut = 0; // default to no timeout
  executeCommandCount = -1;
  executeEnvCount = -1;
  
  envFile.assign(tempnam(NULL,NULL));
  stdoutFile.assign(tempnam(NULL,NULL));
  stderrFile.assign(tempnam(NULL,NULL));
}

TestObject::~TestObject() {
  
   if (executeCommand!=NULL) {
 	for (int i = 0; i < executeCommandCount; i++) {
 	  if (executeCommand[i]) {
 		delete executeCommand[i];
 		executeCommand[i] = NULL;
 	  }
 	}
 	executeCommandCount = 0; 	
 	delete [] executeCommand;
 	executeCommand = NULL;	
   }

  if (executeEnvironment!=NULL) {
	for (int i = 0; i < executeEnvCount; i++) {
	  if (executeEnvironment[i]) {
		delete executeEnvironment[i];
		executeEnvironment[i] = NULL;
	  }
	}
	executeEnvCount = 0; 	
	delete [] executeEnvironment;
	executeEnvironment = NULL;	
  }		
 
}

/**
 * Read a serialized test object from the socket, and
 * initialize ourselves with it's values.
 */
void TestObject::readTestObject(int socket)
{
  const char equalsChar = '=';
  
  instanceId.assign(Utils::readString(socket));
   
  executeCommandCount = Utils::readInt(socket);
  executeCommand = new string*[executeCommandCount+1];
  for (int i = 0; i < executeCommandCount; i++) {
	executeCommand[i] = new string(Utils::readString(socket));	
  }


  executeEnvCount = Utils::readInt(socket);
  executeEnvironment = new string*[executeEnvCount+1];
  /* Zero this array in case we fuck up later */
  for (int i = 0; i < executeEnvCount; i++)
	executeEnvironment[i] = NULL;
  
  string placeHolder;
  string value;
  
  for (int i = 0; i < executeEnvCount; i++) {
	placeHolder = Utils::readString(socket);// key
	placeHolder += equalsChar;	                  //  =
	value = Utils::readString(socket);      // value
	placeHolder += value;
	executeEnvironment[i] = new string(placeHolder);	
  }
    
  workDirectory.assign(Utils::readString(socket));
  timeOut = Utils::readInt(socket);  
}

string TestObject::getTestID() {
  return instanceId;
}
	
/**
	 * This method returns the name and path of the file used to store this test object's.
	 * environment.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stderr file.
	 * @author webhiker
	 */	
string TestObject::getEnvFileName() {
	  return envFile;  
}
	
/**
	 * This method returns the name and path of the file used to store this test object's.
	 * std error output.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stderr file.
	 * @author webhiker
	 */	
string TestObject::getStdOutFileName() {
	return stdoutFile;
}
	
/**
	 * This method returns the name and path of the file used to store this test object's.
	 * std output.
	 * WARNING: this method is only valid if called on the agent machine.
	 * @return name and path of the stdout file.
	 * @author webhiker
	 */	
string TestObject::getStdErrFileName() {
	return stderrFile;  
}

/**
	 * This method returns the work directory of this TestObject.
	 * @return a string object representing the work directory of this TestObject.
	 * All files unzipped or created during execution will/should be placed in the directory to
	 * allow for easy cleaning of tests.
	 * @author webhiker
	 */	
string TestObject::getWorkDirectory() {
  return workDirectory;
}	
	
/**
	 * This method sets the length of time this TestObject is allowed to run
	 * before it will be forcibly killed.
	 * The value is calculated as a value in seconds since the TestObject was executed.
	 * @return the timeout value in seconds.
	 * @author webhiker
	 */
int TestObject::getTimeout() {
  return timeOut;
}

const char *TestObject::delim = {" \t\n"};	

/**
 * This method returns the first word occuring in the command string.
 */
string **TestObject::getExecuteCommand()
{
  return executeCommand;	
}

string **TestObject::getExecuteEnv()
{
  return executeEnvironment;
}

int TestObject::getExecuteEnvCount()
{
  return executeEnvCount;	
}

int TestObject::getCommandCount()
{
  return executeCommandCount;	
}
