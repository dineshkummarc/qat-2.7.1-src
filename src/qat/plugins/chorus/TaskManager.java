package qat.plugins.chorus;

/**
 * This class handles the execution of a single process, and manages it's output stream collection etc.
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
// standard Java imports
import java.lang.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;


import qat.plugins.chorus.Console;
import qat.plugins.chorus.ExecProcess;

/**
 * This class is responsible for executing a single TestObject, and starting new threads to read it's standard output
 * and error output streams.
 * It allows for killing of the started TestObject at anytime by calling the cancel() method.
 */
public class TaskManager extends JPanel implements Runnable, ActionListener, ItemListener {
	
	private String host;
	private boolean running;
	private JTable procList;
	private String titles[] = {"Uid","Pid","Name","Dbg","Stat","Unknown"};
	private Console console;
	private ExecProcess apsCommand;
	private JButton connectedIcon;
	
    public TaskManager(String host, Console console) {
		this.host = host;
		this.console = console;
		// set up the screen components
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEtchedBorder());
		add(new JLabel("Process List",SwingConstants.CENTER),BorderLayout.NORTH);
		String data[][] = {titles};
		procList = new JTable(new DefaultTableModel(data,titles));
		// ensure fixed width font
		procList.setFont(new Font(procList.getFont().getName(),Font.PLAIN,procList.getFont().getSize()-2));
		procList.getTableHeader().setFont(procList.getFont());
		procList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		
		JScrollPane scrollPane = new JScrollPane(procList);
		add(scrollPane,BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel();
		JButton button;
		southPanel.add(button = new JButton("End Process"));
		southPanel.add(connectedIcon = new JButton("OFFLINE"));
		connectedIcon.setOpaque(true);
		add(southPanel,BorderLayout.SOUTH);
		button.addActionListener(this);
    }

	public void run() {
		try {
			running = true;
			apsCommand = new ExecProcess("rsh "+host+" aps",null);
			while(running) {
				if(host!=null) {
					if (host.length()>0) {
						updateView();
					}
				}
				Thread.sleep(1000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateView() throws Exception {
		apsCommand.setCommand("rsh "+host+" aps");
		parseResults(apsCommand.getProcessOutput());
	}
	
	private void parseResults(String results) {
		try {
			StringTokenizer words = new StringTokenizer(results);
			// skip the title strings
			words.nextToken();words.nextToken();words.nextToken();
			words.nextToken();words.nextToken();
			((DefaultTableModel)procList.getModel()).setNumRows(words.countTokens()/titles.length);
			int row = 0;
			int col = 0;
			while(words.hasMoreTokens()) {
				((DefaultTableModel)procList.getModel()).setValueAt(words.nextToken().trim(),row,col);
				col++;			
				if (col==titles.length) {
					col = 0;
					row++;
				}
			}
			connectedIcon.setText("ONLINE");
			connectedIcon.setBackground(Color.green);
		}
		catch (Exception e) {
			goOffline();
		}
	}
	
	private void goOffline() {
		connectedIcon.setText("OFFLINE");
		connectedIcon.setBackground(Color.red);
		((DefaultTableModel)procList.getModel()).setNumRows(0);
		repaint();
	}
	
	public void interrupt() {
		running = false;
	}
	
	private void endProcess() {
		try {
			String pId = (String)((DefaultTableModel)procList.getModel()).getValueAt(procList.getSelectedRow(),1);
			String command = "rsh "+host+" akill "+pId;
			ExecProcess killProcess = new ExecProcess(command,console);
			killProcess.start();
			Thread.sleep(150);
			updateView();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			console.requestFocus();
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource()).getText().equals("End Process")) {
				endProcess();
			}
		}
	}
	
	public void itemStateChanged(ItemEvent e) {
		host = e.getItem().toString();
		goOffline();
		if (apsCommand != null) {
			apsCommand.interrupt();
		}
	}
}
