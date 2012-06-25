package qat.components;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 *
 */

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import qat.common.Common;
import qat.common.Utils;
import qat.common.SwingUtils;
import qat.gui.QAT;

public class StatusWindow extends JFrame {
	
	private JLabel msgLabel;
	private JProgressBar gauge;
	private QAT parent;	    
    
	public StatusWindow(QAT parent, String title, String message, boolean visible) {		
		super(title);
		this.parent = parent;
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(msgLabel = new JLabel(Common.SYNTAX_HIGHLIGHTING,JLabel.CENTER),BorderLayout.NORTH);
		getContentPane().add(gauge=new JProgressBar(),BorderLayout.SOUTH);
		SwingUtils.setLocationRelativeTo(this,parent);
		pack();
		setVisible(visible);
		setMessage(message);
    }
	
	public StatusWindow(QAT parent, String title, String message) {	
		this(parent,title,message,true);
	}
	
	public StatusWindow(QAT parent, String title, String message, int maxGauge) {
		this(parent,title,message);
		gauge.setMaximum(maxGauge);
	}
	
	public void setMessage(String message) {
		msgLabel.setText(message);		
		((JComponent)getContentPane()).paintImmediately(((JComponent)getContentPane()).getVisibleRect());
	}
	
	
	public void setMessage(String message, int gaugeVal) {
		setMessage(message);
		gauge.setValue(gaugeVal);
		((JComponent)getContentPane()).paintImmediately(((JComponent)getContentPane()).getVisibleRect());
	}
	
	public void setMaximum(int max) {
		gauge.setMaximum(max);
	}
}
