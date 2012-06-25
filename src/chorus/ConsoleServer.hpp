/*
 * @author webhiker
 * @version 1.0, 31 July 2000
 *
 */
#ifndef CONSOLE_SERVER
#define CONSOLE_SERVER
#include <cstdio>
#include <cstdarg>
#include <string>
using namespace std;

#include "Utils.hpp"

class ConsoleServer {
	
	
private:
  static ConsoleServer *consoleClientList[];
  static int debugLevel; // this is the level of detail we will use for debug message. 0 = none, 10 = max
  static int consoleClientListCount;
	 
  int clientSocket;
  int clientDebugLevel;
  int clientDebugMsg(char *msg, int level);

public:
  ConsoleServer(int socket, int debugLevel);
	
  ~ConsoleServer();
  
  static int getClientListCount();
		
  static void debugMsg(int level, const char *fmt_str, ...);
	
  static void debugMsgF(int level, FILE *fileHandle, const char *fmt_str, ...);
	
  static void setDebugLevel(int l);
	
  static int getDebugLevel();
	
  static void removeClient(int i);
	
  static void addClient(int socket, int clientDebugLevel);
};


#endif
