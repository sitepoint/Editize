package com.editize.editorkit;

import java.beans.*;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * An ArticleTextAction that is specialized for use in a text editor's toolbar.
 * <p>
 * When linked to a JEditorPane, this Action maintains a boolean property based
 * on the caret location. The intention being that a ToggleButton in a toolbar
 * can monitor that property as a PropertyListener and therefore display the state
 * of the property at the cursor.
 *
 * @author Kevin Yank
 */
public abstract class BooleanStateAwareTextAction extends EditizeTextAction
	implements CaretListener, DocumentListener, PropertyChangeListener
{
	private JEditorPane assignedEditor = null;
	private Document assignedDoc = null;
	private boolean state = false;

	public BooleanStateAwareTextAction(String name)
	{
		super(name);
	}

	public BooleanStateAwareTextAction(String name, JEditorPane editor)
	{
		super(name);
		setAssignedEditor(editor);
	}

	protected final JEditorPane getAssignedEditor(ActionEvent e)
	{
		if (assignedEditor != null) return assignedEditor;
		return super.getEditor(e);
	}

	public JEditorPane getAssignedEditor()
	{
		return assignedEditor;
	}

	public void setAssignedEditor(JEditorPane editor)
	{
		Object oldValue = assignedEditor;
		if (assignedEditor != null)
		{
			assignedEditor.removeCaretListener(this);
			assignedEditor.removePropertyChangeListener(this);
			assignedDoc.removeDocumentListener(this);
		}
		assignedEditor = editor;
		assignedDoc = editor.getDocument();
		editor.addCaretListener(this);
		editor.addPropertyChangeListener(this);
		assignedDoc.addDocumentListener(this);
		// Obtain the initial state
		caretUpdate(
			new CaretEvent(editor)
						{
				Caret caret;
							public int getDot() { return ((JEditorPane)getSource()).getCaret().getDot(); }
				public int getMark() { return ((JEditorPane)getSource()).getCaret().getMark(); }
						}
		);
		firePropertyChange("assignedEditor",oldValue,editor);
	}

	/**
	 * Should be called whenever the state changes at least. Typically the
	 * calling functions will be the concrete implementations of actionPerformed
	 * and caretUpdate.
	 *
	 * @param newState
	 */
	protected void setState(boolean newState)
	{
		boolean oldState = state;
		state = newState;
		if (newState != oldState) firePropertyChange("state",new Boolean(oldState),new Boolean(newState));
	}

	public boolean getState()
	{
		return state;
	}

	/**
	 * Updates the state based on CaretEvents from the assigned editor.
	 *
	 * @param evt
	 */
	public final void caretUpdate(CaretEvent evt)
	{
					setState(getStateFromCaretLocation(evt));
	}

	/**
	 * Insert updates are accompanied by CaretUpdates, so we don't need
	 * to react to them to maintain the state.
	 *
	 * @param evt
	 */
	public void insertUpdate(DocumentEvent evt) {}

	/**
	 * Remove updates are accompanied by CaretUpdates, so we don't need
	 * to react to them to maintain the state.
	 *
	 * @param evt
	 */
	public void removeUpdate(DocumentEvent evt) {}

	/**
	 * Updates the state based on DocumentEvents from the assigned editor.
	 *
	 * @param evt
	 */
	public void changedUpdate(DocumentEvent evt)
	{
		setState(getStateFromCaretLocation(evt));
	}

	public abstract boolean getStateFromCaretLocation(DocumentEvent e);
	public abstract boolean getStateFromCaretLocation(CaretEvent e);

	/**
	 * Ensures that we're always listening to changes from the assigned
	 * editor's document.
	 * @param evt
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("document") &&
			evt.getSource() == getAssignedEditor())
		{
			assignedDoc.removeDocumentListener(this);
			assignedDoc = getAssignedEditor().getDocument();
			assignedDoc.addDocumentListener(this);
		}
	}
}
