package qat.plugins.chorus;

// @(#)Console.java 1.9 00/08/31 

// java import
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Console extends JTextArea implements KeyListener, ItemListener {
	private static final int MAX_LENGTH = 32000;
	private String promptString="$ ";
	private String command="";
	private Color foreground = Color.white;
	private Color background = Color.blue;
	private ArrayList history;
	private int historyIndex;
	private String hostName;
	
    public Console (String hostName, int rows, int cols) {
        super(rows,cols);
		this.hostName = hostName;
		
		addKeyListener(this);
		setText(promptString);
		setForeground(foreground);
		setBackground(background);
		// ensure fixed width font
		setFont(new Font("Monospaced",Font.PLAIN,getFont().getSize()));
		history = new ArrayList();
		historyIndex = 0;
		scrollToEnd();
		setPrompt(hostName+" $ ");
    }
	
	public String getHostName() {
		return hostName;
	}
	
	public void clear() {
		setText(promptString);
		command = "";
		scrollToEnd();
	}
	
	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {
		case KeyEvent.VK_ENTER : 
			if (isEditable()) {
				append(promptString);
			}
			if (command.length()>0) {
				history.add(command);
				historyIndex = history.size();
			}
			command = "";
			break;
		default:
			command+=e.getKeyChar();
		}
		
		scrollToEnd();			
	}
	
	public void keyPressed(KeyEvent e) {
		scrollToEnd();
	}
	
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_BACK_SPACE : 
			if (command.length()>1)
				command = command.substring(0,command.length()-2);
			else {
				append(" ");
				command = "";
				scrollToEnd();
			}
			break;
		case KeyEvent.VK_KP_UP :
		case KeyEvent.VK_UP : 
			if (historyIndex<=0)
				historyIndex = history.size()-1;
			else historyIndex--;
			setCommand(history.get(historyIndex).toString());
			break;
		case KeyEvent.VK_KP_DOWN :
		case KeyEvent.VK_DOWN : 
			if (historyIndex>=(history.size()-1))
				historyIndex = 0;
			else historyIndex++;
			setCommand(history.get(historyIndex).toString());
			break;
		}
		scrollToEnd();
	}
	
	private void setCommand(String newCommand) {
		scrollToEnd();
		replaceRange(promptString+newCommand,getText().length()-command.length()-promptString.length(),getText().length());
		command = newCommand;
	}
	
	public String getCommandLine() {
		return command;
	}
	
	public void scrollToEnd() {
		try {
			setCaretPosition(getText().length());
		}
		catch (Exception ex) {
		}
	}
	
	public void setPrompt(String newPrompt) {
		scrollToEnd();
		replaceRange(newPrompt+command,getText().length()-command.length()-promptString.length(),getText().length());
		promptString = newPrompt;
		scrollToEnd();
	}
	
	public String getPrompt() {
		return promptString;
	}
	
	public synchronized  void append(String s) {
		if (getText().length()>MAX_LENGTH) {
			replaceRange(null,0,s.length());
		}
		super.append(s);
		scrollToEnd();
	}
	
	public void itemStateChanged(ItemEvent e) {
		requestFocus();		
	}	
}
