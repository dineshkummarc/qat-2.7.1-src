package qat.gui;

import qat.common.ProtocolConstants;
import qat.gui.TestTree;

import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.tree.TreeNode;

public class ReportObject extends Object {
	private TestTree tree;
	
	public ReportObject(TestTree tree) {
		this.tree = tree;
	}
	
	private ArrayList getTestsByResult(ArrayList tests, int result) {
		ArrayList results = new ArrayList(tests.size());
		for (int i = 0; i < tests.size(); i++)
			if (((TestSpecification)(((TestTreeNode)tests.get(i)).getUserObject())).getStatus()==result)
				results.add(tests.get(i));
		return results;
	}
	
	private void printTreeSorted(PrintManager pm, String sessionName) {
		int passed = 0;
		int failed = 0;
		int notrun = 0;
		int unresolved = 0;
		pm.setHeader("TEST STATUS :"+sessionName+" (SORTED BY STATUS)");
		pm.newPage();
		ArrayList treeElements = tree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		TreeNode parent=null;
		TreeNode path[];
		pm.println("Test directory root :"+tree.getRoot());
		pm.newLine();
		pm.newLine();
		// print out passed tests
		pm.println("Passed :");
		pm.printLine('-');
		ArrayList passedTests = getTestsByResult(treeElements,ProtocolConstants.PASSED);
		passed = passedTests.size();
		for (int i = 0; i < passedTests.size(); i++) {
			node = (TestTreeNode)passedTests.get(i);
			test = ((TestSpecification)node.getUserObject());
			pm.println(node.getRoot().toString()+":"+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
		}
		pm.newLine();

		// print out notrun tests
		pm.println("Not run :");
		pm.printLine('-');
		ArrayList notrunTests = getTestsByResult(treeElements,ProtocolConstants.NOTRUN);
		notrun = notrunTests.size();
		for (int i = 0; i < notrunTests.size(); i++) {
			node = (TestTreeNode)notrunTests.get(i);
			test = ((TestSpecification)node.getUserObject());
			pm.println(node.getRoot().toString()+":"+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
		}
		pm.newLine();
		
		// print out unresolved tests
		pm.println("Unresolved :");
		pm.printLine('-');
		ArrayList unresolvedTests = getTestsByResult(treeElements,ProtocolConstants.UNRESOLVED);
		unresolved = unresolvedTests.size();
		for (int i = 0; i < unresolvedTests.size(); i++) {
			node = (TestTreeNode)unresolvedTests.get(i);
			test = ((TestSpecification)node.getUserObject());
			pm.println(node.getRoot().toString()+":"+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
		}
		pm.newLine();
		
		// print out failed tests
		pm.println("Failed :");
		pm.printLine('-');
		ArrayList failedTests = getTestsByResult(treeElements,ProtocolConstants.FAILED);
		failed = failedTests.size();
		for (int i = 0; i < failedTests.size(); i++) {
			node = (TestTreeNode)failedTests.get(i);
			test = ((TestSpecification)node.getUserObject());
			pm.println(node.getRoot().toString()+":"+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
		}
		pm.newLine();
		
		// now print the totals
		pm.printLine('-');
		pm.println(PrintManager.padRight("PASSED :"+passed,' ',15)+
				   PrintManager.padRight("FAILED :"+failed,' ',15)+
				   PrintManager.padRight("NOTRUN :"+notrun,' ',15)+
				   PrintManager.padRight("UNRESOLVED :"+unresolved,' ',15)+
				   PrintManager.padRight("TOTAL :"+(passed+failed+notrun+unresolved),' ',15));
		pm.printLine('-');
	}
	
	private void printTreeNonPassedOnly(PrintManager pm, String sessionName) {
		int passed = 0;
		int failed = 0;
		int notrun = 0;
		int unresolved = 0;
		pm.setHeader("TEST STATUS :"+sessionName+" (ONLY FAILED,NOTRUN AND UNRESOLVED)");
		pm.newPage();
		ArrayList treeElements = tree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		TreeNode parent=null;
		TreeNode path[];
		String tabString = " ";
		pm.println("Test directory root :"+tree.getRoot());
		pm.newLine();
		pm.newLine();
		for (int i = 0; i < treeElements.size(); i++) {
			node = (TestTreeNode)treeElements.get(i);
			// print out each time new root is followed
			if (!(node.getUserObject() instanceof TestSpecification)){
				if (parent!=node.getParent()) {
					parent = node.getParent();
					path = node.getPath();
					String pathStr = "";
					for (int j = 1; j < path.length-1; j++)
						pathStr = pathStr+"."+path[j].toString();
					pm.setFontStyle(Font.BOLD);
					pm.println(pathStr);
					pm.setFontStyle(Font.PLAIN);
					tabString = "  ";
					for (int j = 0; j < path.length; j++) {
						tabString = tabString + "--";
					}
				}
			}
			test = ((TestSpecification)node.getUserObject());
			if (test.getStatus()!=ProtocolConstants.PASSED)
				pm.println(tabString+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
			switch (test.getStatus()) {
			case ProtocolConstants.PASSED : passed++;break;
			case ProtocolConstants.FAILED : failed++;break;
			case ProtocolConstants.NOTRUN : notrun++;break;
			case ProtocolConstants.UNRESOLVED : unresolved++;break;
			}
		}
		// now print the totals
		pm.printLine('-');
		pm.println(PrintManager.padRight("PASSED :"+passed,' ',15)+
				   PrintManager.padRight("FAILED :"+failed,' ',15)+
				   PrintManager.padRight("NOTRUN :"+notrun,' ',15)+
				   PrintManager.padRight("UNRESOLVED :"+unresolved,' ',15)+
				   PrintManager.padRight("TOTAL :"+(passed+failed+notrun+unresolved),' ',15));
		pm.printLine('-');
	}
	
	private void printTreeAll(PrintManager pm, String sessionName) {
		int passed = 0;
		int failed = 0;
		int notrun = 0;
		int unresolved = 0;
		pm.setHeader("TEST STATUS :"+sessionName+" (ALL TESTS)");
		pm.newPage();
		ArrayList treeElements = tree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		TreeNode parent=null;
		TreeNode path[];
		String tabString = " ";
		pm.println("Test directory root :"+tree.getRoot());
		pm.newLine();
		pm.newLine();
		for (int i = 0; i < treeElements.size(); i++) {
			node = (TestTreeNode)treeElements.get(i);
			// print out each time new root is followed
			if (!(node.getUserObject() instanceof TestSpecification)){
				if (parent!=node.getParent()) {
					parent = node.getParent();
					path = node.getPath();
					String pathStr = "";
					for (int j = 1; j < path.length-1; j++)
						pathStr = pathStr+"."+path[j].toString();
					pm.setFontStyle(Font.BOLD);
					pm.println(pathStr);
					pm.setFontStyle(Font.PLAIN);
					tabString = "  ";
					for (int j = 0; j < path.length; j++) {
						tabString = tabString + "--";
					}
				}
			}
			test = ((TestSpecification)node.getUserObject());
			pm.println(tabString+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
			switch (test.getStatus()) {
			case ProtocolConstants.PASSED : passed++;break;
			case ProtocolConstants.FAILED : failed++;break;
			case ProtocolConstants.NOTRUN : notrun++;break;
			case ProtocolConstants.UNRESOLVED : unresolved++;break;
			}
		}
		// now print the totals
		pm.printLine('-');
		pm.println(PrintManager.padRight("PASSED :"+passed,' ',15)+
				   PrintManager.padRight("FAILED :"+failed,' ',15)+
				   PrintManager.padRight("NOTRUN :"+notrun,' ',15)+
				   PrintManager.padRight("UNRESOLVED :"+unresolved,' ',15)+
				   PrintManager.padRight("TOTAL :"+(passed+failed+notrun+unresolved),' ',15));
		pm.printLine('-');
	}
	
	public synchronized void printTree(PrintManager pm, String sessionName, int printType) {
		pm.setHeader("CURRENT SESSION PROPERTIES :"+sessionName);
		pm.printHeader();
		ArrayList elements = new ArrayList();
		String name;
		for (Enumeration e = tree.getDefaultProperties().propertyNames() ; e.hasMoreElements() ;) {
			name = (String)e.nextElement();
			elements.add(name+"="+tree.getDefaultProperties().getProperty(name)+System.getProperty("line.separator"));
		}
		Object parray[] = elements.toArray();
		Arrays.sort(parray);
		for (int i = 0; i < parray.length; i++) {
			pm.println((String)parray[i]);
		}
		pm.newLine();
		pm.newLine();
		pm.newLine();
		// now print out our tree
		switch(printType) {
		case 0 : printTreeAll(pm,sessionName);
			break;
		case 1 :printTreeSorted(pm,sessionName);
			break;
		case 2 :printTreeNonPassedOnly(pm,sessionName);
			break;
		}
	}
	
	public void printAsTextFile(String fileName, String sessionName) throws IOException {
		int passed = 0;
		int failed = 0;
		int notrun = 0;
		int unresolved = 0;
		String line = "---------------------------------------------------------------------------------";
		PrintWriter pm = new PrintWriter(new FileOutputStream(new File(fileName)));
		pm.println(line);
		pm.println("CURRENT SESSION PROPERTIES :"+sessionName);
		pm.println(line);
		ArrayList elements = new ArrayList();
		String name;
		for (Enumeration e = tree.getDefaultProperties().propertyNames() ; e.hasMoreElements() ;) {
			name = (String)e.nextElement();
			elements.add(name+"="+tree.getDefaultProperties().getProperty(name));
		}
		Object parray[] = elements.toArray();
		Arrays.sort(parray);
		for (int i = 0; i < parray.length; i++) {
			pm.println((String)parray[i]);
		}
		pm.println(line);
		pm.println("");
		pm.println("");
		pm.println(line);	

		pm.println("TEST STATUS :"+sessionName+" (ALL TESTS)");
		ArrayList treeElements = tree.getAllTestNodes();
		TestSpecification test;
		TestTreeNode node;
		TreeNode parent=null;
		TreeNode path[];
		String tabString = " ";
		pm.println("Test directory root :"+tree.getRoot());
		pm.println(line);
		pm.println("");
		for (int i = 0; i < treeElements.size(); i++) {
			node = (TestTreeNode)treeElements.get(i);
			// print out each time new root is followed
			if (!(node.getUserObject() instanceof TestSpecification)){
				if (parent!=node.getParent()) {
					parent = node.getParent();
					path = node.getPath();
					String pathStr = "";
					for (int j = 1; j < path.length-1; j++)
						pathStr = pathStr+"."+path[j].toString();
					pm.println(pathStr);
					tabString = "  ";
					for (int j = 0; j < path.length; j++) {
						tabString = tabString + "--";
					}
				}
			}
			test = ((TestSpecification)node.getUserObject());
			pm.println(tabString+PrintManager.padRight(test.toString(),'.',25)+" "+test.getStatusString());
			switch (test.getStatus()) {
			case ProtocolConstants.PASSED : passed++;break;
			case ProtocolConstants.FAILED : failed++;break;
			case ProtocolConstants.NOTRUN : notrun++;break;
			case ProtocolConstants.UNRESOLVED : unresolved++;break;
			}
		}
		// now print the totals
		pm.println(line);
		pm.println(PrintManager.padRight("PASSED :"+passed,' ',15)+
				   PrintManager.padRight("FAILED :"+failed,' ',15)+
				   PrintManager.padRight("NOTRUN :"+notrun,' ',15)+
				   PrintManager.padRight("UNRESOLVED :"+unresolved,' ',15)+
				   PrintManager.padRight("TOTAL :"+(passed+failed+notrun+unresolved),' ',15));		pm.println(line);
		pm.close();
	}
}
