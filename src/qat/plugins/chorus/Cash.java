package qat.plugins.chorus;


// @(#)Cash.java 1.17 00/08/31 

// java import
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

public class Cash extends JFrame implements KeyListener, ActionListener, ItemListener {
	public static final int HEIGHT = 25;
	public static final int WIDTH = 80;
	TaskManager taskManager;
	private EComboBox hostNameList;
	private Console currentConsole;
	private ExecProcess execp;
    private Hashtable consoleList;
	private JViewport consoleViewport;
	private boolean canExit;
	
	public Cash (String hostStr) {
        super("Chorus shell");
		consoleList = new Hashtable();
		setupScreen(hostStr);
		loadProperties();
		canExit=true;
    }
	
	/**
	 * This form will not be allowed to call System.exit().
	 */
	public Cash(String hostStr,boolean canExit) {
		this(hostStr);
		this.canExit = canExit;
	}
	
	private void loadProperties() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(System.getProperty("user.home")+File.separator+".cash"));
			int count = Integer.parseInt(p.getProperty("HostCount","0"));
			for (int i = 0; i < count; i++) {
				hostNameList.addItem(p.getProperty("Host"+Integer.toString(i)));
				consoleList.put(p.getProperty("Host"+Integer.toString(i)),
								new Console(p.getProperty("Host"+Integer.toString(i)),HEIGHT,WIDTH));
			}
			if (hostNameList.getItemCount()>1) {
				hostNameList.setSelectedIndex(1);
			}
		}
		catch (Exception e) {
			System.out.println("Could not load properties :"+e.toString());
		}
	}
	
	private void saveProperties() {
		try {
			Properties p = new Properties();
			ArrayList hostList  = hostNameList.getItems();
			p.setProperty("HostCount",Integer.toString(hostList.size()));
			for (int i = 0; i < hostList.size(); i++)
				p.setProperty("Host"+Integer.toString(i),hostList.get(i).toString());
			p.store(new FileOutputStream(System.getProperty("user.home")+File.separator+".cash"),
					"Cash Properties");
		}
		catch (Exception e) {
			System.out.println("Could not save properties :"+e.toString());
		}
	}
	
	private void setupScreen(String hostStr) {
		JButton button;
		
		Container main = getContentPane();
		main.setLayout(new BorderLayout());
		
		// set up the North area
		JPanel northPanel = new JPanel();
		northPanel.setBorder(BorderFactory.createEtchedBorder());
		northPanel.add(new JLabel("Host"),BorderLayout.WEST);
		northPanel.add(hostNameList = new EComboBox(),BorderLayout.CENTER);
		hostNameList.setEditable(true);
		if (hostStr!="") 
			hostNameList.addItem(hostStr);
		hostNameList.addItemListener(this);
		
		main.add(northPanel,BorderLayout.NORTH);
		
		// set up the Center console area
		currentConsole = new Console(hostStr,HEIGHT,WIDTH);
		currentConsole.addKeyListener(this);
		hostNameList.addItemListener(currentConsole);
		
		JScrollPane scroller = new JScrollPane();
		consoleViewport = scroller.getViewport();
		consoleViewport.add(currentConsole);
		consoleList.put(hostStr,currentConsole);
		main.add(scroller,BorderLayout.CENTER);
		
		// set up the East area
		taskManager = new TaskManager(hostStr,currentConsole);
		taskManager.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		new Thread(taskManager).start();
		hostNameList.addItemListener(taskManager);
		main.add(taskManager,BorderLayout.EAST);
		
		// setup the South area
		JPanel southPanel = new JPanel();
		southPanel.add(button = new JButton("Clear"));
		button.addActionListener(this);
		southPanel.add(button = new JButton("Exit"));
		button.addActionListener(this);
		main.add(southPanel,BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		currentConsole.requestFocus();
	}
	
	public void keyTyped(KeyEvent e) {		
	}
	
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyChar()) {
		case KeyEvent.VK_ENTER : 
			executeCommand(currentConsole.getCommandLine());
			break;
		}
	}
	
	public void keyReleased(KeyEvent e) {
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource()).getText().equals("Exit")) {
				saveProperties();
				if (canExit) {
					System.exit(0);				
				}
				else {
					setVisible(false);
					taskManager.interrupt();
					if (execp!=null)
						execp.interrupt();
					dispose();
				}
			}
			if (((JButton)e.getSource()).getText().equals("Clear")) {
				currentConsole.clear();
				currentConsole.requestFocus();
			}
		}
	}
	
	private void executeCommand(String command) {
		if (command.length()>0) {
			command = "rsh "+hostNameList.getSelectedItem().toString()+" "+command;
			execp = new ExecProcess(command,currentConsole);
			execp.setCommand(command);
			execp.start();
		}
	}
	
	public static void Useage() {
		System.out.println("Useage :Cash <host_name>");
		System.exit(-1);
	}
	
	public static final void main(String args[]) {
		System.out.println("Chorus Terminal V1.0");
		if ((args.length!=1)&&
			(!(new File(System.getProperty("user.home")+File.separator+".cash").exists()))) {
			Useage();
		}
		else {
			Cash cash;
			if (args.length>0)
				cash = new Cash(args[0]);
			else
				cash = new Cash("");
		}
	}
	
	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (Exception e) {
					e.printStackTrace();
		}
	}
	
	public void itemStateChanged(ItemEvent e) {
		if (hostNameList.getSelectedItem()!=null) {
			String newConsoleName = hostNameList.getSelectedItem().toString();
			if (!newConsoleName.equals(currentConsole.getHostName())) {
				hostNameList.removeItemListener(currentConsole);
				currentConsole.removeKeyListener(this);
				if (consoleList.get(newConsoleName)==null) {
					currentConsole = new Console(newConsoleName,HEIGHT,WIDTH);
				}
				else {
					currentConsole = (Console)consoleList.get(newConsoleName);
				}
				hostNameList.addItemListener(currentConsole);
				currentConsole.addKeyListener(this);
				consoleViewport.setView(currentConsole);
				consoleList.put(newConsoleName,currentConsole);
			}
		}
		currentConsole.requestFocus();		
	}
}
