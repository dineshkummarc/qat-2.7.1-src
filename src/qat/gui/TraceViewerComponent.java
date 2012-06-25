package qat.gui;

import java.awt.event.*;
import java.awt.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.io.*;
import java.util.Date;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import qat.common.Common;
import qat.common.Utils;
import qat.common.Resources;

public class TraceViewerComponent extends JComponent implements ActionListener, 
    HyperlinkListener, 
    ChangeListener {
    private JEditorPane env, stdout, stderr;
    private String commandID;
    private TestSpecification test;
    private JLabel modifiedLabel;
    private JButton searchButton;
    private JTextField searchText;
    private QAT parent;
    private JTabbedPane centerTabPane;
	
    public TraceViewerComponent(QAT parent) {
	this.parent = parent;
	setupComponents();
    }
	
    public TraceViewerComponent(QAT parent, TestSpecification test, String commandID) {
	this(parent);
	loadTraces(test,commandID);
    }
	
	
    public void setCommandId(String commandID) {
	this.commandID = commandID;
    }
	
    public void setTest(TestSpecification test) {
	this.test = test;
    }
	
    public void loadTraces(TestSpecification test) {
	if (this.test.equals(test)) {
	    loadTraces();
	}
    }
	
    public void clearTraces() {
	PageLoader.setText(env,Resources.getString("empty"));
	PageLoader.setText(stdout,Resources.getString("empty"));
	PageLoader.setText(stderr,Resources.getString("empty"));
	modifiedLabel.setText("");
    }
	
    public void loadTraces(TestSpecification test, String commandID) {
	this.test = test;
	this.commandID = commandID;
	loadTraces();
    }
	
    public boolean equals(Object o, String s) {
	try {
	    return ((test.equals(o))&&(commandID.equals(s)));
	}
	catch (NullPointerException e) {
	    return false;
	}
    }
	
    public void loadTraces() {
	modifiedLabel.setText("");
		
	// now load the text		
	loadFile(env,test.getEnvironmentTraceFileName(parent.getProjectResultsDirectory(),
						      commandID));
	loadFile(stdout,test.getStdOutTraceFileName(parent.getProjectResultsDirectory(),
						    commandID));
	loadFile(stderr,test.getStdErrTraceFileName(parent.getProjectResultsDirectory(),
						    commandID));
    }
	
    public void setupComponents() {		
	// set up the screen components
	setLayout(new BorderLayout());
	// --------------- north -------------------
	JPanel north = new JPanel(new GridLayout(1,2));
	north.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	JPanel temp = new JPanel();
	temp.add(new JLabel(Resources.getString("recievedFromAgent"),SwingConstants.RIGHT));
	temp.add(modifiedLabel = new JLabel("",SwingConstants.LEFT));
	north.add(temp);

	temp = new JPanel();
	searchButton = new JButton(Resources.getString("search"),
				   new ImageIcon(Resources.getImageResource("searchImage")));
	searchButton.addActionListener(this);
	temp.add(searchButton);
	searchText = new JTextField(12);
	temp.add(searchText);
	north.add(temp);
	add(north,BorderLayout.NORTH);
		
	// ---------------- center -----------------
	centerTabPane = new JTabbedPane(JTabbedPane.TOP);
	centerTabPane.setBorder(new EtchedBorder());
		
	// add the env pane
	JPanel envPanel = new JPanel();
	env = new JEditorPane();
	env.setEditable(false);
	JScrollPane scroller = new JScrollPane();
	JViewport port = scroller.getViewport();
	port.add(env);
	centerTabPane.addTab(Resources.getString("environment"), 
			     new ImageIcon(Resources.getImageResource("envImage")),
			     scroller);
		
	// add the stdout pane
	JPanel stdoutPanel = new JPanel();
	stdout = new JEditorPane();
	stdout.setEditable(false);
	scroller = new JScrollPane();

	port = scroller.getViewport();
	port.add(stdout);
	centerTabPane.addTab(Resources.getString("standardOutput"), 
			     new ImageIcon(Resources.getImageResource("stdoutImage")),
			     scroller);
		
	// add the stderr pane
	JPanel stderrPanel = new JPanel();
	stderr = new JEditorPane();
	stderr.setEditable(false);
	scroller = new JScrollPane();

	port = scroller.getViewport();
	port.add(stderr);
	centerTabPane.addTab(Resources.getString("standardError"),
			     new ImageIcon(Resources.getImageResource("stderrImage")),
			     scroller);
		
	add(centerTabPane,BorderLayout.CENTER);		
    }
	
    public void search() {
	JEditorPane activeEditor=null;
	switch (centerTabPane.getSelectedIndex()) {
	case 0 : activeEditor = env   ;break;
	case 1 : activeEditor = stdout;break;
	case 2 : activeEditor = stderr;break;
	}
	ParserOutputViewer.search(activeEditor,searchText.getText());
    }
	
    private void loadFile(JEditorPane c, String fileName) {
	try {			
	    PageLoader.setText(c,Resources.getString("empty"));
	    c.removeHyperlinkListener(this);
			
	    File f = new File(fileName);
	    if (f.exists()) {
		if (modifiedLabel.getText().equals("")) {
		    modifiedLabel.setText((new Date(f.lastModified())).toString());
		}
		// don't load empty trace files
		if (f.length()>0) {
		    //		    Thread loader = new PageLoader(f.toURL(),c);
		    //		    SwingUtilities.invokeLater(loader);
		    PageLoader.setPage(c,f.toURL());
		}
		else {
		    PageLoader.setText(c,Resources.getString("empty"));
		}
	    }
	    else {
		PageLoader.setText(c,Resources.getString("fileNotFound")+f.toString());
	    }
	}
	catch (Throwable e) {
	    PageLoader.setText(c,Resources.getString("error")+":"+fileName);
	}
	finally {
	    c.addHyperlinkListener(this);
	}
    }
	
    /* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    if (((JButton)e.getSource()).getText().equals(Resources.getString("search"))) {	
		search();
		return;
	    }
	}
    }
	
    public void hyperlinkUpdate(HyperlinkEvent e) {
	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    JEditorPane pane = (JEditorPane) e.getSource();
	    if (e instanceof HTMLFrameHyperlinkEvent) {
		HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
		HTMLDocument doc = (HTMLDocument)pane.getDocument();
		doc.processHTMLFrameHyperlinkEvent(evt);
	    } 
			
	    else {								
		try {
		    pane.getEditorKit().createDefaultDocument();
		    pane.setPage(e.getURL());
		} 
		catch (Throwable t) {
		    System.out.println("Error processing link :"+t.getMessage());
		}
	    }
	}
    }
  
    // change listener methods
    public void stateChanged(ChangeEvent e) {
	if (isVisible()) {
	    TestTreeNode node = (TestTreeNode)e.getSource();
	    if (node.getUserObject() instanceof TestSpecification) {
		loadTraces((TestSpecification)node.getUserObject());
	    }
	}
    }
  
}
