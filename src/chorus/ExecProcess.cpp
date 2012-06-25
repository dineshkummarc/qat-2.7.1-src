/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#include "ExecProcess.hpp"
  

ExecProcess::ExecProcess(TestObject *t, 
						 int level, 
						 int mode) {
  test = t;
  debugLevel = level;
  showOutput = mode;
  setDaemon(FALSE);  
}

ExecProcess::~ExecProcess()
{	
  if (test!=NULL) {
	delete test;	
	test = NULL;
  }
}

/** 
	 * This method returns the TestObject associated with this Object.
	 * @return the TestObject associated with this object.
	 */
TestObject *ExecProcess::getTestObject() {
  return test;
}

/** 
 * This method should not be called directly, but rather via ObjectName.start() to
 * ensure the thread is started correctly.
 */
void ExecProcess::run() {
  
  string **argsString;	
  string **envString;

  argsString = test->getExecuteCommand();
  envString = test->getExecuteEnv();
	
  exitValue = NOT_YET_SET;
  int process;
  int pidChild;
	
  // create our env file as well
  ConsoleServer::debugMsg(1,"Opening environment file %s\n\n",test->getEnvFileName().c_str());
  try {
	FILE *envFile = Utils::createF(test->getEnvFileName());		
		
	ConsoleServer::debugMsgF(1,envFile,"Agent details :\n");
	ConsoleServer::debugMsgF(1,envFile,"---------------\n");
	ConsoleServer::debugMsgF(1,envFile,"Host name\n");
	ConsoleServer::debugMsgF(1,envFile,"\n",1);
		
	ConsoleServer::debugMsgF(1,envFile,"Command :\n");
	ConsoleServer::debugMsgF(1,envFile,"---------------------\n");	
	for (int i = 0; i < (test->getCommandCount()); i++) {	  
	  ConsoleServer::debugMsgF(1,envFile,"Argument %d is %s\n",i,argsString[i]->c_str());
	}
	ConsoleServer::debugMsgF(1,envFile,"\n");
		
	ConsoleServer::debugMsgF(1,envFile,"Process environment :%d\n",test->getExecuteEnvCount());
	ConsoleServer::debugMsgF(1,envFile,"---------------------\n");
	for (int i = 0; i < test->getExecuteEnvCount(); i++) {	  
	  ConsoleServer::debugMsgF(1,envFile,"%s\n",envString[i]->c_str());	  
	}
		
	ConsoleServer::debugMsgF(1,envFile,"\n");
		
	pidChild = fork(); 

	if (pidChild == -1) {

	  ConsoleServer::debugMsg(1,"Failed to fork :%s errno=%d\n", strerror(errno),errno);
	}
	else{
	  if (pidChild==0) {
		// this is the child process		
		// so redirect it's stdout and stderr to file			
		redirectOutput(envFile);	
		
		// create the prog_args and prog_env arrays
		char **prog_args = new char*[test->getCommandCount()+1];
		for (int i = 0; i < test->getCommandCount(); i++) {
		  prog_args[i] = (char *)argsString[i]->c_str();		  
		}
		prog_args[test->getCommandCount()]=NULL;
		
		char **prog_env = new char*[test->getExecuteEnvCount()+1];
		for (int i = 0; i < test->getExecuteEnvCount(); i++) {
		  prog_env[i] = (char *)envString[i]->c_str();		  
		}
		prog_env[test->getExecuteEnvCount()]=NULL;
		
		process = execve(argsString[0]->c_str(),		
				 (char*const*)prog_args,
				 (char*const*)prog_env); 
		if ( process < 0 ) {		  
		  ConsoleServer::debugMsg(1,"Error starting process :%s\n",strerror(errno));
		}
		
		// clean up prog_args
		delete prog_args;
		// clean up env
		delete prog_env;

		exit(process);			
	  }
	  else {			
		// this is the parent process
		pid = pidChild;	
		startTime = time(0);
				
		char *timebuff = Utils::allocateChars(STR_LENGTH);	
		timebuff = ctime_r(&startTime,timebuff);			
		ConsoleServer::debugMsgF(1,envFile,"Process %d started at %s\n",pidChild,timebuff);
		delete [] timebuff;			
		timebuff = NULL;
				
		fclose(envFile);
	  }
	}
  }
  catch (char * message) {
	ConsoleServer::debugMsg(1,"Error trying to run the process :%s\n",message);
  }
	
}

/**
 * Indicates the process to start will be a daemon, and so don't bother 
 * keeping it's traces.
 */
void ExecProcess::setDaemon(int isd) 
{
  isADaemon = isd;  
}

int ExecProcess::isDaemon() 
{
  return isADaemon;
}


void ExecProcess::redirectOutput(FILE *envFile) 
{		  		  		
  ConsoleServer::debugMsgF(3,envFile,"Opening stderr file:%s\n",test->getStdErrFileName().c_str());
  dup2( Utils::createH(test->getStdErrFileName()),STDERR_FILENO);
		
  ConsoleServer::debugMsgF(3,envFile,"Opening stdout file:%s\n",test->getStdOutFileName().c_str());
  dup2(Utils::createH(test->getStdOutFileName()),STDOUT_FILENO);
}

/**
	 * This function returns the exit value of the running process. If the process is not finished, this method 
	 * will block until it finishes, or the TestObject timeout value is reached.
	 * If the test times-out, it is explicitly killed, as if a ACTION_TESTOBJECT_KILL message was sent by the harness.
	 * @return the exit value of the process.
	 */
int ExecProcess::getExitValue() {
  if (exitValue!=NOT_YET_SET) {
	ConsoleServer::debugMsg(9,"Process already finished (%d) - returning previous exit code\n",exitValue);
  }
  else {
	while ((!testTimedOut())&
		   (stillRunning(pid))) {
	  switch (exitValue) {
	  case NOT_YET_SET:
		ConsoleServer::debugMsg(9,"Checking process exit value (%s) - will timeout in %d seconds\n",
								"Still running",
								getTimeLeft());
		break;
	  case TIMEDOUT_STATE :
		ConsoleServer::debugMsg(9,"Checking process exit value (%s) - will timeout in %d seconds\n",
								"Timed out",
								getTimeLeft());
		break;
	  }		
	  sleep(1);
	}
	// exit value set by either testTimedOut() or stillRunning()
  }
  return exitValue;
}

/**
 * Returns the exit value of the process if it has finished, else
 * it returns a negative value. Will not block.
 */
int ExecProcess::checkExitValue()   
{
  // call this method to set up the correct value
  stillRunning(pid);
  // return the result
  return exitValue;  
}

long ExecProcess::getTimeLeft() {
  time_t now = time(0);
  time_t left = (test->getTimeout()-(now-startTime));
  if (left>0)
	return left;
  else
	return 0;	
}
	
/**
	 * Will return false until the test has timed out, and then it will return true.
	 * A test is considered as timed out if it runs longer than the value of TestObject.geTimeout() which
	 * is measured in seconds.
	 */
int ExecProcess::testTimedOut() {
  if (test==NULL) {
	return TRUE;
  }
  
  if ((test->getTimeout()!=0)
	  &(getTimeLeft()==0)) {
	interrupt();
	return TRUE;
  }
  else {
	return FALSE;
  }
}

int ExecProcess::stillRunning(pid_t child_pid) 
{
  if (exitValue==NOT_YET_SET){
	int stat_loc;	
	int result;

	result = waitpid(child_pid, &stat_loc, WNOHANG);

	if (result==0){	  
	  return TRUE;				
	}
	else {
	  if (result==child_pid) {
		if (WIFSIGNALED(stat_loc))
		  exitValue = WTERMSIG(stat_loc);
		else
		  exitValue = WEXITSTATUS(stat_loc);
		return FALSE;		
	  }
	  else {
		ConsoleServer::debugMsg(1,"Error retrieving exit value :%s\n",strerror(errno));
		exitValue = NOT_YET_SET;
		return FALSE;				
	  }
	}		
  }
  else {
	return FALSE;
  }
}

/** 
 * This will kill the TestObject execution and delete all the resource associated with this
 * TestObject execution.
 */
void ExecProcess::interrupt() {
  if (stillRunning(pid)) {
	ConsoleServer::debugMsg(1,"Interrupting process %d\n",pid);

	ConsoleServer::debugMsg(1,"Regular ACTOR Kill requested\n");
	if (kill(pid,SIGTERM)!=0) {	  
	  ConsoleServer::debugMsg(1,"Process %d did not respond to kill SIGTERM %d request - trying harder\n",pid,SIGTERM);
	  if (kill(pid,SIGKILL)!=0) {	  
		ConsoleServer::debugMsg(1,"Warning: Process %d did not respond to kill SIGKILL %d - may still be running\n",pid,SIGKILL);
	  }
	}
	exitValue = TIMEDOUT_STATE;	
  }
  else {
	ConsoleServer::debugMsg(1,"Process %d did not appear to be running (already finished?)\n",pid);
  }
}

pid_t ExecProcess::getPid() {
  return pid;  
}
