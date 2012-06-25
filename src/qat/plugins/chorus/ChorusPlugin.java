package qat.plugins.chorus;

// @(#)ChorusPlugin.java 1.8 00/09/01 

// java import
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
//

// qat import
import qat.gui.*;
import qat.components.*;
import qat.plugins.chorus.*;
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
public class ChorusPlugin extends Object implements PluginInterface {
	
	private String pluginName = "Chorus Plugin";
	private QATInterface qat;
	
	public ChorusPlugin() {
	}
	
	/**
	 * This method is called when the plugin is activated through the
	 * QAT Plugin menu.
	 */
	public void activatePlugin(QATInterface qat) {
		Cash cash = new Cash("localhost",false);
	}
	
	/**
	 * This is the name which will be displayed in the plugin menu.
	 */
	public String getPluginName() {
		return pluginName;
	}
}
