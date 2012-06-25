package qat.gui;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;


import qat.common.Resources;
import qat.common.SwingUtils;
import qat.common.Utils;
import qat.common.ProtocolConstants;

public class EditAgent extends JDialog implements MouseListener {
	private JButton okButton, cancelButton, detectButton, killButton;
	private JTextField agentName, agentPort, agentArch, agentOS;
	private JLabel agentNumber;
	private AgentInfo parent;
	private int index;
	
    public EditAgent(AgentInfo parent, String title) {
		super(parent,title,true);
		this.parent = parent;
		getContentPane().setLayout(new BorderLayout());
		JPanel center = new JPanel(new GridLayout(5,2));
		center.add(new JLabel(Resources.getString("number"),
							  SwingConstants.CENTER));
		center.add(agentNumber = new JLabel("1"));
		center.add(new JLabel(Resources.getString("name"),
							  SwingConstants.CENTER));
		String localHost;
		try {
			localHost = InetAddress.getLocalHost().getHostName();
		}
		catch (Exception e) {
			System.out.println("You have a network configuration problem : Could not resolve localhost");
			localHost = "localhost";
		}
		center.add(agentName = new JTextField(localHost));
		center.add(new JLabel(Resources.getString("port"),
							  SwingConstants.CENTER));
		center.add(agentPort = new JTextField("9999"));
		center.add(new JLabel(Resources.getString("architecture"),
							  SwingConstants.CENTER));
		center.add(agentArch = new JTextField(10));
		center.add(new JLabel(Resources.getString("os"),SwingConstants.CENTER));center.add(agentOS = new JTextField(10));
		JPanel south = new JPanel();		
		south.add(detectButton = new JButton(Resources.getString("detect"),getImageResource("refreshAgentImage")));
		detectButton.addMouseListener(this);
		south.add(killButton = new JButton(Resources.getString("killAgent"),getImageResource("killAgentImage")));
		killButton.addMouseListener(this);
		south.add(okButton = new JButton(Resources.getString("ok")));
		okButton.addMouseListener(this);
		south.add(cancelButton = new JButton(Resources.getString("cancel")));
		cancelButton.addMouseListener(this);
		getContentPane().add(center,BorderLayout.CENTER);
		getContentPane().add(south,BorderLayout.SOUTH);
		pack();
		SwingUtils.setLocationRelativeTo(this,parent);
    }
	
	public void setIndex(int i) {
		index = i;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void setAgentNumber(String s) {
		agentNumber.setText(s);
	}
	
	public String[] getAgent() {
		return getAgent(agentNumber.getText(),
						agentName.getText(),
						agentPort.getText(),
						agentArch.getText(),
						agentOS.getText());
	}
	
	public static String[] getAgent(String number, String name, String port, String arch, String os) {
		String sarray[] = new String[5];
		sarray[0] = number;
		sarray[1] = name;
		sarray[2] = port;
		sarray[3] = arch;
		sarray[4] = os;
		return sarray;
	}
	/**
	 * This method sets the agent fields from a format as found in the properties
	 * file.
	 */
	public void setAgent(String agent[]) {
		agentNumber.setText(agent[0]);
		agentName.setText(agent[1]);
		agentPort.setText(agent[2]);
		agentArch.setText(agent[3]);
		agentOS.setText(agent[4]);
	}
	
	public void detectAgent() {
		String resultArch = Resources.getString("error");
		String resultOS = Resources.getString("error");
		try {
			// get the architecture
			Socket socket = new Socket(agentName.getText(),(new Integer(agentPort.getText())).intValue());
			socket.setSoTimeout(3000);
			DataInputStream  inStream  = new DataInputStream(socket.getInputStream());
			DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeInt(ProtocolConstants.GETINFO_ARCH);
			resultArch = inStream.readUTF();
			inStream.close();
			outStream.close();
			socket.close();
			// get the os
			socket = new Socket(agentName.getText(),(new Integer(agentPort.getText())).intValue());
			socket.setSoTimeout(3000);
			inStream  = new DataInputStream(socket.getInputStream());
			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeInt(ProtocolConstants.GETINFO_OS);
			resultOS = inStream.readUTF();
			inStream.close();
			outStream.close();
			socket.close();
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this,
										  e.toString(),
										  Resources.getString("error"),
										  JOptionPane.INFORMATION_MESSAGE);
		}
		agentArch.setText(resultArch);
		agentOS.setText(resultOS);
	}
	
	private ImageIcon getImageResource(String resource) {
		return new ImageIcon(Resources.getResource(Resources.getString(resource)));
	}
	
	public void killAgent() {
		int code;
		try {
			Socket socket = new Socket(agentName.getText(),(new Integer(agentPort.getText())).intValue());
			socket.setSoTimeout(3000);
			DataInputStream  inStream  = new DataInputStream(socket.getInputStream());
			DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeInt(ProtocolConstants.KILLAGENT);
			code = inStream.readInt();
			code = inStream.readInt();
			inStream.close();
			outStream.close();
			socket.close();
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this,
										  e.toString(),
										  Resources.getString("error"),
										  JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public 	void mouseClicked(MouseEvent e) {
		try {			
			if ((JButton)e.getSource() == okButton) {
				if (getTitle().equals(Resources.getString("addAgent"))) {
					parent.addAgent(this);
				}
				if (getTitle().equals(Resources.getString("editAgent"))) {
					parent.editAgent(this,getIndex());
				}
				setVisible(false);
				parent.setVisible(true);
				dispose();
				return;
			}			
			if ((JButton)e.getSource() == cancelButton) {
				setVisible(false);
				parent.setVisible(true);
				dispose();
				return;
			}
			if ((JButton)e.getSource() == detectButton) {
				detectAgent();
				return;
			}
			if ((JButton)e.getSource() == killButton) {
				killAgent();
				return;
			}
		}
		catch (Exception x) {
			x.printStackTrace();
		}		
	} 	
	public void  mouseEntered(MouseEvent e) {
	}
	public void   mouseExited(MouseEvent e) {
	}
	public void  mousePressed(MouseEvent e) {
	}
	public void  mouseReleased(MouseEvent e) {
	}
}
