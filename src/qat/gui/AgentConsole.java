package qat.gui;

// @(#)AgentConsole.java 1.12 01/04/25 

// java import
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.io.*;
import java.util.*;
//

import qat.common.*;

public class AgentConsole extends JFrame implements ActionListener, Runnable {
  
    private String index, name;
    private int port;
    private int debugLevel, maxBytes;
    private JTextArea console, detailText;
    private JViewport viewPort;
    private boolean running;
  
    public AgentConsole(String index, String name, int port, int debugLevel, int maxBytes) {
	super("Agent Console");
	this.index = index;
	this.name = name;
	this.port = port;
	this.debugLevel = debugLevel;
	this.maxBytes = maxBytes;
	running = true;
	setupScreen();
	setVisible(true);
    }
  
    public void run() {
	Socket socket = null;
	DataInputStream inStr=null;
	DataOutputStream outStr=null;
	try {
	    socket = new Socket(name, port);
	    socket.setSoTimeout(0);
	    inStr= new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	    outStr= new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	  
	    // send the action code
	    outStr.writeInt(ProtocolConstants.GETCONSOLE);
	    // send our required debug level
	    outStr.writeInt(debugLevel);
	    outStr.flush();
	  
	    // read the response code
	    if (inStr.readInt() != ProtocolConstants.RESPONSE_PROCESSING)
		throw new Exception("Did not recieve correct response from the agent");
	  
	    String line;
	    do {
		line = inStr.readUTF();
		if (line !=null) {					
		    // immediately write response to agent
		    outStr.writeInt(ProtocolConstants.RESPONSE_FINISHED_OK);
		    outStr.flush();
		    // update our component
		    console.append(line);
		    console.setCaretPosition(console.getCaretPosition()+line.length());
		}
		else {
		    // limit the size of the buffer
		    if (console.getCaretPosition() >=maxBytes) {
			console.setText("");
		    }
		    Thread.sleep(250);
		}
	    } while (running);
	    outStr.writeInt(ProtocolConstants.RESPONSE_FINISHED_ERROR);
	}
	catch (Throwable t) {
	    console.append(t.toString());
	    console.setCaretPosition(console.getCaretPosition()+t.toString().length());
	}
	finally {
	    try { 
		outStr.writeInt(ProtocolConstants.RESPONSE_FINISHED_ERROR);
		socket.close();
	    }
	    catch (Throwable t) {
	    }
	}
    }
  
    private void interrupt() {
	running = false;
    }
  
    private void setupScreen() {
	// set up the screen components
	JPanel main = new JPanel(new BorderLayout());
	// --------------- north -------------------
	JPanel north = new JPanel(new FlowLayout());
	north.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	JPanel temp;
	JLabel label;
		
	temp = new JPanel(new FlowLayout());
	label = new JLabel(Resources.getString("name"),SwingConstants.CENTER);
	temp.add(label);
	label = new JLabel(name,SwingConstants.CENTER);
	temp.add(label);
	temp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	north.add(temp);
		
	temp = new JPanel(new FlowLayout());
	label = new JLabel(Resources.getString("port"),SwingConstants.CENTER);
	temp.add(label);
	label = new JLabel(Integer.toString(port),SwingConstants.CENTER);
	temp.add(label);
	temp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	north.add(temp);
		
	temp = new JPanel(new FlowLayout());
	label = new JLabel(Resources.getString("debugLevelLabel"),SwingConstants.CENTER);
	temp.add(label);
	label = new JLabel(Integer.toString(debugLevel),SwingConstants.CENTER);
	temp.add(label);
	temp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	north.add(temp);
		
	main.add(north,BorderLayout.NORTH);
		
	// ---------------- center -----------------
	JPanel centerTrace = new JPanel(new BorderLayout());
	//center.setBorder(new EtchedBorder());
		
	// add the console pane
	console = new JTextArea();
	JScrollPane scroller = new JScrollPane();
	viewPort = scroller.getViewport();
	viewPort.add(console);
	centerTrace.add(scroller,BorderLayout.CENTER);
		
	// add the agent details component
	JPanel centerDetails = new JPanel(new BorderLayout());
	detailText = new JTextArea();
	centerDetails.add(new JScrollPane(detailText,
					  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
	Object details[] = processCHECKAGENT(name,Integer.toString(port));
	String lineSep = System.getProperty("line.separator");
	for (int i = 0; i < details.length; i++) {
	    detailText.append(details[i].toString()+lineSep);
	}
	JTabbedPane tab = new JTabbedPane();
	tab.add("Console",centerTrace);
	tab.add("Details",centerDetails);
		
	main.add(tab,BorderLayout.CENTER);
	// ---------------- south -----------------
	JPanel south = new JPanel();
	JButton b;
	south.add(b = new JButton(Resources.getString("clearHistory")));
	b.addActionListener(this);
	south.add(b = new JButton(Resources.getString("close")));
	b.addActionListener(this);
	main.add(south,BorderLayout.SOUTH);
		
	getContentPane().add(main);
	pack();
	setSize(640,480);
    }
	
    /**
     * This method retrives all the properties of the agent we are connecting to.
     */
    private Object[] processCHECKAGENT(String agentName,String agentPort) {
	Socket socket = null;
	DataInputStream  inStream = null;
	DataOutputStream outStream = null;
	String resultCode ="0";
	ArrayList results = new ArrayList();
	try {
	    socket = new Socket(agentName,(new Integer(agentPort)).intValue());
	    socket.setSoTimeout(3000);
	    inStream  = new DataInputStream(socket.getInputStream());
	    outStream = new DataOutputStream(socket.getOutputStream());
	    outStream.writeInt(ProtocolConstants.CHECKAGENT);
	    if (inStream.readInt()!=ProtocolConstants.RESPONSE_PROCESSING)
		throw new Exception("Error response from agent");
	    // read the number of properties being sent
	    int propertyCount = inStream.readInt();
	    String key, value;
	    for (int i = 0; i < propertyCount; i++) {
		key = index+"."+inStream.readUTF();
		value = inStream.readUTF();
		results.add(key+"="+value);
	    }
	    if (inStream.readInt()!=ProtocolConstants.RESPONSE_FINISHED_OK)
		throw new Exception("Error response from agent");										
	}
	catch (Exception e) {
	    results.add(e.toString());	
	}
	finally {
	    try {if (inStream != null) inStream.close();}catch (IOException e) {results.add(e.toString());}
	    try {if (outStream != null) outStream.close();}catch (IOException e) {results.add(e.toString());}
	    try {if (socket != null) socket.close();}catch (IOException e) {results.add(e.toString());}
	    Object resultsArray[] = results.toArray();
	    Arrays.sort(resultsArray);
	    return resultsArray;
	}
    }
	
    /* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    if (((JButton)e.getSource()).getText().equals(Resources.getString("clearHistory"))) {
		console.setText("");
	    }
	    if (((JButton)e.getSource()).getText().equals(Resources.getString("close"))) {
		setVisible(false);
		interrupt();
		dispose();
	    }
	}
    }
    }
