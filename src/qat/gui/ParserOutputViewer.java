package qat.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import qat.common.*;
import qat.gui.*;

public class ParserOutputViewer extends JComponent implements TreeSelectionListener, ChangeListener, ActionListener {
	private JEditorPane parserText;
	private JLabel testTitle;
	private TestSpecification currTest;
	private QAT parent;
	private JButton searchButton;
	private JTextField searchText;
	
	public ParserOutputViewer(QAT parent) {
		super();
		this.parent = parent;
		setupScreen();
	}
  
	public ParserOutputViewer(QAT parent, TestSpecification test) {
		this(parent);
		setObjectView(test);
	}
  
	public void setupScreen() {
		setLayout(new BorderLayout());
		JPanel north = new JPanel();
		testTitle = new JLabel(Resources.getString("empty"),SwingConstants.CENTER);
		north.add(testTitle);
		searchButton = new JButton(Resources.getString("search"),
								   new ImageIcon(Resources.getImageResource("searchImage")));
		searchButton.addActionListener(this);
		north.add(searchButton);
		searchText = new JTextField(12);
		north.add(searchText);
		
		add(north,BorderLayout.NORTH);
		parserText = new JEditorPane();
		parserText.setBackground(Common.parserOutputBackground);
		parserText.setForeground(Common.parserOutputForeground);
		// ensure a fixed font width font
		Font old = parserText.getFont();
		if (old != null)
			parserText.setFont(new Font("Monospaced",old.getStyle(),old.getSize()));
		
		JScrollPane scrollPane;
		add(scrollPane = new JScrollPane(parserText,
										 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
										 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
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
		testTitle.setText(test.getTestName());
		File f = new File(test.getParserTraceFileName(parent.getProjectResultsDirectory()));
		PageLoader.setText(parserText,Resources.getString("empty"));
		// load the parser output file
		try {
			PageLoader.setPage(parserText, f.toURL());
		}
		catch (java.net.MalformedURLException ex) {
			ex.printStackTrace();
		}
	}
	
	public Dimension getPreferredSize() {
		return super.getSize();
	}
	
	/* ----------- THESE ARE THE EVENT HANDLING ROUTINES ------------*/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource()).getText().equals(Resources.getString("search"))) {	
				search();
				return;
			}
		}
	}
	
	private void search() {
		search(parserText,searchText.getText());
	}

    public static void search(JEditorPane editor, String searchString) {
	try {
	    int index  = editor.getDocument().getText(0,editor.getDocument().getLength()).indexOf(searchString,
												  editor.getCaretPosition());
	    if (index>=0) {
		editor.setCaretPosition(index);
		editor.moveCaretPosition(index+searchString.length());
		editor.getCaret().setSelectionVisible(true);
		editor.getCaret().setVisible(true);
	    }
	    else {
		editor.setCaretPosition(0);
		editor.getCaret().setVisible(false);
		JOptionPane.showMessageDialog(editor.getParent(), 
					      Resources.getString("searchFailed")+" ("+searchString+")",
					      Resources.getString("notFound"), 
					      JOptionPane.INFORMATION_MESSAGE);
	    }
	}
	catch (javax.swing.text.BadLocationException ex) {
	    ex.printStackTrace();
	}
    }
}
