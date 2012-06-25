

// @(#)DiscoveryResponder.java 1.4 00/10/06 
#ifndef DISCOVERY_RESPONDER
#define DISCOVERY_RESPONDER
#include <string>
using namespace std;

extern "C" {
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
}

#include "defs.h"
#include "Common.hpp"
#include "AgentThread.hpp"

// Some stupid chorus systems don't define socklen_t in
// their socket.h, so we need to wipe their noses for them.
#ifndef	_SOCKLEN_T
#define	_SOCKLEN_T
typedef u_int32_t socklen_t;
#endif


class DiscoveryResponder:public Runnable
{
private:
	
  string agentResponseString;
  SOCKET discoverySocket;
  struct sockaddr_in sin;
  struct ip_mreq multiCastGroup;
  const static int BUFFSIZE = 255;
  bool running;
	
public:
  DiscoveryResponder (int multicastPort, int agentPort);
			
  void run();
  
  int getPeriod();
  
  void sendResponse(string s, struct sockaddr_in *requester);

};
#endif
