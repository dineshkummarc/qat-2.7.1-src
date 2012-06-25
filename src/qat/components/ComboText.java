package qat.components;

// @(#)ComboText.java 1.7 01/05/31 

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import qat.common.Common;
import qat.common.Utils;

public class ComboText extends JPanel {
    private JLabel label;
    private JTextField field;
	
    public ComboText (String labelStr, String defaultVal) {
	super(new GridLayout(1,2));
        label = new JLabel(labelStr,SwingConstants.CENTER);
	field = new JTextField(defaultVal,15);
	add(label);
	add(field);
    }
	
    public ComboText (String labelStr, int fieldWidth) {
        label = new JLabel(labelStr,SwingConstants.CENTER);
	field = new JTextField(fieldWidth);
	add(label);
	add(field);
    }
	
    public String getText() {
	return field.getText();
    }
	
    public void setText(String str) {
	field.setText(str);
    }
	
    public void setToolTipText(String tip) {
	super.setToolTipText(tip);
	label.setToolTipText(tip);
	field.setToolTipText(tip);
    }
}
