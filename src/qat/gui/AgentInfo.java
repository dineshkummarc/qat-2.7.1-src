package qat.gui;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import qat.common.*;

public class AgentInfo extends JDialog implements MouseListener {
    private JButton addAgentButton,
	editAgentButton,
	delAgentButton,
	killAgentsButton,
	refreshAgentsButton, 
	discoverAgentsButton, 
	okButton, 
	cancelButton;
    private JTable agentTable;
    private DefaultTableModel model;
    private String titles[];
    private Properties original;
    private static final int MAX_AGENT_COUNT = 250;
    private static final int COLUMN_COUNT=5;
    private QAT parent;
	
    public AgentInfo(QAT parent,String title, Properties p) {
	super(parent,title,true);
	this.parent = parent;
	titles = new String[COLUMN_COUNT];
	titles[0] = Resources.getString("number");
	titles[1] = Resources.getString("name");
	titles[2] = Resources.getString("port");
	titles[3] = Resources.getString("architecture");
	titles[4] = Resources.getString("os");
	original = p;
	setupScreen(p);
	updateAgentsFromProperties(p);
	SwingUtils.setLocationRelativeTo(this,parent);
	setVisible(true);
    }
    
    private ImageIcon getImageResource(String resource) {
	try {
	    return new ImageIcon(Resources.getResource(Resources.getString(resource)));
	}
	catch (Exception e) {
	    System.out.println("Missing resource:"+Resources.getString(resource)+"="+resource);
	    return null;
	}
    }
	
    private JButton addButton(Container container, String text, String image) {
	JButton button = new JButton(Resources.getString(text),getImageResource(image));
	button.setVerticalTextPosition(SwingConstants.BOTTOM);
	button.setHorizontalTextPosition(SwingConstants.CENTER);
	button.setMargin(new Insets(1,1,1,1));

	container.add(button);
	button.addMouseListener(this);
	return button;
    }

    private void setupScreen(Properties p) {
	JPanel agentsPanel = new JPanel(new BorderLayout());
	model = new DefaultTableModel(titles,0);
	agentTable = new JTable(model);
	agentTable.setRowSelectionAllowed(true);
	JScrollPane scrollPane = new JScrollPane(agentTable,
						 ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	agentsPanel.add(scrollPane,BorderLayout.CENTER);
	JPanel buttonPanel = new JPanel(new GridLayout(4,1));
	JPanel tempPanel;
	tempPanel = new JPanel(new FlowLayout());
	addAgentButton = addButton(tempPanel, "addAgent", "addAgentImage");
	buttonPanel.add(tempPanel);
	editAgentButton = addButton(tempPanel, "editAgent", "editAgentImage");
	buttonPanel.add(tempPanel);
	tempPanel = new JPanel(new FlowLayout());
	delAgentButton = addButton(tempPanel, "deleteAgent", "deleteAgentImage");
	buttonPanel.add(tempPanel);
	killAgentsButton = addButton(tempPanel, "killAgents", "killAgentImage");
	buttonPanel.add(tempPanel);
	tempPanel = new JPanel(new FlowLayout());
	refreshAgentsButton = addButton(tempPanel, "refreshAgents", "refreshAgentImage");
	buttonPanel.add(tempPanel);
	discoverAgentsButton = addButton(tempPanel, "discoverAgents", "discoverAgentImage");
	buttonPanel.add(tempPanel);
	tempPanel = new JPanel(new FlowLayout());
	JButton upButton = addButton(tempPanel, "moveUp", "upImage");
	JButton dnButton = addButton(tempPanel, "moveDown", "downImage");
	buttonPanel.add(tempPanel);
		
	upButton.setPreferredSize(dnButton.getPreferredSize());
	addAgentButton.setPreferredSize(discoverAgentsButton.getPreferredSize());
	editAgentButton.setPreferredSize(discoverAgentsButton.getPreferredSize());
	delAgentButton.setPreferredSize(discoverAgentsButton.getPreferredSize());
	killAgentsButton.setPreferredSize(discoverAgentsButton.getPreferredSize());
	refreshAgentsButton.setPreferredSize(discoverAgentsButton.getPreferredSize());
	discoverAgentsButton.setPreferredSize(discoverAgentsButton.getPreferredSize());

	agentsPanel.add(new JScrollPane(buttonPanel,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.EAST);
	JPanel southPanel = new JPanel(new FlowLayout());
	southPanel.add(okButton = new JButton(Resources.getString("ok")));okButton.addMouseListener(this);
	southPanel.add(cancelButton = new JButton(Resources.getString("cancel")));cancelButton.addMouseListener(this);
	agentsPanel.add(southPanel,BorderLayout.SOUTH);	

	getContentPane().add(agentsPanel);
	pack();
    }
	
    private void centreWindow() {
	// now centre this window in the middle of the screen
	Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
	d.height = d.height / 2;
	d.width = d.width / 2;
	setLocation (d.width-(getSize().width / 2), d.height-(getSize().height / 2));
    }
	
    /**
     * This method syncs the properties and agents list.
     * Reads the properties file, and scan it's for agents. Any agents are added to
     * the list of agents 
     */
	
    private void updateAgentsFromProperties(Properties properties) {		
	ArrayList agents = getAgents(properties);
	ArrayList currAgent;
	for (int i = 0; i < agents.size(); i++) {
	    currAgent = (ArrayList)agents.get(i);
	    model.addRow(currAgent.toArray());			
	}
    }
	
    /**
     * This method returns a ArrayList containing a list of ArrayList, each of which contains
     * the name, port, os and arch objects.
     */
    public static ArrayList getAgents(Properties properties) {
	ArrayList result = new ArrayList();
	ArrayList currAgent;
	String name;
	// check for up to MAX_AGENT_COUNT agents
	for (int i = 1; i < MAX_AGENT_COUNT; i++) {
	    name = properties.getProperty(Common.host+i+Common.hostNamePattern,"");
	    if (name != "") {
		currAgent = new ArrayList(COLUMN_COUNT);
		currAgent.add(Integer.toString(result.size()+1));
		currAgent.add(name);
		currAgent.add(properties.getProperty(Common.host+i+Common.hostPortPattern,Resources.getString("undefined")));
		currAgent.add(properties.getProperty(Common.host+i+Common.hostArchPattern,Resources.getString("undefined")));
		currAgent.add(properties.getProperty(Common.host+i+Common.hostOSPattern,Resources.getString("undefined")));
		result.add(currAgent);
	    }
	}
	return result;
    }
	
    /** 
     * Convert a Properties object to a String of form
     * PROP=VAL\n, PROP2=VAL2\n
     */
    private static String propertiesToString(Properties properties) {
	String propertiesStr = "";
	String name;
	for (Enumeration e = properties.propertyNames() ; e.hasMoreElements() ;) {
	    name = (String)e.nextElement();
	    propertiesStr = propertiesStr+(name+"="+properties.getProperty(name)+System.getProperty("line.separator"));
	}
	return propertiesStr;
    }
	
    /**
     * This method returns the next available agent number, in case they
     * are for some reason not in sequence or missing sequences.
     */
    private int getFreeAgentNumber() {
	for (int i = 1; i < MAX_AGENT_COUNT; i++) {
	    if (!numberExists(i)) {
		return i;
	    }
	}
	return MAX_AGENT_COUNT;
    }
	
    private void addAgent() {
	EditAgent addAgent = new EditAgent(this,Resources.getString("addAgent"));
	// find first free agent number
	addAgent.setAgentNumber(Integer.toString(getFreeAgentNumber()));
	addAgent.setVisible(true);
    }
	
    public void addAgent(EditAgent agent) {
	model.addRow(agent.getAgent());
    }
	
    /**
     * This method returns true if we have an agent corresponding to the value number.
     */
    private boolean numberExists(int number) {
	for (int i = 0; i < model.getRowCount(); i++)
	    if (number==Integer.parseInt((String)model.getValueAt(i,0)))
		return true;
	return false;
			
    }
	
    /**
     * Returns the index of this item in our table, or negative if
     * it is not in our table.
     */
    private int agentExists(String name, String port) {
	String currName, currPort;
	for (int i = 0; i < model.getRowCount(); i++) {
	    if (((String)model.getValueAt(i,1)).equals(name)) {
		if (((String)model.getValueAt(i,2)).equals(port)) {
		    return i;
		}
	    }
	}
	return -1;
    }
	
    private void nothingSelected() {
	JOptionPane.showMessageDialog(this,
				      Resources.getString("nothingSelected"),
				      Resources.getString("error"),
				      JOptionPane.INFORMATION_MESSAGE);
    }
	
    private void editAgent() {
	EditAgent editAgent;
	int indexes[] = agentTable.getSelectedRows();
	if (indexes.length==0) {
	    nothingSelected();
	    return;
	}
	for (int i = 0; i < indexes.length; i++) {
	    editAgent = new EditAgent(this,Resources.getString("editAgent"));
	    editAgent.setAgent(EditAgent.getAgent((String)model.getValueAt(indexes[i],0),
						  (String)model.getValueAt(indexes[i],1),
						  (String)model.getValueAt(indexes[i],2),
						  (String)model.getValueAt(indexes[i],3),
						  (String)model.getValueAt(indexes[i],4)));
	    editAgent.setIndex(indexes[i]);					
	    editAgent.setVisible(true);					
	}
    }
	
    public void editAgent(EditAgent editAgent, int i) {
	String sarray[] = editAgent.getAgent();
	model.setValueAt(sarray[0],i,0);
	model.setValueAt(sarray[1],i,1);
	model.setValueAt(sarray[2],i,2);
	model.setValueAt(sarray[3],i,3);
	model.setValueAt(sarray[4],i,4);
    }
	
    private void delAgent() {	
	int indexes[] = agentTable.getSelectedRows();
	if (indexes.length==0) {
	    nothingSelected();
	    return;
	}
	// first sort the list into ascending order
	Arrays.sort(indexes);
		
	for (int i = indexes.length-1; i >= 0 ; i--) {
	    model.removeRow(indexes[i]);
	}
	// shift up the remaining agents to keep sync on the number
	for (int i = 0; i < agentTable.getRowCount(); i++) {
	    model.setValueAt(Integer.toString(i+1),i,0);
	}
    }
	
    private void killAgents() {	
	EditAgent editAgent = new EditAgent(this,Resources.getString("editAgent"));
	ArrayList deadAgents = new ArrayList(model.getRowCount());
		
	int indexes[] = agentTable.getSelectedRows();
	if (indexes.length==0) {
	    nothingSelected();
	    return;
	}
	for (int i = 0; i < indexes.length; i++) {
	    editAgent.setAgent(EditAgent.getAgent((String)model.getValueAt(indexes[i],0),
						  (String)model.getValueAt(indexes[i],1),
						  (String)model.getValueAt(indexes[i],2),
						  (String)model.getValueAt(indexes[i],3),
						  (String)model.getValueAt(indexes[i],4)));
	    // make sure we kill each entry once only
	    if (!deadAgents.contains(editAgent.getAgent()[1]+editAgent.getAgent()[2])) {
		editAgent.killAgent();
		deadAgents.add(editAgent.getAgent()[1]+editAgent.getAgent()[2]);
	    }
	}
	editAgent.dispose();
    }
	
    private void refreshAgent(int index, EditAgent editAgent) {
	if (index >=0) {
	    editAgent.setAgent(EditAgent.getAgent((String)model.getValueAt(index,0),
						  (String)model.getValueAt(index,1),
						  (String)model.getValueAt(index,2),
						  (String)model.getValueAt(index,3),
						  (String)model.getValueAt(index,4)));
	    editAgent.detectAgent();
	    String sarray[] = editAgent.getAgent();
	    model.setValueAt(sarray[0],index,0);
	    model.setValueAt(sarray[1],index,1);
	    model.setValueAt(sarray[2],index,2);
	    model.setValueAt(sarray[3],index,3);
	    model.setValueAt(sarray[4],index,4);
	    model.fireTableRowsUpdated(index,index);
	}
    }
	
    private void refreshAgents() {
	EditAgent editAgent = new EditAgent(this,Resources.getString("editAgent"));
		
	int indexes[] = agentTable.getSelectedRows();
	if (indexes.length==0) {
	    nothingSelected();
	    return;
	}
	for (int i = 0; i < indexes.length; i++)
	    refreshAgent(indexes[i],editAgent);
	editAgent.dispose();
    }
	
    private void discoverAgents() {
	DiscoveryProbe probe = new DiscoveryProbe();
	ArrayList response = probe.getResponse();
	String discoveredAgent[] = new String[COLUMN_COUNT];
	String eol = "\n";
	String responseString;
	int pos;
	int index;
	for (int i = 0; i < response.size(); i++) {
	    responseString = (String)response.get(i);
	    discoveredAgent[0] = String.valueOf(getFreeAgentNumber());
	    discoveredAgent[1] = responseString.substring(0,responseString.indexOf(eol)).trim();
	    pos = discoveredAgent[1].length()+1;
	    discoveredAgent[2] = responseString.substring(pos,responseString.indexOf(eol,pos)).trim();
	    pos += discoveredAgent[2].length()+1;
	    discoveredAgent[3] = responseString.substring(pos,responseString.indexOf(eol,pos)).trim();
	    pos += discoveredAgent[3].length()+1;
	    discoveredAgent[4] = responseString.substring(pos,responseString.length()).trim();
	    index = agentExists(discoveredAgent[1],discoveredAgent[2]);
	    if (index < 0) {
		model.addRow(discoveredAgent);
	    }
	    else {
		// exists already, so replace current value
		discoveredAgent[0] = String.valueOf(index+1);
		for (int j = 0; j < COLUMN_COUNT; j++) {
		    model.setValueAt(discoveredAgent[j],index,j);
		}
	    }
	}
    }
	
    private void moveAgentUp() {
	int i = agentTable.getSelectedRow();
	if (i > 0) {
	    model.moveRow(i, i, i-1);
	    agentTable.clearSelection();
	    agentTable.addRowSelectionInterval(i-1,i-1);
	    // now switch the number indices
	    String t = (String)model.getValueAt(i,0);
	    model.setValueAt(model.getValueAt(i-1,0),i,0);
	    model.setValueAt(t,i-1,0);
	}
    }
	
    private void moveAgentDown() {
	int i = agentTable.getSelectedRow();
	if (i < (agentTable.getRowCount()-1)) {
	    model.moveRow(i, i, i+1);
	    agentTable.clearSelection();
	    agentTable.addRowSelectionInterval(i+1,i+1);
	    // now switch the number indices
	    String t = (String)model.getValueAt(i,0);
	    model.setValueAt(model.getValueAt(i+1,0),i,0);
	    model.setValueAt(t,i+1,0);			
	}
    }
	
    public void updateProperties(Properties p) {
	String name="", port="", arch="", os="", number="";
	// first clear all the entries we don't want
	for (int i = 0; i <MAX_AGENT_COUNT; i++) {
	    number = Integer.toString(i);
	    if (p.getProperty(Common.host+number+Common.hostNamePattern)!=null) {
		p.remove(Common.host+number+Common.hostNamePattern);
		p.remove(Common.host+number+Common.hostPortPattern);
		p.remove(Common.host+number+Common.hostArchPattern);
		p.remove(Common.host+number+Common.hostOSPattern);
	    }
	}
	// now add all the new entries
	int count = model.getRowCount();
	for (int i = 0; i < model.getRowCount(); i++) {
	    number = (String)model.getValueAt(i,0);
	    p.put(Common.host+number+Common.hostNamePattern,(String)model.getValueAt(i,1));
	    p.put(Common.host+number+Common.hostPortPattern,(String)model.getValueAt(i,2));
	    p.put(Common.host+number+Common.hostArchPattern,(String)model.getValueAt(i,3));
	    p.put(Common.host+number+Common.hostOSPattern,  (String)model.getValueAt(i,4));
	}
	// now add our agent count
	p.put(Common.AGENT_COUNT,Integer.toString(count));
    }
	
    public 	void mouseClicked(MouseEvent e) {
	if (((JButton)e.getSource()).getText().equals(Resources.getString("addAgent"))) {
	    addAgent();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("editAgent"))) {
	    editAgent();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("deleteAgent"))) {
	    delAgent();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("killAgents"))) {
	    killAgents();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("refreshAgents"))) {
	    refreshAgents();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("discoverAgents"))) {
	    discoverAgents();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("ok"))) {
	    setVisible(false);
	    parent.editAgentSettingsCallback(this);
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("cancel"))) {
	    setVisible(false);
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("moveUp"))) {
	    moveAgentUp();
	    return;
	}
	if (((JButton)e.getSource()).getText().equals(Resources.getString("moveDown"))) {
	    moveAgentDown();
	    return;
	}		
    }
    public void  mouseEntered(MouseEvent e) {
    }
    public void   mouseExited(MouseEvent e) {
    }
    public void  mousePressed(MouseEvent e) {
    }
    public void  mouseReleased(MouseEvent e) {
    }
    }
