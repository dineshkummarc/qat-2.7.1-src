/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#ifndef EXEC_PROCESS
#define EXEC_PROCESS

#include <string>
#include <cstdio>
#include <fstream>
#include <iostream>
using namespace std;

extern "C" {
 
#include <errno.h>
#include <wait.h>
#include <signal.h>
#include <sys/stat.h>
#include <fcntl.h>
}

#include "ConsoleServer.hpp"
#include "Runnable.hpp"
#include "TestObject.hpp"

class ExecProcess :public Runnable
{
	
private:
  const static  int NOT_YET_SET          = -9997;
  const static  int TIMEDOUT_STATE       = -9998;
  const static  int ILLEGAL_THREAD_STATE = -5;
  const static  int NULL_POINTER         = -6;
  const static  int OTHER_EXCEPTION      = -7;
	
  TestObject *test;
  pid_t pid;
  time_t startTime;
  int exitValue;
  int debugLevel; // 0 = none, 10 = max
  int showOutput;
  int isADaemon;
  
public:
  /** 
   * This constructs a new ExecProcess with the specified TestObject
   * @param t - the TestObject that will be executed when the start() method is called.
   * @param so - the PrintStream to use for output.
   * @param level - the detail of debug message we want.
   * @param mode - if true, the output is displayed to stdout
   */
  ExecProcess(TestObject *t, 
			  int level,
			  int mode);
	
  ~ExecProcess();
	
  /** 
   * This method returns the TestObject associated with this Object.
   * @return the TestObject associated with this object.
   */
  TestObject *getTestObject();
	
  /** 
   * This method should not be called directly, but rather via ObjectName.start() to
   * ensure the thread is started correctly.
   */
  void run();
	
	
  /**
   * This function returns the exit value of the running process. If the process is not finished, this method 
   * will block until it finishes, or the TestObject timeout value is reached.
   * If the test times-out, it is explicitly killed, as if a ACTION_TESTOBJECT_KILL message was sent by the harness.
   * @return the exit value of the process.
   */
  int getExitValue();
  
  /**
   * Returns the exit value of the process if it has finished, else
   * it returns a negative value. Will not block.
   */
  int checkExitValue();
  
	
  long getTimeLeft();
	
  /**
   * Will return false until the test has timed out, and then it will return true.
   * A test is considered as timed out if it runs longer than the value of TestObject.geTimeout() which
   * is measured in seconds.
   */
  int testTimedOut();
	
  /** 
   * This will kill the TestObject execution and free all the resource associated with this
   * TestObject execution.
   */
  void interrupt();
  
  /**
   * Indicates the process to start will be a daemon, and so don't bother 
   * keeping it's traces.
   */
  void setDaemon(int isADaemon);
  
  int isDaemon();
  
  
  /**
   * Returns 0 if the process pid is still running.
   */
  int stillRunning(pid_t pid);
  
  void redirectOutput(FILE *envFile);
    
  pid_t getPid();   
};
#endif
