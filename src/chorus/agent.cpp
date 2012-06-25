/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#include <string>
#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <netdb.h>

// our includes
#include "defs.h"
#include "Utils.hpp"
#include "Common.hpp"
#include "ConsoleServer.hpp"

#include "Runnable.hpp"
#include "AgentThread.hpp"
#include "DiscoveryResponder.hpp"


const int backlog =  50;        // Number of pending connections allowed

class Agent 
{
private:
  unsigned short portNumber;     // Default server port number
  SOCKET serverSocket;
  SOCKET sock;
	
	
public:
  void run() 
  {				
#ifdef MULTI_THREADED
	DiscoveryResponder *responder = new DiscoveryResponder(Common::MultiCastPort,portNumber);
	Utils::start_thread(responder);		
#endif				
	serverSocket = socket(AF_INET, SOCK_STREAM, 0);
	if (serverSocket == INVALID_SOCKET)
	  throw "socket";
	int opt=1;
	if(setsockopt(serverSocket,SOL_SOCKET,SO_REUSEADDR,&opt,sizeof(int))!=0) {			
	  throw "Sockopt error";
	}
		
	struct sockaddr_in serverAddr;                      // Server address information
	serverAddr.sin_family      = AF_INET;               // Internet address family
	serverAddr.sin_port        = htons(portNumber);     // Port number in network byte order
	serverAddr.sin_addr.s_addr = htonl(INADDR_ANY);     // Use IP address of this machine
	memset(&(serverAddr.sin_zero), 0, 8);               // Zero out rest of structure
	  
	int rc = bind(serverSocket, (struct sockaddr*) &serverAddr, sizeof(struct sockaddr));
	  
	if (rc == -1) {
	  ConsoleServer::debugMsg(0,"Error binding to port\n");
	  throw "bind";
	}
				
	rc = listen(serverSocket, backlog);
	if (rc == -1)
	  throw "listen";
		
#ifdef CHORUS_DAEMON
	/* become a daemon to be able to run despite the death of the father */
	setpgid((pid_t) 0, (gid_t) 0);
	close(0);
	close(1);
	close(2);
#endif
	AgentThread *agentThread ;
	struct sockaddr_in clientAddr;                    // Client address information
	socklen_t clientAddrSize = sizeof(struct sockaddr_in);  // Size of client address information	
	while (TRUE) {
	  try {															
		ConsoleServer::debugMsg(1,"======================================================================\n");
		ConsoleServer::debugMsg(1,"Listening for connections on port %d\n" ,portNumber);
		sock = accept(serverSocket, 
					  (struct sockaddr*) &clientAddr, 
					  (socklen_t *)&clientAddrSize);
		setSocketOptions(sock);
		
		ConsoleServer::debugMsg(1,"Connection established with %s\n",getHostName(clientAddr).c_str());
		agentThread = new AgentThread(sock, debugLevel,TRUE);
#ifdef MULTI_THREADED
		// start new thread
		Utils::start_thread(agentThread);
#else
		// run it in the current thread
		agentThread->run();
#endif
		if (errno == EINTR) {				
		  ConsoleServer::debugMsg(0,"Internal error :%s\n",strerror(errno));
		}
	  }			
	  catch (char *message) {
		ConsoleServer::debugMsg(0,"Something horrible happened %s\n" ,message);
	  }		  
	} 		
  }
  
  static string getHostName(struct sockaddr_in clientAddr) 
  {
	string result;
	
	struct hostent *hp;

    hp = gethostbyaddr ((char *)&clientAddr.sin_addr.s_addr,
                        sizeof clientAddr.sin_addr.s_addr, AF_INET);
	result.assign(hp ? hp->h_name : inet_ntoa(clientAddr.sin_addr));
    return result;
  }
  
  
  void setSocketOptions(int socketHandle)
  {
	// TODO - set socket timeout
	//		struct timeval tv;
	//		tv.tv_sec = 5;	
	//		tv.tv_usec = 0;
	
	//		setsockopt(socketHandle,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv));
	
  }

  Agent(const char *port, const char *dbl) 
  {
	portNumber = atoi(port);
	debugLevel = atoi(dbl);
	serverSocket = INVALID_SOCKET;
	sock         = INVALID_SOCKET;
	ConsoleServer::setDebugLevel(debugLevel); 
	run();		
  };
	
  ~Agent()
  {
	ConsoleServer::debugMsg(0,"Cleaning up sockets\n");
	close(serverSocket);
  }
	
	
private:
  int debugLevel;
	
};



static void usage()
{

  ConsoleServer::debugMsg(0,"Native Agent %s\n",Common::VERSION);
  ConsoleServer::debugMsg(0,"    Useage: agent <port_no> [debug_level]\n");
  ConsoleServer::debugMsg(0,"    (Build : %s %s)\n",__DATE__,__TIME__);

};

int main(int argc, char* argv[]) 
{  
  if (argc==1) {
	usage();
	exit(0);
  }
  else {
	try
	  {
		ConsoleServer::debugMsg(0,"Native Agent %s\n",Common::VERSION);
		ConsoleServer::debugMsg(0,"    (Build : %s %s)\n",__DATE__,__TIME__);
		       
		Agent agent(argv[1],argv[2]);
		exit(0);
	  }
	catch ( char* message )
	  {
		ConsoleServer::debugMsg(0,"Error starting server - %s\n",message);
		exit(1);
	  }	
  }
};
