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

import qat.common.*;

public class TraceViewer extends JFrame implements ActionListener {
	
	private TraceViewerComponent traceViewerComponent;	
	
	private TraceViewer(QAT parent) {
		super("Trace Viewer");
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(traceViewerComponent = new TraceViewerComponent(parent),BorderLayout.CENTER);
		// ---------------- south -----------------
		JPanel south = new JPanel(new BorderLayout());
		JButton b;
		south.add(b = new JButton(Resources.getString("close")),BorderLayout.CENTER);
		b.addActionListener(this);
		getContentPane().add(south,BorderLayout.SOUTH);
		pack();
		setSize(640,480);
	}
	
	public TraceViewer(QAT parent, TestSpecification test, String commandID) {
		this(parent);
		traceViewerComponent.loadTraces(test,commandID);
		setVisible(true);
	}
	
	public void setCommandId(String commandID) {
		traceViewerComponent.setCommandId(commandID);
	}
	
	public void setTest(TestSpecification test) {
		traceViewerComponent.setTest(test);
	}
	
	public void loadTraces(TestSpecification test) {
		traceViewerComponent.loadTraces(test);
	}
	
	public void loadTraces(TestSpecification test, String commandID) {
		traceViewerComponent.loadTraces(test,commandID);
	}
	
	private void loadTraces() {
		traceViewerComponent.loadTraces();
		if (!isVisible())
			setVisible(true);
	}
	
	private void closeViewer() {
		setVisible(false);
		traceViewerComponent.clearTraces();
	}
	
	
	/* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource()).getText().equals(Resources.getString("close"))) {	
				closeViewer();
				return;
			}
		}
	}
	
	
	public ChangeListener getChangeListener() {
		return traceViewerComponent;
	}
	
	public boolean equals(Object o, String s) {
		return traceViewerComponent.equals(o,s);
	}
  
}
