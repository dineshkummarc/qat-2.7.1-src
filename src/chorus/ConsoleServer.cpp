#include "ConsoleServer.hpp"

// define static variables here
int ConsoleServer::debugLevel = 10;// this is the level of detail we will use for debug message. 0 = none, 10 = max
ConsoleServer *ConsoleServer::consoleClientList[25];
int ConsoleServer::consoleClientListCount=0;

ConsoleServer::ConsoleServer(int socket, int thisdebugLevel) {
	clientSocket = socket;
	clientDebugLevel = thisdebugLevel;
}

ConsoleServer::~ConsoleServer()
{
}

int ConsoleServer::clientDebugMsg(char *msg, int level) {
	
  if (level<=clientDebugLevel) {

	// write the output to this client
	try {			
	  string msgString = string(msg);		
	  Utils::writeString(clientSocket,msgString);
	}
	catch (char *msg) {
	  return -1;			
	}
		
	try {			
	  // read a byte to verify client is still active
	  int code = Utils::readInt(clientSocket);
			
	  if (code != RESPONSE_FINISHED_OK) {
		return -1;			
	  }
	}
	catch (char *msg) {
	  return -1;			
	}			
  }
  return 0;	
}

void ConsoleServer::debugMsg(int level, const char *fmt_str, ...) 
{
	ConsoleServer *consoleClient;
	// retrieve the paramters
	char *str_cpy= new char[STR_LENGTH];		
	va_list args;
	va_start(args, fmt_str);			
	vsprintf(str_cpy, fmt_str, args);		
	va_end(args);
	
	if ((level <=  ConsoleServer::debugLevel)&&
		(stdout!=NULL)) {
		fprintf(stdout,str_cpy);
		fflush(stdout);			
	}
	
	for (int i = 0; i < getClientListCount(); i++) {
		try {
			consoleClient = (ConsoleServer*)consoleClientList[i];
			if (consoleClient->clientDebugMsg(str_cpy,level)!=0) {
				printf("Console client seems to have died\n");
				removeClient(i);
				printf("Removed client for Console server list\n");
			}
		}
		catch (char *ex) {
			printf("%s\n",ex);			
		}
	}	
	delete [] str_cpy;	
}

void ConsoleServer::debugMsgF(int level, FILE *fileHandle, const char *fmt_str, ...)
{	
	// retrieve the paramters
	char *str_cpy= new char[STR_LENGTH];		
	va_list args;
	va_start(args, fmt_str);			
	vsprintf(str_cpy, fmt_str, args);		
	va_end(args);
		
	// now write to stdout if required
	if ((level<= ConsoleServer::debugLevel)&&
		(fileHandle!=NULL)) {		
	
		// to console
		if (fileHandle != stdout) {
			printf(str_cpy);
		}
		
		// to file now
		if (fileHandle>0) {
			fprintf(fileHandle,str_cpy);
			fflush(fileHandle);		
			va_end(args);
		}		
		
		delete [] str_cpy;		
	}
}

void ConsoleServer::setDebugLevel(int l) 
{
	ConsoleServer::debugLevel = l;
}
  
int ConsoleServer::getDebugLevel() {
	return ConsoleServer::debugLevel;
}

int ConsoleServer::getClientListCount() {
  return consoleClientListCount;  
}

void ConsoleServer::removeClient(int i) {
	
	if (consoleClientListCount==0)
		return;
	
	ConsoleServer *consoleServer = (ConsoleServer *)consoleClientList[i];	
 	try {	  
		close(consoleServer->clientSocket);
		delete consoleServer;		
 	}
 	catch (char * e) {
		printf("%s\n",e);
 	}
	for (i = i; i < getClientListCount(); i++)
		consoleClientList[i]=consoleClientList[i+1];
	consoleClientList[getClientListCount()]=NULL;
	consoleClientListCount--;
}
  
void ConsoleServer::addClient(int socket, int thisclientDebugLevel) {
	consoleClientList[getClientListCount()] = new ConsoleServer(socket,thisclientDebugLevel);
	consoleClientListCount++;
	
	debugMsg(3,"There are now %d client(s) registered on the console\n",getClientListCount());
}
