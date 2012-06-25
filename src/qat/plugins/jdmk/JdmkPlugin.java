package qat.plugins.jdmk;

// @(#)JdmkPlugin.java 1.10 01/02/19 

// java import
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.util.Properties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//

// qat import
import qat.gui.*;
import qat.components.*;
//

/**
 * This class is used for fine-tuning the QAT GUI to specific project related
 * requirements.
 * It does it's work on the project properties, and session properties
 * used by the QAT, and provides hooks for integrating it into
 * the QAT itself.
 * It must implement the default constructor, which will be called to
 * instantiate the class.
 */
public class JdmkPlugin extends Object implements PluginInterface, ActionListener {
	
	private String pluginName = "Jdmk Plugin";
	private Properties projectProperties;
	private JFrame dialog;
	private QATInterface qat;
	
	public JdmkPlugin() {
	}
	
	/**
	 * This method is called when the plugin is activated through the
	 * QAT Plugin menu.
	 */
	public void activatePlugin(QATInterface qat) {
		this.qat = qat;
		dialog = new JFrame(pluginName);
		dialog.getContentPane().setLayout(new GridLayout(2,1));
		JButton button = new JButton("Edit selected file");
		dialog.getContentPane().add(button);
		button.addActionListener(this);
		button = new JButton("Close");
		dialog.getContentPane().add(button);
		button.addActionListener(this);
		dialog.pack();
		dialog.setSize(640,480);
		dialog.setVisible(true);
	}
	
	/**
	 * This is the name which will be displayed in the plugin menu.
	 */
	public String getPluginName() {
		return pluginName;
	}
	
	
	private void editSelectedFile() {
		try {
			TestSpecification test = qat.getSelectedTest();
			new  Notepad(test.getTestSpecPath(),
						 null,
						 false);
		}
		catch(java.lang.IndexOutOfBoundsException ex) {
			JOptionPane.showMessageDialog(null, "No files are selected!", "Error", JOptionPane.ERROR_MESSAGE); 
		}
	}
	
		/* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if (b.getText().equals("Edit selected file")) {
				editSelectedFile();
				return;
			}
			if (b.getText().equals("Close")) {
				dialog.setVisible(false);
				return;
			}
		}
	}
}
