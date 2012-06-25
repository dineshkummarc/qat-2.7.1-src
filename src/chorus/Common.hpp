/*
 * @author webhiker

 * @version 1.0, 26 April 2000
 *
 */

#ifndef COMMON_H
#define COMMON_H
class Common 
{
	public:
	/**
	 * This is the current version of the package.
	 * All classes refer to this variable when version control is needed.
	 */
	const static char * DiscoveryProbeString;
	
	const static char * VERSION;
	
	const static char * MultiCastGroup;
	
	const static int    MultiCastPort;
	
	const static int    TimeToLive;
  
  	const static int    TimeToDiscover;
	
};

#endif
