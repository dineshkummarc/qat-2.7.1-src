package qat.gui;
/**
 *
 * This object displays the state of all the tests being used by the test suite.
 * A single test can have one of four states:
 * 1)Passed -  the test has been run, and passed
 * 2)Failed - the tests has been run, and failed
 * 3)Unresolved - the tests has been run, but is unresolved
 * 4)NotRun - the test has not been run yet
 * 4)Running - the test is currently running
 *
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import qat.gui.*;
import qat.common.*;
import qat.parser.*;
import qat.components.*;

public class TestTree extends JComponent implements MouseListener, TreeExpansionListener {
	
    private JTree tree;
    private TestTreeNode treeRoot=null;
    private JScrollPane treeView;
    // declare the pop-up menu and it's listener
    private NodeMenu nodePopupMenu;
    private NodeMenuListener nodeMenuListener;
    private Properties defaultProperties;
    private ArrayList changeListenerList;
    private ArrayList selectionListenerList;
    private String projectFileName;
    private int expandLevel;
	
    /**
     * This constructor creates the tree
     */
    public TestTree() {
	super();
	setupScreen();		
    }
	
    private void setupScreen() {
	expandLevel = 0;
	setLayout(new BorderLayout());
	nodePopupMenu = new NodeMenu("Menu");
	nodeMenuListener = new NodeMenuListener();
	tree = (new JTree(treeRoot = new TestTreeNode(Resources.getString("notests"))));
	tree.addMouseListener(this);
	tree.setModel(new TestTreeModel(this,treeRoot));
	tree.addMouseListener(nodeMenuListener);
	treeView = new JScrollPane(tree, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);				
	add(treeView,BorderLayout.CENTER);
	changeListenerList = new ArrayList(1);
	selectionListenerList = new ArrayList();
	setBorder(BorderFactory.createTitledBorder(Resources.getString("testSelection")));
    }
	
	
    /**
     * This add a listener interested in recieiveing events whenever a node of this tree changes.
     * No duplicates are allowed.
     */
    public void addChangeListener(ChangeListener c) {
	removeChangeListener(c);
	changeListenerList.add(c);
    }
  
    public void removeChangeListener(ChangeListener c) {
	changeListenerList.remove(c);
    }
  
    public void fireChangeEvent(TestTreeNode node) {
	ChangeEvent e = new ChangeEvent(node);
	for (int i = 0; i < changeListenerList.size(); i++) {
	    ((ChangeListener)changeListenerList.get(i)).stateChanged(e);
	}
    }
  
    public void addTreeSelectionListener(TreeSelectionListener l) {
	tree.addTreeSelectionListener(l);
	selectionListenerList.add(l);
    }
	
    /**
     * When we select tests by keyword selection, it is neccesary to remove all registered
     * selection listeners to prevent each trace file being
     * loaded as the tests are selected.
     */
    public void pauseListeners() {
	for (int i = 0; i < selectionListenerList.size(); i++) {
	    tree.removeTreeSelectionListener((TreeSelectionListener)selectionListenerList.get(i));
	}
    }
	
    /**
     * This method re-adds all required selection listeners to this tree.
     */
    public void resumeListeners() {
	for (int i = 0; i < selectionListenerList.size(); i++) {
	    tree.addTreeSelectionListener((TreeSelectionListener)selectionListenerList.get(i));
	}
    }
  
    public void setDefaultProperties(Properties p) {
	this.defaultProperties = p;
    }
	
    public Properties getDefaultProperties() {
	return defaultProperties;
    }
	
    public TestTreeNode getRoot() {
	return treeRoot;
    }
	
    public String getProjectRoot() {
	return treeRoot.toString();
    }
  
    /**
     * This method will return the test which has a matching
     * testSpecPath, which should be unique for each test.
     */
    public TestSpecification getTestSpecification(String testSpecPath) {
	ArrayList tests = getAllTests();
	for (int i = 0; i < tests.size(); i++) {
	    if (((TestSpecification)tests.get(i)).getTestSpecPath().equals(testSpecPath))
		return (TestSpecification)tests.get(i);
	}
	return null;
    }
    public String[] getKeyWordList() {
	ArrayList allTests = getAllTests();
	ArrayList keyWords = new ArrayList();
	String theseKeys[];
	for (int i = 0; i < allTests.size(); i++) {
	    theseKeys = ((TestSpecification)allTests.get(i)).getKeyWords();
	    for (int j = 0; j < theseKeys.length; j++) {
		if (!keyWords.contains(theseKeys[j]))
		    keyWords.add(theseKeys[j]);
	    }
	}
	// now convert the ArrayList to a String array
	String[] keys = new String[keyWords.size()];
	for (int i = 0; i < keyWords.size(); i++)
	    keys[i] = (String)keyWords.get(i);
	return keys;
    }
	
    /**
     * This method clears all the tests currently in the tree, and
     * load all tests & subdirectorys contained within the specified path.
     * @param dirpath - the root directory to look for the test files in.
     */
    public void loadNewTestDir(String dirPath, StatusWindow status) {
	this.projectFileName = projectFileName;
	TestTreeNode newRoot = new TestTreeNode(dirPath);
	// get the required test finder interface
	GenericTestFinder testFinder=new GenericTestFinder(getDefaultProperties());
	testFinder.setProjectRoot(dirPath);
		
	loadTestDir(newRoot, dirPath, testFinder, status);
	((TestTreeModel)tree.getModel()).setRoot(treeRoot = newRoot);
	((TestTreeModel)tree.getModel()).reload(treeRoot);
	//	parser.finish();
	sortTree();
    }
	
    private void loadTestDir(TestTreeNode thisRoot, 
			     String dirPath, 
			     TestFinderInterface testFinder,
			     StatusWindow status) {
	File root = new File(dirPath);
	if (!root.exists())
	    return;
	File node;
	String fileList[] = root.list();
		
	if (fileList==null) {
	    System.out.println(root.toString()+" "+Resources.getString("cannotReadError"));
	}
	else {
	    for (int i = 0; i < fileList.length; i++) {
		getParentQAT().update();
		status.setMaximum(fileList.length-2);
		status.setMessage(Utils.trimFileName(dirPath,40),i);
		node = new File(root.getPath()+File.separator+fileList[i]);
		if (node.isDirectory()) {
		    TestTreeNode newRoot = new TestTreeNode(fileList[i]);
		    newRoot.setAllowsChildren(true);
		    newRoot.setUserObject(fileList[i]);
		    thisRoot.add(newRoot);
		    loadTestDir(newRoot,node.getPath(), testFinder, status);
		}
		else {
		    if (testFinder.isTestFile(node)) {
			ParserInterface parser = testFinder.getParser(node);
			parser.prepare(getParentQAT().getProjectResultsDirectory());
			addFileNode(thisRoot,dirPath+File.separator+node.getName(), parser);
			parser.finish();
		    }
		}
	    }
	}
	// don't add any empty directories, only those containing qat files, or
	// subdirectories containing qat files
	if ((thisRoot.getChildCount()==0)&&(thisRoot.getAllowsChildren())) {
	    thisRoot.removeFromParent();
	}
    }
	
    public void removeAllNodes() {
	treeRoot.removeAllChildren();
	treeRoot.setUserObject(Resources.getString("notests"));
	nodeChanged(treeRoot);
	((TestTreeModel)tree.getModel()).reload(treeRoot);
    }
	
    /**
     * This method is called to re-check if any new tests exist in the directory thisRoot, or if any have been deleted.
     */
    public void parseTestsFrom(TestTreeNode thisRoot, StatusWindow status) {
	Object pathSegments[] = getTreePath(thisRoot).getPath();
	String path = ((TestTreeNode)pathSegments[0]).getUserObject().toString();
	for (int i = 1; i < pathSegments.length; i++)
	    path = path+File.separator+((TestTreeNode)pathSegments[i]).getUserObject().toString();
		
	// remove the children first
	thisRoot.removeAllChildren();
	// decide which test finder to use
	GenericTestFinder testFinder=new GenericTestFinder(getDefaultProperties());
	testFinder.setProjectRoot(getProjectRoot());

	loadTestDir(thisRoot, path, testFinder, status);

	// this seems to be neccesary else the tree kinda hangs
	((TestTreeModel)tree.getModel()).reload(treeRoot);
	sortTree(thisRoot);
    }
	
    /**
     * This method returns the TestTreeNode matching the
     * TreePath parameter.
     */
    public TestTreeNode getNode(TreePath tree) {
	return (TestTreeNode)tree.getLastPathComponent();
    }
	
    /**
     * This method returns the TestTreeNode matching the
     * test parameter.
     */
    public TestTreeNode getNode(TestSpecification test) {
	ArrayList nodeList = getAllTestNodes(treeRoot);
	TestTreeNode treeNode;
	for (int i = 0; i < nodeList.size(); i++) {
	    treeNode = ((TestTreeNode)nodeList.get(i));
	    if (treeNode.getUserObject() instanceof TestSpecification) {
		if (((TestSpecification)treeNode.getUserObject()).getTestName().equals(test.getTestName())) {
		    return (TestTreeNode)nodeList.get(i);
		}
	    }
	}
	return null;
    }
    /*
     * Returns the tree path corresponding to the node.
     */
    public TreePath getTreePath(TestTreeNode node) {
	return new TreePath(node.getPath());
    }
	
    /**
     * Returns the tree object used by TestTree.
     */
    public JTree getTree() {
	return tree;
    }
	
    /**
     * Searches for the node in the tree matching test, and
     * sets it to selected.
     * If not visible, the tree will be expanded until this node is visible.
     */
    public void selectNode(TestTreeNode node) {
	tree.addSelectionPath(getTreePath(node));
    }
    /**
     * Searches for the node in the tree matching test, and
     * sets it to selected.
     * If not visible, the tree will be expanded until this node is visible.
     */
    public void selectTest(TestSpecification test) {
	selectNode(getNode(test));
    }
	
    /**
     * Selects all the nodes in the tree, and expands the entire tree
     * so that all the selected nodes are visible.
     */
    public void selectAll() {
	for (int i = 0; i != tree.getRowCount(); i++) {
	    tree.expandRow(i);
	}
	tree.setSelectionInterval(0,tree.getRowCount());
    }
	
    /**
     * Unselects all the nodes in the tree.
     */
    public void unSelectAll() {
	//		for (int i = 0; i != tree.getRowCount(); i++) {
	//			tree.expandRow(i);
	//		}
	tree.setSelectionInterval(-1,-1);
    }
	
    public void selectAllWithStatus(int status) {
	ArrayList tests = getAllTests();
	TestSpecification test;
	for (int i = 0; i < tests.size(); i++) {
	    test = (TestSpecification)tests.get(i);
	    if (test.getStatus()==status) {
		selectTest(test);
	    }
	}
    }
	
    public void expandAll() {
	expandLevel = treeRoot.getDepth();
	expandChildren(treeRoot);
    }
	
    public void expandLevel() {
	if ((treeRoot.getDepth()-1)>expandLevel) {
	    expandLevel++;
	    expandChildren(treeRoot);
	}
    }
    public void collapseLevel() {
	if (expandLevel>=0) {
	    expandLevel--;
	    collapseChildren(treeRoot);
	}
    }
    
    /**
     * This methof expands the specified node making
     * all it's children visible.
     */
    private void expandChildren(TestTreeNode node) {
	tree.expandPath(getTreePath(node));
	if (node.getLevel() < expandLevel) {
	    for (int i = 0; i < node.getChildCount(); i++)
		expandChildren((TestTreeNode)node.getChildAt(i));
	}
	else return;
    }
	
    /**
     * This method collapses the specified node making
     * all it's children visible.
     */
    private void collapseChildren(TestTreeNode node) {
	tree.collapsePath(getTreePath(node));
	if (node.getLevel() <= expandLevel) {
	    for (int i = 0; i < node.getChildCount(); i++)
		collapseChildren((TestTreeNode)node.getChildAt(i));
	}
	else return;
    }
	
    /**
     * This method returns an array of all selected TestSpecifications.
     * If a sub-directory is selected, any tests contained within that directory are considered
     * as selected.
     */
    public ArrayList getSelectedTests() {
	ArrayList selectedTests = new ArrayList();
	TestTreeNode node;
	TreePath[] selectedPaths = tree.getSelectionPaths();
	// check if ANY tests are selected
	if (selectedPaths==null)
	    return selectedTests;
	for (int i = 0; i < selectedPaths.length; i++) {
	    node = getNode(selectedPaths[i]);
	    if (node.getUserObject() instanceof TestSpecification) {
		selectedTests.add(node.getUserObject());
	    }
	    else {
		// if a directory is selected, and not expanded, add all subtests
		if (!tree.isExpanded(selectedPaths[i])) {
		    ArrayList t = getAllTests(node);
		    for (int j = 0; j < t.size(); j++)
			selectedTests.add(t.get(j));
		}
	    }
	}
	return selectedTests;
    }
	
    /**
     * This method returns an array of all selected TestTreeNodes.
     * If a sub-directory is selected, any tests contained within that directory are considered
     * as selected.
     */
    public ArrayList getSelectedTestNodes() {
	ArrayList selectedTests = new ArrayList();
	TestTreeNode node;
	TreePath[] selectedPaths = tree.getSelectionPaths();
	// check if ANY tests are selected
	if (selectedPaths==null)
	    return selectedTests;
	for (int i = 0; i < selectedPaths.length; i++) {
	    node = getNode(selectedPaths[i]);
	    if (node.getUserObject() instanceof TestSpecification) {
		selectedTests.add(node);
	    }
	    else {
		// if a directory is selected, and not expanded, add all subtests
		if (!tree.isExpanded(selectedPaths[i])) {
		    ArrayList t = getAllTestNodes(node);
		    for (int j = 0; j < t.size(); j++)
			selectedTests.add(t.get(j));
		}
	    }
	}
	return selectedTests;
    }
    
    /**
     * This method searchs the tree starting at node, until a TestSpecification name
     * matches the parameter test, and then replaces it with test.
     * Note: the node is not explictly repainted
     * @param test - the test we will replace the one in the list with
     * @return - returns true if the test was found and replaced, else returns false.
     */
    public boolean setTest(TestSpecification test) {
	return setTest(test,treeRoot);
    }
	
    /**
     * This method searchs the tree starting at node, until a TestSpecification name
     * matches the parameter test, and then replaces it with test.
     * @param test - the test we will replace the one in the list with
     * @param node - the node to start search for test at.
     * @return - returns true if the test was found and replaced, else returns false.
     */
    private boolean setTest(TestSpecification test, TestTreeNode node) {
	if (node==null) {
	    return false;			
	}
	if (node.getUserObject() instanceof TestSpecification) {
	    if (((TestSpecification)node.getUserObject()).getTestSpecPath().equals(test.getTestSpecPath())) {
		node.setUserObject(test);
		nodeChanged(node);
		fireChangeEvent(node);
		return true;
	    }
	    else {
		return false;
	    }
	}
	else {
	    if (node.getChildCount()>0) {
		TestTreeNode child = (TestTreeNode)node.getFirstChild();
		while(child!=null) {
		    if (setTest(test,child)) {
			return true;				 
		    }
		    child = (TestTreeNode)node.getChildAfter(child);
		}
		return false;
	    }
	    else {
		return false;
	    }
	}
    }
	
    /**
     * This method returns all the TestTreeNodes in the test tree.
     * @return a ArrayList containing all the TestTreeNodes containing TestSpecification 
     * objects in this tree.
     */	
    public ArrayList getAllTestNodes() {
	return getAllTestNodes(treeRoot);
    }
    
    /**
     * This method returns all the tests in the test tree.
     * @return a ArrayList containing all the TestSpecification objects in this tree.
     */
    public ArrayList getAllTests() {
	return getAllTests(treeRoot);
    }
	
    /**
     * This method returns all the tests in the test tree on a per node basis.
     * Used by the Http interface for displaying test overview.
     * @return an ArrayList of ArrayLists, each containing in position zero the String name for a node, and in the rest all the TestSpecification objects.
     */
    public ArrayList getAllTestsByParent() {
	return getAllTestsByParent(new ArrayList(), (TestTreeNode)treeRoot);
    }
	
    private ArrayList getAllTestsByParent(ArrayList resultList, TestTreeNode root) {
	ArrayList result = new ArrayList();
	for (int i = 0; i < root.getChildCount(); i++) {
	    if (root.getChildAt(i).isLeaf()) {
		result.add(((TestTreeNode)root.getChildAt(i)).getUserObject());
	    }
	    else {
		resultList = getAllTestsByParent(resultList, (TestTreeNode)root.getChildAt(i));
	    }
	}
	if (result.size()>0) {
	    TreeNode path[] = root.getPath();
	    String pathStr = "";
	    for (int i = 0; i < path.length; i++)
		pathStr += path[i].toString()+File.separator;
	    result.add(0,pathStr.substring(0,pathStr.length()-1));
	    resultList.add(result);
	}
	return resultList;
    }
	
    /**
     * This method returns all the TestTreeNodes starting at the node parameter.
     * @param node - the root to start building the test list from.
     * @return a ArrayList containing all the TestTreeNode objects in this tree.
     */
    public ArrayList getAllTestNodes(TestTreeNode node) {
	ArrayList result = new ArrayList(node.getChildCount());
	for (Enumeration e = node.depthFirstEnumeration() ; e.hasMoreElements() ;) {
	    node = (TestTreeNode)e.nextElement();
	    if (node.getUserObject() instanceof TestSpecification)
		result.add(node);
	}
	return result;
    }
    
    /**
     * This method returns all the tests starting at the node parameter.
     * @param node - the root to start building the test list from.
     * @return a ArrayList containing all the TestSpecification objects in this tree.
     */
    public ArrayList getAllTests(TestTreeNode node) {
	ArrayList result = new ArrayList(node.getChildCount());
	for (Enumeration e = node.depthFirstEnumeration() ; e.hasMoreElements() ;) {
	    node = (TestTreeNode)e.nextElement();
	    if (node.getUserObject() instanceof TestSpecification)
		result.add(node.getUserObject());
	}
	return result;
    }

    /**
     * This method returns all the TestTreeNode in the tree.
     * @return a ArrayList containing all the TestTreeNode objects in this tree.
     */
    public ArrayList getAllNodes() {
	return getAllNodes(treeRoot);
    }
    /**
     * This method returns all the TestTreeNode starting at the node parameter.
     * @param node - the root to start building the test list from.
     * @return a ArrayList containing all the TestTreeNode objects in this tree.
     */
    public ArrayList getAllNodes(TestTreeNode node) {
	ArrayList result = new ArrayList(node.getChildCount());
	for (Enumeration e = node.depthFirstEnumeration() ; e.hasMoreElements() ;) {
	    node = (TestTreeNode)e.nextElement();
	    result.add(node);
	}
	return result;
    }
    
    private void addFileNode(TestTreeNode thisRoot, String node, ParserInterface parser) {
	TestSpecification test;
	TestTreeNode treeNode = new TestTreeNode(test = new TestSpecification(node));
	parseTestNode(treeNode,parser);
	thisRoot.add(treeNode);
    }
    
    private void addDirNode(TestTreeNode thisRoot, String dirNode) {
	TestTreeNode treeNode = new TestTreeNode(dirNode);
	treeNode.setAllowsChildren(true);
        thisRoot.add(treeNode);
    }
	
    public void parseTestNode(TestTreeNode node, ParserInterface parser) {
	TestSpecification test = (TestSpecification)node.getUserObject();
	// clear any trace files in the results directory
	test.clearTraceFiles(getParentQAT().getProjectResultsDirectory());
		
	test = parseTest(test,
			 parser);
	setTest(test,node);
    }
	
    public TestSpecification parseTest(TestSpecification test, ParserInterface parser) {
	test.parseTest(getParentQAT().getProjectResultsDirectory(),
		       defaultProperties,
		       parser); // handle to the parser instance
	return test;
    }
	
    /**
     * This method resets the status of all selected tests to NOTRUN.
     */
    public void resetSelectedTestStatus() {
	ArrayList selectedTests = getSelectedTests();
	TestSpecification test;
	for (int i = 0; i < selectedTests.size(); i++) {
	    test = (TestSpecification)selectedTests.get(i);
	    test.setStatus(ProtocolConstants.NOTRUN);
	}
    }
	
    public void nodeChanged(TestTreeNode node) {
	((TestTreeModel)tree.getModel()).nodeChanged(node);			
    }
	
    /**
     * This method writes out the test tree to file.
     */
    public void saveTests(ObjectOutputStream out) throws IOException, ClassNotFoundException {
	out.writeObject((String)(treeRoot.getUserObject()));
	saveNode(out,treeRoot);
    }
	
    public void saveNode(ObjectOutputStream out, TestTreeNode node) throws IOException, ClassNotFoundException {
	out.writeObject(new Integer(node.getChildCount()));
	for (int i = 0; i < node.getChildCount(); i++) {
	    if (node.getChildAt(i).isLeaf()) {
		((TestSpecification)((TestTreeNode)node.getChildAt(i)).getUserObject()).writeData(out);
	    }
	    else {
		out.writeObject((String)((TestTreeNode)node.getChildAt(i)).getUserObject());
		saveNode(out,(TestTreeNode)node.getChildAt(i));
	    }
	}
    }
	
    /**
     * This method loads the test tree from file.
     */
    public void loadTests(ObjectInputStream in) throws IOException, ClassNotFoundException {
	treeRoot = new TestTreeNode((String)in.readObject());
	loadNode(in,treeRoot);
	((TestTreeModel)tree.getModel()).setRoot(treeRoot);
    }
	
    public void loadNode(ObjectInputStream in, TestTreeNode parent) throws IOException, ClassNotFoundException {
	TestSpecification test;
	TestTreeNode child;
	Object object = in.readObject();
	int count = ((Integer)object).intValue();
	for (int i = 0; i < count; i++) {
	    object = in.readObject();
	    if (((String)object).equals(Common.SERIALIZED_NODE_HEADERV10)|
		((String)object).equals(Common.SERIALIZED_NODE_HEADERV11)|
		((String)object).equals(Common.SERIALIZED_NODE_HEADERV12)) {
		test = new TestSpecification(in,(String)object);
		parent.add(new TestTreeNode(test,false));
	    }
	    else {
		child = new TestTreeNode((String)object);
		parent.add(child);
		loadNode(in,child);
	    }
	}
    }
    
    /** 
     * This method gives us a handle to the parent QAT to pass on to the NodePopup Menu,
     * so we can use it's
     * runSingleTest method and have access to the GUI from the NodeMenu.
     */
    public void setParentQAT(QAT q) {
	nodePopupMenu.setParentQAT(q);
    }
	
    public QAT getParentQAT() {
	return nodePopupMenu.getParentQAT();
    }
  
    /**
     * This method sorts all the children contained in this node.
     */
    private void sortNode(TestTreeNode node) {
	if (node.getChildCount()==0) {
	    return;
	}
	TestTreeNode temp1, temp2;
	for (int i = 0; i < node.getChildCount(); i++) {
	    for (int j = i+1; j < node.getChildCount(); j++) {
		try {
		    temp1 = (TestTreeNode)node.getChildAt(i);
		    temp2 = (TestTreeNode)node.getChildAt(j);
		    if (temp1.getUserObject().toString().compareTo(temp2.getUserObject().toString())>0) {
			// swap the two objects					
			node.insert(temp2,i);
			node.insert(temp1,j);
		    }
		}
		catch (Exception e) {
		    System.out.println("Internal error occured - please report...."+e);
		}
	    }
			
	}
    }
	
    /** 
     * This method sorts all of the nodes in this tree.
     */
    public void sortTree() {
	sortTree(treeRoot);
	// now sync the model
	((TestTreeModel)tree.getModel()).reload(treeRoot);
    }
	
    /**
     * This method is called recursively, use by sortTree() to
     * sort all the nodes of a tree alphabetically.
     */
    private void sortTree(TestTreeNode thisRoot) {
	if (thisRoot.isLeaf()) {
	    return;
	}
	else {
	    sortNode(thisRoot);
	    for (int i = 0; i < thisRoot.getChildCount(); i++) {
		sortTree((TestTreeNode)thisRoot.getChildAt(i));
	    }
	}
    }
	
    public void mouseClicked(MouseEvent e) {
	if (e.getClickCount()==2) {
	    TreePath selPath = tree.getClosestPathForLocation(e.getX(), e.getY());
	    TestTreeNode node = getNode(selPath);
	    if (node.getUserObject() instanceof TestSpecification) {
		// double clicked a Test
		getParentQAT().runSingleTest((TestSpecification)node.getUserObject());
	    }
	    else {
		if (node.isLeaf()) {
		    // double clicked a trace file
					
		    TestSpecification test;
		    try {
			test = (TestSpecification)((TestTreeNode)node.getParent()).getUserObject();
		    }
		    catch (NullPointerException ex) {
			// happens if the entire tree is empty
			return;
		    }
		    nodePopupMenu.updateTraceViewerList(test,node.getUserObject().toString());
		}
	    }
	}
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {		
    }
    public void mousePressed(MouseEvent e) {		
    }
    public void mouseExited(MouseEvent e) {		
    }
	
	
    class NodeMenuListener extends Object implements MouseListener {
	// this maybeShow shit needs to be done in both these methods else it only works on NT or
	// Solaris, but not both.
	public void mousePressed(MouseEvent e) {
	    maybeShowPopup(e);
	}
		
	public void mouseReleased(MouseEvent e) {
	    maybeShowPopup(e);
	}
	  
	public void mouseClicked(MouseEvent e) {
	}
	  
	public void mouseEntered(MouseEvent e) {
	}
	  
	public void mouseExited(MouseEvent e) {
	}
	  
	private void maybeShowPopup(MouseEvent e) {
	    if (e.isPopupTrigger()) {
		nodePopupMenu.setSource(getNode(tree.getClosestPathForLocation(e.getX(), e.getY())));
		nodePopupMenu.show(e.getComponent(), e.getX(), e.getY());
	    }
	}
    }
	
    public void treeExpanded(TreeExpansionEvent event) {
	int newLevelDepth;
	if ((newLevelDepth = event.getPath().getPath().length) > expandLevel) {
	    System.out.println("newLevelDepth="+newLevelDepth);
	    expandLevel = newLevelDepth;
	}
    }
	
    public void treeCollapsed(TreeExpansionEvent event) {
	int newLevelDepth;
	if ((newLevelDepth = event.getPath().getPath().length) < expandLevel) {
	    System.out.println("newLevelDepth="+newLevelDepth);
	    expandLevel = newLevelDepth;
	}
    }
    }
