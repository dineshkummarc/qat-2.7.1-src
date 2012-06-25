package qat.agent;

// @(#)DiscoveryResponder.java 1.11 01/04/25 

// java import
import java.net.*;
import java.util.*;
import qat.common.Common;
import qat.common.ConsoleServer;

public class DiscoveryResponder extends Thread {
	private String agentResponseString;
	private MulticastSocket socket;
	private static final int BUFFSIZE = 255;
	private boolean running;
	private Random random;
	
    public DiscoveryResponder (String multicastGroupMask, int multicastPort, int agentPort) {
		try {
			random = new Random();
			this.agentResponseString = 
				InetAddress.getLocalHost().getHostName()+"\n"+ // host name
				String.valueOf(agentPort)+"\n"+                // host port
				System.getProperty("os.arch")+"\n"+            // host arch
				System.getProperty("os.name");                 // host os
			InetAddress multicastGroup = InetAddress.getByName(multicastGroupMask);
			socket = new MulticastSocket(multicastPort);
			socket.joinGroup(multicastGroup);
			try {
				// this function is not available in 1.1.x
				socket.setTimeToLive(Common.TimeToLive);				
			}
			catch (java.lang.NoSuchMethodError ex) {
				socket.setTimeToLive(Common.TimeToLive);
			}			
		}
		catch(Exception ex) {
			ConsoleServer.debugStackTrace(ex);
		}
    }
	
	private void clearBytes(byte buff[]) {
		for (int i = 0; i < buff.length; i++)
			buff[i] = 0;
	}
	
	/**
	 * This method returns a random value in the range
	 * 0..Common.TimeToDiscover, assuming a network latency
	 * of 1 second.
	 */
	private int getPeriod() {
		return Math.abs(random.nextInt() % (Common.TimeToDiscover-1000));
	}
	
	public void run() {
		byte[] buff = new byte[BUFFSIZE];
		DatagramPacket packet = new DatagramPacket(buff, BUFFSIZE);
		running = true;
		while(running) {
			try {
				socket.receive(packet);
				if ((new String(packet.getData())).indexOf(Common.DiscoveryProbeString)==0) {
					// wait a random period (1..10 seconds)
					// to prevent network flooding
					// due to all agents replying at the same time
					Thread.sleep(getPeriod());
					// now send our response
					sendResponse(agentResponseString,
								 packet.getAddress(),
								 packet.getPort());
				}
			}
			catch (Exception ex) {
				ConsoleServer.debugStackTrace(ex);
				running = false;
			}
		}
	}
	
	private void sendResponse(String s, InetAddress replyAddress, int replyPort) throws Exception {
		byte buff[] = s.getBytes();
		DatagramPacket packet = new DatagramPacket(buff, 
												   buff.length,
												   replyAddress,
												   replyPort);
		//socket.send(packet,Common.TimeToLive);
		int ttl = socket.getTimeToLive(); 
		socket.setTimeToLive(Common.TimeToLive); 
		socket.send(packet); 
		socket.setTimeToLive(ttl); 
	}

}
