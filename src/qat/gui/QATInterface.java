package qat.gui;

/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;

import qat.common.*;
import qat.parser.*;
import qat.gui.*;

/**
 * This class is the main object of the GUI for the test harness. All other gui objects are used by
 * or contained in this object.
 *
 * @author webhiker
 * @version %W %E
 */
public interface QATInterface {
	
	/**
	 * Loads the project named in fileName.
	 * @param fileName - name of the project to load.
	 */
	public void loadProject(String fileName) throws IOException;
	
	/**
	 * Save the current project to the specified fileName.
	 * @param quiet - if true, no status dialogs will be displayed to monitor save
	 * progress.
	 */
	public void saveProject(String fileName, boolean quiet);
    
	/**
	 * This indicates the proejct properties have changed, and they will
	 * be reloaded.
	 */
    public void firePropertiesChanged(Properties properties);
	
	/**
	 * This form automatically calculates all the values required to display the entire test
	 * tree results.
	 */
	public void updateStatus(String msg);
	
	/**
	 * Reparses all tests from this node. Used by the NodeMenu
	 * object.
	 */
	public void parseTest(TestTreeNode testNode, ParserInterface parser);
	
	/**
	 * Reloads all the tests starting from the TESTPATH root.
	 */
	public void parseTests();
	
	/**
	 * Reparses only the selected tests.
	 */
	public void parseSelectedTests();
	
	/**
	 * Stops the current test run.
	 */
	public void stopTestRun();
	
	/**
	 * Selects all the tests in the tree.
	 */
	public void selectAll();
	
	/**
	 * Returns the currectly selected test, or the first one
	 * of more than one is selected.
	 */
	public TestSpecification getSelectedTest();
	
	/**
	 * This method runs all tests with status PASSED, returning false if none were run for
	 * some reason (such as none exist etc).
	 * Used by the Http interface.
	 */
	public boolean runPassedTests();
	
	/**
	 * This method runs all tests with status FAILED, returning false if none were run for
	 * some reason (such as none exist etc).
	 * Used by the Http interface.
	 */
	public boolean runFailedTests();
	
	/**
	 * This method runs all tests with status UNRESOLVED, returning false if none were run for
	 * some reason (such as none exist etc).
	 * Used by the Http interface.
	 */
	public boolean runUnresolvedTests();
	
	/**
	 * This method runs all tests with status NOTRUN, returning false if none were run for
	 * some reason (such as none exist etc).
	 * Used by the Http interface.
	 */
	public boolean runNotRunTests();
	
	/**
	 * This method runs all tests with matching status, returning false if none were run for
	 * some reason (such as none exist etc).
	 * Used by the Http interface.
	 */
	public boolean runTestsWithStatus(int status);
	
	/**
	 * Run currently selected tests.
	 */
	public void runSelectedTests();
	
	/**
	 * Returns a handle to the JFrame object of the QAT for use in displaying
	 * or creating modal dialogs within the same Swing Thread context as QAT.
	 */
	public JFrame getOwnerHandle();
}
