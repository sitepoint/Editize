package com.editize.editorkit;

import javax.swing.text.StyledEditorKit;
import java.beans.PropertyChangeListener;
import com.editize.editorkit.EditizeEditorKit.NotifyingUndoManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

public class UndoAction
		extends StyledEditorKit.StyledTextAction
		implements PropertyChangeListener
{
	protected static final int UNDO = 1;
	protected static final int REDO = 2;
	private int type;
	private javax.swing.undo.UndoManager undoMan;
	public UndoAction(int type, NotifyingUndoManager undoMan)
	{
		super(type == UNDO ? "undo" : "redo");
		switch (type) {
			case UNDO:
				this.type = type;
				setEnabled(undoMan.canUndo());
				break;
			case REDO:
				this.type = type;
				setEnabled(undoMan.canRedo());
				break;
			default:
				throw new IllegalArgumentException("UndoAction type needs to be UNDO or REDO!");
		}
		this.undoMan = undoMan;
		undoMan.addPropertyChangeListener(this);
	}

	/**
	 * Toggles the bulleted attribute.
	 */
	public void actionPerformed(ActionEvent e)
	{
		switch (type) {
			case UNDO:
				if (undoMan.canUndo()) undoMan.undo();
				break;
			case REDO:
				if (undoMan.canRedo()) undoMan.redo();
				break;
		}
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (20/07/2001 3:43:33 PM)
	 * @return java.lang.Object
	 * @param key java.lang.String
	 */
	public Object getValue(String key) {
	//	if (key.equals(AbstractAction.SHORT_DESCRIPTION))
	//		return type == UNDO ? undoMan.getUndoPresentationName() : undoMan.getRedoPresentationName();
		return super.getValue(key);
	}

	/**
	 * Insert the method's description here.
	 * Creation date: (28/08/2001 11:49:38 PM)
	 * @param e java.beans.PropertyChangeEvent
	 */
	public void propertyChange(PropertyChangeEvent e) {
		switch (type) {
			case UNDO:
				if (e.getPropertyName().equals("canUndo"))
					setEnabled(((Boolean)e.getNewValue()).booleanValue());
				if (e.getPropertyName().equals("undoPresentationName"))
					putValue(SHORT_DESCRIPTION,e.getNewValue());
				break;
			case REDO:
				if (e.getPropertyName().equals("canRedo"))
					setEnabled(((Boolean)e.getNewValue()).booleanValue());
				if (e.getPropertyName().equals("redoPresentationName"))
					putValue(SHORT_DESCRIPTION,e.getNewValue());
				break;
		}
	}
}
