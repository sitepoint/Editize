package com.editize;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.Toolkit;
import java.beans.*;

/**
 * A document model that only accepts positive integer strings
 * (or zero) as input. Attempts to insert strings not representative
 * of integers will be met with a system beep.
 */
public class PositiveIntegerDocument extends PlainDocument implements DocumentListener
{
	private int theInt = 0;
	private boolean allowZero;

	public PositiveIntegerDocument()
	{
		this(false);
	}

	public PositiveIntegerDocument(boolean allowZero)
	{
		super();
		this.allowZero = allowZero;
		addDocumentListener(this);
	}

	protected void updateInt()
	{
		try
		{
			int oldInt = theInt;
			theInt = Integer.parseInt(getText(0,getLength()));
			if (theInt != oldInt)
			{
				pcs.firePropertyChange("integer",oldInt,theInt);
			}
		}
		catch (NumberFormatException ex)
		{
			theInt = 0;
		}
		catch (BadLocationException ex)
		{
			// Should never happen
			throw new RuntimeException("Error obtaining PositiveIntegerDocument text.");
		}
	}

	public void insertUpdate(DocumentEvent evt)
	{
		updateInt();
	}

	public void changedUpdate(DocumentEvent evt)
	{
		updateInt();
	}

	public void removeUpdate(DocumentEvent evt)
	{
		updateInt();
	}

	/**
	 * Attempts to insert a string into the document. Produces a system
	 * beep if the input does not represent a positive (or zero) integer.
	 */
	public void insertString(int offset, String str, AttributeSet a)
			throws BadLocationException
	{
		StringBuffer intString;

                // Ignore zero-length and null strings
                if (str == null || str.length() == 0) return;

		intString = new StringBuffer(getText(0,getLength()));
		intString.insert(offset,str);

		try
		{
			int theInt = new Integer(intString.toString()).intValue();
			if (theInt < 0 || !allowZero && theInt == 0)
			{
				throw new NumberFormatException();
			}
			super.insertString(offset, str, a);
		}
		catch (NumberFormatException e)
		{
			Toolkit.getDefaultToolkit().beep();
		}
	}

	public int getInteger()
	{
		return theInt;
	}

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		pcs.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		pcs.removePropertyChangeListener(l);
	}
}
