package qat.gui;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import qat.parser.*;
import qat.common.*;
import qat.components.*;
import qat.gui.*;

/**
 * This class is responsible for displaying a processing the pop-up menu when the user
 * right-clicks a test displayed in the test gui.
 *
 * @author webhiker
 * @version %W %E
 */
public class NodeMenu extends JPopupMenu implements ActionListener, WindowListener {
    // these are all the actions defined for this pop-up menu
    private JMenuItem runThisAction;
    private JMenuItem runSelectedAction;
    private JMenuItem editAction;
    private JMenuItem parseThisAction;
    private JMenuItem parseSelectedAction;
    private JMenuItem reloadDirectoryAction;
	
    private QAT parent;
    private TestTreeNode node; // the node clicked to activate this menu
    private JMenu editListPropMenu, editListMiscMenu, viewOutputMenu;
    private JMenuItem editListPropItems[], editListMiscItems[], viewOutputItems[];
    private TestSpecification test=null;	
    private ArrayList traceViewerList;
	
    public NodeMenu(String title) {
	super(title);
	traceViewerList = new ArrayList();

	add(editAction = new JMenuItem(Resources.getString("editNode"),
				       new ImageIcon(Resources.getImageResource("editImage"))));
	editAction.addActionListener(this);
	add(editListPropMenu = new JMenu(Resources.getString("editProperties")));
	add(editListMiscMenu = new JMenu(Resources.getString("editIncludes")));
	
	addSeparator();
	add(parseThisAction = new JMenuItem(Resources.getString("parseNode"),
					    new ImageIcon(Resources.getImageResource("parseThisImage"))));
	parseThisAction.addActionListener(this);
	add(parseSelectedAction = new JMenuItem(Resources.getString("parseSelected"),
						new ImageIcon(Resources.getImageResource("parseSelectedImage"))));
	parseSelectedAction.addActionListener(this);
	add(reloadDirectoryAction = new JMenuItem(Resources.getString("reloadDirectory"),
						  new ImageIcon(Resources.getImageResource("parseAllImage"))));
	reloadDirectoryAction.addActionListener(this);
	
	addSeparator();
	add(viewOutputMenu = new JMenu(Resources.getString("viewCommandOutput")));
		
	addSeparator();
	add(runThisAction = new JMenuItem(Resources.getString("runNode"),
					  new ImageIcon(Resources.getImageResource("runSelectedImage"))));
	runThisAction.addActionListener(this);
	add(runSelectedAction = new JMenuItem(Resources.getString("runSelectedNodes"),
					      new ImageIcon(Resources.getImageResource("runSelectedImage"))));
	runSelectedAction.addActionListener(this);

	pack();
    }
  
    private void addMenuItemsT(JMenu menu, String items[]) {
	// first clear all the items
	if (menu!=null) {
	    menu.removeAll();
	}
	// now add the string array unless it's empty
	if ((items!=null)&&(items.length>0)) {
	    menu.setEnabled(true);
	    JMenuItem menuItems[] = new JMenuItem[items.length];
	    for (int i = 0; i < items.length; i++) {
		menu.add(new ViewTraceAction(items[i]));
	    }
	}
	else {
	    menu.setEnabled(false);
	    menu.add(new JMenuItem(Resources.getString("empty")));
	}
    }
	
    private void addMenuItemsN(JMenu menu, String items[]) {
	// first clear all the items
	if (menu!=null) {
	    menu.removeAll();
	}
	// now add the string array unless it's empty
	if ((items!=null)&&(items.length>0)) {
	    menu.setEnabled(true);
	    JMenuItem menuItems[] = new JMenuItem[items.length];
	    for (int i = 0; i < items.length; i++) {
		menu.add(new NotepadAction(items[i]));
	    }
	}
	else {
	    menu.setEnabled(false);
	    menu.add(new JMenuItem(Resources.getString("empty")));
	}
    }
	
    private void updateMenu(TestSpecification test) {		
	this.test = test;
	invalidate();		
		
	// add the properties files list
	addMenuItemsN(editListPropMenu,test.getIncludePropList());
		
	// add the misc files list
	addMenuItemsN(editListMiscMenu,test.getIncludeMiscList());
		
	// add the view output files list
	addMenuItemsT(viewOutputMenu,test.getViewOutputList());
		
	setLabel(test.getTestName());
	validate();
    }
    
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	editAction.setEnabled(enabled);
	editListPropMenu.setEnabled(enabled);
	editListMiscMenu.setEnabled(enabled);
	parseThisAction.setEnabled(enabled);
	runThisAction.setEnabled(enabled);
	viewOutputMenu.setEnabled(enabled);
	reloadDirectoryAction.setEnabled(!enabled);
    }

    public void setSource(Object o) {
	try {
	    node = (TestTreeNode)o;
	    if (node.getUserObject() instanceof TestSpecification) {
		updateMenu((TestSpecification)node.getUserObject());
		setEnabled(true);
		return;
	    }
	    else {
		// if it's a leaf, and not a TestSpecification, it must be a stdout or stderr file
		setEnabled(false);
	    }
	}
	catch (Exception e) {
	    setEnabled(false);
	    System.out.println("Internal error : 932432");
	}
    }
    
	
    public void setMenuVisible(boolean b) {
	setVisible(false);
    }
	
    /** 
     * This method gives us a handle to the parent QAT so we can use it's
     * runSingleTest method and have access to the GUI from the NodeMenu.
     */
    public void setParentQAT(QAT q) {
	parent = q;
    }
    
    public QAT getParentQAT() {
	return parent;
    }
	
    /**
     * Loop through our list of trace viewers, and returns the one corresponding to test and trace,
     * else returns null.
     */
    private TraceViewer getTraceViewer(TestSpecification test, String trace) {
	TraceViewer traceViewer;
	// first see if we are already viewing it
	for (int i = 0; i < traceViewerList.size(); i++) {
	    traceViewer = (TraceViewer)traceViewerList.get(i);
	    if (traceViewer.equals(test,trace))
		return traceViewer;
	}
	// else try and re-use a non-visible window
	for (int i = 0; i < traceViewerList.size(); i++) {
	    traceViewer = (TraceViewer)traceViewerList.get(i);
	    if (!traceViewer.isVisible()) {
		traceViewer.setCommandId(trace);
		traceViewer.setTest(test);
		return traceViewer;
	    }
	    // ok, we need to create a new one altogether
	}
	return null;
    }
	
    /**
     * Either updates the visible trace viewers, or creates new ones, depending on the re-use flag.
     */
    public void updateTraceViewerList(TestSpecification test, String trace) {
	TraceViewer traceViewer;
	if ((traceViewerList.size()==0)||
	    (!new Boolean(parent.getProperty(Common.REUSE_TRACE_WINDOW,"true")).booleanValue())) {
	    if ((traceViewer=getTraceViewer(test,trace))==null) {
		traceViewerList.add(traceViewer = new TraceViewer(getParentQAT(),test,trace));
		parent.addChangeListener(traceViewer.getChangeListener());
		traceViewer.addWindowListener(this);
	    }
	    traceViewer.loadTraces(test,trace);
	    traceViewer.setVisible(true);
	}
	else {
	    ((TraceViewer)traceViewerList.get(0)).loadTraces(test,trace);
	    ((TraceViewer)traceViewerList.get(0)).setVisible(true);
	}
    }
	
    /**
     * This method replaces the beginning of any filename over
     * 10 characters long with dots.
     */
    private String shortenFileName(String fileName) {
	if (fileName.length()>10)
	    return "..."+File.separator+Utils.extractFileName(fileName);
	else
	    return fileName;
    }
	
    private void runSelected() {
	setMenuVisible(false);
	if (parent!=null) {
	    parent.runSelectedTests();
	}
	else {
	    System.out.println("Parent QAT not set for this menu");
	}
    }
	
    private void parseSelected() {
	setMenuVisible(false);
	if (parent!=null) {
	    parent.parseSelectedTests();
	}
	else {
	    System.out.println("Parent QAT not set for this menu");
	}
    }
	
    private void runThis() {
	parent.getTestTree().unSelectAll();
	parent.getTestTree().selectTest(test);
	runSelected();
    }
	
    private void parseThis() {
	parent.getTestTree().unSelectAll();
	parent.getTestTree().selectTest(test);
	parseSelected();
    }
	
    private void editSelected(ActionEvent e) {
	try {
	    setMenuVisible(false);
	    TestFinderInterface testFinder = new GenericTestFinder(parent.getProperties());
	    new  Notepad(test.getTestSpecPath(),
			 testFinder.getParser(new File(test.getTestSpecPath())),
			 new Boolean(parent.getProperty(Common.SYNTAX_HIGHLIGHTING,"true")).booleanValue());
	}
	catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
	
    private void reloadDirectory() {
	parent.reloadDirectory(node);
    }
	
    public void actionPerformed(ActionEvent e) {
	if (e.getSource().equals(runThisAction)) {
	    runThis();
	    return;
	}
	if (e.getSource().equals(runSelectedAction)) {
	    runSelected();
	    return;
	}
	if (e.getSource().equals(parseThisAction)) {
	    parseThis();
	    return;
	}
	if (e.getSource().equals(parseSelectedAction)) {
	    parseSelected();
	    return;
	}
	if (e.getSource().equals(editAction)) {
	    editSelected(e);
	    return;
	}
	if (e.getSource().equals(reloadDirectoryAction)) {
	    reloadDirectory();
	    return;
	}
    }
	
    // INTERNAL WINDOW LISTENER OPERATIONS
    public void windowClosing(WindowEvent e) {		
    }

    public void windowClosed(WindowEvent e) {
	traceViewerList.remove(e.getSource());
	parent.removeChangeListener((ChangeListener)e.getSource());
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }
	
    class NotepadAction extends AbstractAction {
	String s;
	public NotepadAction(String text) {
	    super(shortenFileName(text));
	    s = text;
	}
		
	public void actionPerformed(ActionEvent e) {
	    setMenuVisible(false);
	    try {
		TestFinderInterface testFinder = new GenericTestFinder(parent.getProperties());		
		new  Notepad(s,
			     testFinder.getParser(new File(s)),
			     new Boolean(parent.getProperty(Common.SYNTAX_HIGHLIGHTING,"true")).booleanValue());
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
    }

    class ViewTraceAction extends AbstractAction {
	private String trace;
	public ViewTraceAction(String s) {
	    super(s);
	    trace = s;
	}
		
	public void actionPerformed(ActionEvent e) {
	    setMenuVisible(false);			
	    updateTraceViewerList(test,trace);
	}
    }
    }
