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
import qat.gui.*;
import qat.parser.*;
import qat.components.*;

//import Agent;

/**
 * This class is the main object of the GUI for the test harness. All other gui objects are used by
 * or contained in this object.
 *
 * @author webhiker
 * @version %W %E
 */
public class QAT extends JFrame implements QATInterface, ActionListener, WindowListener {
    public  KeywordComponent keywordComponent;
    private JLabel statusLabel, parserLabel;
    private String serializedTreeName;
    private Properties projectProperties;
    private Cursor waitCursor=null, defaultCursor=null;
    private Gauge statusGauge;
    private TestTree testTree;
    private ParserOutputViewer parserOutputViewer;  
    private TestDetailViewer testDetailViewer;  
    private TestRunner testRunner;
    private TimerRunner timerRunner;
    private JToolBar toolbar;
    private JMenuBar menuBar;
    private JMenu consoleMenu, pluginMenu;
    private Hashtable pluginList;
    private HttpQATHandler httpQATHandler;
    private String projectFileName;
    // instanceSuffix is used to to allow each new instance of QAT to use
    // it's own property settings
    private String instanceSuffix;
    private static ArrayList instanceList;
    private static Properties sessionProperties;
    private JSplitPane leftrightSplit, topbottomSplit;
	
    // the internal frame components
    RuntimeComponent runtimeComponent;
	
    static {
	setSessionProperties(new Properties());
	instanceList = new ArrayList();
    }
	
    /*
     * This constructs a new Qat instance.
     * Passing visible as false indicates we are starting a commandline
     * version, and no GUI components will be set tot visible.
     */
    public QAT(boolean visible) {
	super();		
	// this section ensure we can run multiple instance of the QAT
	// harness in the same JVM
	instanceList.add(this);
	if (instanceList.size()==1) {
	    instanceSuffix = "";
	}
	else {
	    instanceSuffix = Integer.toString(instanceList.size());
	}
		
	SplashScreen splash = new SplashScreen(this, new ImageIcon(Resources.getImageResource("splashImage")),Resources.getString("pleaseWait"));
	if (visible)
	    splash.showSplash();
	pluginList = new Hashtable();
	splash.setText(Resources.getString("pleaseWait"),10);
	setTitle(Resources.getString("pleaseWait"));
	setIconImage(new ImageIcon(Resources.getImageResource("sunLogo")).getImage());
	testRunner = new TestRunner(this);
	timerRunner = new TimerRunner(this,testRunner);
	getContentPane().setLayout(new BorderLayout());
	splash.setText(Resources.getString("splashText1"),17);
	addMenuBar();
	splash.setText(Resources.getString("splashText2"),24);
	addToolBar();
	splash.setText(Resources.getString("splashText3"),31);
	addCenterPanel(splash);
	splash.setText(Resources.getString("splashText9"),73);
	addStatusBar();
	splash.setText(Resources.getString("splashText10"),80);
	pack();
	splash.setText(Resources.getString("splashText11"),87);
	loadSession();

	setTitle(Resources.getString("title")+" "+Common.VERSION+" "+projectFileName);
	addWindowListener(this);
		
	validate();
	splash.setText(Resources.getString("pleaseWait"),94);
	leftrightSplit.setDividerLocation(0.25);
	setVisible(visible);
	splash.setText(Resources.getString("pleaseWait"),98);
		
	splash.setText(Resources.getString("splashText12"),100);
	String httpPortNumber = getProperty(Common.HTTP_PORT_KEY,
					    Common.HTTP_PORT_DEFAULT);
	httpQATHandler = new HttpQATHandler(httpPortNumber, this);
	httpQATHandler.start();
						
	splash.hideSplash();
	splash.dispose();			
    }
	
    /**
     * This is the normal constructor for launching the QAT gui.
     */
    public QAT() {
	this(true);
    }
  
    private void saveFrameState(JFrame f, String key) {
	// charge Properties with frame size & position
	setSessionProperty(key+"Height",Integer.toString(f.getSize().height));
	setSessionProperty(key+"Width",Integer.toString(f.getSize().width));
	setSessionProperty(key+"X",Integer.toString((int)f.getLocation().getX()));
	setSessionProperty(key+"Y",Integer.toString((int)f.getLocation().getY()));
    }
	
    private JFrame loadFrameState(JFrame f, String key) {
	if (getSessionProperty(key+"X","")=="") {
	    f.pack();
	    f.setSize(640,480);
	}
	else {
	    // adjust screen position
	    Point location;
	    location = new Point(new Integer(getSessionProperty(key+"X")).intValue(),
				 new Integer(getSessionProperty(key+"Y")).intValue());
	    f.setLocation(location);
	    Dimension d;
	    d = new Dimension(new Integer(getSessionProperty(key+"Width")).intValue(),
			      new Integer(getSessionProperty(key+"Height")).intValue());
	    f.setSize(d);
	}
	return f;
    }
	
    /**
     * Retrieve info about last session from the specified file name.
     */
    private void loadSession(String fileName) {
	try {
	    FileInputStream in = new FileInputStream(new File(fileName));
	    Utils.touch(fileName);
	    getSessionProperties().load(in);
	    in.close();
	    // adjust screen position and size to last saved settings
	    loadFrameState((JFrame)this,"Screen");
	    // load plugins if any are configured
	}
	catch (Exception e) {
	    showError("Problem loading session.properties :"+e.getMessage(),
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
	try {
	    // load default project
	    loadProject(getSessionProperty(Common.PROJECT_PROPERTYNAME_KEY, Common.getHarnessBaseDirectory()+
					   File.separator+
					   Utils.ensureSuffix(Common.DEFAULT_PROJECTNAME,Common.PROJECT_FILE_SUFFIX)));
	}
	catch (Exception e) {
	    showError("Problem loading project :"+e.getMessage(),
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
    }
	
    /**
     * Retrieve info about last session.
     */
    private void loadSession() {
	String fileName = Common.getHarnessBaseDirectory()+File.separator+Common.SESSION_FILE_NAME;
	loadSession(fileName);
    }
	
    /**
     * Saves info about last project in use, screen size etc.
     */
    private void saveSession() {
	try {
	    // charge session Properties with current screen size & position
	    saveFrameState(this,"Screen");			
	    String fileName = Common.getHarnessBaseDirectory()+File.separator+Common.SESSION_FILE_NAME;
	    Utils.touch(fileName);
	    FileOutputStream in = new FileOutputStream(new File(fileName));
	    getSessionProperties().store(in,Common.SESSION_FILE_NAME);
	    in.flush();
	    in.close();
	}
	catch (Exception e) {
	    showError("Problem saving session.properties :"+e.getMessage(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
    }
	
    /**
     * Prompt the user for a new properties file to load.
     */
    private void loadProject() {
	JFileChooser projectFile = new JFileChooser(projectFileName);
	projectFile.setFileHidingEnabled(false);
	projectFile.setDialogTitle(Resources.getString("selectProject"));
	ExtensionFileFilter filter = new ExtensionFileFilter(Common.PROJECT_FILE_SUFFIX,"Project files");
	projectFile.addChoosableFileFilter(filter);
	projectFile.setFileFilter(filter);
	try {
	    if (projectFile.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
		loadProject(projectFile.getSelectedFile().getCanonicalPath());
	    }
	}
	catch (java.io.IOException e) {
	    showError(Resources.getString("unexpectedError")+e.getMessage(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
    }
	
    /**
     * Loads the project named in fileName.
     */
    public void loadProject(String fileName) throws IOException {
	setProjectProperties(new Properties());
	projectFileName = fileName;
	File f = new File(projectFileName);
	if (f.exists()) {
	    BufferedInputStream in;
	    getProjectProperties().load(in = new BufferedInputStream(new FileInputStream(f)));

	    in.close();
	    keywordComponent.loadKeyWordHistFromProperties(projectProperties); 

	    // check if we need to load a new look and feel
	    // for this project session
	    try {
		String defaultLF = UIManager.getLookAndFeel().getClass().getName();
		if (!getProperty(Common.LOOK_AND_FEEL_KEY,
				 defaultLF).equals(defaultLF)) {
		    try {
			UIManager.setLookAndFeel(getProperty(Common.LOOK_AND_FEEL_KEY,
							     defaultLF));
			SwingUtilities.updateComponentTreeUI(this);
		    }
		    catch (Throwable t) {
			JOptionPane.showMessageDialog(this,
						      t.toString(),
						      Resources.getString("error"),
						      JOptionPane.ERROR_MESSAGE);
		    }
		}
	    } 
	    catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	// load previous tree
	serializedTreeName = Utils.ensureSuffix(projectFileName,Common.SERIALIZED_TREE_EXTENSION);
	loadTree(serializedTreeName);

	// push the current projectProperties onto the (possibly) new testTree
	testTree.setParentQAT(this);
    }
	
    /**
     * This method creates a new, blank project using the name default.prj.
     */
    private void newProject() {
	clearProjectProperties();
	clearSessionProperties();
	setSessionProperty(Common.PROJECT_PROPERTYNAME_KEY,
			   Common.getHarnessBaseDirectory()+
			   File.separator+
			   Utils.ensureSuffix(Common.DEFAULT_PROJECTNAME,Common.PROJECT_FILE_SUFFIX));
	projectFileName = getSessionProperty(Common.PROJECT_PROPERTYNAME_KEY);
		
	// clear existing tree
	testTree.removeAllNodes();
		

	// push the current projectProperties onto the (possibly) new testTree
	testTree.setParentQAT(this);
	projectPropertiesChanged();
    }
	
    private void loadTree(String fileName) {
	try {
	    serializedTreeName = fileName;
	    File f = new File (serializedTreeName);
	    if (f.exists()) {
		BufferedInputStream in = new BufferedInputStream((new FileInputStream(f)));
		// now read the data
		ObjectInputStream serializedObject = new ObjectInputStream(in);											
		testTree.loadTests(serializedObject);
		keywordComponent.loadTestsKeywords();
		projectPropertiesChanged();
		testTreeChanged();
	    }
	    else {
		showError(Resources.getString("missingProjectSerFile")+serializedTreeName, 
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE);
	    }
	}
	catch (InvalidClassException ex) {
	    showError(Resources.getString("notCompatible"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	    projectPropertiesChanged();
	    parseTests();
	}
	catch (StreamCorruptedException x) {
	    showError(Resources.getString("corruptedFile"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	    projectPropertiesChanged();
	    parseTests();
	}
	catch (Exception e) {
	    showError("Sorry, due to new packaging you need to reparse the tests again..", 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
	finally {
	    testTree.repaint();
	}
    }
	
    private void saveProjectAs() {		
	JFileChooser projectFile = new JFileChooser(projectFileName);
	projectFile.setFileHidingEnabled(false);
	projectFile.setDialogTitle(Resources.getString("selectProjectName"));
	projectFile.setDialogType(JFileChooser.SAVE_DIALOG); 
	ExtensionFileFilter filter = new ExtensionFileFilter(Common.PROJECT_FILE_SUFFIX,"Project files");
	projectFile.addChoosableFileFilter(filter); 
	projectFile.setFileFilter(filter);
	try {
	    if (projectFile.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {				
		String oldProjectDir = Common.getProjectResultsDirectory(projectFileName);
		projectFileName = Utils.ensureSuffix(projectFile.getSelectedFile().getCanonicalPath(),Common.PROJECT_FILE_SUFFIX);
		saveProject(projectFileName,false);
		// now we need to copy the results if they exist
		StatusWindow waitWindow = new StatusWindow(this,Resources.getString("pleaseWait"),"");
		try {
		    SwingUtils.copy(oldProjectDir,Common.getProjectResultsDirectory(projectFileName),waitWindow);
		}
		catch (Exception e) {
		    // don't report errors - shouldn't be usefull here
		}
		finally {
		    waitWindow.setVisible(false);
		}										
	    }
	}
	catch (java.io.IOException e) {
	    showError(Resources.getString("unexpectedError")+e.getMessage(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);	
	}
	finally {
	    projectPropertiesChanged();
	}
    }
	
    /**
     * This method saves the current projectdisplaying GUI status windows
     * during the save process.
     */
    public void saveProject() {
	saveSession();
	saveProject(projectFileName,false);
    }
	
    /**
     * This method saves the current project without displaying any GUI status windows
     * during the save process.
     */
    public void saveProject(boolean quiet) {
	saveProject(projectFileName, quiet);
    }
	
    public void saveProject(String fileName, boolean quiet) {
	StatusWindow waitWindow;
	if (!quiet)
	    waitWindow = new StatusWindow(this,
					  Resources.getString("pleaseWait"),
					  Resources.getString("savingProject")+" :"+fileName);
	else
	    waitWindow = null;
	projectFileName = fileName;
	    
	try {
	    if (!quiet) waitWindow.setMessage(Resources.getString("projectProperties"));
	    // create the file & parent dirs if they don't exist
	    Utils.touch(projectFileName);
	    // charge with the keyword history properties
	    keywordComponent.saveKeyWordHistToProperties(projectProperties);
	    // store version information for this properties file
	    setProperty(Common.PROPERTIES_VERSION,Common.CURRENT_PROPERTIES_VERSION);

	    // save the project.properties file
	    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(projectFileName));
	    getProjectProperties().store(out,Common.SERIALIZED_NODE_HEADERV11);
	    out.flush();
	    out.close();
	    // save the tree structure
	    if (!quiet) waitWindow.setMessage(Resources.getString("savingTree"));
	    saveTree(serializedTreeName);
	}
	catch (Exception e) {
	    showError("3:"+Resources.getString("unexpectedError")+e.toString(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
	finally {
	    if (!quiet)
		waitWindow.setVisible(false);
	    projectPropertiesChanged();
	    testTreeChanged();
	}
    }
    
    private void saveTree(String fileName) {
	try {
	    serializedTreeName = fileName;
	    Utils.touch(serializedTreeName);
	    BufferedOutputStream out_ser = new BufferedOutputStream((new FileOutputStream(fileName)));
	    // now write the data
	    ObjectOutputStream serializedObject = new ObjectOutputStream(out_ser);
	    testTree.saveTests(serializedObject);
	    out_ser.flush();
	    out_ser.close();
	}
	catch (Exception e) {
	    showError("4:"+Resources.getString("unexpectedError")+e.toString(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}	
    }
	
    /**
     * This method will copy a project.prj and project.ser files, as well as the corresponding results,
     * if they exist.
     * Note : Projects may not be imported/exported across different file systems, due to the fact that
     * the file nodes are file-system specific, and each root of the test tree
     * neccesarily includes/excludes drive-specifier information, depending on the OS.
     */
    private void importProject() {
	JFileChooser projectFile = new JFileChooser(projectFileName);
	projectFile.setFileHidingEnabled(false);
	projectFile.setDialogTitle(Resources.getString("selectProjectName"));
	projectFile.setDialogType(JFileChooser.SAVE_DIALOG);
	projectFile.setApproveButtonText(Resources.getString("importProject"));
	ExtensionFileFilter filter = new ExtensionFileFilter(Common.PROJECT_FILE_SUFFIX,"Project files");
	projectFile.addChoosableFileFilter(filter); 
	projectFile.setFileFilter(filter);
	if (projectFile.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
	    StatusWindow waitWindow = new StatusWindow(this,Resources.getString("pleaseWait"),"");
	    try {
		String importProjectName = Utils.removeSuffix(Utils.extractFileName(projectFile.getSelectedFile().getCanonicalPath()));
		String importProjectPath = Utils.extractPath(projectFile.getSelectedFile().getCanonicalPath());
		String importResultsDirectory = importProjectPath+File.separator+Common.RESULTS_DIR_NAME+File.separator+importProjectName;
		String src, dest;
		waitWindow.setMessage("Project file");
		// copy the prj file to our .qat directory
		src=projectFile.getSelectedFile().getCanonicalPath();
		dest=Common.getHarnessBaseDirectory();
		//System.out.println("Copying :"+src+" to :"+dest);
		SwingUtils.copy(src,dest,waitWindow);
				
		waitWindow.setMessage("Serialiased file");
		// copy the ser file to out .qat directory
		src = Utils.ensureSuffix(projectFile.getSelectedFile().getCanonicalPath(),Common.SERIALIZED_TREE_EXTENSION);
		dest = Common.getHarnessBaseDirectory();
		//System.out.println("Copying :"+src+" to :"+dest);
		SwingUtils.copy(src,dest,waitWindow);
				
		// copy the results directory to out .qat/harness/results directory
		try {
		    waitWindow.setMessage("Results");
		    src = importResultsDirectory;
		    dest = Common.getHarnessBaseDirectory()+File.separator+Common.RESULTS_DIR_NAME+File.separator+importProjectName;
		    //System.out.println("Copying :"+src+" to :"+dest);
		    SwingUtils.copy(src,dest,waitWindow);
		}
		catch (IOException e) {
		    // don't report, it only means no results directory was found to be copied.
		}
				
		// load this project
		loadProject(Common.getHarnessBaseDirectory()+File.separator+Utils.ensureSuffix(importProjectName,Common.PROJECT_FILE_SUFFIX));
	    }
	    catch (Exception e) {
		e.printStackTrace();
	    }
	    finally {
		waitWindow.setVisible(false);
	    }
	    projectPropertiesChanged();
	}
    }
	
    private void exportProject() {
	JFileChooser projectFile = new JFileChooser(projectFileName);
	projectFile.setSelectedFile(new File(getSessionProperty(Common.PROJECT_PROPERTYNAME_KEY,
								Common.getHarnessBaseDirectory()+
								File.separator+
								Utils.ensureSuffix(Common.DEFAULT_PROJECTNAME,Common.PROJECT_FILE_SUFFIX))));
	projectFile.setFileHidingEnabled(false);
	projectFile.setDialogTitle(Resources.getString("selectExportName"));
	projectFile.setDialogType(JFileChooser.SAVE_DIALOG);
	projectFile.setApproveButtonText(Resources.getString("exportProject"));
	ExtensionFileFilter filter = new ExtensionFileFilter(Common.PROJECT_FILE_SUFFIX,"Project files");
	projectFile.addChoosableFileFilter(filter); 
	projectFile.setFileFilter(filter);
	if (projectFile.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
	    // do some sanity checking on what the user selected
	    try {
		// check if source==dest, if so exit with error
		if (projectFile.getSelectedFile().getCanonicalPath().equals(getSessionProperty(Common.PROJECT_PROPERTYNAME_KEY,
											       Common.getHarnessBaseDirectory()+
											       File.separator+
											       Utils.ensureSuffix(Common.DEFAULT_PROJECTNAME,Common.PROJECT_FILE_SUFFIX)))) {
		    showError(Resources.getString("exportErrorSameDest"),
			      Resources.getString("error"), 
			      JOptionPane.ERROR_MESSAGE);
		    return;
		}
		// check if dest already exists
		if ((new File(projectFile.getSelectedFile().getCanonicalPath())).exists()) {
		    if (JOptionPane.YES_OPTION!=JOptionPane.showConfirmDialog(this,
									      Resources.getString("exportDestExists"))) {
			return;
		    }
		}
	    }
	    catch (IOException e) {
		showError(e.toString(),
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE);
	    }
	    // now copy it
	    StatusWindow waitWindow = new StatusWindow(this,Resources.getString("pleaseWait"),"");
	    try {
		String exportProjectName = Utils.removeSuffix(Utils.extractFileName(projectFile.getSelectedFile().getCanonicalPath()));
		String exportProjectPath = Utils.extractPath(projectFile.getSelectedFile().getCanonicalPath());
		String exportResultsDirectory = exportProjectPath+File.separator+Common.RESULTS_DIR_NAME+File.separator+exportProjectName;
		String src, dest;
				
		// copy the prj file to our .qat directory
		src=Common.getHarnessBaseDirectory()+File.separator+Utils.ensureSuffix(Common.getProjectName(projectFileName),Common.PROJECT_FILE_SUFFIX);
		dest=exportProjectPath+File.separator+Utils.ensureSuffix(exportProjectName,Common.PROJECT_FILE_SUFFIX);
		//System.out.println("Copying :"+src+" to :"+dest);
		SwingUtils.copy(src,dest,waitWindow);
				
		// copy the ser file to out .qat directory
		src=Common.getHarnessBaseDirectory()+File.separator+Utils.ensureSuffix(Common.getProjectName(projectFileName),Common.SERIALIZED_TREE_EXTENSION);
		dest=exportProjectPath+File.separator+Utils.ensureSuffix(exportProjectName,Common.SERIALIZED_TREE_EXTENSION);
		//System.out.println("Copying :"+src+" to :"+dest);
		SwingUtils.copy(src,dest,waitWindow);
				
		// copy the results directory to out .qat/harness/results directory
		try {
		    src = Common.getProjectResultsDirectory(projectFileName);
		    dest=exportResultsDirectory;
		    Utils.checkSubDirsExist(dest);
		    //System.out.println("Copying :"+src+" to :"+dest);
		    SwingUtils.copy(src,dest,waitWindow);
		}
		catch (IOException e) {
		    // don't report, it only means no results directory was found to be copied.
		}
				
	    }
	    catch (Exception e) {
		e.printStackTrace();
	    }
	    finally {
		waitWindow.setVisible(false);
	    }
	}
    }
  
    public TestTree getTestTree() {
	return testTree;
    }
    
    public void firePropertiesChanged(Properties properties) {
	projectProperties = properties;
	projectPropertiesChanged();
    }
	
    private void projectPropertiesChanged() {
	testTree.setDefaultProperties(projectProperties);
	setTitle(Resources.getString("title")+" "+Utils.extractFileName(projectFileName));
	serializedTreeName = Utils.ensureSuffix(projectFileName,Common.SERIALIZED_TREE_EXTENSION);
	setSessionProperty(Common.PROJECT_PROPERTYNAME_KEY,projectFileName);
	updateStatus(Resources.getString("done")+" ("+Utils.extractFileName(projectFileName)+")");			
		
	// check if we need to restart the http server on another port
	if (httpQATHandler!=null) {
	    if (!getProperty(Common.HTTP_PORT_KEY,Common.HTTP_PORT_DEFAULT).equals(httpQATHandler.getPort())) {
		httpQATHandler.stopServer();
		httpQATHandler = new HttpQATHandler(getProperty(Common.HTTP_PORT_KEY,
								Common.HTTP_PORT_DEFAULT),this);
		httpQATHandler.start();
	    }
	}
	updateConsoleMenu();
	updatePluginMenu();
    }
	
    /**
     * Update the plugin menu to reflect the user defined
     * plugin classes we have installed.
     */
    private void updatePluginMenu() {
	try {
	    pluginMenu.removeAll();
	    if (getProperty(Common.PLUGIN_CLASSES,new String()).length()>0) {
		StringTokenizer tokens = new StringTokenizer(getProperty(Common.PLUGIN_CLASSES));
		String className;
		PluginInterface plugin;
		JMenuItem item;
		while (tokens.hasMoreTokens()) {
		    try {
			className = tokens.nextToken();
			plugin = (PluginInterface)((this.getClass().forName(className)).newInstance());
			pluginList.put(plugin.getPluginName(),plugin);
			item = new JMenuItem(plugin.getPluginName());
			item.addActionListener(this);
			pluginMenu.add(item);
		    }
		    catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	    else {
		pluginMenu.add(new JMenuItem(Resources.getString("pluginMenuEmpty")));
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
	
    /**
     *  Set the agent console menu to reflect new list of agents.
     */
    private void updateConsoleMenu() {
	int agentCount = Integer.parseInt(getProperty(Common.AGENT_COUNT,"0"));
	int currAgent;
	JMenuItem item;
	consoleMenu.removeAll();
	for (int i = 0; i < agentCount;i++) {
	    currAgent = i+1;
	    consoleMenu.add(item = new JMenuItem("Agent "+currAgent+" ("+
						 getProperty(Common.host+currAgent+Common.hostNamePattern)+" "+
						 getProperty(Common.host+currAgent+Common.hostPortPattern)+")"));
	    item.addActionListener(this);
	}
    }
	
    private void testTreeChanged() {
	testTree.setParentQAT(this);
    }
	
    private void addMenuBar() {
	// ---------- set up the system menu ------------
	menuBar = new JMenuBar();
	JMenu menu;
	JMenuItem item;
	// ------------------add the file menu-------------------
	menu = new JMenu(Resources.getString("file"));
	// create new blank project
	menu.add(item = new JMenuItem(Resources.getString("newProject"),
				      new ImageIcon(Resources.getImageResource("newProjectImage"))));		
	item.addActionListener(this);
	// load project
	menu.add(item = new JMenuItem(Resources.getString("loadProject"),
				      new ImageIcon(Resources.getImageResource("loadProjectImage"))));
	item.addActionListener(this);
	// save project
	menu.add(item = new JMenuItem(Resources.getString("saveProject"),
				      new ImageIcon(Resources.getImageResource("saveProjectImage"))));
	item.addActionListener(this);
	// save project as
	menu.add(item = new JMenuItem(Resources.getString("saveProjectAs"),
				      new ImageIcon(Resources.getImageResource("saveProjectAsImage"))));
	item.addActionListener(this);
				
	menu.addSeparator();
	// import/export project
	menu.add(item = new JMenuItem(Resources.getString("importProject"),
				      new ImageIcon(Resources.getImageResource("importImage"))));
	item.addActionListener(this);
	menu.add(item = new JMenuItem(Resources.getString("exportProject"),
				      new ImageIcon(Resources.getImageResource("exportImage"))));
	item.addActionListener(this);
	menu.addSeparator();
	// launch new QAT instance
	menu.add(item = new JMenuItem(Resources.getString("launchQatInstance")));
	item.addActionListener(this);
	// close this QAT instance
	menu.add(item = new JMenuItem(Resources.getString("closeQatInstance")));
	item.addActionListener(this);
	menu.addSeparator();
		
	// run selected tests
	menu.add(item = new JMenuItem(Resources.getString("runSelectedTests"),
				      new ImageIcon(Resources.getImageResource("runSelectedImage"))));		
	item.addActionListener(this);
	// stop current run
	menu.add(item = new JMenuItem(Resources.getString("stopCurrentRun"),
				      new ImageIcon(Resources.getImageResource("stopRunImage"))));
	item.addActionListener(this);	
	menu.addSeparator();
	// reparse tests from current working directory
	menu.add(item = new JMenuItem(Resources.getString("reparseTests"),
				      new ImageIcon(Resources.getImageResource("parseAllImage"))));
	item.addActionListener(this);
	// reparse selected tests
	menu.add(item = new JMenuItem(Resources.getString("parseSelected"),
				      new ImageIcon(Resources.getImageResource("parseSelectedImage"))));
	item.addActionListener(this);
	menu.addSeparator();
	// exit
	menu.add(item = new JMenuItem(Resources.getString("exit"),
				      new ImageIcon(Resources.getImageResource("exitImage"))));
	item.addActionListener(this);
	menuBar.add(menu);
	// ------------------add the edit menu-------------------
	menu = new JMenu(Resources.getString("edit"));
	// select all tests
	menu.add(item = new JMenuItem(Resources.getString("selectAll"),
				      new ImageIcon(Resources.getImageResource("selectAllImage"))));
	item.addActionListener(this);
	menu.addSeparator();
	menu.add(item = new JMenuItem(Resources.getString("projectSettings"),
				      new ImageIcon(Resources.getImageResource("projectSettingsImage"))));
	item.addActionListener(this);
	menuBar.add(menu);

	// ------------------add the agents menu-------------------
	menu = new JMenu(Resources.getString("agents"));
	consoleMenu = new JMenu(Resources.getString("connect"));
	consoleMenu.add(item = new JMenuItem(Resources.getString("connectAgentEmpty"),
					     new ImageIcon(Resources.getImageResource("connectImage"))));
	menu.add(consoleMenu);		

	menu.add(item = new JMenuItem(Resources.getString("agentSettings"),
				      new ImageIcon(Resources.getImageResource("agentSettingsImage"))));
	item.addActionListener(this);
	menuBar.add(menu);

	// ------------------add the plugin menu-------------------
	pluginMenu = new JMenu(Resources.getString("plugin"));
	pluginMenu.add(item = new JMenuItem(Resources.getString("pluginMenuEmpty"),
					    new ImageIcon(Resources.getImageResource("pluginImage"))));
	menuBar.add(pluginMenu);

	// ------------------add the report menu-------------------
	menu = new JMenu(Resources.getString("report"));
	// compare test results
	menu.add(item = new JMenuItem(Resources.getString("compareResults"),
				      new ImageIcon(Resources.getImageResource("compareImage"))));
	item.addActionListener(this);
	menu.addSeparator();
	// print all tests
	menu.add(item = new JMenuItem(Resources.getString("printAll"),
				      new ImageIcon(Resources.getImageResource("printImage"))));
	item.addActionListener(this);
	// print all tests (sort by status)
	menu.add(item = new JMenuItem(Resources.getString("printAllSorted"),
				      item.getIcon()));
	item.addActionListener(this);
	// print all tests (non-passed only)
	menu.add(item = new JMenuItem(Resources.getString("printAllNonPassed"),
				      item.getIcon()));
	item.addActionListener(this);
	// print as text file
	menu.add(item = new JMenuItem(Resources.getString("printAsTextFile"),
				      item.getIcon()));
	item.addActionListener(this);
	menu.addSeparator();
	// generate Html report
	menu.add(item = new JMenuItem(Resources.getString("generateHtmlReport"),
				      new ImageIcon(Resources.getImageResource("htmlImage"))));
	item.addActionListener(this);
	menuBar.add(menu);
	// ------------------add the help menu-------------------
	menu = new JMenu(Resources.getString("help"));
	menu.add(item = new JMenuItem(Resources.getString("about"),
				      new ImageIcon(Resources.getImageResource("aboutImage"))));
	item.addActionListener(this);
	menuBar.add(menu);
	setJMenuBar(menuBar);
    }
	
    /**
     * Create the toolbar.  By default this reads the 
     * resource file for the definition of the toolbar.
     */
    private void addToolBar() {
	Container frame = getContentPane();
	toolbar = new JToolBar();
	String[] toolKeys = {"loadProject", "saveProject", "-", "-", 
			     "runSelected", "parseSelected","stopRun","-", "-", 
			     "timerRun", "-", "-",							 
			     "expandAll",
			     "collapseTree",
			     "expandTree"};
	for (int i = 0; i < toolKeys.length; i++) {
	    if (toolKeys[i].equals("-")) {
		toolbar.add(Box.createHorizontalStrut(5));
	    } 
	    else {
		toolbar.add(createToolbarButton(toolKeys[i]));
	    }
	}
	toolbar.add(Box.createHorizontalGlue());
	frame.add(toolbar,BorderLayout.NORTH);
    }
	
    private JButton createToolbarButton(String key) {
	URL url = Resources.getImageResource(key + "Image");
        JButton b = new JButton(new ImageIcon(url)) {
		public float getAlignmentY() { return 0.5f; }
	    };
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1,1,1,1));
		
	b.setToolTipText(Resources.getString(key + "ToolTip"));
		
	b.addActionListener(this); 
        return b;
    }
	
    private void addStatusBar() {
	//  add the status bar here.
	JPanel statusBar = new JPanel(new GridLayout(1,3));
	statusLabel = new JLabel("",SwingConstants.CENTER);
	statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	statusBar.add(statusLabel);
	parserLabel = new JLabel("",SwingConstants.CENTER);
	parserLabel.setForeground(Color.black);
	parserLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	statusBar.add(parserLabel);
	JPanel p2 = new JPanel(new GridLayout(1,1));
	statusGauge = new Gauge();
	statusGauge.setForeground(Color.green, Color.red, Color.blue, new Color(99,99,156), Color.orange);
	p2.add(statusGauge);
	p2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	statusBar.add(p2);
	runtimeComponent.setStatus(Resources.getString("ready"),0,0,0,0,0,0);
	getContentPane().add(statusBar,BorderLayout.SOUTH);
    }
	
    private void addCenterPanel(SplashScreen splash) {
	// create the TestTree
	splash.setText(Resources.getString("splashText4"),38);
	testTree = new TestTree();
		
	// create the runtime panel
	splash.setText(Resources.getString("splashText5"),45);
	runtimeComponent = new RuntimeComponent(this);				
		
	// create the keyword component
	splash.setText(Resources.getString("splashText6"),52);
	keywordComponent = new KeywordComponent(testTree);
		
	// create the ParserOutputViewer
	splash.setText(Resources.getString("splashText7"),59);
	parserOutputViewer = new ParserOutputViewer(this);
	testTree.addTreeSelectionListener(parserOutputViewer);
	testTree.addChangeListener(parserOutputViewer);
		
	// create the test detail viewer
	splash.setText(Resources.getString("splashText8"),66);
	testDetailViewer = new TestDetailViewer(this);
	testTree.addTreeSelectionListener(testDetailViewer);
	testTree.addChangeListener(testDetailViewer);
				
	JComponent detail = new JLabel("right");
	JTabbedPane runtimeTab = new JTabbedPane();
	runtimeTab.add("Parser Output",parserOutputViewer);
	runtimeTab.add("Runtime",runtimeComponent);
	runtimeTab.add("Keywords",keywordComponent);
		
	leftrightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,true,testTree,testDetailViewer);
	leftrightSplit.setDividerSize(5);

	JComponent bottom = new JLabel("bottom");
	topbottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,leftrightSplit,runtimeTab);
	getContentPane().add(topbottomSplit,BorderLayout.CENTER);					
	try {
	    topbottomSplit.setResizeWeight(1);	
	}
	catch (Throwable ex) {
	    // this method does not exist in jdk1.2.2
	    System.out.println("Warning "+ex.toString());
	}
    }
	
    /**
     * Set the status bar to display the message specified in msg.
     * @param msg - the message to display.
     */
    public void setStatus(String msg) {
	statusLabel.setText(msg);
	statusLabel.repaint();
	runtimeComponent.updateMemoryStatus();
	runtimeComponent.updateRunProgress(testRunner.getRunTotal(),
					   testRunner.getRunProgress(),
					   testRunner.getElapsedTime(),
					   testRunner.getRemainingTime());
    }
	
    public Object[] calculateStatus() {
	return calculateStatus(testTree.getAllTests());
    }
	
    public Object[] calculateStatus(ArrayList tests) {
	int passed=0, failed=0, unresolved=0, notrun=0, pending=0;
	statusGauge.setMax(tests.size());
	for (int i = 0; i < tests.size(); i++) {
	    switch (((TestSpecification)tests.get(i)).getStatus()) {
	    case ProtocolConstants.PASSED : passed++;
		break;
	    case ProtocolConstants.FAILED : failed++;
		break;
	    case ProtocolConstants.UNRESOLVED : unresolved++;
		break;
	    case ProtocolConstants.NOTRUN : notrun++;
		break;				
	    case ProtocolConstants.PENDING : pending++;
		break;
	    }
	}
	Object objArray[] = new Object[5];
	objArray[0] = new Integer(passed);
	objArray[1] = new Integer(failed);
	objArray[2] = new Integer(unresolved);
	objArray[3] = new Integer(notrun);
	objArray[4] = new Integer(pending);
	return objArray;
    }		
	
    /**
     * This form automatically calculates all the values required to display the entire test
     * tree results.
     */
	
    public void updateStatus(String msg) {
	updateStatus(msg,0);		
    }
	
    public void updateStatus(String msg, double runCount) {
	updateStatus(msg,testTree.getAllTests(),runCount);		
    }
	
    public void updateStatus(String msg, ArrayList tests) {
	updateStatus(msg,tests,0);
    }
	
    public void updateStatus(String msg, ArrayList tests, double runCount) {
	Object objArray[] = calculateStatus(tests);
	runtimeComponent.setStatus(msg,
				   ((Integer)objArray[0]).doubleValue(), // passed
				   ((Integer)objArray[1]).doubleValue(), // failed
				   ((Integer)objArray[2]).doubleValue(), // unresolved
				   ((Integer)objArray[3]).doubleValue(), // notrun
				   ((Integer)objArray[4]).doubleValue(), // pending
				   runCount); // running
    }
	
    private void loadTests() {
	setWaitCursor();
	JFileChooser testDir = new JFileChooser(testTree.getProjectRoot());
	testDir.setFileHidingEnabled(false);
	testDir.setDialogTitle(Resources.getString("selectTestDir"));
	testDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	try {
	    if (testDir.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
		StatusWindow waitWindow = new StatusWindow(this,
							   Resources.getString("pleaseWait"),
							   Resources.getString("loadingProject"));
		testTree.loadNewTestDir(testDir.getSelectedFile().getCanonicalPath(), 
					waitWindow);
		waitWindow.setVisible(false);
	    }
	}
	catch (java.io.IOException e) {
	    showError(Resources.getString("unexpectedError")+e.getMessage(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
	// update the keyword list
	keywordComponent.loadTestsKeywords();
	setDefaultCursor();
	// reload the properties
	projectPropertiesChanged();
	testTreeChanged();
    }
	
    public void parseTests() {
	parseTests(true);
    }
	
    public void parseTests(boolean visible) {
	StatusWindow waitWindow = new StatusWindow(this,
						   Resources.getString("pleaseWait"),
						   Resources.getString("reloadingProject"),
						   visible);
	setWaitCursor();
	// remove previous trace & stderr/stdout files
	waitWindow.setMessage(getProperty(Common.PROJECTPATH_KEY, Resources.getString("clearResults")));
	try {
	    SwingUtils.delete(Common.getProjectResultsDirectory(projectFileName),waitWindow);
	}
	catch (IOException e) {
	    //e.printStackTrace();
	    // not too important - we can ignore this error
	    // because maybe it doesn't exist yet
	}
			
	// load the new tests of the disk
	waitWindow.setMessage(getProperty(Common.PROJECTPATH_KEY, testTree.getProjectRoot()));
	testTree.loadNewTestDir(getProperty(Common.PROJECTPATH_KEY, testTree.getProjectRoot()),
				waitWindow);
	
	// update the keyword list
	keywordComponent.loadTestsKeywords();
	setDefaultCursor();
	waitWindow.setVisible(false);
	projectPropertiesChanged();
	testTreeChanged();
	updateStatus(Resources.getString("finishedParsing"));
    }
	
    /**
     * This will relook at the disk only from this directory onwards, instead of re-parsing the entire
     * tree off disk.
     */
    public void reloadDirectory(TestTreeNode testNode) {
	StatusWindow waitWindow = new StatusWindow(this,
						   Resources.getString("pleaseWait"),
						   Resources.getString("reloadDirectory"));
	testTree.parseTestsFrom(testNode,waitWindow);
	waitWindow.setVisible(false);
	updateStatus(Resources.getString("finishedParsing"));
    }	
	
    public void parseTest(TestTreeNode testNode, ParserInterface parser) {
	updateStatus(testNode.toString());
	testTree.parseTestNode(testNode,parser);
	testTree.nodeChanged(testNode);
    }
	
    public void parseSelectedTests() {
	// make sure we cannot start another run while this one is running
	if ((!testRunner.isRunning())&&
	    (!timerRunner.isRunning())) {
	    testRunner = new TestRunner(this);
	    testRunner.setEvaluationMode(true);
	    ArrayList selectedTests = testTree.getSelectedTests();
	    if (selectedTests.size()>0) {
		// make sure we have at least one agent defined
		int agentCount = Integer.parseInt(getProperty(Common.AGENT_COUNT,"0"));
		if (agentCount>0) {
		    // check if we need to clear their status first
		    if (new Boolean(getProperty(Common.RESET_STATUS,"false")).booleanValue())
			testTree.resetSelectedTestStatus();
		    testRunner.setRunList(selectedTests);
		    testRunner.setProjectRoot(testTree.getProjectRoot());
		    testRunner.start();
		    statusGauge.setMax(selectedTests.size());
		}
		else {
		    showError(Resources.getString("noAgentsDefined"), 
			      Resources.getString("error"), 
			      JOptionPane.ERROR_MESSAGE); 
		}
	    }
	    else {
		showError(Resources.getString("noTestsSelected"), 
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE); 
	    }
	}
	else {
	    showError(Resources.getString("runInProgress"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE); 
	}		
    }
	
    /**
     * This forces a repaint of the QAT gui - used in TestTree
     * when changing the status window.
     */
    public void update() {
	toolbar.repaint();
	menuBar.repaint();
    }
	
    private void editProject() {
	EditProject editProject = new EditProject(this,projectProperties,Utils.extractFileName(projectFileName));		
    }
	
    public void editProjectCallback(EditProject editProject) {
	projectProperties = editProject.getProperties();
	projectPropertiesChanged();
	keywordComponent.loadKeyWordHistFromProperties(projectProperties);
	if (editProject.needToReloadTests()) {
	    StatusWindow waitWindow = new StatusWindow(this,
						       Resources.getString("pleaseWait"),
						       Resources.getString("reloadingProject"));
	    testTree.loadNewTestDir(getProperty(Common.PROJECTPATH_KEY, Common.getHarnessBaseDirectory()),
				    waitWindow);
	    setProperty(Common.PROJECTPATH_KEY,testTree.getProjectRoot());
	    waitWindow.setVisible(false);
	}			
    }
	
    private void editAgentSettings() {
	AgentInfo editAgents = new AgentInfo(this,Resources.getString("agentSettings"),projectProperties);		
    }
	
    public void editAgentSettingsCallback(AgentInfo editAgents) {
	editAgents.updateProperties(projectProperties);
	projectPropertiesChanged();
    }
	
    public boolean isTestRunning() {
	return ((testRunner.isRunning())||
		(timerRunner.isRunning()));
    }
	
    public void stopTestRun() {
	// make sure we cannot start another run while this one is running
	if (testRunner.isRunning()) {
	    testRunner.interrupt();
	    updateStatus(Resources.getString("interrupted"));
	    return;
	}
	if (timerRunner.isRunning()) {
	    timerRunner.interrupt();
	    updateStatus(Resources.getString("interrupted"));
	    return;
	}
	showError(Resources.getString("noRunInProgress"), 
		  Resources.getString("error"), 
		  JOptionPane.ERROR_MESSAGE); 
    }
	
    /**
     * Selects all the tests in the tree.
     */
    public void selectAll() {
	testTree.selectAll();
    }
	
    public void generateHtmlReport() {
	JFileChooser dirChooser = new JFileChooser(projectFileName);
	dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	dirChooser.setDialogTitle(Resources.getString("selectHtmlRootDir"));
	dirChooser.setApproveButtonText("Generate");
	if (dirChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
	    StatusWindow status=status = new StatusWindow(this,Resources.getString("generating"),Resources.getString("pleaseWait"));
	    try {
		generateHtmlReport(dirChooser.getSelectedFile().getCanonicalPath(),status);
	    }
	    catch (Exception ex) {
		showError(Resources.getString("unexpectedError")+ex.getMessage(), 
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE);	
	    }
	    finally {
		status.setVisible(false);
	    }
	}
    }
	
    public void generateHtmlReport(String directoryRoot, StatusWindow status) throws Exception {
	HttpReport report = new HttpReport(this,"http://"+InetAddress.getLocalHost().getHostName()+":"+
					   getProperty(Common.HTTP_PORT_KEY,
						       Common.HTTP_PORT_DEFAULT),
					   directoryRoot,
					   status);
    }
	
    private boolean match(String keys[], String key) {
	for (int i = 0; i < keys.length; i++)
	    if (keys[i].indexOf(key)>=0)
		return true;
	return false;
    } 
	
    private void timerRun() {
	// make sure we cannot start another run while this one is running
	if (timerRunner.isRunning()==false) {
	    ArrayList selectedTests = testTree.getSelectedTests();
	    if (selectedTests.size()>0) {
		if (timerRunner.promptForDate()) {
		    timerRunner.setRunList(selectedTests);
		    timerRunner.start();
		}
		else {
		    timerRunner.interrupt();
		}
	    }
	    else {
		showError(Resources.getString("noTestsSelected"), 
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE); 
	    }
	}
	else {
	    showError(Resources.getString("runInProgress"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE); 
	}
    }
	
    private void timerStop() {
	if (timerRunner!=null) {
	    timerRunner.interrupt();
	}
    }
	
    /**
     * This method runs all tests with status PASSED, returning false if none were run for
     * some reason (such as none exist etc).
     * Used by the Http interface.
     */
    public boolean runPassedTests() {
	return runTestsWithStatus(ProtocolConstants.PASSED);
    }
	
    /**
     * This method runs all tests with status FAILED, returning false if none were run for
     * some reason (such as none exist etc).
     * Used by the Http interface.
     */
    public boolean runFailedTests() {
	return runTestsWithStatus(ProtocolConstants.FAILED);
    }
	
    /**
     * This method runs all tests with status UNRESOLVED, returning false if none were run for
     * some reason (such as none exist etc).
     * Used by the Http interface.
     */
    public boolean runUnresolvedTests() {
	return runTestsWithStatus(ProtocolConstants.UNRESOLVED);
    }
	
    /**
     * This method runs all tests with status NOTRUN, returning false if none were run for
     * some reason (such as none exist etc).
     * Used by the Http interface.
     */
    public boolean runNotRunTests() {
	return runTestsWithStatus(ProtocolConstants.NOTRUN);
    }
	
    /**
     * This method runs all tests with matching status, returning false if none were run for
     * some reason (such as none exist etc).
     * Used by the Http interface.
     */
    public boolean runTestsWithStatus(int status) {
	testTree.unSelectAll();
	testTree.selectAllWithStatus(status);
	if (testTree.getSelectedTests().size()==0)
	    return false;
	else
	    runSelectedTests();
	return true;
    }
	
    public void runSelectedTests() {
	runSelectedTests(false);
    }
	
    public void runSelectedTests(boolean quiet) {
	// make sure we cannot start another run while this one is running
	if ((!testRunner.isRunning())&&
	    (!timerRunner.isRunning())) {
	    testRunner = new TestRunner(this);
	    ArrayList selectedTests = testTree.getSelectedTests();
	    if (selectedTests.size()>0) {
		// make sure we have at least one agent defined
		int agentCount = Integer.parseInt(getProperty(Common.AGENT_COUNT,"0"));
		if (agentCount>0) {
		    // check if we need to clear their status first
		    if (new Boolean(getProperty(Common.RESET_STATUS,"false")).booleanValue())
			testTree.resetSelectedTestStatus();
		    testRunner.setRunList(selectedTests);
		    testRunner.setProjectRoot(testTree.getProjectRoot());
		    testRunner.start();
		    statusGauge.setMax(selectedTests.size());
		}
		else {
		    showError(Resources.getString("noAgentsDefined"), 
			      Resources.getString("error"), 
			      JOptionPane.ERROR_MESSAGE); 
		}
	    }
	    else {
		showError(Resources.getString("noTestsSelected"), 
			  Resources.getString("error"), 
			  JOptionPane.ERROR_MESSAGE); 
	    }
	}
	else {
	    showError(Resources.getString("runInProgress"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE); 
	}
    }
	
    public TestSpecification getSelectedTest() {
	ArrayList selectedTests = testTree.getSelectedTests();
	return (TestSpecification)selectedTests.get(0);
    }
	
    /**
     * This allows a single test to be run.
     * It is normally called by the Run method of NodeMenu when you
     * right-click a test.
     */
    public void runSingleTest(TestSpecification test) {
	// make sure we cannot start another run while this one is running
	if ((!testRunner.isRunning())&&
	    (!timerRunner.isRunning())) {
	    testRunner = new TestRunner(this);
	    ArrayList v = new ArrayList(1);
	    v.add(test);
	    testRunner.setRunList(v);
	    testRunner.setProjectRoot(testTree.getProjectRoot());
	    testRunner.start();
	}
	else {
	    showError(Resources.getString("runInProgress"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE); 
	}
    }
	
    /**
     * This method is called by the testRunner once the test result is known, and allows the test
     * node to be repainted with the proper result.
     */
    public void updateTest(TestSpecification test, boolean evaluating) {
	testTree.setTest(test);
	// only autosave during test run, not during evaluation parse
	if (!evaluating) {
	    // now save the tree if autosave is enabled and the test is not RUNNING
	    if ((getProperty(Common.AUTOSAVE_PROJECT,"false").equals("true"))&&
		(test.getStatus()!=ProtocolConstants.RUNNING)) {
		saveProject(true);
	    }
	}
    }
	
    /**
     * This method saves this instances session and project properties, and if
     * no run/parse is in progress, will close the window.
     * If it is the last visible Qat instance, the JVM will
     * be halted as well.
     */
    public void closeQatInstance(boolean quiet) {
	// make sure we cannot exit while running tests
	if ((!testRunner.isRunning())&&
	    (!timerRunner.isRunning())) {
	    saveProject(quiet);
	    instanceList.remove(this);
	    setVisible(false);
	    dispose();
	    if (instanceList.size()==0)
		System.exit(0);
	}
	else {
	    showError(Resources.getString("runInProgress"), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE); 
	}
    }
	
    private void exit() {
	Object instanceListArray[] = instanceList.toArray();
	if (instanceListArray.length>1) {
	    if (JOptionPane.showConfirmDialog(this, 
					      Resources.getString("closeAllQatInstances"),
					      Resources.getString("warning"),
					      JOptionPane.YES_NO_OPTION,
					      JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION) {
		for (int i = 0; i< instanceListArray.length; i++) {
		    ((QAT)instanceListArray[i]).closeQatInstance(false);
		}
	    }
	}
	else {
	    ((QAT)instanceListArray[0]).closeQatInstance(false);
	}
    }
	
    private void compareResults() {
	CompareResults compareResults = new CompareResults(this,
							   Utils.ensureSuffix(projectFileName,Common.SERIALIZED_TREE_EXTENSION),
							   Utils.ensureSuffix(projectFileName,Common.SERIALIZED_TREE_EXTENSION));
    }
	
    private void launchQatInstance() {
	QAT newQat = new QAT();
    }
	
    private void printTestTree(int printType) {		
	PrintManager p = new PrintManager("Print job title",this);
	if (p.startPrint()) {
	    StatusWindow waitWindow = new StatusWindow(this,Resources.getString("pleaseWait"),"Printing in progress");
	    ReportObject report = new ReportObject(testTree);
	    report.printTree(p,projectFileName,printType);
	    p.endPrint();
	    waitWindow.setVisible(false);
	}
    }
	
    private void setWaitCursor() {
	if (waitCursor==null)
	    waitCursor=new Cursor(Cursor.WAIT_CURSOR);
	setCursor(waitCursor);
    }
	
    private void setDefaultCursor() {
	if (defaultCursor==null)
	    defaultCursor=new Cursor(Cursor.DEFAULT_CURSOR);
	setCursor(defaultCursor);		
    }
	
    public Properties getProjectProperties() {
	return projectProperties;
    }
	
    public void setProjectProperties(Properties p) {
	projectProperties = p;
    }
	
    public void clearProjectProperties() {
	projectProperties.clear();
    }
	
    /**
     * This allows other classes access to the project property values.
     */
    public String getProperty(String key, String defaultValue) {
	return projectProperties.getProperty(key,defaultValue);
    }
	
    /**
     * This allows other classes access to the project property values.
     */
    public String getProperty(String key) {
	return projectProperties.getProperty(key);
    }
	
    /**
     * This allows other classes access to the project property values.
     */
    public void setProperty(String key, String defaultValue) {
	projectProperties.setProperty(key,defaultValue);
    }
	
    /**
     * This allows other classes access to the project property values.
     */
    public Properties getProperties() {
	return projectProperties;
    }
	
    /**
     * This allows us to retrieve properties for this
     * session and this Qat instance.
     */
    private Properties getSessionProperties() {
	return sessionProperties;
    }
	
    /**
     * This allows us to set properties for this
     * session and this Qat instance.
     */
    private static void setSessionProperties(Properties newSessionProperties) {
	sessionProperties = newSessionProperties;
    }
	
    /**
     * This allows us to retrieve properties for this
     * session and this Qat instance.
     */
    private String getSessionProperty(String key, String defaultValue) {
	return sessionProperties.getProperty(key+instanceSuffix,
					     defaultValue);
    }
	
    /**
     * This allows us to retrieve properties for this
     * session and this Qat instance.
     */
    private String getSessionProperty(String key) {
	return sessionProperties.getProperty(key+instanceSuffix);
    }
	
    /**
     * This allows us to set properties for this
     * session and this Qat instance.
     */
    private void setSessionProperty(String key, String value) {
	sessionProperties.setProperty(key+instanceSuffix,
				      value);
    }
	
    /**
     * This allows us to clear properties for this
     * session and this Qat instance.
     */
    private void clearSessionProperties() {
	sessionProperties.clear();
    }
	
    /**
     * This allows other classes to update the parser output window.
     */
    public void setParserView(TestSpecification test) {
	parserOutputViewer.setObjectView(test);
	testDetailViewer.setObjectView(test);
    }
	
    public void setStatusGauge(double passed, double failed, double unresolved, double pending, double notrun) {
	statusGauge.setValue(passed, 
			     failed, 
			     unresolved,
			     pending, 
			     notrun);
    }
	
    public void setStatusGaugeMax(int max) {
	statusGauge.setMax(max);
    }
	
    public void addChangeListener(ChangeListener listener) {
	getTestTree().addChangeListener(listener);
    }
	
    public void removeChangeListener(ChangeListener listener) {
	getTestTree().removeChangeListener(listener);
    }
	
    private void activatePlugin(String description) {
	PluginInterface plugin = ((PluginInterface)pluginList.get(description));
	plugin.activatePlugin(this);
    }
	
    //	public void removeChangeListener(ChangeListener listener) {
    //		getTestTree().removeChangeListener(listener);
    //	}
	
    /**
     * Used to fulfil the QATInterface requirement to allow
     * plugins to create modal dialogs based on the same
     * Swing worker thread etc.
     */
    public JFrame getOwnerHandle() {
	return this;
    }
	
    /**
     * Returns a handle to the label which displays the current QASH command being
     * parsed. Used by ParserInterface.
     */
    public JLabel getParserStatusLabel() {
	return parserLabel;
    }
	
    /**
     * This method returns the directory into which all parser trace files are put,
     * and all traces retrieved from the agent by the parser.
     */
    public String getProjectResultsDirectory() {
	return Common.getProjectResultsDirectory(projectFileName);
    }
	
    /* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JMenuItem) {
	    // exit
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("exit"))) {
		exit();
	    }
	    // stop test run
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("stopCurrentRun"))) {
		stopTestRun();
		return;
	    }
	    // run selected tests
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("runSelectedTests"))) {
		runSelectedTests();
		return;
	    }
	    // compare test results
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("compareResults"))) {
		compareResults();
		return;
	    }
	    // printall tests sorted
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("printAll"))) {
		printTestTree(0);
		return;
	    }
	    // printall  tests
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("printAllSorted"))) {
		printTestTree(1);
		return;
	    }
	    // printall nonpassed tests
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("printAllNonPassed"))) {
		printTestTree(2);
		return;
	    }
	    // print to text file
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("printAsTextFile"))) {
		JFileChooser textFileDialog = new JFileChooser(projectFileName);
		textFileDialog.setDialogTitle(Resources.getString("printAsTextFile"));
		textFileDialog.setApproveButtonText("Print");
		ExtensionFileFilter filter = new ExtensionFileFilter("txt","Text file");
		textFileDialog.addChoosableFileFilter(filter);
		textFileDialog.setFileFilter(filter);
		try {
		    if (textFileDialog.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
			ReportObject report = new ReportObject(testTree);
			report.printAsTextFile(textFileDialog.getSelectedFile().getCanonicalPath(),projectFileName);
		    }
		}
		catch (java.io.IOException ex) {
		    showError(Resources.getString("unexpectedError")+ex.getMessage(), 
			      Resources.getString("error"), 
			      JOptionPane.ERROR_MESSAGE);	
		}
		return;
	    }
	    // generate Html report
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("generateHtmlReport"))) {
		generateHtmlReport();				
		return;
	    }
	    // select all tests
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("selectAll"))) {
		selectAll();
		return;
	    }
	    // load tests from directory
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("selectTestDir"))) {			
		loadTests();
		return;			
	    }
	    // reparse tests in current test directory
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("reparseTests"))) {
		parseTests();
		return;			
	    }	
	    // reparse selected tests
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("parseSelected"))) {
		parseSelectedTests();
		return;			
	    }	
	    // import project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("importProject"))) {
		importProject();
		return;
	    }
	    // export project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("exportProject"))) {
		exportProject();
		return;
	    }
	    // new Qat instance
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("launchQatInstance"))) {
		launchQatInstance();
		return;
	    }
	    // close Qat instance
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("closeQatInstance"))) {
		closeQatInstance(false);
		return;
	    }
	    // new project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("newProject"))) {
		newProject();
		return;
	    }
	    // load project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("loadProject"))) {
		loadProject();
		return;
	    }
	    // save project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("saveProject"))) {
		saveProject();
		return;
	    }
	    // save project as
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("saveProjectAs"))) {
		saveProjectAs();
		return;
	    }
	    // edit project
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("projectSettings"))) {
		editProject();
		return;
	    }
	    // edit editAgent Settings
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("agentSettings"))) {
		editAgentSettings();
		return;
	    }
	    // connect to agent console
	    if (((JMenuItem)e.getSource()).getText().startsWith("Agent ")) {
		String s = ((JMenuItem)e.getSource()).getText();
		String agentIndex = s.substring(s.indexOf(' ')+1,s.indexOf(' ')+2);
		s = s.substring(s.indexOf('(')+1,s.lastIndexOf(')'));
		String agentName = s.substring(0,s.indexOf(' '));
		String agentPort = s.substring(s.indexOf(' ')+1,s.length());
		AgentConsole agentConsole  = new AgentConsole(agentIndex,
							      agentName,
							      Integer.parseInt(agentPort),
							      Integer.parseInt(getProperty(Common.CONSOLE_DEBUG_LEVEL,
											   Common.CONSOLE_DEBUG_LEVEL_VALUE)),
							      Integer.parseInt(getProperty(Common.CONSOLE_BUFFER_SIZE,
											   Common.CONSOLE_BUFFER_SIZE_VALUE)));
		Thread x = new Thread(agentConsole);
		x.start();
		return;
	    }
			
	    // about dialog
	    if (((JMenuItem)e.getSource()).getText().equals(Resources.getString("about"))) {		
		AboutDialog about = new AboutDialog(this);
		return;
	    }
			
	    // could be a plugin launch
	    if (pluginList.get(((JMenuItem)e.getSource()).getText())!=null) {
		activatePlugin(((JMenuItem)e.getSource()).getText());
		return;
	    }			
	}
	if (e.getSource() instanceof JButton) {
	    // load project (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("loadProjectToolTip"))) {	
		loadProject();
		return;
	    }
	    // run selected tests (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("runSelectedToolTip"))) {	
		runSelectedTests();
		return;
	    }
	    // parse selected tests (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("parseSelectedToolTip"))) {	
		parseSelectedTests();
		return;
	    }
	    // stop run (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("stopRunToolTip"))) {	
		stopTestRun();
		return;
	    }
	    // timer run (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("timerRunToolTip"))) {	
		timerRun();
		return;
	    }
	    // save project (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("saveProjectToolTip"))) {	
		saveProject();
		return;
	    }
	    // expand test tree level (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("expandTreeToolTip"))) {	
		testTree.expandLevel();
		return;
	    }
	    // expand entire tree (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("expandAllToolTip"))) {	
		testTree.expandAll();
		return;
	    }
	    // collapse test tree level (toolbar button)
	    if (((JButton)e.getSource()).getToolTipText().equals(Resources.getString("collapseTreeToolTip"))) {	
		testTree.collapseLevel();
		return;
	    }
	    showError("Unhandled button :"+((JButton)e.getSource()).getText(), 
		      Resources.getString("error"), 
		      JOptionPane.ERROR_MESSAGE);
	}
    }
	
    public void windowClosed(WindowEvent e) {
    }
	
    public void windowIconified(WindowEvent e) {
    }
	
    public void windowDeiconified(WindowEvent e) {
    }
	
    public void windowActivated(WindowEvent e) {
    }
	
    public void windowDeactivated(WindowEvent e) {
    }
	
    public void windowClosing(WindowEvent e) {
	closeQatInstance(false);
    }
	
    public void windowOpened(WindowEvent e) {
    }
	
    private void showError(String message, String title, int type) {
	if (isVisible()) {
	    JOptionPane.showMessageDialog(this, 
					  message,
					  title, 
					  type);
	}
	else {
	    System.out.println(title+":"+message);
	}
    }
    }
