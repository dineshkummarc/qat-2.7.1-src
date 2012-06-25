package qat.gui;

// @(#)RuntimeComponent.java 1.9 01/04/22 

// java import
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
//

import qat.common.*;

public class RuntimeComponent extends JComponent {
	private JLabel passedLabel, 
		failedLabel, 
		notrunLabel, 
		pendingLabel, 
		unresolvedLabel, 
		totalLabel, 
		elapsedTime, 
		remainingTime;
	private JProgressBar memoryState, runProgress;
	private QAT parent;
	
    public RuntimeComponent(QAT parent) {
		super();
        this.parent = parent;
		setupScreen();
    }
	
	private void setupScreen() {
		setLayout(new GridLayout(1,2));
		JPanel runtimePanel1 = new JPanel(new GridLayout(7,1));
		runtimePanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		JPanel runtimePanel2 = new JPanel(new GridLayout(6,1));
		runtimePanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		memoryState = new JProgressBar();
		memoryState.setStringPainted(true);
		memoryState.setBorderPainted(true);
		
		runProgress = new JProgressBar();
		runProgress.setStringPainted(true);
		runProgress.setBorderPainted(true);
// run panel 2 - lhs
		JPanel temp;
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("passed"),JLabel.CENTER));
		temp.add(passedLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("failed"),JLabel.CENTER));
		temp.add(failedLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("unresolved"),JLabel.CENTER));
		temp.add(unresolvedLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("notrun"),JLabel.CENTER));
		temp.add(notrunLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("pending"),JLabel.CENTER));
		temp.add(pendingLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.setBorder(BorderFactory.createEtchedBorder());
		temp.add(new JLabel(Resources.getString("total"),JLabel.CENTER));
		temp.add(totalLabel = new JLabel("",JLabel.CENTER));
		runtimePanel1.add(temp);
		
		// run panel 2 - rhs
		// blank
		runtimePanel2.add(new JPanel());
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("system_memory"),JLabel.CENTER));
		temp.add(memoryState);
		runtimePanel2.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("run_progress"),JLabel.CENTER));
		temp.add(runProgress);
		runtimePanel2.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("time_elapsed"),JLabel.CENTER));
		temp.add(elapsedTime = new JLabel());
		runtimePanel2.add(temp);
		
		temp = new JPanel(new GridLayout(1,2));
		temp.add(new JLabel(Resources.getString("time_left"),JLabel.CENTER));
		temp.add(remainingTime = new JLabel());		
		runtimePanel2.add(temp);
		
		// blank
		runtimePanel2.add(new JPanel());
		
		this.add(runtimePanel1);
		this.add(runtimePanel2);
	}
	
	/**
	 * Set the status bar to display the message specified in msg,
	 * as well as updating the statusGauge to the desired values.
	 * @param msg - the message to display.
	 * @param passed - the number of passed tests.
	 * @param failed - the number of failed tests.
	 * @param unresolved - the number of unresolved tests.
	 */	
	public void setStatus(String msg, double passed, double failed, double unresolved, double notrun, double pending, double running) {
		parent.setStatus(msg);
		parent.setStatusGauge(passed, failed, unresolved, pending, notrun+running);
		passedLabel.setText(Integer.toString((int)passed));
		failedLabel.setText(Integer.toString((int)failed));
		unresolvedLabel.setText(Integer.toString((int)unresolved));
		notrunLabel.setText(Integer.toString((int)(notrun+running)));
		pendingLabel.setText(Integer.toString((int)(pending)));
		totalLabel.setText(Integer.toString((int)(passed+failed+unresolved+notrun+running+pending)));
	}
	
	public void updateMemoryStatus() {
		Runtime r = Runtime.getRuntime();
		memoryState.setMaximum((int)r.totalMemory());
		memoryState.setValue((int)r.freeMemory());		
	}
	
	public void updateRunProgress(int total, int progress, String elapsedtime, String remainingtime) {
		runProgress.setMaximum(total);
		runProgress.setValue(progress);
		elapsedTime.setText(elapsedtime);
		remainingTime.setText(remainingtime);
	}
}
