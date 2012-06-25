package qat.gui;

import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import qat.common.*;
import qat.gui.*;

public class CompareResults extends JFrame implements ActionListener {
	
	private JTable matchingResultsTable, differentResultsTable, unmatchedResultsTable;
	private JButton startCompare;
	private JTextField resultsPathA, resultsPathB;
	private JScrollPane scrollPane1, scrollPane2, scrollPane3;
	private JButton browseA, browseB;
	private JLabel totalMatching, totalDifferent, totalUnmatched;
	Object titles[] = {"Test Name", "Test Path", "Status",""};
	
	public CompareResults(QAT parent) {
		super(Resources.getString("compareResultsTitle"));
		setupScreen();
		SwingUtils.setLocationRelativeTo(this,parent);
		setVisible(true);
	}
	
	public CompareResults(QAT parent,String pathA, String pathB) {
		this(parent);
		resultsPathA.setText(pathA);
		resultsPathB.setText(pathB);
	}
	
	private void setupMenu() {
				// ---------- set up the system menu ------------
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		JMenuItem item;
		// ------------------add the file menu-------------------
		menu = new JMenu(Resources.getString("file"));
		// load
		menu.add(item = new JMenuItem("Start"));
		item.addActionListener(this);
		// close
		menu.add(item = new JMenuItem("Close"));
		item.addActionListener(this);
		
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
	}
	
	private void setupScreen() {
		setupMenu();
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		// ------------ north panel -----------
		JPanel resultSelectionPanel = new JPanel(new GridLayout(2,1));
		resultSelectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		// results set A
		JPanel temp = new JPanel(new BorderLayout());
		temp.add(new JLabel("Result A:"),BorderLayout.WEST);
		temp.add(resultsPathA = new JTextField(),BorderLayout.CENTER);
		temp.add(browseA = new JButton("Browse"),BorderLayout.EAST);
		browseA.addActionListener(this);
		resultSelectionPanel.add(temp);
		// results set B
		temp = new JPanel(new BorderLayout());
		temp.add(new JLabel("Result B:"),BorderLayout.WEST);
		temp.add(resultsPathB = new JTextField(),BorderLayout.CENTER);
		temp.add(browseB = new JButton("Browse"),BorderLayout.EAST);
		browseB.addActionListener(this);
		resultSelectionPanel.add(temp);
		container.add(resultSelectionPanel,BorderLayout.NORTH);
		
		// ------------ center panel -----------
		JTabbedPane resultsPanel = new JTabbedPane();
		matchingResultsTable = new JTable(new DefaultTableModel(titles,0));
		scrollPane1 = new JScrollPane(matchingResultsTable,
									 ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
									  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// this can be removed when Java 1.3 is standard because it will beimplemented internally by default
		scrollPane1.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
		// end remove
		resultsPanel.add(scrollPane1,"Same status");
		
		differentResultsTable = new JTable(new DefaultTableModel(titles,0));
		scrollPane2 = new JScrollPane(differentResultsTable,
									 ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
									  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// this can be removed when Java 1.3 is standard because it will beimplemented internally by default
		scrollPane2.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
		// end remove
		resultsPanel.add(scrollPane2,"Different status");
		
		unmatchedResultsTable =new JTable(new DefaultTableModel(titles,0));
		scrollPane3 = new JScrollPane(unmatchedResultsTable,
									 ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
									  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		// this can be removed when Java 1.3 is standard because it will beimplemented internally by default
		scrollPane3.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
		// end remove
		resultsPanel.add(scrollPane3,"Unmatched tests");
		container.add(resultsPanel,BorderLayout.CENTER);
		// ------------ south panel -----------
		JPanel southPanel = new JPanel(new GridLayout(1,7));
		southPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));

		southPanel.add(new JLabel("Matching :",SwingConstants.RIGHT));
		southPanel.add(totalMatching = new JLabel("0",SwingConstants.LEFT));
		southPanel.add(new JLabel("Different :",SwingConstants.RIGHT));
		southPanel.add(totalDifferent = new JLabel("0",SwingConstants.LEFT));
		southPanel.add(new JLabel("Unmatched :",SwingConstants.RIGHT));
		southPanel.add(totalUnmatched = new JLabel("0",SwingConstants.LEFT));
		southPanel.add(startCompare = new JButton("Start"));
		startCompare.addActionListener(this);
		container.add(southPanel,BorderLayout.SOUTH);
		// ------------ east  panel -----------
		// ------------ west  panel -----------		
		pack();
		setSize(640,480);
	}
	
	private void clearModel(DefaultTableModel model) {
		int pos;
		while ((pos=model.getRowCount())>0)
			model.removeRow(pos-1);
		
	}
	
	private void loadResults() {
		try {
			scrollPane1.invalidate();
			TestTree tree1 = new TestTree();
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(resultsPathA.getText())));
			tree1.loadTests(in);
			in.close();
			TestTree tree2 = new TestTree();
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(resultsPathB.getText())));
			tree2.loadTests(in);
			in.close();
			ArrayList results1 = tree1.getAllTests();
			ArrayList results2 = tree2.getAllTests();
			int index;
			
			// get all the matching results
			ArrayList matchingResults = new ArrayList();
			ArrayList differentResults = new ArrayList();
			ArrayList otherResult = new ArrayList();
			ArrayList unmatchedResults = new ArrayList();
			for (int i = results1.size()-1; i >= 0; i--) {
				index = indexOfTest((TestSpecification)results1.get(i),results2);
				if (index >=0) {
					if (((TestSpecification)results1.get(i)).getStatus()==((TestSpecification)results2.get(index)).getStatus()) {
						matchingResults.add(results1.get(i));
					}
					else {
						differentResults.add(results1.get(i));
						otherResult.add(results2.get(index));
					}
					results1.remove(i);
					results2.remove(index);
				}
				else {
					unmatchedResults.add(results1.get(i));
					results1.remove(i);
				}
			}
			// any tests left over in results2 are unmatched as well
			for (int i = 0; i < results2.size(); i++)
				unmatchedResults.add(results2.get(i));
			
			// now copy this data into the respective tables
			Object rowData[] = new Object[4];
			DefaultTableModel matchingResultsModel = (DefaultTableModel)matchingResultsTable.getModel();
			clearModel(matchingResultsModel);
			TestSpecification test;
			for (int i = 0; i < matchingResults.size(); i++) {
				test = (TestSpecification)matchingResults.get(i);
				rowData[0]=test.getTestName();
				rowData[1]=test.getTestSpecPath();
				rowData[2]=test.getStatusString();
				rowData[3]="";// not used
				matchingResultsModel.addRow(rowData);
			}
			matchingResultsTable.setModel(matchingResultsModel);
			totalMatching.setText(Integer.toString(matchingResults.size()));
			
			DefaultTableModel differentResultsModel = (DefaultTableModel)differentResultsTable.getModel();
			clearModel(differentResultsModel);
			for (int i = 0; i < differentResults.size(); i++) {
				test = (TestSpecification)differentResults.get(i);
				rowData[0] = test.getTestName();
				rowData[1]=test.getTestSpecPath();
				rowData[2]=test.getStatusString()+"/"+((TestSpecification)otherResult.get(i)).getStatusString();
				differentResultsModel.addRow(rowData);
			}
			differentResultsTable.setModel(differentResultsModel);
			totalDifferent.setText(Integer.toString(differentResults.size()));
			
			DefaultTableModel unmatchedResultsModel = (DefaultTableModel)unmatchedResultsTable.getModel();
			clearModel(unmatchedResultsModel);
			for (int i = 0; i < unmatchedResults.size(); i++) {
				test = (TestSpecification)unmatchedResults.get(i);
				rowData[0] = test.getTestName();
				rowData[1]=test.getTestSpecPath();
				rowData[2]=test.getStatusString();
				unmatchedResultsModel.addRow(rowData);
			}
			unmatchedResultsTable.setModel(unmatchedResultsModel);
			totalUnmatched.setText(Integer.toString(unmatchedResults.size()));
		}
		catch (Exception e) {
			System.out.println("Error loading results file"+e.toString());
			e.printStackTrace();
		}
		finally {
			scrollPane1.validate();						
		}
	}
	
	private int indexOfTest(TestSpecification test, ArrayList testList) {
		TestSpecification currTest;
		for (int i = 0; i < testList.size(); i++) {
			currTest = (TestSpecification)testList.get(i);
			if (currTest.getTestName().equals(test.getTestName()))
				return i;
		}
		return -1;
	}
	
	private void browse(JTextField textField) {
		JFileChooser resultsFile = new JFileChooser(textField.getText());
		resultsFile.setDialogTitle("Select results file");
		ExtensionFileFilter filter = new ExtensionFileFilter(Common.SERIALIZED_TREE_EXTENSION,"Serialised test trees");
		resultsFile.addChoosableFileFilter(filter);
		resultsFile.setFileFilter(filter);
		try {
			if (resultsFile.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
				textField.setText(resultsFile.getSelectedFile().getCanonicalPath());
			}
		}
		catch (java.io.IOException e) {
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource())==browseA) {
				browse(resultsPathA);
				return;
			}
			if (((JButton)e.getSource())==browseB) {
				browse(resultsPathB);
				return;
			}
			if (((JButton)e.getSource()).getText().equals("Start")) {
				loadResults();
			}
		}
		if (e.getSource() instanceof JMenuItem) {
			// load
			if (((JMenuItem)e.getSource()).getText().equals("Start")) {
				loadResults();
			}
			// close
			if (((JMenuItem)e.getSource()).getText().equals("Close")) {
				setVisible(false);
				dispose();
			}
		}
	}
}
