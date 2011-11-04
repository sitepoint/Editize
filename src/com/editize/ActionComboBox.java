package com.editize;

import java.awt.event.*;
import javax.swing.*;
/**
 * Insert the type's description here.
 * Creation date: (07/07/2001 3:00:52 PM)
 * @author:
 */
public class ActionComboBox extends JComboBox implements ActionListener
{
	/**
	 * ActionComboBox constructor comment.
	 */
	public ActionComboBox()
	{
		super();
		addActionListener(this);
	}
/**
 * Insert the method's description here.
 * Creation date: (07/07/2001 3:29:55 PM)
 * @param e java.awt.event.ActionEvent
 */
public void actionPerformed(ActionEvent e) {
	Action a = getSelectedAction();
	if (a != null) a.actionPerformed(e);
}
/**
 * Insert the method's description here.
 * Creation date: (07/07/2001 3:04:18 PM)
 * @param a javax.swing.Action
 */
public void addAction(Action a) {
	addItem(a);
}
/**
 * Insert the method's description here.
 * Creation date: (07/07/2001 3:04:44 PM)
 * @return javax.swing.Action
 */
public Action getSelectedAction() {
	return (Action)getSelectedItem();
}
}
