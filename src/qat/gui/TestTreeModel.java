package qat.gui;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.io.*;

import qat.common.ProtocolConstants;
import qat.common.Resources;
import qat.gui.TestTree;

public class TestTreeModel extends DefaultTreeModel {
    private TestTree parent;
	private static ImageIcon passedIcon, failedIcon, notrunIcon, unresolvedIcon, runningIcon, brokenIcon, pendingIcon;
	
    static {
		passedIcon = new ImageIcon(Resources.getResource(Resources.getString("passedNodeImage")));
		failedIcon = new ImageIcon(Resources.getResource(Resources.getString("failedNodeImage")));
		notrunIcon = new ImageIcon(Resources.getResource(Resources.getString("notrunNodeImage")));
		unresolvedIcon = new ImageIcon(Resources.getResource(Resources.getString("unresolvedNodeImage")));
		runningIcon = new ImageIcon(Resources.getResource(Resources.getString("runningNodeImage")));
		brokenIcon = new ImageIcon(Resources.getResource(Resources.getString("brokenNodeImage")));
		pendingIcon = new ImageIcon(Resources.getResource(Resources.getString("pendingNodeImage")));
    }
    
    public TestTreeModel(TestTree p, TreeNode root) {
		super(root);
		parent = p;
    }
	
	public static ImageIcon getIcon(int status) {
		switch (status) {
		case ProtocolConstants.PASSED     : 
			return passedIcon;
		case ProtocolConstants.FAILED     : 
			return failedIcon;
		case ProtocolConstants.NOTRUN     : 
			return notrunIcon;
		case ProtocolConstants.UNRESOLVED : 
			return unresolvedIcon;
		case ProtocolConstants.RUNNING : 
			return runningIcon;
		case ProtocolConstants.PENDING : 
			return pendingIcon;
		}
		return brokenIcon;
	}
	
    public boolean isLeaf(Object node) {
		if (((TestTreeNode)node).getUserObject() instanceof TestSpecification) {
			setTestSpecificationIcon((TestSpecification)((TestTreeNode)node).getUserObject());
		}
		return super.isLeaf(node);
    } 
	
	private void setTestSpecificationIcon(TestSpecification test) {
		ImageIcon blank;
		switch (test.getStatus()) {
		case ProtocolConstants.PASSED     : 
			blank = passedIcon;
			break;
		case ProtocolConstants.FAILED     : 
			blank = failedIcon;
			break;
		case ProtocolConstants.NOTRUN     : 
			blank = notrunIcon;
			break;
		case ProtocolConstants.UNRESOLVED : 
			blank = unresolvedIcon;
			break;
		case ProtocolConstants.RUNNING : 
			blank = runningIcon;
			break;
		case ProtocolConstants.PENDING : 
			blank = pendingIcon;
			break;
		default :
			blank = brokenIcon;
			break;
		}
		((DefaultTreeCellRenderer)parent.getTree().getCellRenderer()).setLeafIcon(blank);
	}
}
