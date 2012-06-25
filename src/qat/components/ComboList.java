package qat.components;
// @(#)ComboText.java 1.5 01/05/25 

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import qat.common.Common;
import qat.common.Utils;

public class ComboList extends JPanel {
	private JLabel label;
	private JComboBox field;
	private static final String seperatorChar = ",";
	/**
	 * Creates a gui component with label and a ComboBox containing the items
	 * separated by commas, specified in defaulVal. The first item in the list will
	 * be selected.
	 */
    public ComboList (String labelStr, String defaultVal, String possibleValues) {
        label = new JLabel(labelStr,SwingConstants.CENTER);
		Vector items = new Vector();
		StringTokenizer tokens = new StringTokenizer(possibleValues,seperatorChar);
		while(tokens.hasMoreTokens()) {
			items.add(tokens.nextToken());
		}
		field = new JComboBox(items);
		field.setSelectedItem(defaultVal);
		add(label);
		add(field);
    }
	
	/**
	 * Recreate a comma seperated list of entries to be able
	 * to store this object as a property.
	 */
	public String getText() {
		String stringVal = "";
		for (int i = 0; i < field.getItemCount(); i++) {
			stringVal += field.getItemAt(i).toString();
			if (i != field.getItemCount()-1)
				stringVal += seperatorChar;
		}
		return stringVal;
	}
	
	public String getValue() {
		return field.getSelectedItem().toString();
	}
	
	public void setToolTipText(String tip) {
		super.setToolTipText(tip);
		label.setToolTipText(tip);
		field.setToolTipText(tip);
	}
}
