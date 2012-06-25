package qat.gui;

// @(#)PluginInterface.java 1.4 00/07/28 

// java import
import java.util.Properties;
import javax.swing.JFrame;

import qat.gui.QATInterface;
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
public interface PluginInterface {
		
	/**
	 * This method is called when the plugin is activated through the
	 * QAT Plugin menu.
	 */
	public void activatePlugin(QATInterface qat);
	
	/**
	 * This is the name which will be displayed in the plugin menu.
	 */
	public String getPluginName();
}
