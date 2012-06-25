package qat.gui;

// @(#)DiscoveryProbe.java 1.12 01/04/25 

// java import
import java.net.*;
import java.util.ArrayList;

import qat.common.*;
import qat.gui.*;

public class DiscoveryProbe {
	private static final int BUFFSIZE = 255;
    private MulticastSocket socket;
	private InetAddress group;
	private byte buffer[] = new byte[BUFFSIZE];
	
	public DiscoveryProbe () {		
		try {
			group = InetAddress.getByName(Common.MultiCastGroup);
			socket = new MulticastSocket();
			socket.joinGroup(group);
			socket.setSoTimeout(Common.TimeToDiscover);
			socket.setTimeToLive(Common.TimeToLive);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
    }
	
	public ArrayList getResponse() {
		ArrayList response = new ArrayList();
		DatagramPacket pingPacket = new DatagramPacket(buffer, 
													   Common.DiscoveryProbeString.length(),
													   group,
													   Common.MultiCastPort);		
		try {
			// send the broadcast ping
			for (int i = 0; i < Common.DiscoveryProbeString.length(); i++)
				buffer[i] = (byte)Common.DiscoveryProbeString.charAt(i);
			socket.send(pingPacket);
			
			// read the responses now
			DatagramPacket replyPacket = new DatagramPacket(buffer, 
															BUFFSIZE,
															group,
															Common.MultiCastPort);
			while (1 < 2) {
				clearBytes(replyPacket.getData());
				replyPacket.setLength(BUFFSIZE);
				socket.receive(replyPacket);
				response.add(new String(replyPacket.getData()));
				Thread.yield();
			}
		}
		catch (java.io.InterruptedIOException ex) {
			// normal - we don't block waiting for responses after 5 seconds			
		}
		catch (java.io.IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				socket.leaveGroup(group);
				socket.close();
			}
			catch (Throwable t) {
				
			}
			return response;
		}
	}
	
	private void clearBytes(byte buff[]) {
		for (int i = 0; i < buff.length; i++)
			buff[i] = 0;
	}
}
