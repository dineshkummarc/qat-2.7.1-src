

// @(#)DiscoveryResponder.java 1.4 00/10/06 

#include "DiscoveryResponder.hpp"

DiscoveryResponder::DiscoveryResponder(int multicastPort, int agentPort) {
	
	char buff[STR_LENGTH];
	string hostString = AgentThread::getHostString();
	string archString = AgentThread::getArchString();
	string osString = AgentThread::getOsString();
	
	sprintf(buff,"%s\n%d\n%s\n%s",
			hostString.c_str(),
			agentPort,
			archString.c_str(),
			osString.c_str());
	agentResponseString.assign(buff);
	
	// create and open the socket
	discoverySocket = socket(AF_INET, SOCK_DGRAM, 0);
	
	sin.sin_family = AF_INET; 
	sin.sin_addr.s_addr = htonl(INADDR_ANY); 
	sin.sin_port = htons(multicastPort); 
	bind(discoverySocket, (struct sockaddr *) &sin, sizeof (sin));
		
	/* IP multicast address of group */
	multiCastGroup.imr_multiaddr.s_addr = inet_addr(Common::MultiCastGroup);
	
	/* local IP address of interface */
	multiCastGroup.imr_interface.s_addr = htonl(INADDR_ANY);
	setsockopt (discoverySocket, IPPROTO_IP, IP_ADD_MEMBERSHIP, &multiCastGroup, sizeof(multiCastGroup));
	
	// time to live
	unsigned char ttl = Common::TimeToLive;
	setsockopt(discoverySocket, IPPROTO_IP, IP_MULTICAST_TTL, &ttl, sizeof(ttl));
	
	// init random number generator
	srandom(time(0));	
}

	
void DiscoveryResponder::run() {
	char message[STR_LENGTH];
	int bytesRecieved;
	socklen_t sinlen = sizeof(sin);
	
	while (1) { 
		bytesRecieved = recvfrom(discoverySocket, 
								 message, 
								 sizeof(message), 
								 0, 
								 (struct sockaddr *)&sin, 
								 (socklen_t *)&sinlen); 
		if (bytesRecieved <= 0) { 
			if (bytesRecieved == 0) { 
				break; 
			} 
			ConsoleServer::debugMsg(1,"recvfrom error :%s\n",strerror(errno));
		} 
		message[bytesRecieved] = '\0';
		if (strcmp(message,Common::DiscoveryProbeString)==0) {						
			sendResponse(agentResponseString, &sin);			
		}
		
	}
}

/*
 * This method returns a random sleep period, in milliseconds.
 */
int DiscoveryResponder::getPeriod() {  
  return (abs((int)random() % (Common::TimeToDiscover-1000))); // milliseconds
}

void DiscoveryResponder::sendResponse(string s, struct sockaddr_in *requester) {
	int bytesRecieved;
	int requesterLen = sizeof(struct sockaddr);
	
	/* sleep for a random period to prevent network saturation */
	/* from all agents replying at once. */	
	Utils::sleep(getPeriod());
		
	bytesRecieved = sendto(discoverySocket, 
						   s.c_str(), 
						   s.length()*sizeof(char), 
						   0, 
						   (struct sockaddr *)requester,
						   requesterLen); 
	if (bytesRecieved < 0) { 
	  ConsoleServer::debugMsg(1,"Response error :%s\n",strerror(errno));
	} 	
}
