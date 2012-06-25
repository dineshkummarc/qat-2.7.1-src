package qat.gui;

// @(#)KeywordComponent.java 1.15 01/04/22 

// java import
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.util.Properties;
import java.util.ArrayList;

import qat.gui.*;
import qat.components.*;
import qat.common.*;

public class KeywordComponent extends JComponent implements ActionListener {
	
	private JCheckBox passedCheckBox, 
		failedCheckBox,
		unresolvedCheckBox,
		notrunCheckBox, 
		allCheckBox;
	private JComboBox keyWordExprHist;
	private JComboBox keyWordList;	
	private JTextField keyWordExpr;
	private TestTree testTree;
	
    public KeywordComponent (TestTree testTree) {
		super();
		this.testTree = testTree;
		setupScreen();
    }
	
	private void setupScreen() {
		setLayout(new GridLayout(1,2));
		JPanel centerPanel = new JPanel(new GridLayout(4,2));
		JPanel temp;
		JButton button;
		temp = new JPanel(new GridLayout(1,1));
		temp.add(button = new JButton(Resources.getString("applyKeywordExpr")));
		button.addActionListener(this);
		centerPanel.add(temp);
		centerPanel.add(keyWordExpr = new JTextField());
		
		temp = new JPanel(new GridLayout(1,1));
		temp.add(button = new JButton(Resources.getString("applyKeywordExprHist")));
		button.addActionListener(this);
		centerPanel.add(temp);
		centerPanel.add(keyWordExprHist = new JComboBox());
		
		centerPanel.add(new JLabel(Resources.getString("clearHistoryDesc"),SwingConstants.CENTER));
		centerPanel.add(button = new JButton(Resources.getString("clearHistory")));
		button.addActionListener(this);
		
		centerPanel.add(new JLabel(Resources.getString("keyWordLookup"),SwingConstants.CENTER));		
		keyWordList = new JComboBox();
		centerPanel.add(keyWordList);
		add(centerPanel);
		
		JPanel southPanel = new JPanel(new GridLayout(1,2));
		JPanel southRight = new JPanel(new GridLayout(2,2));
		southRight.setBorder(BorderFactory.createTitledBorder(Resources.getString("applyExprTo")));
		passedCheckBox     = new JCheckBox(Resources.getString("passed"));
		failedCheckBox     = new JCheckBox(Resources.getString("failed"));
		unresolvedCheckBox = new JCheckBox(Resources.getString("unresolved"));
		notrunCheckBox     = new JCheckBox(Resources.getString("notrun"));
		allCheckBox = new JCheckBox(Resources.getString("all"),true);
		southRight.add(new JPanel());
		southRight.add(passedCheckBox);
		southRight.add(failedCheckBox);
		southRight.add(unresolvedCheckBox);
		southRight.add(notrunCheckBox);
		southRight.add(allCheckBox);
		southPanel.add(southRight);
		add(southRight);
	}
	
	/**
	 * This method is exposed to allow test selection via the commandline interface
	 * to Qat.
	 */
	public void applyKeywordExpression(String exprString) {
		keyWordExpr.setText(exprString);
		applyKeywordExpression();
	}
	
	/**
	 * Selects all tests matching the keyword expression entered.
	 */
	private void applyKeywordExpression() {
		ArrayList allTests = testTree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		testTree.pauseListeners();
		for (int i = 0; i < allTests.size(); i++) {
			node = (TestTreeNode)allTests.get(i);
			test = (TestSpecification)node.getUserObject();
			if (applyToTest(test)) // Check CheckBox status
				if (Evaluater.evaluateExpression(keyWordExpr.getText(),test.getKeyWords())) {
					testTree.selectNode(node);
				}
		}
		testTree.resumeListeners();
		// add this expression to our history, if it's not already there
		keyWordExprHist.removeItem(keyWordExpr.getText());
		keyWordExprHist.addItem(keyWordExpr.getText());
		keyWordExprHist.setSelectedItem(keyWordExpr.getText());
	}
	
	/**
	 * This refreshes our list of all available keywords as reported by parsing the test tree.
	 */
	public void loadTestsKeywords() {
		// read the values from the qat files
		if (keyWordList.getItemCount()>0)
			keyWordList.removeAllItems();
		String[] keyWords;
		keyWords = testTree.getKeyWordList();
		for (int i = 0; i < keyWords.length; i++) {
			keyWordList.addItem(keyWords[i]);
		}
	}
	
	/**
	 * Returns true if the test parameters status matches one of the
	 * check boxes selected for the ApplyKeyWord selection.
	 */
	private boolean applyToTest(TestSpecification test) {
		// apply to passed tests
		if ((passedCheckBox.isSelected())&&
			(test.getStatus()==ProtocolConstants.PASSED))
			return true;
		else
			// apply to failed tests
			if ((failedCheckBox.isSelected())&&
				(test.getStatus()==ProtocolConstants.FAILED))
				return true;
			else
				// apply to unresolved tests
				if ((unresolvedCheckBox.isSelected())&&
					(test.getStatus()==ProtocolConstants.UNRESOLVED))
					return true;
				else
					// apply to notrun tests
					if ((notrunCheckBox.isSelected())&&
						(test.getStatus()==ProtocolConstants.NOTRUN))
						return true;
					else
						// apply to all tests
						if (allCheckBox.isSelected())
							return true;
		return false;
	}
	
	private void applyKeywordExpressionHist() {
		ArrayList allTests = testTree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		testTree.pauseListeners();
		for (int i = 0; i < allTests.size(); i++) {
			node = (TestTreeNode)allTests.get(i);
			test = (TestSpecification)node.getUserObject();
			if (applyToTest(test)) // Check CheckBox status
				if (Evaluater.evaluateExpression((String)keyWordExprHist.getSelectedItem(),test.getKeyWords()))
					testTree.selectNode(node);
		}
		testTree.resumeListeners();
	}
	
	private void clearHistory() {
		if (keyWordExprHist.getItemCount()>0) {
			keyWordExprHist.removeAllItems();
			keyWordExprHist.setSelectedItem("");
		}
	}
	
		/**
	 * This method loads the keyword history out of the project properties files
	 * and stores it in the ComboBox component. They are then deleted out of the project properties.
	 */
	public void loadKeyWordHistFromProperties(Properties projectProperties) {
		try {
			if (keyWordExprHist.getItemCount()>0)
				keyWordExprHist.removeAllItems();
			int count = new Integer(projectProperties.getProperty("KeyWordHistoryCount","0")).intValue();
			for (int i = 0; i < count; i++) {
				keyWordExprHist.addItem(projectProperties.getProperty("KeyWordHistory"+Integer.toString(i)));
				projectProperties.remove("KeyWordHistory"+Integer.toString(i));
			}
			projectProperties.remove("KeyWordHistoryCount");
		}
		catch (Exception e) {
			System.out.println("Error detected while loading keyword history from current project");
			e.printStackTrace();
		}
	}
	
	public void saveKeyWordHistToProperties(Properties projectProperties) {
		int count = keyWordExprHist.getItemCount();
		projectProperties.put("KeyWordHistoryCount",Integer.toString(count));
		for (int i = 0; i < count; i++) {
			projectProperties.put("KeyWordHistory"+Integer.toString(i),keyWordExprHist.getItemAt(i));
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		// apply keyword expression
		if (((JButton)e.getSource()).getText().equals(Resources.getString("applyKeywordExpr"))) {	
			applyKeywordExpression();
			return;
		}
		// apply keyword expression from history
		if (((JButton)e.getSource()).getText().equals(Resources.getString("applyKeywordExprHist"))) {	
			applyKeywordExpressionHist();
			return;
		}
		// clear keyword history
		if (((JButton)e.getSource()).getText().equals(Resources.getString("clearHistory"))) {	
			clearHistory();
			return;
		}
	}
}
