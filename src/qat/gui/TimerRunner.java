package qat.gui;

// @(#)TimerRunner.java 1.12 01/04/22 

// java import
import java.awt.*;
import java.util.*;
import java.lang.*;
import java.awt.event.*;
//

// swing import
import javax.swing.*;
import javax.swing.border.*;
//


// qat import
import qat.gui.QAT;
import qat.common.Resources;
import qat.gui.TestRunner;
//

public class TimerRunner extends Thread implements ActionListener {
    private QAT parent;
    private TestRunner testRunner;
    private JDialog dateWindow;
    private Date runTime;
    private ArrayList runList;
    private boolean isRunning = false;
    private JTextField hour,minute,day,month,year;
    private JButton okButton, cancelButton;
    private boolean okSelected=false;
	
    public TimerRunner (QAT parent, TestRunner testRunner) {
	this.parent = parent;
	this.testRunner = testRunner;
    }
	
    private static String addZero(int i) {
	if (i<10)
	    return "0"+Integer.toString(i);
	else
	    return Integer.toString(i);
    }
	
    private void initDate() {
	Calendar calendar = Calendar.getInstance();
	calendar.setTime(new Date());
	hour.setText(addZero(calendar.get(Calendar.HOUR_OF_DAY)));
	minute.setText(addZero(calendar.get(Calendar.MINUTE)));
	day.setText(addZero(calendar.get(Calendar.DAY_OF_MONTH)));
	month.setText(addZero(calendar.get(Calendar.MONTH)));
	year.setText(addZero(calendar.get(Calendar.YEAR)));
    }
	
    public void setDate(Date runTime) {
	this.runTime = runTime;
    }
	
    private boolean validateDate() {
	try {
	    Calendar c =  Calendar.getInstance();
	    c.set(Integer.parseInt(year.getText()),
		  Integer.parseInt(month.getText()),
		  Integer.parseInt(day.getText()),
		  Integer.parseInt(hour.getText()),
		  Integer.parseInt(minute.getText()));
	    setDate(c.getTime());
	    return true;
	}
	catch (NumberFormatException e) {
	    JOptionPane.showMessageDialog(dateWindow, 
					  Resources.getString("invalidDate"),
					  Resources.getString("error"), 
					  JOptionPane.ERROR_MESSAGE);
	    initDate();
	    return false;
	}
    }
	
    public boolean promptForDate() {
	dateWindow = new JDialog(parent,true);
	dateWindow.setTitle(Resources.getString("timerRun"));
	JPanel centerl = new JPanel(new GridLayout(2,2));
	centerl.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	JPanel centerr = new JPanel(new GridLayout(2,3));
	centerr.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	centerl.add(new JLabel(Resources.getString("hour"),JLabel.CENTER));
	centerl.add(new JLabel(Resources.getString("minute"),JLabel.CENTER));
	centerr.add(new JLabel(Resources.getString("day"),JLabel.CENTER));
	centerr.add(new JLabel(Resources.getString("month"),JLabel.CENTER));
	centerr.add(new JLabel(Resources.getString("year"),JLabel.CENTER));
	Calendar calendar = Calendar.getInstance();
	calendar.setTime(new Date());
	centerl.add(hour=new JTextField());
	centerl.add(minute=new JTextField());
	centerr.add(day=new JTextField());
	centerr.add(month=new JTextField());
	centerr.add(year=new JTextField());
	JPanel center = new JPanel();
	center.add(centerl);
	center.add(centerr);
	JPanel south = new JPanel();
	south.add(okButton=new JButton(Resources.getString("ok")));
	okButton.addActionListener(this);
	south.add(cancelButton=new JButton(Resources.getString("cancel")));
	cancelButton.addActionListener(this);
	dateWindow.getContentPane().add(center,BorderLayout.CENTER);
	dateWindow.getContentPane().add(south,BorderLayout.SOUTH);
	initDate();
	dateWindow.pack();
	dateWindow.setLocationRelativeTo(parent);
	dateWindow.show();
	return okSelected;
    }
	
    public void setRunList(ArrayList runList) {
	this.runList = runList;
    }
	
    public void run() {
	isRunning = true;
	Date now;
	while ((runTime.compareTo(now = new Date())>0)&&
	       (isRunning)) {
	    sleep();
	}
	if (isRunning) {
	    testRunner.setRunList(runList);
	    testRunner.start();		
	}
	isRunning  =false;
    }
	
    public void interrupt() {
	isRunning = false;
	testRunner.interrupt();
    }
	
    public boolean isRunning() {
	return this.isRunning;
    }
	
    private void sleep() {
	try {
	    Thread.sleep(3000);
	    Thread.yield();
	}
	catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
	
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JButton) {
	    if (e.getSource()==okButton) {
		if (validateDate()) {
		    okSelected = true;
		    dateWindow.setVisible(false);
		    dateWindow.dispose();
		    return;
		}
		else {
		    dateWindow.show();
		}
	    }
	    if (e.getSource()==cancelButton) {
		okSelected = false;
		dateWindow.setVisible(false);
		dateWindow.dispose();
		return;
	    }
	}
    }
}
