package qat.plugins.chorus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class EComboBox extends JPanel implements MouseListener {
    private JButton addButton, delButton;
    private JPanel editPanel;
    private JComboBox comboBox;

    public EComboBox() {
		setLayout(new BorderLayout());
		addButton = new JButton("+");addButton.setToolTipText("Add this item");
		delButton = new JButton("-");delButton.setToolTipText("Remove this item");
		editPanel = new JPanel(new BorderLayout());
		editPanel.add(addButton,BorderLayout.EAST);
		addButton.addMouseListener(this);addButton.setBackground(new Color(0xAAFFAA));
		editPanel.add(delButton,BorderLayout.WEST);
		delButton.addMouseListener(this);delButton.setBackground(new Color(0xFFAAFF));
		add(comboBox = new JComboBox(),"Center");
    }
    
	
	public void addItemListener(ItemListener l) {
		comboBox.addItemListener(l);
	}
	
	public void removeItemListener(ItemListener l) {
		comboBox.removeItemListener(l);
	}
	
	/**
	 * This method takes a space separated string and converts it into a
	 * list of items added to this combo box.
	 */
	public void setList(String list) {
		try {
			StringTokenizer token = new StringTokenizer(list," ");
			while (token.hasMoreTokens())
				addItem(token.nextToken());
		}
		catch (Exception e) {
		}
	}
	
	/**
	 * This method returns all the items in this combo box
	 * as a space seperated string.
	 */
	public String getList() {
		String list = new String();
		for (int i = 0; i < getItemCount(); i++) {
			list += getItemAt(i)+" ";
		}
		return list.trim();
	}
	
    public void addItem(Object item) {
		if (!contains(item))
			comboBox.addItem(item);
    }
	
	public boolean contains(Object item) {
		return getItems().contains(item);
	}
	
    public void setEditable(boolean editable) {
		comboBox.setEditable(editable);
		if (editable) {
			// add the editing buttons
			add(editPanel,BorderLayout.EAST);
		}
		else {
			// remove the editing buttons
			remove(editPanel);
		}
		validateTree();
    }

    public boolean isEditable() {
		return comboBox.isEditable();
    }    
 
    public int getItemCount() {
		return comboBox.getItemCount();
    }
	
	public ArrayList getItems() {
		ArrayList all = new ArrayList(comboBox.getItemCount());
		for (int i = 0; i <comboBox.getItemCount(); i++)
			all.add(getItemAt(i));
		return all;
	}
	
    public Object getItemAt(int pos) {
		return comboBox.getItemAt(pos);
    }

    public Object getSelectedItem() {
		return comboBox.getSelectedItem();
    }

    public int getSelectedIndex() {
		return comboBox.getSelectedIndex();
    }
	
	public void setSelectedIndex(int i) {
		comboBox.setSelectedIndex(i);
    }

    public void removeAllItems() {
		comboBox.removeAllItems();
    }

    public void cloneTo(EComboBox c) {
		if (comboBox.getItemCount()>0)
			comboBox.removeAllItems();
		for (int i = 0; i < c.getItemCount(); i++) {
			comboBox.addItem(c.getItemAt(i));
		}
    }

    public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof JButton) {
			if (((JButton)e.getSource())==addButton) {
				addItem(comboBox.getSelectedItem());
				return;
			}
			if (((JButton)e.getSource())==delButton) {
				comboBox.removeItem(comboBox.getSelectedItem());
				if (comboBox.getItemCount()>0)
					comboBox.setSelectedIndex(0);
				else
					comboBox.setSelectedItem("");
				return;
			}
		}
    }

    public void mousePressed(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
}
