package com.editize;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
/**
 * Focus manager that allows Ctrl-I key events through. Overrides
 * the DefaultFocusManager's behavior, which is to treat Ctrl-I as
 * a TAB event.
 * Creation date: (07/07/2001 4:01:11 PM)
 * @author: Kevin Yank
 */
public class CtrlIFocusManager extends DefaultFocusManager
{
	public void processKeyEvent(Component focusedComp, KeyEvent evt)
	{
		// If the key pressed is an "I"...
		if (evt.getKeyCode() == KeyEvent.VK_I)
			{
			// ...and CTRL is pressed
			if ((evt.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
				{
				return; // Do nothing special
			}
		}

		// Otherwise, handle with the superclass
		super.processKeyEvent(focusedComp, evt);
	}
}
