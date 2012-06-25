/*
 *
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#include "AgentThread.hpp"

AgentThread::AgentThread(int sck, int dbl, int sh_out)
{
  sock = sck;
  debugLevel = dbl;
  showOutput = sh_out;  
};

ExecProcess *AgentThread::processPool[MAX_PROCESS_POOL_COUNT];
int AgentThread::processCount = 0;
int AgentThread::my_mutex=FALSE;

AgentThread::~AgentThread()
{		
};

void AgentThread::processZIP_SEND_REQUEST(int socketHandle)
{
  try {
	
	ConsoleServer::debugMsg(1,"Processing ZIP_SEND_REQUEST\n");
	// read the work directory to use
	string workDir = Utils::readString(socketHandle);
	// now read the associated zip file, if it exists
	string zipFileName = Utils::readString(socketHandle);
	string fullZipFilePath = string(workDir.c_str());
	fullZipFilePath += "/";
	fullZipFilePath += zipFileName;
				
	ConsoleServer::debugMsg(1,"Creating %s\n",fullZipFilePath.c_str());
		
	// create the zip file, test directory and it's parents if they don't exist
	Utils::touch(fullZipFilePath);			
		
	// now read the associatedbytes and write them to file
	size_t counter = 0;
	size_t size = Utils::readLong(socketHandle);
	size_t bytesRead = 0;		
	size_t expect;
	FILE *file_ptr;
	file_ptr = Utils::createF(fullZipFilePath);		
	unsigned char buf[BUFSIZ+1];
	ConsoleServer::debugMsg(5,"Attempting to read :%ld bytes from the socket connection\n",size);
		
	do {
	  ((size-counter) < BUFSIZ) ? expect = (size-counter) : expect = BUFSIZ;
	  bytesRead = Utils::readBytes(socketHandle,buf,expect);	
			
	  if (bytesRead>0) fwrite(buf,1,bytesRead,file_ptr);			
	  counter+=bytesRead;			
	} while ((counter < size)&&
			 (bytesRead>0));
		
	fflush(file_ptr);
	fclose(file_ptr);
	
	ConsoleServer::debugMsg(5,"Read :%ld bytes\n",counter);
	
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	// unzip the zip file now
	ConsoleServer::debugMsg(5,"Unzipping  the file :%s to base dir:%s\n",
							fullZipFilePath.c_str(),
							workDir.c_str());
	Utils::unzipFiles(fullZipFilePath,workDir);
	ConsoleServer::debugMsg(9,"File unzipped OK\n");
		
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
				
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing ZIP_SEND_REQUEST :%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};	

void AgentThread::processZIP_CLEAN_REQUEST(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing ZIP_CLEAN_REQUEST\n");
	// read the work directory to use
	string workDir = Utils::readString(socketHandle);
	// now read the associated zip file name
	string zipFileName = Utils::readString(socketHandle);
	string fullZipFilePath = string(workDir.c_str());
	fullZipFilePath +="/";
	fullZipFilePath +=zipFileName;
			
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
			
	// delete the unzipped files now
	ConsoleServer::debugMsg(5,"Cleaning up the unzipped files :\n",fullZipFilePath.c_str());
	Utils::cleanUnzippedFiles(fullZipFilePath,workDir);
	// delete the zip file itself
	ConsoleServer::debugMsg(5,"Cleaning up the zip file :%s\n",fullZipFilePath.c_str());
	Utils::delete_file(fullZipFilePath);
					
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing ZIP_CLEAN_REQUEST :%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};	

void AgentThread::processDAEMONSTART_REQUEST(int socketHandle)
{				
  try {
	ConsoleServer::debugMsg(1,"Processing DAEMONSTART_REQUEST\n");
	// read the serialized TestObject which we will execute
	TestObject *test = new TestObject();
	test->readTestObject(socketHandle);
			
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
			
	// execute the test
	ExecProcess *startedProcess = addProcess(new ExecProcess(test,
															debugLevel,
															showOutput));
	
	startedProcess->setDaemon(TRUE);
	startedProcess->run();
	ConsoleServer::debugMsg(1,
							"Added daemon pid %d to process pool (total %d processes)\n",
							startedProcess->getPid(),
							getProcessCount());
 
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);			
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing DAEMONSTART_REQUEST execute request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processDAEMONCLEAN_REQUEST(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing DAEMONCLEAN_REQUEST\n");
	
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	
	//clean the files now
	for (int i = getProcessCount()-1; i >=0; i--) {
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();		  
	  if (getProcess(i)->isDaemon()) {
		ConsoleServer::debugMsg(1,"Cleaning daemon %d\n",i);
		// clean up the trace files
		ConsoleServer::debugMsg(1,
								"Cleaning environment file :%s\n",
								curr->getEnvFileName().c_str());
		if (Utils::delete_file(curr->getEnvFileName())!=0)
		  ConsoleServer::debugMsg(1,"No environment file was found :%s\n",
								  curr->getEnvFileName().c_str());
		ConsoleServer::debugMsg(1,"Cleaning stdout file :%s\n",
								curr->getStdOutFileName().c_str());
		if (Utils::delete_file(curr->getStdOutFileName())!=0)
		  ConsoleServer::debugMsg(1,"No stdout file was found :%s\n",
								  curr->getStdOutFileName().c_str());
		ConsoleServer::debugMsg(1,"Cleaning stderr file :%s\n",
								curr->getStdErrFileName().c_str());
		if (Utils::delete_file(curr->getStdErrFileName())!=0)
		  ConsoleServer::debugMsg(1,"No stderr file was found :%s\n",
								  curr->getStdErrFileName().c_str());
		
		// remove from our list of test objects
		ConsoleServer::debugMsg(1,"Removing process from process pool\n");
		
		delProcess(i);							
	  }
	}
	
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
  catch (char * message) {
	ConsoleServer::debugMsg(1,"Error processing DAEMONCLEAN_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processCMDSTART_REQUEST(int socketHandle)
{				
  try {
	ConsoleServer::debugMsg(1,"Processing CMDSTART_REQUEST\n");
	// read the serialized TestObject which we will execute
	TestObject *test = new TestObject();
	test->readTestObject(socketHandle);
			
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
			
	// execute the test
	ExecProcess *startedProcess = addProcess(new ExecProcess(test,
															debugLevel,
															showOutput));
	
	startedProcess->run();
	ConsoleServer::debugMsg(1,
							"Added pid %d to process pool (total %d processes)\n",
							startedProcess->getPid(),
							getProcessCount());
			
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);			
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing CMDSTART_REQUEST execute request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};	

void AgentThread::processCMDSTATUS_REQUEST(int socketHandle)
{	
  TestObject test;
  
  try {
	ConsoleServer::debugMsg(1,"Processing CMDSTATUS_REQUEST\n");
	// read the serialized TestObject which we will stop
	test.readTestObject(socketHandle);
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);			
			
	// find the exit code, blocking if neccesary
	int wasRunning = FALSE;
	for (int i = 0; i < getProcessCount(); i++) {
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();		  
	  if (curr->getTestID().compare(test.getTestID())==0) {
		ConsoleServer::debugMsg(8,"Found started process :%s\n",test.getTestID().c_str());
		int status = getProcess(i)->getExitValue();				
		ConsoleServer::debugMsg(5,"Returning CMDSTATUS value :%d\n",status);
		Utils::writeInt(socketHandle,status);
		wasRunning = TRUE;
		break;
	  }
	}
	if (!wasRunning) {
	  // the process was never started, so assume it failed
	  ConsoleServer::debugMsg(1,"Process was never started :%s\n",test.getTestID().c_str());
	  Utils::writeInt(socketHandle,FAILED);
	}
	else {	  
	  // now send a signal indicating we have finished
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
  }
  catch (char * message) {
	ConsoleServer::debugMsg(1,"Error processing CMDSTATUS_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
  
}

void AgentThread::processCMDCLEAN_REQUEST(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing CMDCLEAN_REQUEST\n");
	// read the serialized TestObject which we will stop
	TestObject test;
	
	test.readTestObject(socketHandle);
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
		
	//clean the files now
	for (int i = 0; i < getProcessCount(); i++) {
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();		  
	  if (curr->getTestID().compare(test.getTestID())==0) {
		// clean up the trace files
		ConsoleServer::debugMsg(1,
								"Cleaning environment file :%s\n",
								curr->getEnvFileName().c_str());
		if (Utils::delete_file(curr->getEnvFileName())!=0)
		  ConsoleServer::debugMsg(1,"No environment file was found :%s\n",
								  curr->getEnvFileName().c_str());
		ConsoleServer::debugMsg(1,"Cleaning stdout file :%s\n",
								curr->getStdOutFileName().c_str());
		if (Utils::delete_file(curr->getStdOutFileName())!=0)
		  ConsoleServer::debugMsg(1,"No stdout file was found :%s\n",
								  curr->getStdOutFileName().c_str());
		ConsoleServer::debugMsg(1,"Cleaning stderr file :%s\n",
								curr->getStdErrFileName().c_str());
		if (Utils::delete_file(curr->getStdErrFileName())!=0)
		  ConsoleServer::debugMsg(1,"No stderr file was found :%s\n",
								  curr->getStdErrFileName().c_str());
		
		// remove from our list of test objects
		ConsoleServer::debugMsg(1,"Removing process from process pool\n");
		
		delProcess(i);						
		
		break;
	  }
	}
		
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
  catch (char * message) {
	ConsoleServer::debugMsg(1,"Error processing CMDCLEAN_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};	

void AgentThread::grabMutex() 
{
	while (my_mutex) 
	  Utils::sleep(1000); // milliseconds
	my_mutex = TRUE;
}

void AgentThread::freeMutex() 
{
  my_mutex = FALSE;  
}

void AgentThread::sendFile(string fileName, int socketHandle) {
  //check if it exists first
  if (!Utils::exists(fileName)) {
	// indicate file did not exist
	ConsoleServer::debugMsg(4,"File not found! :%s\n",fileName.c_str());
	Utils::writeLong(socketHandle, 0);// size of file
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
  else {				
	long size = Utils::size(fileName);	
	ConsoleServer::debugMsg(1,"Sending file %s (%d bytes)\n",
							fileName.c_str(), 
							size);
	if (size>=0) {
	  FILE *file_ptr;
	  try {				
		file_ptr = Utils::openF(fileName);
		if (file_ptr!=NULL) {
		  Utils::writeLong(socketHandle, size);// size of file
		  unsigned char buf[BUFSIZ+1];
		  size_t bytesRead;
		  size_t totalWritten = 0;
		  while (!feof(file_ptr)) {
			bytesRead = fread(buf,sizeof(unsigned char),BUFSIZ,file_ptr);
			Utils::writeBytes(socketHandle,buf,bytesRead);
			totalWritten += bytesRead;					
		  };
		  fflush(file_ptr);
		  fclose(file_ptr);
		  ConsoleServer::debugMsg(1,"Wrote (%ld bytes)\n", (long)totalWritten);
		}
	  }			
	  catch (char * message) {
		ConsoleServer::debugMsg(1,"Problem opening file :%s (%s)\n",fileName.c_str(),strerror(errno));			
		Utils::writeLong(socketHandle, 0);// size of file
	  }			
	}
	else {			
	  ConsoleServer::debugMsg(1,"Wrote (%ld bytes)\n",size);
	}
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
}

void AgentThread::processGETTRACEPATHS_REQUEST(int socketHandle) 
{
  try {
	ConsoleServer::debugMsg(1,"Processing GETTRACEPATHS_REQUEST\n");
	// read the serialized TestObject which we want the exit code of
	TestObject test;
	
	test.readTestObject(socketHandle);
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	int wasRun = FALSE;			
	for (int i = 0; i < getProcessCount(); i++) {
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();		  
	  if (curr->getTestID().compare(test.getTestID())==0) {
		wasRun = true;
		Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
		ConsoleServer::debugMsg(9,"Sending trace file paths\n");
		ConsoleServer::debugMsg(9,"Env file    :%s\n",curr->getEnvFileName().c_str());		
		ConsoleServer::debugMsg(9,"StdOut file :%s\n",curr->getStdOutFileName().c_str());		
		ConsoleServer::debugMsg(9,"StdErr file :%s\n",curr->getStdErrFileName().c_str());
		
		// ---- send the env, stdout and stderr files --------------------
		Utils::writeString(socketHandle,curr->getEnvFileName());
		Utils::writeString(socketHandle,curr->getStdOutFileName());
		Utils::writeString(socketHandle,curr->getStdErrFileName());	
		// ----------------------------------
		break;
	  }
	}
	
	if (!wasRun) {
	  // the process was never started, so assume it failed
	  ConsoleServer::debugMsg(1,"Process was never started :%s\n",test.getTestID().c_str());			
	  Utils::writeInt(socketHandle,RESPONSE_FINISHED_ERROR);
	}
	else {
	  // now send a signal indicating we have finished
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
	
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing GETTRACEPATHS_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }  
}

void AgentThread::processCMDGETTRACE_REQUEST(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing CMDGETTRACE_REQUEST\n");
	// read the serialized TestObject which we want the exit code of
	TestObject test;
	
	test.readTestObject(socketHandle);
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	int wasRun = FALSE;			
	for (int i = 0; i < getProcessCount(); i++) {
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();		  
	  if (curr->getTestID().compare(test.getTestID())==0) {
		wasRun = true;
		// ---- send the env, stdout and stderr files --------------------
		sendFile(curr->getEnvFileName(), socketHandle);
		sendFile(curr->getStdOutFileName(), socketHandle);
		sendFile(curr->getStdErrFileName(), socketHandle);	
		// ----------------------------------
		break;
	  }
	}
	
	if (!wasRun) {
	  // the process was never started, so assume it failed
	  ConsoleServer::debugMsg(1,"Process was never started :%s\n",test.getTestID().c_str());			
	  Utils::writeInt(socketHandle,FAILED);
	}
	else {
	  // now send a signal indicating we have finished
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
	
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing CMDGETTRACE_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
}	

void AgentThread::processCMDSTOP_REQUEST(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing CMDSTOP_REQUEST\n");
	// read the serialized TestObject which we will stop
	TestObject test;
	
	test.readTestObject(socketHandle);
	
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	// find the exit code, blocking for requested timeout if neccesary
	int wasRunning = FALSE;
	int status;
	
	for (int i = 0; i < getProcessCount(); i++) {
	  
	  // check for any instance of test running with the same id
	  TestObject *curr = getProcess(i)->getTestObject();
	  if (curr==NULL) {		
		printf("Error: Null process found on process list");
		return;		
	  }
	  
	  if (curr->getTestID().compare(test.getTestID())==0) {
		ConsoleServer::debugMsg(5,"Stopping process\n");
		wasRunning = TRUE;
		status = getProcess(i)->checkExitValue();
		// flush and close it's output streams
		getProcess(i)->interrupt();
		Utils::sendSignal(socketHandle,status);
		break;
	  }		  
	}
	
	if (!wasRunning) {
	  // the process was never started, so assume it failed
	  ConsoleServer::debugMsg(1,"Process was not running :%s\n",test.getTestID().c_str());			
	}
	else {	  
	  // now send a signal indicating we have finished
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}

  }
  catch (char * message) {
	ConsoleServer::debugMsg(1,"Error processing CMDSTOP_REQUEST request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processGETINFO_OS(int socketHandle)
{
  try {
	  ConsoleServer::debugMsg(2,"Processing GETINFO_OS request\n");	
	  string osname = getOsString();	  
	  Utils::writeString(socketHandle,osname);
  }
  catch (char* message) {
	ConsoleServer::debugMsg(1,"Error getting OS information:%s\n",message);
  }
};

string AgentThread::getHostString() 
{
  struct utsname uts;
  
	if (uname(&uts) == -1) {	  
	  printf("Error getting host string\n");
	  return string("error");	  
	}
	else {	  
	  return string(uts.nodename);
	}
}

string AgentThread::getOsString() 
{
  struct utsname uts;
  
  if (uname(&uts) == -1) {	
	printf("Error getting OS string\n");
	return string();	
  }
  else {
	return string(uts.sysname);
  }  
}

string AgentThread::getArchString() 
{
	string machine_arch;
	
#ifdef CHORUS
	// this ifdef can be removed when they fix the bug in which
	// utsname returns the same value for arch & os values.
	int mib[2];
	size_t len;
	mib[0] = CTL_HW;
	mib[1] = HW_MACHINE;
	len = STR_LENGTH;		
	char arch[STR_LENGTH];		
		
	if (sysctl(mib, 2, arch, &len, NULL, 0)!=0) {
	  printf("Error getting GETINFO_ARCH\n");
	}
		
	machine_arch.assign(arch);		
#else
	struct utsname uts;
	if (uname(&uts) == -1)
	  printf("Error getting GETINFO_ARCH");
	machine_arch.assign(uts.machine);		
#endif
	return machine_arch;	
}

void AgentThread::processGETINFO_ARCH(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(2,"Processing GETINFO_ARCH request\n");
	
	string machine_arch = getArchString();
	
	Utils::writeString(socketHandle,machine_arch);
  }
  catch (char* message) {
	ConsoleServer::debugMsg(1,"Error getting ARCH information:%s\n",message);
  }
};

/**
 * This method is resonsible for retrieving a file from the agent and sending it to the harness.
 */
void AgentThread::processGETFILE(int socketHandle)
{
  ConsoleServer::debugMsg(2,"Processing GETFILE request\n");
	
  // read the name of the file wanted
  string fileName = Utils::readString(socketHandle);
	
  //check if it exists first
  if (!Utils::exists(fileName)) {
	// indicate file did not exist
	ConsoleServer::debugMsg(9,"File not found! :%s\n",fileName.c_str());
	// indicate fake file length
	Utils::writeLong(socketHandle,-1l);
	// indicate an error occurred
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
  else {
	// send the file
	long length = Utils::size(fileName);
	ConsoleServer::debugMsg(9,"Sending file %s of size %ld\n",fileName.c_str(), length);
	Utils::writeLong(socketHandle, length);// size of file
			
	try {
	  FILE *file_ptr;
	  file_ptr = Utils::openF(fileName);
	  unsigned char buf[BUFSIZ+1];
	  int bytesRead; 
	  while (!feof(file_ptr)) {
		bytesRead = fread(buf,1,BUFSIZ,file_ptr);				
		Utils::writeBytes(socketHandle,buf,bytesRead);
	  };
	  fflush(file_ptr);	
	  fclose(file_ptr);
	  ConsoleServer::debugMsg(10,"Sending RESPONSE_FINISHED_OK\n");
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
	catch (char * message) {
	  ConsoleServer::debugMsg(10,"Sending RESPONSE_FINISHED_ERROR :%s\n",message);
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
	}				
  }
	
};

void AgentThread::processSENDFILE(int socketHandle)
{
  ConsoleServer::debugMsg(1,"Processing SENDFILE request\n");
  // read the name of the file to place the bytes into
  string fileName = Utils::readString(socketHandle);

  // now read the associatedbytes and write them to file
  long counter = 0;
  long size;	
  int bytesRead;
  int expect;
  try {
	size = Utils::readLong(socketHandle);
	ConsoleServer::debugMsg(5,"Attempting to read :%ld bytes from the socket connection\n",size);
	FILE *file_ptr;
	ConsoleServer::debugMsg(5,"Opening file %s\n",fileName.c_str());
	Utils::touch(fileName);
	file_ptr = Utils::openF(fileName);
	ConsoleServer::debugMsg(10,"Opened file %s\n",fileName.c_str());
	unsigned char buf[BUFSIZ+1];
		
	do {
	  ((size-counter) < BUFSIZ) ? expect = (size-counter) : expect = BUFSIZ;
	  bytesRead = Utils::readBytes(socketHandle,buf,expect);	
			
	  if (bytesRead>0) fwrite(buf,1,bytesRead,file_ptr);			
	  counter+=bytesRead;			
	} while ((counter < size)&&
			 (bytesRead>0));
	
	fflush(file_ptr);
	fclose(file_ptr);
	
	ConsoleServer::debugMsg(5,"Read :%ld bytes\n",counter);
	// chmod the file to allow execution, reading etc
	if (chmod(fileName.c_str(),S_IRWXU | S_IRWXG | S_IRWXO)!=0)
	  ConsoleServer::debugMsg(3,"Couldn't change permissions on %s (%s)\n",fileName.c_str(),strerror(errno));
		
	// now send a signal indicating we have finished
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
  catch (char *message) {
	ConsoleServer::debugMsg(3,"Error sending file : %s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }	
	
};

void AgentThread::processDELFILE(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing DELFILE request\n");
	// read the name of the file to delete
	string fileName = Utils::readString(socketHandle);
	ConsoleServer::debugMsg(8,"Attempting to delete :%s\n",fileName.c_str());
	// now delete the file or directory
	int result = Utils::delete_file(fileName);
	// now send a signal indicating we have finished
	if (result==0) {
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
	else {
	  ConsoleServer::debugMsg(3,"Couldn't delete file/directory :%s (%s)\n",fileName.c_str(),strerror(errno));			
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
	}
		
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing DELFILE request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processCHECKFILE(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing CHECKFILE request\n");
	// read the name of the file to check
	string fileName = Utils::readString(socketHandle);	
	
	// now check if it exists or not
	if (!Utils::exists(fileName)) {
	  ConsoleServer::debugMsg(1,"File does not exist :%s\n",fileName.c_str());
	  Utils::sendSignal(socketHandle,1);
	}
	else {
	  ConsoleServer::debugMsg(1,"File exists :%s\n",fileName.c_str());
	  Utils::sendSignal(socketHandle,0);								
	}
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing CHECKFILE request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processMKDIR(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing MKDIR request\n");
	// read the name of the file to create
	string fileName = Utils::readString(socketHandle);
	ConsoleServer::debugMsg(8,"Attempting to create :%s\n",fileName.c_str());
	// now create the directory
	if (Utils::mk_dirp(fileName)==0) {		
	  // finished OK
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	}
	else {
	  ConsoleServer::debugMsg(8,"Error creating directory :%s\n",fileName.c_str());
	  Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
	}	
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing MKDIR request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

void AgentThread::processGETCONSOLE(int socketHandle)
{
  try {
	ConsoleServer::debugMsg(1,"Processing GETCONSOLE request\n");
	// read the desired debug level
	int requestedDebugLevel = Utils::readInt(socketHandle);
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
	ConsoleServer::addClient(socketHandle, requestedDebugLevel);
  }
  catch (char *message) {
	ConsoleServer::debugMsg(1,"Error processing GETCONSOLE request:%s\n",message);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
};

/**
 * A client is registering to recieve console traces live from
 * this agent.
 */
void AgentThread::processCHECKAGENT(int socketHandle) {
	
  try {
	ConsoleServer::debugMsg(1,"Processing CHECKAGENT request\n");
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
		
	struct utsname uts;
	if (uname(&uts) == -1)
	  printf("Error getting system properties\n");
		
							   
	string properties[12];
		
	properties[0]  =  string("os.name");
	properties[1]  =  getOsString();	
	properties[2]  =  string("os.arch");
	properties[3]  =  getArchString();	
	properties[4]  =  string("os.version");
	properties[5]  =  string(uts.release);
	properties[6]  =  string("file.separator");
	properties[7]  =  string("/");
	properties[8]  =  string("path.separator");
	properties[9]  =  string(":");
	properties[10] =  string("line.separator");
	properties[11] =  string("\n");
	// TODO
	// send user.name, etc
		
	// indicate number of properties we will send
	Utils::writeInt(socketHandle,6);
	// and send the properties
	for (int i = 0; i < 12; i++) {		  
	  Utils::writeString(socketHandle, properties[i]);
	}
		

	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
  }
  catch (char *e) {
	ConsoleServer::debugMsg(1,"Error processing CHECKAGENT request:%s\n",+e);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
}

/**
 * This will instruct the agent to self-destruct.
 */
void AgentThread::processKILLAGENT(int socketHandle) {
	
  try {
	ConsoleServer::debugMsg(1,"Processing KILLAGENT request\n");
	// now send a signal indicating we are processing the request
	Utils::sendSignal(socketHandle,RESPONSE_PROCESSING);
		
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_OK);
	AgentThread::shutDown(true);
		
	exit(0);		
  }
  catch (char *e) {
	ConsoleServer::debugMsg(1,"Error processing KILLAGENT request:%s\n",+e);
	Utils::sendSignal(socketHandle,RESPONSE_FINISHED_ERROR);
  }
}

void AgentThread::run() 
{	
  int request = Utils::readInt(sock);
  try {
	switch (request) {
	case ZIP_SEND_REQUEST      : processZIP_SEND_REQUEST(sock);
	  break;			
	case ZIP_CLEAN_REQUEST     : processZIP_CLEAN_REQUEST(sock);
	  break;
	case DAEMONSTART_REQUEST   : processDAEMONSTART_REQUEST(sock);
	  break;
	case DAEMONCLEAN_REQUEST   : processDAEMONCLEAN_REQUEST(sock);
	  break;
	case CMDSTART_REQUEST      : processCMDSTART_REQUEST(sock);
	  break;
	case CMDSTATUS_REQUEST     : processCMDSTATUS_REQUEST(sock);
	  break;
	case CMDCLEAN_REQUEST      : processCMDCLEAN_REQUEST(sock);
	  break;
	case CMDGETTRACE_REQUEST   : processCMDGETTRACE_REQUEST(sock);
	  break;
	case CMDSTOP_REQUEST       : processCMDSTOP_REQUEST(sock);
	  break;
	case GETTRACEPATHS_REQUEST : processGETTRACEPATHS_REQUEST(sock);
	  break;
	case GETINFO_OS            : processGETINFO_OS(sock);
	  break;
	case GETINFO_ARCH          : processGETINFO_ARCH(sock);
	  break;
	case GETFILE               : processGETFILE(sock);
	  break;
	case SENDFILE              : processSENDFILE(sock);
	  break;
	case DELFILE               : processDELFILE(sock);
	  break;
	case CHECKFILE             : processCHECKFILE(sock);
	  break;
	case MKDIR                 : processMKDIR(sock);
	  break;
	case GETCONSOLE            : processGETCONSOLE(sock);
	  break;
	case CHECKAGENT            : processCHECKAGENT(sock);
	  break;
	case KILLAGENT             : processKILLAGENT(sock);
	  break;
	default : ConsoleServer::debugMsg(0,"Recieved unknown action code:%d\n",request);
	}		
  }
	
  catch (char * message) {
	ConsoleServer::debugMsg(0,"Error handling request :%s\n",message);
  }
  if (request!=GETCONSOLE) {			
	AgentThread::shutDown(TRUE);		
  }
  else {
	AgentThread::shutDown(FALSE);		
  }		
  delete this;					
};
	
void AgentThread::shutDown(int closeHandles) {
  if (closeHandles==TRUE) {
	ConsoleServer::debugMsg(1,"Closing input and output streams\n");	
	close(sock);
  }
	
  ConsoleServer::debugMsg(1,"======================================================================\n");
};


ExecProcess *AgentThread::addProcess(ExecProcess *process) {
  grabMutex();
  processPool[processCount] = process;
  processCount++;
  freeMutex();
  return processPool[processCount-1];  
};

ExecProcess *AgentThread::getProcess(int i)
{
  if (i < getProcessCount())
	return processPool[i];
  else
	return NULL;  
};

void AgentThread::delProcess(int i) 
{
  grabMutex();
  ExecProcess *processToDelete = getProcess(i);
  
  for (int j = i; j < getProcessCount(); j++)
	processPool[j] = processPool[j+1];				
  
  processPool[processCount] = NULL;
  processCount--;
  
  // remove from our list of test objects
  if (processToDelete) {
	ConsoleServer::debugMsg(1,"Garbage collecting the removed process resources for process %d\n",i);
	delete processToDelete;
	processToDelete = NULL;
  }
  
  
  freeMutex();
};

int AgentThread::getProcessCount() 
{
  return processCount;  
}
