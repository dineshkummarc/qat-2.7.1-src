/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */
#ifndef AGENT_THREAD
#define AGENT_THREAD

#include <cstdio>
#include <string>
using namespace std;

extern "C" {	
#include <sys/utsname.h>        // For uname
}

#include "defs.h"
#include "ConsoleServer.hpp"
#include "Utils.hpp"
#include "Runnable.hpp"
#include "ExecProcess.hpp"
#include "TestObject.hpp"

#ifdef CHORUS
#include <sys/types.h>
#include <sys/sysctl.h>
#endif

class AgentThread :public Runnable
{
	
public:
	
  AgentThread(int sck, int dbl, int sh_out);
	
  ~AgentThread();
	
  void run();

  void processZIP_SEND_REQUEST(int socketHandle);
	
  void processZIP_CLEAN_REQUEST(int socketHandle);
	
  void processDAEMONSTART_REQUEST(int socketHandle);  
  
  void processDAEMONCLEAN_REQUEST(int socketHandle);
  
  void processCMDSTART_REQUEST(int socketHandle);
	
  void processCMDSTATUS_REQUEST(int socketHandle);
	
  void processCMDCLEAN_REQUEST(int socketHandle);
	
  void processCMDGETTRACE_REQUEST(int socketHandle);
  
  void processGETTRACEPATHS_REQUEST(int socketHandle);

  void processCMDSTOP_REQUEST(int socketHandle);
	
  void processGETINFO_OS(int socketHandle);
	
  void processGETINFO_ARCH(int socketHandle);
	
  void processGETFILE(int socketHandle);
	
  void processSENDFILE(int socketHandle);
	
  void processDELFILE(int socketHandle);
	
  void processKILLALL(int socketHandle);
	
  void processCHECKFILE(int socketHandle);
	
  void processMKDIR(int socketHandle);
	
  void processGETCONSOLE(int socketHandle);	
	
  void processCHECKAGENT(int socketHandle);	
	
  void processKILLAGENT(int socketHandle);	

  void sendFile(string fileName, int socketHandle);
	
  void shutDown(int closeHandles);	

  ExecProcess *addProcess(ExecProcess *process);
  
  ExecProcess *getProcess(int i);
  
  void delProcess(int i);  int getProcessCount();
	
	static string getHostString();
	
	static string getArchString();
	
	static string getOsString();
	

private:
  int sock;
  int debugLevel;
  int showOutput;
  void grabMutex();
  void freeMutex();  

  static ExecProcess *processPool[];
  static int processCount;
  static int my_mutex;
	
};
#endif
