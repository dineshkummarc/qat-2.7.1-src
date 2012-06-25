package qat.gui;
/**
 * This class is responsible for displaying the output traces for the selected test.
 *
 * @author webhiker
 * @version %W %E
 */
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import qat.common.*;
import qat.gui.*;

public class TestDetailViewer extends JComponent implements TreeSelectionListener,
    ChangeListener, 
    ListSelectionListener {
    private JEditorPane testDescription;
    private JLabel testAuthor, testBugInfo;
    private TestSpecification currTest;
    private JPanel outputPanel;
    private JList outputList;
    private TraceViewerComponent traceViewer;
    private Object selectedItem;
    private QAT parent;
	
    public TestDetailViewer(QAT parent) {
	super();
	this.parent = parent;
	setupScreen();
    }
  
    public TestDetailViewer(QAT parent,TestSpecification test) {
	this(parent);
	setObjectView(test);
    }
  
    public void setupScreen() {
	// create detail panel
	JPanel detailPanel = new JPanel(new BorderLayout());
	JPanel north = new JPanel(new BorderLayout());
	JPanel left = new JPanel(new GridLayout(2,1));
	left.add(new JLabel(Resources.getString("testAuthor")+":",SwingConstants.RIGHT));
	left.add(new JLabel(Resources.getString("testBugInfo")+":",SwingConstants.RIGHT));
	north.add(left,BorderLayout.WEST);
	JPanel right = new JPanel(new GridLayout(2,1));
	right.add(testAuthor = new JLabel(Resources.getString("empty"),SwingConstants.LEFT));
	right.add(testBugInfo = new JLabel(Resources.getString("empty"),SwingConstants.LEFT));
	north.add(right,BorderLayout.CENTER);
	detailPanel.add(north,BorderLayout.NORTH);
	detailPanel.setBorder(BorderFactory.createTitledBorder(Resources.getString("testDetail")));

	testDescription = new JEditorPane();
	testDescription.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
	testDescription.setEditable(false);
	testDescription.setForeground(Common.testDetailDescriptionForeground);
	testDescription.setBackground(Common.testDetailDescriptionBackground);
		
	JScrollPane scrollPane;
	detailPanel.add(scrollPane = new JScrollPane(testDescription,
						     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
			BorderLayout.CENTER);
		
	// create the output panel
	outputPanel = new JPanel(new BorderLayout());
	outputList = new JList();
	outputList.setForeground(Common.testDetailOutputForeground);
	outputList.setBackground(Common.testDetailOutputBackground);
	outputList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	outputList.addListSelectionListener(this);
	JPanel listPanel = new JPanel(new BorderLayout());
	listPanel.add(new JScrollPane(outputList,
				      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.CENTER);

	traceViewer = new TraceViewerComponent(parent);

	//	outputPanel.add(listPanel,BorderLayout.WEST);
	//	outputPanel.add(traceViewer,BorderLayout.CENTER);
	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,listPanel,traceViewer);
	outputPanel.add(splitPane,BorderLayout.CENTER);
	splitPane.setDividerSize(3);
	splitPane.setContinuousLayout(true);
	splitPane.setDividerLocation(130);
	outputPanel.setBorder(BorderFactory.createTitledBorder(Resources.getString("recievedFromAgent")));

	JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
	tabbedPane.add(Resources.getString("viewCommandOutput"),outputPanel);
	tabbedPane.add("Detail",detailPanel);
		
	setLayout(new GridLayout(1,1));
	add(tabbedPane);
    }
	
    /**
     * TreeSelectionListener methods - this method is called each time
     * a tree node is selected by the user.
     */
    public void valueChanged(TreeSelectionEvent e) {
	TreePath path = e.getPath();
	TestTreeNode node = (TestTreeNode)path.getLastPathComponent();
	if (node.getUserObject() instanceof TestSpecification) {
	    setObjectView((TestSpecification)node.getUserObject());	  
	}
    }
  
    /**
     * ChangeListener methods - this method is called each time
     * a test changed state. Only if we are currently displaying this test do we
     * actually perform the update.
     */
    public void stateChanged(ChangeEvent e) {
	TestTreeNode node = (TestTreeNode)e.getSource();
	if (node.getUserObject() instanceof TestSpecification) {
	    if (node.getUserObject().equals(currTest)) {
		setObjectView((TestSpecification)node.getUserObject());
	    }
	}
    }
  
    public void setObjectView(TestSpecification test) {
	currTest = test;
	// set the title
	testAuthor.setText(test.getTestAuthor());
	testBugInfo.setText(test.getTestBugInfo());
	StringReader reader=null;
	try {
	    reader = new StringReader(test.getTestDescription());
	    //			testDescription.read(reader, new javax.swing.text.html.HTMLDocument());
	    testDescription.read(reader, testDescription.getDocument());
	    reader.close();
	}
	catch (Exception ex) {
	    System.out.println("Malformed HTML text for tes description :"+test.getTestDescription());
	}
		
	// recover previously selected item for possible re-display
	if (outputList.getSelectedValue()!=null)
	    selectedItem = outputList.getSelectedValue();
		
	// set up the output traces		
	String outputCommands[] = test.getViewOutputList();
	DefaultListModel listModel = new DefaultListModel();
	for (int i = 0; i < outputCommands.length; i++) {
	    listModel.addElement(outputCommands[i]);
	}
	outputList.setModel(listModel);
	outputList.setSelectedValue(selectedItem,true);
    }
	
    public void valueChanged(ListSelectionEvent e) {
	if (!e.getValueIsAdjusting()) {
	    if (outputList.getSelectedValue()!=null)
		traceViewer.loadTraces(currTest,outputList.getSelectedValue().toString());
	    else
		traceViewer.clearTraces();
	}
    }

    }
