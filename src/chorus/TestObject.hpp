/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#ifndef TESTOBJECT_HPP
#define TESTOBJECT_HPP
#include <cstdio>
#include <string>
using namespace std;

#include "Utils.hpp"

/** The TestObject class represents all the information required to execute a single test on a single agent.
 */
class TestObject
{
  
private:
  string instanceId;
  string **executeCommand; // command only
  string **executeEnvironment;
  string workDirectory;
  
  string envFile;
  string stdoutFile;
  string stderrFile;
  
  int timeOut;
  int executeCommandCount;
  int executeEnvCount;		
  static const char *delim;
  
public:
  /** This constructor creates a blank TestObject.
   */
  TestObject();
  ~TestObject();
  
  void readTestObject(int socket);	

  string getTestID();	
	
  /**
   * This method returns the name and path of the file used to store this test object's.
   * environment.
   * WARNING: this method is only valid if called on the agent machine.
   * @return name and path of the stderr file.
   * @author webhiker
   */	
  string getEnvFileName();
	
	
  /**
   * This method returns the name and path of the file used to store this test object's.
   * std error output.
   * WARNING: this method is only valid if called on the agent machine.
   * @return name and path of the stderr file.
   * @author webhiker
   */	
  string getStdOutFileName();
	
	
  /**
   * This method returns the name and path of the file used to store this test object's.
   * std output.
   * WARNING: this method is only valid if called on the agent machine.
   * @return name and path of the stdout file.
   * @author webhiker
   */	
  string getStdErrFileName();
	

  /**
   * This method returns the work directory of this TestObject.
   * @return a string object representing the work directory of this TestObject.
   * All files unzipped or created during execution will/should be placed in the directory to
   * allow for easy cleaning of tests.
   * @author webhiker
   */	
  string getWorkDirectory();
	
	
  /**
   * This method sets the length of time this TestObject is allowed to run
   * before it will be forcibly killed.
   * The value is calculated as a value in seconds since the TestObject was executed.
   * @return the timeout value in seconds.
   * @author webhiker
   */
  int getTimeout();
  string **getExecuteCommand();
  string **getExecuteEnv();
  int getExecuteEnvCount();
  int getCommandCount();
  
};

#endif
